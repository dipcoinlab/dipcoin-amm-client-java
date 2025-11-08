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

package io.dipcoin.sui.amm;

import io.dipcoin.sui.amm.client.AmmClient;
import io.dipcoin.sui.amm.config.IntervalExtension;
import io.dipcoin.sui.amm.constant.AmmNetwork;
import io.dipcoin.sui.amm.model.request.AddLiquidityParams;
import io.dipcoin.sui.amm.model.request.RemoveLiquidityParams;
import io.dipcoin.sui.amm.model.request.SwapParams;
import io.dipcoin.sui.amm.wallet.WalletKey;
import io.dipcoin.sui.crypto.Ed25519KeyPair;
import io.dipcoin.sui.model.transaction.SuiTransactionBlockResponse;
import io.dipcoin.sui.protocol.SuiClient;
import io.dipcoin.sui.protocol.http.HttpService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.math.BigInteger;

/**
 * @author : Same
 * @datetime : 2025/10/6 18:19
 * @Description :
 */
@Slf4j
@ExtendWith(IntervalExtension.class)
public class AmmClientTest {

    protected SuiClient suiClient;

    protected AmmClient ammClient;

    /**
     * Whether write operations are on-chain.
     */
    protected static final boolean ENABLE_SEND = false;

    @BeforeEach
    protected void setUp() {
        AmmNetwork ammNetwork = AmmNetwork.TESTNET;
        HttpService suiService = new HttpService(ammNetwork.getConfig().suiRpc());
        this.suiClient = SuiClient.build(suiService);
        this.ammClient = new AmmClient(ammNetwork, suiClient);
    }

    // --------------------- write API ---------------------

    @Test
    @Tag("suite")
    void testAddLiquidity() throws IOException {
        AddLiquidityParams params = new AddLiquidityParams();
        params.setPoolId("0xf2cddb6036ffc128430fefab738a34d0ecb147ac28f25c64cfd9039a945e904e");
        params.setTypeX("0x5c68f3d2ebfd711454da300d6abf3c7254dc9333cd138cdc68e158ebffd24483::coins::USDC");
        params.setTypeY("0x0000000000000000000000000000000000000000000000000000000000000002::sui::SUI");
        params.setAmountX(BigInteger.valueOf(1000000L));
        params.setAmountY(BigInteger.valueOf(1500000L));
        // slippage tolerance 1%
        params.setSlippage(BigInteger.valueOf(100L));

        // gas price 1000 (For dynamic queries, please refer to the `getReferenceGasPrice()` method in `SuiClient`)
        // gas limit 0.1 SUI (BigInteger.TEN.pow(8))1000
        SuiTransactionBlockResponse response = ammClient.addLiquidity(params, WalletKey.suiKeyPair, 1000L, BigInteger.TEN.pow(8));
        // https://testnet.suivision.xyz/txblock/8XKvP3zqdEfo5CAaDwcpCkehkzoZ1mc8yGjnZLdJWV2U
        log.info("Response: {}", response);
    }

    @Test
    @Tag("suite")
    void testAddLiquidityFlip() throws IOException {
        AddLiquidityParams params = new AddLiquidityParams();
        params.setPoolId("0xf2cddb6036ffc128430fefab738a34d0ecb147ac28f25c64cfd9039a945e904e");
        params.setTypeY("0x5c68f3d2ebfd711454da300d6abf3c7254dc9333cd138cdc68e158ebffd24483::coins::USDC");
        params.setTypeX("0x0000000000000000000000000000000000000000000000000000000000000002::sui::SUI");
        params.setAmountX(BigInteger.valueOf(1000000L));
        params.setAmountY(BigInteger.valueOf(1500000L));
        // slippage tolerance 1%
        params.setSlippage(BigInteger.valueOf(100L));

        // gas price 1000 (For dynamic queries, please refer to the `getReferenceGasPrice()` method in `SuiClient`)
        // gas limit 0.1 SUI (BigInteger.TEN.pow(8))1000
        SuiTransactionBlockResponse response = ammClient.addLiquidity(params, WalletKey.suiKeyPair, 1000L, BigInteger.TEN.pow(8));
        // https://testnet.suivision.xyz/txblock/4foJEuozTbtezmYgpBo6YEUmNHidRhWJFRqdAdJspj43
        log.info("Response: {}", response);
    }

