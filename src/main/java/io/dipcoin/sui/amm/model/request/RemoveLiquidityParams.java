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

package io.dipcoin.sui.amm.model.request;

import lombok.Data;

import java.math.BigInteger;

/**
 * @author : Same
 * @datetime : 2025/10/15 17:22
 * @Description : Parameters for removing liquidity from a pool
 */
@Data
public class RemoveLiquidityParams {

    /** Pool ID to remove liquidity from */
    private String poolId;

    /** Token X type in package::module::struct format */
    private String typeX;

    /** Token Y type in package::module::struct format */
    private String typeY;

    /** Amount of LP tokens (Coin<LP<X,Y>> type) to remove */
    private BigInteger removeLpAmount;

    /** Slippage tolerance, defaults to 0.05 (5%) if not specified */
    private BigInteger slippage;
    
}
