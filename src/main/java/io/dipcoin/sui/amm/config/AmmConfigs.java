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

package io.dipcoin.sui.amm.config;

import io.dipcoin.sui.amm.model.AmmConfig;

/**
 * @author : Same
 * @datetime : 2025/10/5 10:50
 * @Description : amm network configuration
 */
public final class AmmConfigs {

    private AmmConfigs() {}

    public static final AmmConfig MAINNET_CONFIG = new AmmConfig(
            "https://fullnode.mainnet.sui.io:443",  // RPC node
            "0xdae28ab9ab072c647c4e8f2057a8f17dcc4847e42d6a8258df4b376ae183c872", // packageId
            "0x935229a3c32399e9fb207ec8461a54f56c6af5744c64442435ac217ab28f0d59", // globalId
            "0x55c65b7b67b0ccdf28e13b3b6d204e859dd19556603e3b94137a19306a7254d8" // registeredPoolsId
    );

    public static final AmmConfig TESTNET_CONFIG = new AmmConfig(
            "https://fullnode.testnet.sui.io:443",  // RPC node
            "0x3f52d00499d65dd41602c1cd190cf6771b401ae328d46a172473a7f47be6f83f", // packageId
            "0xe3d52d484e158f164a8650cfd5c8406b545f7e724f70ad40f3747dd6dc39b3c5", // globalId
            "0x52523bbaac35485a1e79c9b46f6b8f53e98ebc17b317695622cb37dbbab46b67" // registeredPoolsId
    );

}