    @Test
    @Tag("suite")
    void testRemoveLiquidity() throws IOException {
        RemoveLiquidityParams params = new RemoveLiquidityParams();
        params.setPoolId("0xf2cddb6036ffc128430fefab738a34d0ecb147ac28f25c64cfd9039a945e904e");
        params.setTypeX("0x5c68f3d2ebfd711454da300d6abf3c7254dc9333cd138cdc68e158ebffd24483::coins::USDC");
        params.setTypeY("0x0000000000000000000000000000000000000000000000000000000000000002::sui::SUI");
        params.setRemoveLpAmount(BigInteger.valueOf(1000000L));
        // slippage tolerance 1%
        params.setSlippage(BigInteger.valueOf(100L));

        // gas price 1000 (For dynamic queries, please refer to the `getReferenceGasPrice()` method in `SuiClient`)
        // gas limit 0.1 SUI (BigInteger.TEN.pow(8))1000
        SuiTransactionBlockResponse response = ammClient.removeLiquidity(params, WalletKey.suiKeyPair, 1000L, BigInteger.TEN.pow(8));
        // https://testnet.suivision.xyz/txblock/81bDEnXXVJZBGiH1wBKHqYxDmeb7rGqzdjXwDKbxyNVr
        log.info("Response: {}", response);
    }

    @Test
    @Tag("suite")
    void testSwapExactXToY() throws IOException {
        SwapParams params = new SwapParams();
        params.setPoolId("0xf2cddb6036ffc128430fefab738a34d0ecb147ac28f25c64cfd9039a945e904e");
        params.setTypeY("0x0000000000000000000000000000000000000000000000000000000000000002::sui::SUI");
        params.setTypeX("0x5c68f3d2ebfd711454da300d6abf3c7254dc9333cd138cdc68e158ebffd24483::coins::USDC");
        params.setAmountIn(BigInteger.valueOf(1500000L));
        // slippage tolerance 1%
        params.setSlippage(BigInteger.valueOf(100L));

        // gas price 1000 (For dynamic queries, please refer to the `getReferenceGasPrice()` method in `SuiClient`)
        // gas limit 0.1 SUI (BigInteger.TEN.pow(8))1000
        SuiTransactionBlockResponse response = ammClient.swapExactXToY(params, WalletKey.suiKeyPair, 1000L, BigInteger.TEN.pow(8));
        // https://testnet.suivision.xyz/txblock/ssBddYuiuNuTsGT94oy39pPiyYkrNJvuwbrQAXYmhwx
        log.info("Response: {}", response);
    }

    @Test
    @Tag("suite")
    void testSwapExactXToYFlip() throws IOException {
        SwapParams params = new SwapParams();
        params.setPoolId("0xf2cddb6036ffc128430fefab738a34d0ecb147ac28f25c64cfd9039a945e904e");
        params.setTypeX("0x0000000000000000000000000000000000000000000000000000000000000002::sui::SUI");
        params.setTypeY("0x5c68f3d2ebfd711454da300d6abf3c7254dc9333cd138cdc68e158ebffd24483::coins::USDC");
        params.setAmountIn(BigInteger.valueOf(1500000L));
        // slippage tolerance 1%
        params.setSlippage(BigInteger.valueOf(100L));

        // gas price 1000 (For dynamic queries, please refer to the `getReferenceGasPrice()` method in `SuiClient`)
        // gas limit 0.1 SUI (BigInteger.TEN.pow(8))1000
        SuiTransactionBlockResponse response = ammClient.swapExactXToY(params, WalletKey.suiKeyPair, 1000L, BigInteger.TEN.pow(8));
        // https://testnet.suivision.xyz/txblock/5EtqpJgz7QYyiZwTotiPbfuMqpAvqNqgWFFUsmJVAXAV
        log.info("Response: {}", response);
    }

