/*
 * Copyright 2025 Dipcoin LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");you may not use this file except in compliance with
 * the License.You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,software distributed under the License is distributed on
 * an "AS IS" BASIS,WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.dipcoin.sui.amm.client;

import io.dipcoin.sui.amm.constant.AmmNetwork;
import io.dipcoin.sui.amm.constant.SwapConstant;
import io.dipcoin.sui.amm.exception.AmmException;
import io.dipcoin.sui.amm.model.request.AddLiquidityParams;
import io.dipcoin.sui.amm.model.request.RemoveLiquidityParams;
import io.dipcoin.sui.amm.model.request.SwapParams;
import io.dipcoin.sui.amm.model.response.Pool;
import io.dipcoin.sui.amm.utils.MathUtil;
import io.dipcoin.sui.amm.utils.PackageUtil;
import io.dipcoin.sui.bcs.PureBcs;
import io.dipcoin.sui.bcs.TypeTagSerializer;
import io.dipcoin.sui.bcs.types.arg.call.CallArgPure;
import io.dipcoin.sui.bcs.types.tag.TypeTag;
import io.dipcoin.sui.bcs.types.transaction.Argument;
import io.dipcoin.sui.bcs.types.transaction.Command;
import io.dipcoin.sui.bcs.types.transaction.ProgrammableMoveCall;
import io.dipcoin.sui.bcs.types.transaction.ProgrammableTransaction;
import io.dipcoin.sui.client.TransactionBuilder;
import io.dipcoin.sui.model.transaction.SuiTransactionBlockResponse;
import io.dipcoin.sui.protocol.SuiClient;
import io.dipcoin.sui.protocol.exceptions.RpcRequestFailedException;
import io.dipcoin.sui.protocol.http.HttpService;
import org.bouncycastle.util.encoders.Base64;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author : Same
 * @datetime : 2025/11/6 10:53
 * @Description : self-implemented wallet signature for offline signed transactions to the on-chain client (implement WalletService)
 */
public class AmmOffSignClient extends AbstractOnChainClient {

    private final AmmWalletService ammWalletService;

    public AmmOffSignClient(AmmNetwork ammNetwork, AmmWalletService ammWalletService) {
        super.ammConfig = ammNetwork.getConfig();
        super.suiClient = SuiClient.build(new HttpService(ammConfig.suiRpc()));
        this.ammWalletService = ammWalletService;
    }

    public AmmOffSignClient(AmmNetwork ammNetwork, SuiClient suiClient, AmmWalletService ammWalletService) {
        super.ammConfig = ammNetwork.getConfig();
        super.suiClient = suiClient;
        this.ammWalletService = ammWalletService;
    }

    // ------------------------- write API -------------------------

