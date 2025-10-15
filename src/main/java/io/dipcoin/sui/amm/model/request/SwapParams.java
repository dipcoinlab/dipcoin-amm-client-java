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

import io.dipcoin.sui.amm.constant.SwapConstant;
import lombok.Data;

import java.math.BigInteger;

/**
 * @author : Same
 * @datetime : 2025/10/6 15:03
 * @Description : Parameters for swap operations
 */
@Data
public class SwapParams {

    /** Pool ID to perform swap in */
    private String poolId;

    /** Token X type in package::module::struct format */
    private String typeX;

    /** Token Y type in package::module::struct format */
    private String typeY;

    /** Input token amount for exact input swaps */
    private BigInteger amountIn;

    /** Output token amount for exact output swaps */
    private BigInteger amountOut;

    /** Slippage tolerance, defaults to 0.05 (5%) if not specified */
    private BigInteger slippage = SwapConstant.DEFAULT_SLIPPAGE;

}