    @Test
    @Tag("suite")
    void testSwapXToExactY() throws IOException {
        SwapParams params = new SwapParams();
        params.setPoolId("0xf2cddb6036ffc128430fefab738a34d0ecb147ac28f25c64cfd9039a945e904e");
        params.setTypeY("0x0000000000000000000000000000000000000000000000000000000000000002::sui::SUI");
        params.setTypeX("0x5c68f3d2ebfd711454da300d6abf3c7254dc9333cd138cdc68e158ebffd24483::coins::USDC");
        params.setAmountOut(BigInteger.valueOf(1500000L));
        // slippage tolerance 1%
        params.setSlippage(BigInteger.valueOf(100L));

        // gas price 1000 (For dynamic queries, please refer to the `getReferenceGasPrice()` method in `SuiClient`)
        // gas limit 0.1 SUI (BigInteger.TEN.pow(8))1000
        SuiTransactionBlockResponse response = ammClient.swapXToExactY(params, WalletKey.suiKeyPair, 1000L, BigInteger.TEN.pow(8));
        // https://testnet.suivision.xyz/txblock/CnAtv9CNe8khoaQrxHdishREc6ZX7sMosA3zsVnwPbBT
        log.info("Response: {}", response);
    }

    @Test
    @Tag("suite")
    void testSwapXToExactYFlip() throws IOException {
        SwapParams params = new SwapParams();
        params.setPoolId("0xf2cddb6036ffc128430fefab738a34d0ecb147ac28f25c64cfd9039a945e904e");
        params.setTypeX("0x0000000000000000000000000000000000000000000000000000000000000002::sui::SUI");
        params.setTypeY("0x5c68f3d2ebfd711454da300d6abf3c7254dc9333cd138cdc68e158ebffd24483::coins::USDC");
        params.setAmountOut(BigInteger.valueOf(1500000L));
        // slippage tolerance 1%
        params.setSlippage(BigInteger.valueOf(100L));

        // gas price 1000 (For dynamic queries, please refer to the `getReferenceGasPrice()` method in `SuiClient`)
        // gas limit 0.1 SUI (BigInteger.TEN.pow(8))1000
        SuiTransactionBlockResponse response = ammClient.swapXToExactY(params, WalletKey.suiKeyPair, 1000L, BigInteger.TEN.pow(8));
        // https://testnet.suivision.xyz/txblock/5txqLSyVXgwwfZrVoQVjMe5ocwzP5GBLebfWmFGZTd9C
        log.info("Response: {}", response);
    }

    public static void main(String[] args) {
        HttpService suiService = new HttpService("https://fullnode.testnet.sui.io:443");
        SuiClient suiClient = SuiClient.build(suiService);
        AmmClient ammClient = new AmmClient(AmmNetwork.TESTNET, suiClient);

        SwapParams params = new SwapParams();
        params.setPoolId("0xf2cddb6036ffc128430fefab738a34d0ecb147ac28f25c64cfd9039a945e904e");
        params.setTypeX("0x0000000000000000000000000000000000000000000000000000000000000002::sui::SUI");
        params.setTypeY("0x5c68f3d2ebfd711454da300d6abf3c7254dc9333cd138cdc68e158ebffd24483::coins::USDC");
        params.setAmountIn(BigInteger.valueOf(1500000L));
        // slippage tolerance 1%
        params.setSlippage(BigInteger.valueOf(100L));

        // gas price 1000 (For dynamic queries, please refer to the `getReferenceGasPrice()` method in `SuiClient`)
        // gas limit 0.1 SUI (BigInteger.TEN.pow(8))1000
        SuiTransactionBlockResponse response = ammClient.swapExactXToY(params, Ed25519KeyPair.decodeHex("xxx"), 1000L, BigInteger.TEN.pow(8));
        // https://testnet.suivision.xyz/txblock/5EtqpJgz7QYyiZwTotiPbfuMqpAvqNqgWFFUsmJVAXAV
        System.out.println("Response: " + response);
    }

}