    /**
     * Add liquidity to a pool
     * @param params Parameters for adding liquidity
     * @param sender The sender for signing the transaction
     * @param gasPrice gas price
     * @param gasBudget gas limit
     * @returns SuiTransactionBlockResponse
     */
    public SuiTransactionBlockResponse addLiquidity(AddLiquidityParams params, String sender, long gasPrice, BigInteger gasBudget) {
        // Validate input parameters
        MathUtil.validateAmount(params.getAmountX());
        MathUtil.validateAmount(params.getAmountY());
        BigInteger slippage = params.getSlippage();
        MathUtil.validateSlippage(slippage);

        // Fetch current pool and global state
        String poolId = params.getPoolId();
        Pool pool = super.getPool(poolId);

        // Sort token types and determine swap direction
        String[] orderType = PackageUtil.orderType(params.getTypeX(), params.getTypeY());
        String typeX = orderType[0];
        String typeY = orderType[1];
        boolean isSwap = !typeX.equals(params.getTypeX());
        BigInteger amountX = isSwap ? params.getAmountY() : params.getAmountX();
        BigInteger amountY = isSwap ? params.getAmountX() : params.getAmountY();
        BigInteger balX = pool.getBalX();
        BigInteger balY = pool.getBalY();

        BigInteger[] calcOptimalCoinValues = MathUtil.calcOptimalCoinValues(amountX, amountY, balX, balY);
        BigInteger coinXDesired = calcOptimalCoinValues[0];
        BigInteger coinYDesired = calcOptimalCoinValues[1];
        BigInteger expectedLp = MathUtil.getExpectedLiquidityAmount(coinXDesired, coinYDesired, balX, balY, pool.getLpSupply());

        BigInteger minAddLiquidityLpAmount = pool.getMinAddLiquidityLpAmount();
        if (expectedLp.compareTo(minAddLiquidityLpAmount) < 0) {
            throw new AmmException("add liquidity too little, expectedLp: " + expectedLp + " is less than min_add_liquidity_lp_amount: " + minAddLiquidityLpAmount);
        }

        BigInteger coinXMin = MathUtil.getSlippageAmount(coinXDesired, slippage);
        BigInteger coinYMin = MathUtil.getSlippageAmount(coinYDesired, slippage);

        // Build transaction to split coins and add liquidity
        ProgrammableTransaction programmableTx = new ProgrammableTransaction();

        AtomicReference<BigInteger> suiUse = new AtomicReference<>(BigInteger.ZERO);
        int splitIndexX = 0;
        if (typeX.equals(SwapConstant.COIN_TYPE_SUI)) {
            splitIndexX = super.splitSui(programmableTx, amountX);
            suiUse.set(amountX);
        } else {
            splitIndexX = super.splitCoin(programmableTx, sender, typeX, amountX);
        }
        int splitIndexY = 0;
        if (typeY.equals(SwapConstant.COIN_TYPE_SUI)) {
            splitIndexY = super.splitSui(programmableTx, amountY);
            suiUse.set(amountY);
        } else {
            splitIndexY = super.splitCoin(programmableTx, sender, typeY, amountY);
        }

        // Type tags
        List<TypeTag> typeTags = new ArrayList<>(2);
        typeTags.add(TypeTagSerializer.parseFromStr(orderType[0], true));
        typeTags.add(TypeTagSerializer.parseFromStr(orderType[1], true));

        ProgrammableMoveCall moveCall = new ProgrammableMoveCall(
                ammConfig.packageId(),
                MODULE,
                SwapConstant.ADD_LIQUIDITY,
                typeTags,
                Arrays.asList(
                        Argument.ofInput(programmableTx.addInput(super.getSharedObject(this.ammConfig.globalId(), false))),
                        Argument.ofInput(programmableTx.addInput(super.getSharedObject(poolId, true))),
                        new Argument.NestedResult(splitIndexX, 0),
                        Argument.ofInput(programmableTx.addInput(
                                new CallArgPure(coinXMin.longValue(), PureBcs.BasePureType.U64))),
                        new Argument.NestedResult(splitIndexY, 0),
                        Argument.ofInput(programmableTx.addInput(
                                new CallArgPure(coinYMin.longValue(), PureBcs.BasePureType.U64)))
                )
        );

        // Command
        Command depositMoveCallCommand = new Command.MoveCall(moveCall);
        List<Command> commands = new ArrayList<>(List.of(
                depositMoveCallCommand
        ));
        programmableTx.addCommands(commands);

        String txBytes;
        try {
            txBytes = TransactionBuilder.serializeTransactionBytes(programmableTx, sender, TransactionBuilder.buildGasData(suiClient, sender, gasPrice, gasBudget, suiUse.get()));
        } catch (IOException e) {
            throw new AmmException("unsafe moveCall addLiquidity failed!", e);
        }

        String signature = ammWalletService.sign(sender, Base64.decode(txBytes));

        try {
            return TransactionBuilder.sendTransaction(suiClient, txBytes, List.of(signature));
        } catch (IOException e) {
            throw new RpcRequestFailedException("Failed to send addLiquidity transaction", e);
        }
    }


