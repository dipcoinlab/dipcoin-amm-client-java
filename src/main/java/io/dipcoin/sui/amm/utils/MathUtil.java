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

package io.dipcoin.sui.amm.utils;

import io.dipcoin.sui.amm.exception.AmmException;

import java.math.BigInteger;

/**
 * @author : Same
 * @datetime : 2025/10/6 15:24
 * @Description : Utility class for performing swap-related mathematical calculations
 */
public class MathUtil {

    private final static BigInteger MAX_FEE_RATE = new BigInteger("2000"); // Maximum transaction fee rate 20%
    private final static BigInteger FEE_SCALE = new BigInteger("10000");
    private final static BigInteger U64_MAX = new BigInteger("18446744073709551615");

    /**
     * Calculate required input amount for desired output amount
     * @param feeRate Fee rate to apply to swap
     * @param amountOut Desired output token amount
     * @param reserveIn Reserve of input token
     * @param reserveOut Reserve of output token
     * @returns Required input amount including fees
     */
    public static BigInteger getAmountIn(BigInteger feeRate, BigInteger amountOut, BigInteger reserveIn, BigInteger reserveOut) {
        if (feeRate.compareTo(MAX_FEE_RATE) >= 0 || feeRate.compareTo(BigInteger.ZERO) < 0) {
            throw new AmmException("Invalid fee rate");
        }
        if (amountOut.compareTo(BigInteger.ZERO) <= 0) {
            throw new AmmException("Zero amount");
        }
        if (reserveIn.compareTo(BigInteger.ZERO) <= 0 || reserveOut.compareTo(BigInteger.ZERO) <= 0) {
            throw new AmmException("Reserves empty");
        }

        // Calculate input required considering fees
        BigInteger feeMultiplier = FEE_SCALE.subtract(feeRate);
        BigInteger numerator = reserveIn.multiply(amountOut).multiply(FEE_SCALE);
        BigInteger denominator = reserveOut.subtract(amountOut).multiply(feeMultiplier);
        // Check if the denominator is zero
        if (denominator.equals(BigInteger.ZERO)) {
            throw new AmmException("Division by zero");
        }

        BigInteger amountIn = numerator.divide(denominator).add(BigInteger.ONE);
        if (amountIn.compareTo(U64_MAX) >= 0) {
            throw new AmmException("U64 overflow");
        }
        return amountIn;
    }
}
