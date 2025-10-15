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
import io.dipcoin.sui.bcs.BcsSerializer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author : Same
 * @datetime : 2025/10/5 16:56
 * @Description : package util
 */
public class PackageUtil {

    private static final int LESS_THAN = -1;
    private static final int EQUAL = 0;
    private static final int GREATER_THAN = 1;
    private static final String ADDR_PREFIX = "0x";

    /**
     * Orders two coin types based on their lexicographical comparison
     * @param typeX First coin type
     * @param typeY Second coin type
     * @returns Tuple of ordered coin types [smaller, larger]
     */
    public static String[] orderType(String typeX, String typeY) {
        return isSortedTypes(typeX, typeY)
                ? new String[]{typeX, typeY}
                : new String[]{typeY, typeX};
    }

    /**
     * Generates LP token name based on coin types
     * This method generates a name for LP tokens by ordering the coin types lexicographically
     * and removing any '0x' prefixes to ensure consistent naming.
     *
     * @param typeX First coin type
     * @param typeY Second coin type
     * @returns LP token name in format `LP-${coinType1}-${coinType2}`
     *
     * @example
     * getLpName(
     *   "0x456::coin::USDC",
     *   "0x789::coin::WSOL"
     * )
     * // Returns: "LP-456::coin::USDC-789::coin::WSOL"
     */
    public static String getLpName(String typeX, String typeY) {
        // Sort coin types for consistent ordering
        String[] orderType = orderType(typeX, typeY);
        // Remove 0x prefix if present
        String sortedTypeX = orderType[0];
        String sortedTypeY = orderType[1];
        String normalizedTypeX = sortedTypeX.startsWith(ADDR_PREFIX)
                ? sortedTypeX.substring(2)
                : sortedTypeX;
        String normalizedTypeY = sortedTypeY.startsWith(ADDR_PREFIX)
                ? sortedTypeY.substring(2)
                : sortedTypeY;

        // Construct LP token name
        return "LP-" + normalizedTypeX + "-" + normalizedTypeY;
    }

    /**
     * Generates LP token type string based on coin types
     * This method generates a complete type identifier for LP tokens. It orders the coin types
     * based on their BCS serialized byte array comparison to ensure consistent LP type identifiers.
     *
     * @param packageId Contract ID
     * @param typeX First coin type
     * @param typeY Second coin type
     * @returns Tuple containing [sortedTypeX, sortedTypeY, lpType] where lpType follows format:
     *          `${packageId}::manage::LP<${coinType1}, ${coinType2}>`
     *
     * @example
     * getLPType(
     *   "0x123",
     *   "0x456::coin::USDC",
     *   "0x789::coin::BTC"
     * )
     * Returns: ["0x456::coin::USDC", "0x789::coin::BTC", "0x123::manage::LP<0x456::coin::USDC, 0x789::coin::BTC>"]
     */
    public static String[] getLpType(String packageId, String typeX, String typeY) {
        // Sort coin types to ensure consistent ordering
        String[] orderType = orderType(typeX, typeY);
        String coinType1 = orderType[0];
        String coinType2 = orderType[1];
        String lpType = packageId + "::manage::LP<" + coinType1 + ", " + coinType2 + ">";
        return new String[]{coinType1, coinType2, lpType};
    }

    /**
     * Check if two token types are in sorted order using BCS serialization
     * @param typeX First token type
     * @param typeY Second token type
     * @throws if token types are the same
     * @returns true if typeX < typeY, false otherwise
     */
    public static boolean isSortedTypes(String typeX, String typeY) {
        if (typeX.equals(typeY)) {
            throw new AmmException("Type X and Type Y cannot be the same");
        }

        // BCS serialize both type names
        byte[] serializedX = serializeTypeName(typeX);
        byte[] serializedY = serializeTypeName(typeY);

        // Compare serialized bytes
        return compareByteArrays(serializedX, serializedY) == LESS_THAN;
    }

    /**
     * BCS serialize Move type name for comparison
     * @param typeName Full type name
     * @returns Serialized bytes
     */
    private static byte[] serializeTypeName(String typeName) {
        // Convert string to byte array
        byte[] rawBytes = typeName.getBytes(StandardCharsets.UTF_8);

        // Serialize byte array
        BcsSerializer ser = new BcsSerializer();
        try {
            ser.writeVector(rawBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ser.toByteArray();
    }

    /**
     * Compare two serialized byte arrays
     * @param bytesX First byte array
     * @param bytesY Second byte array
     * @returns Comparison result (EQUAL, LESS_THAN or GREATER_THAN)
     */
    private static int compareByteArrays(byte[] bytesX, byte[] bytesY) {
        int minLen = Math.min(bytesX.length, bytesY.length);

        // Compare byte by byte
        for (int i = 0; i < minLen; i++) {
            int a = bytesX[i] & 0xFF;
            int b = bytesY[i] & 0xFF;
            if (a < b) return LESS_THAN;
            if (a > b) return GREATER_THAN;
        }

        // If all bytes up to minLen are equal, shorter array comes first
        if (bytesX.length < bytesY.length) return LESS_THAN;
        if (bytesX.length > bytesY.length) return GREATER_THAN;
        return EQUAL;
    }

}