    /**
     * Remove liquidity from a pool
     * @param params Parameters for removing liquidity
     * @param sender The sender for signing the transaction
     * @param gasPrice gas price
     * @param gasBudget gas limit
     * @returns SuiTransactionBlockResponse
     */
    public SuiTransactionBlockResponse removeLiquidity(RemoveLiquidityParams params, String sender, long gasPrice, BigInteger gasBudget) {
        // Validate input parameters
        BigInteger removeLpAmount = params.getRemoveLpAmount();
        MathUtil.validateAmount(removeLpAmount);
        BigInteger slippage = params.getSlippage();
        // Calculate minimum acceptable amounts with slippage protection
        MathUtil.validateSlippage(slippage);

        // Get LP token type based on sorted token types
        String[] lpType = PackageUtil.getLpType(this.ammConfig.packageId(), params.getTypeX(), params.getTypeY());
        String typeX = lpType[0];
        String typeY = lpType[1];

        // Fetch current pool and global state
        String poolId = params.getPoolId();
        Pool pool = super.getPool(poolId);

        BigInteger minRemoveLiquidityLpAmount = pool.getMinAddLiquidityLpAmount().divide(BigInteger.TEN);
        if (removeLpAmount.compareTo(minRemoveLiquidityLpAmount) < 0) {
            throw new AmmException("removeLpAmount: " + removeLpAmount + " is less than min_remove_liquidity_lp_amount: " + minRemoveLiquidityLpAmount);
        }

        // Build transaction to split coins and remove liquidity
        ProgrammableTransaction programmableTx = new ProgrammableTransaction();
        int index = super.splitCoin(programmableTx, sender, lpType[2], removeLpAmount);
        BigInteger balX = pool.getBalX();
        BigInteger balY = pool.getBalY();
        BigInteger lpSupply = pool.getLpSupply();
        BigInteger coinXOut = MathUtil.mulDiv(balX, removeLpAmount, lpSupply);
        BigInteger coinYOut = MathUtil.mulDiv(balY, removeLpAmount, lpSupply);
        BigInteger coinXMin = MathUtil.getSlippageAmount(coinXOut, slippage);
        BigInteger coinYMin = MathUtil.getSlippageAmount(coinYOut, slippage);

        // Type tags
        List<TypeTag> typeTags = new ArrayList<>(2);
        typeTags.add(TypeTagSerializer.parseFromStr(typeX, true));
        typeTags.add(TypeTagSerializer.parseFromStr(typeY, true));

        ProgrammableMoveCall moveCall = new ProgrammableMoveCall(
                ammConfig.packageId(),
                MODULE,
                SwapConstant.REMOVE_LIQUIDITY,
                typeTags,
                Arrays.asList(
                        Argument.ofInput(programmableTx.addInput(super.getSharedObject(this.ammConfig.globalId(), false))),
                        Argument.ofInput(programmableTx.addInput(super.getSharedObject(poolId, true))),
                        new Argument.NestedResult(index, 0),
                        Argument.ofInput(programmableTx.addInput(
                                new CallArgPure(removeLpAmount.longValue(), PureBcs.BasePureType.U64))),
                        Argument.ofInput(programmableTx.addInput(
                                new CallArgPure(coinXMin.longValue(), PureBcs.BasePureType.U64))),
                        Argument.ofInput(programmableTx.addInput(
                                new CallArgPure(coinYMin.longValue(), PureBcs.BasePureType.U64)))
                )
        );

        // Command
        Command depositMoveCallCommand = new Command.MoveCall(moveCall);
        List<Command> commands = new ArrayList<>(List.of(
                depositMoveCallCommand
        ));
        programmableTx.addCommands(commands);

        String txBytes;
        try {
            txBytes = TransactionBuilder.serializeTransactionBytes(programmableTx, sender, TransactionBuilder.buildGasData(suiClient, sender, gasPrice, gasBudget));
        } catch (IOException e) {
            throw new AmmException("unsafe moveCall removeLiquidity failed!", e);
        }

        String signature = ammWalletService.sign(sender, Base64.decode(txBytes));

        try {
            return TransactionBuilder.sendTransaction(suiClient, txBytes, List.of(signature));
        } catch (IOException e) {
            throw new RpcRequestFailedException("Failed to send removeLiquidity transaction", e);
        }
    }

