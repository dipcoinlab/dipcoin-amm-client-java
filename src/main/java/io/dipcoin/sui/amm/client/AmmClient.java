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
import io.dipcoin.sui.amm.model.AmmConfig;
import io.dipcoin.sui.amm.model.request.AddLiquidityParams;
import io.dipcoin.sui.amm.model.request.SwapParams;
import io.dipcoin.sui.amm.model.response.Global;
import io.dipcoin.sui.amm.model.response.Pool;
import io.dipcoin.sui.amm.utils.MathUtil;
import io.dipcoin.sui.amm.utils.PackageUtil;
import io.dipcoin.sui.bcs.PureBcs;
import io.dipcoin.sui.bcs.TypeTagSerializer;
import io.dipcoin.sui.bcs.types.arg.call.CallArgObjectArg;
import io.dipcoin.sui.bcs.types.arg.call.CallArgPure;
import io.dipcoin.sui.bcs.types.arg.object.ObjectArgImmOrOwnedObject;
import io.dipcoin.sui.bcs.types.gas.SuiObjectRef;
import io.dipcoin.sui.bcs.types.tag.TypeTag;
import io.dipcoin.sui.bcs.types.transaction.Argument;
import io.dipcoin.sui.bcs.types.transaction.Command;
import io.dipcoin.sui.bcs.types.transaction.ProgrammableMoveCall;
import io.dipcoin.sui.bcs.types.transaction.ProgrammableTransaction;
import io.dipcoin.sui.client.CommandBuilder;
import io.dipcoin.sui.client.QueryBuilder;
import io.dipcoin.sui.client.TransactionBuilder;
import io.dipcoin.sui.crypto.SuiKeyPair;
import io.dipcoin.sui.model.Request;
import io.dipcoin.sui.model.coin.Coin;
import io.dipcoin.sui.model.extended.DynamicFieldName;
import io.dipcoin.sui.model.move.kind.MoveValue;
import io.dipcoin.sui.model.move.kind.data.MoveObject;
import io.dipcoin.sui.model.move.kind.struct.MoveStructMap;
import io.dipcoin.sui.model.move.kind.struct.MoveStructObject;
import io.dipcoin.sui.model.object.ObjectData;
import io.dipcoin.sui.model.object.ObjectDataOptions;
import io.dipcoin.sui.model.object.SuiObjectResponse;
import io.dipcoin.sui.model.transaction.SuiTransactionBlockResponse;
import io.dipcoin.sui.protocol.SuiClient;
import io.dipcoin.sui.protocol.http.HttpService;
import io.dipcoin.sui.protocol.http.request.GetDynamicFieldObject;
import io.dipcoin.sui.protocol.http.response.SuiObjectResponseWrapper;
import io.dipcoin.sui.pyth.exception.PythException;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author : Same
 * @datetime : 2025/10/5 11:00
 * @Description : amm client
 */
public class AmmClient {

    private final static String MODULE = "router";

    private final static Map<String, CallArgObjectArg> AMM_SHARED = new ConcurrentHashMap<>();

    private final AmmConfig ammConfig;

    private final SuiClient suiClient;

    public AmmClient(AmmNetwork ammNetwork) {
        this.ammConfig = ammNetwork.getConfig();
        this.suiClient = SuiClient.build(new HttpService(ammConfig.suiRpc()));
    }

    public AmmClient(AmmNetwork ammNetwork, SuiClient suiClient) {
        this.ammConfig = ammNetwork.getConfig();
        this.suiClient = suiClient;
    }

    // ------------------------- write API -------------------------

    public SuiTransactionBlockResponse addLiquidity(String sender, AddLiquidityParams params) {
        // Validate input parameters
        if (params.getAmountX().compareTo(BigInteger.ZERO) <= 0 || params.getAmountY().compareTo(BigInteger.ZERO) <= 0) {
            throw new AmmException("Amount must be greater than 0");
        }
        if (params.getSlippage().compareTo(BigInteger.ONE) >= 0) {
            throw new AmmException("Slippage must be less than 100%");
        }
        // Sort token types lexicographically to ensure consistent ordering
        String[] orderType = PackageUtil.orderType(params.getTypeX(), params.getTypeY());

        return null;
    }

