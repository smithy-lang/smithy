/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Functions that make working with numbers easier.
 */
public final class NumberUtils {
    private NumberUtils() {}

    /**
     * Parses a numeric string into the most appropriate Number type.
     *
     * <p>Extreme values ("Infinity", "NaN", and their signed variants) are
     * recognized first and returned as {@link Double}.
     *
     * <p>Strings containing ".", "e", or "E" are treated as decimal numbers
     * and parsed via {@link #parseDecimalNumber}. All other strings are
     * parsed as integers, trying {@link Long} first and falling back to
     * {@link BigInteger} for values that overflow long.
     *
     * @param lexeme A numeric string.
     * @return A Long, BigInteger, Double, or BigDecimal representing the value.
     */
    public static Number parseNumber(String lexeme) {
        Number extremeValue = parseExtremeValue(lexeme);
        if (extremeValue != null) {
            return extremeValue;
        }
        if (lexeme.contains("e") || lexeme.contains("E") || lexeme.contains(".")) {
            return parseDecimalNumber(lexeme);
        }
        try {
            return Long.parseLong(lexeme);
        } catch (NumberFormatException e) {
            return new BigInteger(lexeme);
        }
    }

    private static Number parseExtremeValue(String lexeme) {
        // Short circuit if we won't get a match later.
        if (!lexeme.contains("I") && !lexeme.contains("N")) {
            return null;
        }
        switch (lexeme) {
            case "+Infinity":
            case "Infinity":
                return Double.POSITIVE_INFINITY;
            case "-Infinity":
                return Double.NEGATIVE_INFINITY;
            case "+NaN":
            case "NaN":
            case "-NaN":
                return Double.NaN;
        }
        return null;
    }

    /**
     * Parses a decimal (floating-point or scientific notation) string into the
     * most precise Number type that preserves the value.
     *
     * <p>Extreme values ("Infinity", "NaN", and their signed variants) are
     * recognized first and returned as {@link Double}.
     *
     * <p>Returns a {@link Double} when the value can be represented without
     * precision loss. Returns a {@link BigDecimal} when the value exceeds
     * double range (would become infinity) or loses precision in IEEE-754.
     *
     * @param lexeme A numeric string containing ".", "e"/"E", or an extreme
     *     value literal.
     * @return A Double or BigDecimal representing the parsed value.
     */
    public static Number parseDecimalNumber(String lexeme) {
        Number extremeValue = parseExtremeValue(lexeme);
        if (extremeValue != null) {
            return extremeValue;
        }
        Double doubleValue = Double.valueOf(lexeme);
        if (doubleValue.isInfinite()) {
            // Doubles parse values outside their range as infinity.
            // Literal infinity is handled by the `I` test, so parse the real value.
            return new BigDecimal(lexeme);
        }
        BigDecimal bigDecimalValue = new BigDecimal(lexeme);
        if (isDoublePrecisionCompatible(bigDecimalValue, doubleValue)) {
            return doubleValue;
        }
        return bigDecimalValue;
    }

    /**
     * Checks whether the bigDecimalValue is represented in the doubleValue without
     * a loss of precision due to IEEE-754 floating-point representation.
     *
     * <p>Returns {@code true} unconditionally for infinite and NaN doubles,
     * since those values have no BigDecimal equivalent to compare against.
     *
     * @param bigDecimalValue The value as a BigDecimal.
     * @param doubleValue The value as a double.
     * @return true if the bigDecimalValue is precisely represented by the doubleValue,
     *     or if the doubleValue is infinite or NaN.
     */
    public static boolean isDoublePrecisionCompatible(BigDecimal bigDecimalValue, Double doubleValue) {
        if (doubleValue.isInfinite() || doubleValue.isNaN()) {
            return true;
        }
        return bigDecimalValue.compareTo(BigDecimal.valueOf(doubleValue)) == 0;
    }
}