    /**
     * Swap an exact amount of token X for token Y
     * @param params Swap parameters including amountIn and optional slippage
     * @param sender The sender for signing the transaction
     * @param gasPrice gas price
     * @param gasBudget gas limit
     * @returns SuiTransactionBlockResponse
     */
    public SuiTransactionBlockResponse swapExactXToY(SwapParams params, String sender, long gasPrice, BigInteger gasBudget) {
        // Validate input parameters
        BigInteger amountIn = params.getAmountIn();
        MathUtil.validateAmount(amountIn);
        BigInteger slippage = params.getSlippage();
        MathUtil.validateSlippage(slippage);

        // Fetch current pool and global state
        String poolId = params.getPoolId();
        Pool pool = super.getPool(params.getPoolId());

        // Sort token types and determine swap direction
        String typeX = params.getTypeX();
        String[] orderType = PackageUtil.orderType(typeX, params.getTypeY());
        boolean isSwap = !orderType[0].equals(typeX);
        BigInteger balanceX = isSwap ? pool.getBalY() : pool.getBalX();
        BigInteger balanceY = isSwap ? pool.getBalX() : pool.getBalY();
        BigInteger amountOut = MathUtil.getAmountOut(pool.getFeeRate(), amountIn, balanceX, balanceY);

        BigInteger amountOutMin = MathUtil.getSlippageAmount(amountOut, slippage);
        String functionName = isSwap ? SwapConstant.SWAP_EXACT_Y_TO_X : SwapConstant.SWAP_EXACT_X_TO_Y;

        ProgrammableTransaction programmableTx = new ProgrammableTransaction();

        int splitIndex = 0;
        AtomicReference<BigInteger> suiUse = new AtomicReference<>(BigInteger.ZERO);
        if (typeX.equals(SwapConstant.COIN_TYPE_SUI)) {
            splitIndex = super.splitSui(programmableTx, amountIn);
            suiUse.set(amountIn);
        } else {
            splitIndex = super.splitCoin(programmableTx, sender, typeX, amountIn);
        }

        // Type tags
        List<TypeTag> typeTags = new ArrayList<>(2);
        typeTags.add(TypeTagSerializer.parseFromStr(orderType[0], true));
        typeTags.add(TypeTagSerializer.parseFromStr(orderType[1], true));

        ProgrammableMoveCall moveCall = new ProgrammableMoveCall(
                ammConfig.packageId(),
                MODULE,
                functionName,
                typeTags,
                Arrays.asList(
                        Argument.ofInput(programmableTx.addInput(super.getSharedObject(this.ammConfig.globalId(), false))),
                        Argument.ofInput(programmableTx.addInput(super.getSharedObject(poolId, true))),
                        new Argument.NestedResult(splitIndex, 0),
                        Argument.ofInput(programmableTx.addInput(
                                new CallArgPure(amountOutMin.longValue(), PureBcs.BasePureType.U64)))
                )
        );

        // Command
        Command depositMoveCallCommand = new Command.MoveCall(moveCall);
        List<Command> commands = new ArrayList<>(List.of(
                depositMoveCallCommand
        ));
        programmableTx.addCommands(commands);

        String txBytes;
        try {
            txBytes = TransactionBuilder.serializeTransactionBytes(programmableTx, sender, TransactionBuilder.buildGasData(suiClient, sender, gasPrice, gasBudget, suiUse.get()));
        } catch (IOException e) {
            throw new AmmException("unsafe moveCall swapExactXToY failed!", e);
        }

        String signature = ammWalletService.sign(sender, Base64.decode(txBytes));

        try {
            return TransactionBuilder.sendTransaction(suiClient, txBytes, List.of(signature));
        } catch (IOException e) {
            throw new RpcRequestFailedException("Failed to send swapExactXToY transaction", e);
        }
    }

