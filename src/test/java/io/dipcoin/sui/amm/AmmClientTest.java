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
import io.dipcoin.sui.amm.constant.AmmNetwork;
import io.dipcoin.sui.amm.model.request.SwapParams;
import io.dipcoin.sui.amm.wallet.WalletKey;
import io.dipcoin.sui.model.transaction.SuiTransactionBlockResponse;
import io.dipcoin.sui.protocol.SuiClient;
import io.dipcoin.sui.protocol.SuiService;
import io.dipcoin.sui.protocol.http.HttpService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;

/**
 * @author : Same
 * @datetime : 2025/10/6 18:19
 * @Description :
 */
@Slf4j
public class AmmClientTest {

    public static final String TEST_URL = "https://fullnode.testnet.sui.io:443";
//        public static final String TEST_URL = "https://fullnode.mainnet.sui.io:443";
//    public static final String TEST_URL = "https://rpc-testnet.suiscan.xyz/";
//    public static final String TEST_URL = "https://rpc-mainnet.suiscan.xyz/";

    protected SuiService suiService;

    protected SuiClient suiClient;

    protected AmmClient ammClient;

    /**
     * Whether write operations are on-chain.
     */
    protected static final boolean ENABLE_SEND = false;

    @BeforeEach
    protected void setUp() {
        this.suiService = new HttpService(TEST_URL);
        this.suiClient = SuiClient.build(suiService);
        this.ammClient = new AmmClient(AmmNetwork.TESTNET, suiClient);
    }

    // --------------------- write API ---------------------

    @Test
    void testSwapXToExactY() throws IOException {
        SwapParams params = new SwapParams();
        params.setPooId("0xf2cddb6036ffc128430fefab738a34d0ecb147ac28f25c64cfd9039a945e904e");
        params.setTypeX("0x0000000000000000000000000000000000000000000000000000000000000002::sui::SUI");
        params.setTypeY("0x5c68f3d2ebfd711454da300d6abf3c7254dc9333cd138cdc68e158ebffd24483::coins::USDC");
        params.setAmountOut(BigInteger.valueOf(1500000L));
        // slippage tolerance 1%
        params.setSlippage(BigInteger.valueOf(100L));

        SuiTransactionBlockResponse response = ammClient.swapXToExactY(params, WalletKey.suiKeyPair, 1000L, BigInteger.TEN.pow(8));
        log.info("Response: {}", response);
    }

    @Test
    void testSwapXToExactYFlip() throws IOException {
        SwapParams params = new SwapParams();
        params.setPooId("0xf2cddb6036ffc128430fefab738a34d0ecb147ac28f25c64cfd9039a945e904e");
        params.setTypeY("0x0000000000000000000000000000000000000000000000000000000000000002::sui::SUI");
        params.setTypeX("0x5c68f3d2ebfd711454da300d6abf3c7254dc9333cd138cdc68e158ebffd24483::coins::USDC");
        params.setAmountOut(BigInteger.valueOf(1500000L));
        // slippage tolerance 1%
        params.setSlippage(BigInteger.valueOf(100L));

        SuiTransactionBlockResponse response = ammClient.swapXToExactY(params, WalletKey.suiKeyPair, 1000L, BigInteger.TEN.pow(8));
        log.info("Response: {}", response);
    }

}
