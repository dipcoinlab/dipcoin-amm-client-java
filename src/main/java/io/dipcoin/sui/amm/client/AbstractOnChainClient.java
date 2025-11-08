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

import io.dipcoin.sui.amm.exception.AmmException;
import io.dipcoin.sui.amm.model.AmmConfig;
import io.dipcoin.sui.amm.model.response.Global;
import io.dipcoin.sui.amm.model.response.Pool;
import io.dipcoin.sui.amm.utils.PackageUtil;
import io.dipcoin.sui.bcs.PureBcs;
import io.dipcoin.sui.bcs.types.arg.call.CallArgObjectArg;
import io.dipcoin.sui.bcs.types.arg.call.CallArgPure;
import io.dipcoin.sui.bcs.types.arg.object.ObjectArgImmOrOwnedObject;
import io.dipcoin.sui.bcs.types.gas.SuiObjectRef;
import io.dipcoin.sui.bcs.types.transaction.Argument;
import io.dipcoin.sui.bcs.types.transaction.Command;
import io.dipcoin.sui.bcs.types.transaction.ProgrammableTransaction;
import io.dipcoin.sui.client.CommandBuilder;
import io.dipcoin.sui.client.QueryBuilder;
import io.dipcoin.sui.client.TransactionBuilder;
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
import io.dipcoin.sui.protocol.SuiClient;
import io.dipcoin.sui.protocol.http.request.GetDynamicFieldObject;
import io.dipcoin.sui.protocol.http.response.SuiObjectResponseWrapper;
import io.dipcoin.sui.pyth.exception.PythException;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author : Same
 * @datetime : 2025/11/6 10:54
 * @Description :
 */
public abstract class AbstractOnChainClient {

    private final static Map<String, CallArgObjectArg> AMM_SHARED = new ConcurrentHashMap<>();

    protected final static String MODULE = "router";

    protected SuiClient suiClient;

    protected AmmConfig ammConfig;

    // ------------------------- split coin -------------------------

    /**
     * Split a specified amount of coins from the owner's balance
     * @param programmableTx
     * @param type The coin type (format: packageId::module::struct)
     * @param amount The amount to split
     * @returns ProgrammableTransaction index
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
     * @returns ProgrammableTransaction index
     */
    public int splitSui(ProgrammableTransaction programmableTx, BigInteger amount) {
        programmableTx.addCommand(
                CommandBuilder.splitCoins(
                        List.of(Argument.ofInput(programmableTx.addInput(
                                new CallArgPure(amount.longValue(), PureBcs.BasePureType.U64))))));
        return programmableTx.getCommandsSize() - 1;
    }

    // ------------------------- read API -------------------------

    /**
     * Get pool information
     * @param poolId The ID of the pool to query
     * @returns Pool information response
     */
    public Pool getPool(String poolId) {
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
    public Global getGlobal() {
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
     * @param typeX First token type
     * @param typeY Second token type
     * @returns String Pool ID if found
     */
    public String getPoolId(String typeX, String typeY) {
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
    protected CallArgObjectArg getSharedObject(String objectId, boolean mutable) {
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