    /**
     * Swap token X for an exact amount of token Y
     * @param params Swap parameters including amountOut and optional slippage
     * @param sender The sender for signing the transaction
     * @param gasPrice gas price
     * @param gasBudget gas limit
     * @returns SuiTransactionBlockResponse
     */
    public SuiTransactionBlockResponse swapXToExactY(SwapParams params, String sender, long gasPrice, BigInteger gasBudget) {
        // Validate input parameters
        MathUtil.validateAmount(params.getAmountOut());
        BigInteger slippage = params.getSlippage();
        MathUtil.validateSlippage(slippage);

        // Fetch current pool and global state
        String poolId = params.getPoolId();
        Pool pool = super.getPool(params.getPoolId());

        // Sort token types and determine swap direction
        String typeX = params.getTypeX();
        String[] orderType = PackageUtil.orderType(typeX, params.getTypeY());
        boolean isSwap = !orderType[0].equals(params.getTypeX());
        BigInteger balanceX = isSwap ? pool.getBalY() : pool.getBalX();
        BigInteger balanceY = isSwap ? pool.getBalX() : pool.getBalY();
        BigInteger amountIn = MathUtil.getAmountIn(pool.getFeeRate(), params.getAmountOut(), balanceX, balanceY);

        BigInteger amountInMax = amountIn.multiply(SwapConstant.SLIPPAGE_SCALE).divide(SwapConstant.SLIPPAGE_SCALE.subtract(slippage));
        String functionName = isSwap ? SwapConstant.SWAP_Y_TO_EXACT_X : SwapConstant.SWAP_X_TO_EXACT_Y;

        ProgrammableTransaction programmableTx = new ProgrammableTransaction();

        int splitIndex = 0;
        AtomicReference<BigInteger> suiUse = new AtomicReference<>(BigInteger.ZERO);
        if (typeX.equals(SwapConstant.COIN_TYPE_SUI)) {
            splitIndex = super.splitSui(programmableTx, amountInMax);
            suiUse.set(amountInMax);
        } else {
            splitIndex = super.splitCoin(programmableTx, sender, typeX, amountInMax);
        }

        // Type tags
        List<TypeTag> typeTags = new ArrayList<>(2);
        typeTags.add(TypeTagSerializer.parseFromStr(orderType[0], true));
        typeTags.add(TypeTagSerializer.parseFromStr(orderType[1], true));

        ProgrammableMoveCall moveCall = new ProgrammableMoveCall(
                ammConfig.packageId(),
                MODULE,
                functionName,
                typeTags,
                Arrays.asList(
                        Argument.ofInput(programmableTx.addInput(super.getSharedObject(this.ammConfig.globalId(), false))),
                        Argument.ofInput(programmableTx.addInput(super.getSharedObject(poolId, true))),
                        new Argument.NestedResult(splitIndex, 0),
                        Argument.ofInput(programmableTx.addInput(
                                new CallArgPure(params.getAmountOut().longValue(), PureBcs.BasePureType.U64)))
                )
        );

        // Command
        Command depositMoveCallCommand = new Command.MoveCall(moveCall);
        List<Command> commands = new ArrayList<>(List.of(
                depositMoveCallCommand
        ));
        programmableTx.addCommands(commands);

        String txBytes;
        try {
            txBytes = TransactionBuilder.serializeTransactionBytes(programmableTx, sender, TransactionBuilder.buildGasData(suiClient, sender, gasPrice, gasBudget, suiUse.get()));
        } catch (IOException e) {
            throw new AmmException("unsafe moveCall swapXToExactY failed!", e);
        }

        String signature = ammWalletService.sign(sender, Base64.decode(txBytes));

        try {
            return TransactionBuilder.sendTransaction(suiClient, txBytes, List.of(signature));
        } catch (IOException e) {
            throw new RpcRequestFailedException("Failed to send swapXToExactY transaction", e);
        }
    }

}
