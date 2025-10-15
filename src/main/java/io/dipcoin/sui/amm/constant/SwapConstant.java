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

package io.dipcoin.sui.amm.constant;

import java.math.BigInteger;

/**
 * @author : Same
 * @datetime : 2025/10/5 11:12
 * @Description : amm constant
 */
public interface SwapConstant {

    BigInteger SLIPPAGE_SCALE = new BigInteger("10000");

    BigInteger DEFAULT_SLIPPAGE = new BigInteger("500");

    String COIN_TYPE_SUI = "0x0000000000000000000000000000000000000000000000000000000000000002::sui::SUI";

    String SWAP_EXACT_X_TO_Y = "swap_exact_x_to_y";

    String SWAP_EXACT_Y_TO_X = "swap_exact_y_to_x";

    String SWAP_X_TO_EXACT_Y = "swap_x_to_exact_y";

    String SWAP_Y_TO_EXACT_X = "swap_y_to_exact_x";

}