    /**
     * Split a specified amount of coins from the owner's balance
     * @param programmableTx
     * @param type The coin type (format: packageId::module::struct)
     * @param amount The amount to split
     * @returns ProgrammableTransaction
     */
    public int splitCoin(ProgrammableTransaction programmableTx, String owner, String type, BigInteger amount) {
        // Query available coins of specified type
        List<Coin> coinList = QueryBuilder.getCoins(suiClient, owner, type);
        if (coinList == null || coinList.isEmpty()) {
            throw new AmmException("No " + type + " coins available");
        }

        // Select and accumulate coins until target amount is reached
        AtomicReference<BigInteger> balanceOf = new AtomicReference<>(BigInteger.ZERO);
        List<Coin> selected = new ArrayList<>(coinList.size());
        for (Coin coin : coinList) {
            BigInteger balance = coin.getBalance();
            BigInteger tmpAmount = balanceOf.get().multiply(balance);
            balanceOf.set(tmpAmount);
            selected.add(coin);
            if (tmpAmount.compareTo(amount) >= 0) {
                break;
            }
        }

        int size = selected.size();
        BigInteger totalAmount = balanceOf.get();
        if (balanceOf.get().compareTo(totalAmount) < 0) {
            throw new AmmException(type + " balance is not enough, current total balance: " + totalAmount);
        }

        // Merge multiple coins if necessary
        Coin first = coinList.getFirst();
        String objectId = first.getCoinObjectId();
        long version = first.getVersion();
        String digest = first.getDigest();
        if (size > 1) {
            List<Argument> sources = new ArrayList<>(size - 1);
            coinList.removeFirst();
            for (Coin coin : coinList) {
                String dataObjectId = coin.getCoinObjectId();
                sources.add(Argument.ofInput(programmableTx.addInput(new CallArgObjectArg(new ObjectArgImmOrOwnedObject(new SuiObjectRef(
                        dataObjectId, coin.getVersion(), coin.getDigest()))))));
            }
            Command.MergeCoins mergeCoins = new Command.MergeCoins(Argument.ofInput(programmableTx.addInput(new CallArgObjectArg(new ObjectArgImmOrOwnedObject(new SuiObjectRef(
                    objectId, version, digest))))), sources);
            programmableTx.addCommand(mergeCoins);
        }
        programmableTx.addCommand(
                CommandBuilder.splitCoins(
                        Argument.ofInput(programmableTx.addInput(new CallArgObjectArg(new ObjectArgImmOrOwnedObject(new SuiObjectRef(
                                objectId, version, digest))))),
                        List.of(Argument.ofInput(programmableTx.addInput(
                                new CallArgPure(amount.longValue(), PureBcs.BasePureType.U64))))));
        return programmableTx.getCommandsSize() - 1;
    }

    /**
     * Split a specified amount of coins from the owner's balance
     * @param programmableTx
     * @param amount The amount to split
     * @returns ProgrammableTransaction
     */
    public int splitSui(ProgrammableTransaction programmableTx, BigInteger amount) {
        programmableTx.addCommand(
                CommandBuilder.splitCoins(
                        List.of(Argument.ofInput(programmableTx.addInput(
                                new CallArgPure(amount.longValue(), PureBcs.BasePureType.U64))))));
        return programmableTx.getCommandsSize() - 1;
    }

