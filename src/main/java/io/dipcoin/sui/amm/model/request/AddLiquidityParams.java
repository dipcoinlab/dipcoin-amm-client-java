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
 * @datetime : 2025/10/5 11:20
 * @Description : Parameters for adding liquidity to a pool
 */
@Data
public class AddLiquidityParams {

    /** Pool ID to add liquidity to (e.g. 0x1234...abcdef) */
    private String poolId;

    /** Token X type in package::module::struct format (e.g. 0xdba...::usdc::USDC) */
    private String typeX;

    /** Token Y type in package::module::struct format (e.g. 0xdba...::wsol::WSOL) */
    private String typeY;

    /** Amount of token X to add */
    private BigInteger amountX;

    /** Amount of token Y to add */
    private BigInteger amountY;

    /** Slippage tolerance, defaults to 0.05 (5%) if not specified */
    private BigInteger slippage = SwapConstant.DEFAULT_SLIPPAGE;

}
