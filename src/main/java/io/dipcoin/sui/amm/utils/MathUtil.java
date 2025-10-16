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

import io.dipcoin.sui.amm.constant.SwapConstant;
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
    private final static BigInteger THOUSAND = new BigInteger("1000");

    /**
     * Calculate output amount for a swap given input amount and reserves
     * @param feeRate Fee rate to apply to swap
     * @param amountIn Input token amount
     * @param reserveIn Reserve of input token
     * @param reserveOut Reserve of output token
     * @returns Expected output amount after fees
     */
    public static BigInteger getAmountOut(BigInteger feeRate, BigInteger amountIn, BigInteger reserveIn, BigInteger reserveOut) {
        // Special case: if fee_rate = 0, amount_out = y*amount_in/(x + amount_in)
        validateFeeRate(feeRate);
        if (amountIn.compareTo(BigInteger.ZERO) <= 0) {
            throw new AmmException("Zero amount");
        }
        if (reserveIn.compareTo(BigInteger.ZERO) <= 0 || reserveOut.compareTo(BigInteger.ZERO) <= 0) {
            throw new AmmException("Reserves empty");
        }

        // Calculate output required considering fees
        BigInteger feeMultiplier = FEE_SCALE.subtract(feeRate);
        BigInteger coinInValAfterFees = amountIn.multiply(feeMultiplier);
        BigInteger newReserveIn = reserveIn.multiply(FEE_SCALE).add(coinInValAfterFees);

        // Check if the denominator is zero
        validateZero(newReserveIn);

        BigInteger numerator = coinInValAfterFees.multiply(reserveOut);
        BigInteger amountOut = numerator.divide(newReserveIn);

        if (amountOut.compareTo(U64_MAX) >= 0) {
            throw new AmmException("U64 overflow");
        }
        return amountOut;
    }

    /**
     * Calculate required input amount for desired output amount
     * @param feeRate Fee rate to apply to swap
     * @param amountOut Desired output token amount
     * @param reserveIn Reserve of input token
     * @param reserveOut Reserve of output token
     * @returns Required input amount including fees
     */
    public static BigInteger getAmountIn(BigInteger feeRate, BigInteger amountOut, BigInteger reserveIn, BigInteger reserveOut) {
        validateFeeRate(feeRate);
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
        validateZero(denominator);

        BigInteger amountIn = numerator.divide(denominator).add(BigInteger.ONE);
        if (amountIn.compareTo(U64_MAX) >= 0) {
            throw new AmmException("U64 overflow");
        }
        return amountIn;
    }

    /**
     *
     * @param coinXDesired
     * @param coinYDesired
     * @param coinXReserve
     * @param coinYReserve
     * @return
     */
    public static BigInteger[] calcOptimalCoinValues(BigInteger coinXDesired, BigInteger coinYDesired, BigInteger coinXReserve, BigInteger coinYReserve) {
        if (coinXReserve.compareTo(BigInteger.ZERO) == 0 && coinYReserve.compareTo(BigInteger.ZERO) == 0) {
            return new BigInteger[]{ coinXDesired, coinYDesired };
        }

        BigInteger coinYReturned = mulDiv(coinXDesired, coinYDesired, coinXReserve);
        if (coinYReturned.compareTo(coinYDesired) <= 0) {
            return new BigInteger[]{ coinXDesired, coinYReturned };
        } else {
            BigInteger coinXReturned = mulDiv(coinYDesired, coinXDesired, coinYReserve);
            if  (coinXReturned.compareTo(coinXDesired) >= 0) {
                throw new AmmException("Over limit");
            }
            return new BigInteger[]{ coinXReturned, coinYDesired };
        }

    }
    
    /**
     * Calculate LP token amount to mint for provided liquidity
     * @param optimalCoinX Optimal amount of token X being added
     * @param optimalCoinY Optimal amount of token Y being added
     * @param coinXReserve Current reserve of token X in pool
     * @param coinYReserve Current reserve of token Y in pool
     * @param lpSupply Current total supply of LP tokens
     * @returns Amount of LP tokens to mint
     */
    public static BigInteger getExpectedLiquidityAmount(BigInteger optimalCoinX,
                                                        BigInteger optimalCoinY,
                                                        BigInteger coinXReserve,
                                                        BigInteger coinYReserve,
                                                        BigInteger lpSupply) {
        // First time adding liquidity - use geometric mean
        if (coinXReserve.compareTo(BigInteger.ZERO) == 0 && coinYReserve.compareTo(BigInteger.ZERO) == 0 && lpSupply.compareTo(BigInteger.ZERO) == 0) {
        BigInteger liquidityAmount = optimalCoinX
                    .multiply(optimalCoinY)
                    .sqrt()
                    .subtract(THOUSAND);

            validateOverflow(liquidityAmount);
            return liquidityAmount;
        }

        // Calculate proportional LP tokens based on contribution ratio
        BigInteger xLiq = lpSupply
                .multiply(optimalCoinX)
                .divide(coinXReserve);
        BigInteger yLiq = lpSupply
                .multiply(optimalCoinY)
                .divide(coinYReserve);

        // Return the smaller value to maintain ratio
        if (xLiq.compareTo(yLiq) < 0) {
            validateOverflow(xLiq);
            return xLiq;
        } else {
            validateOverflow(yLiq);
            return yLiq;
        }
    }

    /**
     * Performs multiplication then division: (x * y) / z
     * @param x First number to multiply
     * @param y Second number to multiply
     * @param z Number to divide by
     * @returns Result of (x * y) / z
     * @throws AmmException division by zero or U64 overflow
     */
    public static BigInteger mulDiv(BigInteger x, BigInteger y, BigInteger z) {
        validateZero(z);
        BigInteger result = x.multiply(y).divide(z);
        validateOverflow(result);
        return result;
    }

    /**
     * get the calculated amount after applying slippage
     * @param amount
     * @param slippage
     * @returns Result of( amount * (10000 - slippage) / 10000)
     * @throws AmmException division by zero or U64 overflow
     */
    public static BigInteger getSlippageAmount(BigInteger amount, BigInteger slippage) {
        return amount.multiply(SwapConstant.SLIPPAGE_SCALE.subtract(slippage)).divide(SwapConstant.SLIPPAGE_SCALE);
    }

    /**
     * validate fee rate
     * @param amount
     */
    public static void validateFeeRate(BigInteger amount) {
        if (amount.compareTo(MAX_FEE_RATE) >= 0 || amount.compareTo(BigInteger.ZERO) < 0) {
            throw new AmmException("Invalid fee rate");
        }
    }

    /**
     * validate overflow amount
     * @param amount
     */
    public static void validateOverflow(BigInteger amount) {
        if (amount.compareTo(U64_MAX) > 0) {
            throw new AmmException("U64 overflow");
        }
    }

    /**
     * validate zero amount
     * @param amount
     */
    public static void validateZero(BigInteger amount) {
        if (amount == null || amount.compareTo(BigInteger.ZERO) == 0) {
            throw new AmmException("Division by zero");
        }
    }

    /**
     * validate amount
     * @param amount
     */
    public static void validateAmount(BigInteger amount) {
        if (amount == null || amount.compareTo(BigInteger.ZERO) <= 0) {
            throw new AmmException("Amount must be greater than 0");
        }
    }

    /**
     * validate slippage
     * @param slippage
     */
    public static void validateSlippage(BigInteger slippage) {
        if (slippage == null || slippage.compareTo(SwapConstant.SLIPPAGE_SCALE) >= 0) {
            throw new AmmException("Slippage must be less than 100%");
        }
    }
}