    /**
     * Swap token X for an exact amount of token Y
     * @param params Swap parameters including amountOut and optional slippage
     * @param suiKeyPair The keypair for signing the transaction
     * @param gasPrice gas price
     * @param gasBudget gas limit
     * @returns {Promise<TxResponse>} Transaction response containing status and txId
     */
    public SuiTransactionBlockResponse swapXToExactY(SwapParams params, SuiKeyPair suiKeyPair, long gasPrice, BigInteger gasBudget) {
        // Validate input parameters
        if (params.getAmountOut() == null || params.getAmountOut().compareTo(BigInteger.ZERO) <= 0) {
            throw new AmmException("Amount must be greater than 0");
        }
        BigInteger slippage = params.getSlippage();
        if (slippage.compareTo(SwapConstant.SLIPPAGE_SCALE) >= 0) {
            throw new Error("Slippage must be less than 100%");
        }

        // Fetch current pool and global state
        String pooId = params.getPooId();
        Pool pool = this.getPool(suiClient, params.getPooId());
        String address = suiKeyPair.address();

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
        if (typeX.equals(SwapConstant.COIN_TYPE_SUI)) {
            splitIndex = this.splitSui(programmableTx, amountInMax);
        } else {
            splitIndex = this.splitCoin(programmableTx, address, typeX, amountInMax);
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
                        Argument.ofInput(programmableTx.addInput(this.getSharedObject(this.ammConfig.globalId(), false))),
                        Argument.ofInput(programmableTx.addInput(this.getSharedObject(pooId, true))),
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
        try {
            return TransactionBuilder.sendTransaction(suiClient, programmableTx, suiKeyPair, TransactionBuilder.buildGasData(suiClient, address, gasPrice, gasBudget));
        } catch (IOException e) {
            throw new AmmException(e.getMessage());
        }
    }

    // ------------------------- read API -------------------------

    /**
     * Get pool information
     * @param poolId The ID of the pool to query
     * @returns Pool information response
     */
    public Pool getPool(SuiClient suiClient, String poolId) {
        ObjectData objectData = QueryBuilder.getObjectData(suiClient, poolId, ObjectDataOptions.contentAndTypeTrue());
        MoveObject content = (MoveObject) objectData.getContent();
        MoveStructMap fields = (MoveStructMap) content.getFields();
        Map<String, MoveValue> values = fields.getValues();

        Pool pool = new Pool();
        pool.setBalX(new BigInteger(values.get("bal_x").getValue().toString()));
        pool.setBalY(new BigInteger(values.get("bal_y").getValue().toString()));
        pool.setFeeBalX(new BigInteger(values.get("fee_bal_x").getValue().toString()));
        pool.setFeeBalY(new BigInteger(values.get("fee_bal_y").getValue().toString()));
        pool.setFeeRate(new BigInteger(values.get("fee_rate").getValue().toString()));
        Map<String, String> idMap = (Map) values.get("id").getValue();
        pool.setId(idMap.get("id"));
        Map<String, MoveValue> lpSupplyMap = ((MoveStructObject) values.get("lp_supply").getValue()).getFields();
        pool.setLpSupply(new BigInteger(lpSupplyMap.get("value").getValue().toString()));
        pool.setMinAddLiquidityLpAmount(new BigInteger(values.get("min_add_liquidity_lp_amount").getValue().toString()));
        pool.setMinLiquidity(new BigInteger(values.get("min_liquidity").getValue().toString()));
        return pool;
    }

    /**
     * Get global configuration information
     * @returns Global configuration response
     */
    public Global getGlobal(SuiClient suiClient) {
        ObjectData objectData = QueryBuilder.getObjectData(suiClient, ammConfig.globalId(), ObjectDataOptions.contentAndTypeTrue());
        MoveObject content = (MoveObject) objectData.getContent();
        MoveStructMap fields = (MoveStructMap) content.getFields();
        Map<String, MoveValue> values = fields.getValues();
        boolean paused = (boolean) values.get("has_paused").getValue();
        boolean fee = (boolean) values.get("is_open_protocol_fee").getValue();
        Map<String, String> idMap = (Map) values.get("id").getValue();
        String id = idMap.get("id");
        return new Global(id, paused, fee);
    }

    /**
     * Get pool ID for a given token pair
     * @param suiClient
     * @param typeX First token type
     * @param typeY Second token type
     * @returns {Promise<string>} Pool ID if found
     */
    public String getPoolId(SuiClient suiClient, String typeX, String typeY) {
        String lpName = PackageUtil.getLpName(typeX, typeY);
        GetDynamicFieldObject data = new GetDynamicFieldObject();
        data.setParentObjectId(this.ammConfig.registeredPoolsId());
        data.setName(new DynamicFieldName("0x1::string::String",
                lpName));
        Request<?, SuiObjectResponseWrapper> request = suiClient.getDynamicFieldObject(data);
        SuiObjectResponseWrapper response;
        try {
            response = request.send();
        } catch (IOException e) {
            throw new AmmException(e.getMessage());
        }
        SuiObjectResponse result = response.getResult();
        MoveObject content = (MoveObject) result.getData().getContent();
        MoveStructMap fields = (MoveStructMap) content.getFields();
        MoveValue value = fields.getValues().get("value");
        return value.getValue().toString();
    }

    /**
     * cache shared object
     * @param objectId
     * @param mutable
     * @return
     */
    private CallArgObjectArg getSharedObject(String objectId, boolean mutable) {
        if (null == objectId || objectId.isEmpty()) {
            throw new PythException("objectId is null or empty!");
        }
        CallArgObjectArg objectArg = AMM_SHARED.get(objectId);
        if (objectArg != null) {
            return objectArg;
        }

        CallArgObjectArg sharedObject = TransactionBuilder.buildSharedObject(suiClient, objectId, mutable);
        AMM_SHARED.put(objectId, sharedObject);
        return sharedObject;
    }

}
