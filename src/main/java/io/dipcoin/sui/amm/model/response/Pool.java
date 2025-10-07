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

package io.dipcoin.sui.amm.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigInteger;

/**
 * @author : Same
 * @datetime : 2025/10/5 11:58
 * @Description : Core pool data structure
 */
@Data
public class Pool {
    
    /** Pool ID */
    private String id;
    
    /** Token X balance */
    @JsonProperty("bal_x")
    private BigInteger balX;
    
    /** Token Y balance */
    @JsonProperty("bal_y")
    private BigInteger balY;

    /** Accumulated fee balance for token X */
    @JsonProperty("fee_bal_x")
    private BigInteger feeBalX;

    /** Accumulated fee balance for token Y */
    @JsonProperty("fee_bal_y")
    private BigInteger feeBalY;

    /** Total LP token supply */
    @JsonProperty("lp_supply")
    private BigInteger lpSupply;

    /** Pool fee rate */
    @JsonProperty("fee_rate")
    private BigInteger feeRate;

    /** Minimum liquidity required when first adding liquidity,will leave 1000 */
    @JsonProperty("min_liquidity")
    private BigInteger minLiquidity;

    /** Minimum LP amount required for adding liquidity */
    @JsonProperty("min_add_liquidity_lp_amount")
    private BigInteger minAddLiquidityLpAmount;

}
