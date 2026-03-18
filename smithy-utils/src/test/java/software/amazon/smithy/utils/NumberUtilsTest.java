/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class NumberUtilsTest {

    @Test
    public void parseNumberDelegatesToParseDecimalForDecimalString() {
        Number result = NumberUtils.parseNumber("1.5");
        assertThat(result, instanceOf(Double.class));
        assertThat(result, equalTo(1.5));
    }

    @Test
    public void parseNumberReturnsLongForSmallInteger() {
        Number result = NumberUtils.parseNumber("42");
        assertThat(result, instanceOf(Long.class));
        assertThat(result, equalTo(42L));
    }

    @Test
    public void parseNumberReturnsBigIntegerForOverflowingLong() {
        Number result = NumberUtils.parseNumber("9999999999999999999");
        assertThat(result, instanceOf(BigInteger.class));
        assertThat(result, equalTo(new BigInteger("9999999999999999999")));
    }

    @Test
    public void parseNumberReturnsDoubleForNegativeInfinity() {
        Number result = NumberUtils.parseNumber("-Infinity");
        assertThat(result, instanceOf(Double.class));
        assertThat(result, equalTo(Double.NEGATIVE_INFINITY));
    }

    @Test
    public void parseNumberReturnsDoubleForNaN() {
        Number result = NumberUtils.parseNumber("NaN");
        assertThat(result, instanceOf(Double.class));
        assertThat(result.doubleValue(), equalTo(Double.NaN));
    }

    @Test
    public void parseDecimalNumberReturnsDoubleForCompatibleValue() {
        Number result = NumberUtils.parseDecimalNumber("1.5");
        assertThat(result, instanceOf(Double.class));
        assertThat(result, equalTo(1.5));
    }

    @Test
    public void parseDecimalNumberReturnsBigDecimalForIncompatibleValue() {
        Number result = NumberUtils.parseDecimalNumber("9007199254740993.0");
        assertThat(result, instanceOf(BigDecimal.class));
    }

    @Test
    public void parseDecimalNumberReturnsBigDecimalForOverflow() {
        Number result = NumberUtils.parseDecimalNumber("1e309");
        assertThat(result, instanceOf(BigDecimal.class));
        assertThat(result, equalTo(new BigDecimal("1e309")));
    }

    @Test
    public void parseDecimalNumberReturnsDoubleForInfinityLiteral() {
        Number result = NumberUtils.parseDecimalNumber("Infinity");
        assertThat(result, instanceOf(Double.class));
        assertThat(result, equalTo(Double.POSITIVE_INFINITY));
    }

    @Test
    public void parseDecimalNumberReturnsDoubleForNaNLiteral() {
        Number result = NumberUtils.parseDecimalNumber("NaN");
        assertThat(result, instanceOf(Double.class));
        assertThat(result.doubleValue(), equalTo(Double.NaN));
    }

    @ParameterizedTest
    @MethodSource("compatibleValues")
    public void detectsDoublePrecisionCompatibleValues(BigDecimal bd, Double d) {
        assertThat(NumberUtils.isDoublePrecisionCompatible(bd, d), is(true));
    }

    static Stream<Arguments> compatibleValues() {
        return Stream.of(
                // Zero
                Arguments.of(new BigDecimal("0"), 0.0),
                // Negative zero is mathematically equal to zero
                Arguments.of(new BigDecimal("0"), -0.0),
                // Small positive integer
                Arguments.of(new BigDecimal("1"), 1.0),
                // Trailing zeros in BigDecimal scale don't affect compareTo
                Arguments.of(new BigDecimal("1.00"), 1.0),
                // Negative integer
                Arguments.of(new BigDecimal("-42"), -42.0),
                // Simple decimal that doubles represent exactly
                Arguments.of(new BigDecimal("0.5"), 0.5),
                // Power-of-two fraction
                Arguments.of(new BigDecimal("0.25"), 0.25),
                // Another power-of-two fraction
                Arguments.of(new BigDecimal("0.125"), 0.125),
                // Larger exact integer within double range
                Arguments.of(new BigDecimal("1048576"), 1048576.0),
                // Max safe integer for doubles (2^53)
                Arguments.of(new BigDecimal("9007199254740992"), 9007199254740992.0),
                // Negative max safe integer
                Arguments.of(new BigDecimal("-9007199254740992"), -9007199254740992.0),
                // Small negative decimal
                Arguments.of(new BigDecimal("-0.5"), -0.5),
                // Double.MAX_VALUE round-trips through Double.toString
                Arguments.of(new BigDecimal(String.valueOf(Double.MAX_VALUE)), Double.MAX_VALUE),
                // Double.MIN_VALUE round-trips through Double.toString
                Arguments.of(new BigDecimal(String.valueOf(Double.MIN_VALUE)), Double.MIN_VALUE),
                // 0.1 round-trips: BigDecimal.valueOf(0.1) equals BigDecimal("0.1")
                Arguments.of(new BigDecimal("0.1"), 0.1),
                // 0.2 round-trips cleanly
                Arguments.of(new BigDecimal("0.2"), 0.2),
                // 0.3 round-trips cleanly
                Arguments.of(new BigDecimal("0.3"), 0.3),
                // Negative decimal round-trips cleanly
                Arguments.of(new BigDecimal("-0.1"), -0.1),
                // Infinities are only storable as doubles
                Arguments.of(BigDecimal.ZERO, Double.POSITIVE_INFINITY),
                // NaNs are only storable as doubles
                Arguments.of(BigDecimal.ZERO, Double.NaN));
    }

    @ParameterizedTest
    @MethodSource("incompatibleValues")
    public void detectsDoublePrecisionIncompatibleValues(BigDecimal bd, Double d) {
        assertThat(NumberUtils.isDoublePrecisionCompatible(bd, d), is(false));
    }

    static Stream<Arguments> incompatibleValues() {
        return Stream.of(
                // Integer beyond 2^53Loses precision
                Arguments.of(new BigDecimal("9007199254740993"), 9007199254740993.0),
                // Large integer that rounds in double
                Arguments.of(new BigDecimal("9007199254740995"), 9007199254740995.0),
                // High-precision decimal beyond what double can distinguish
                Arguments.of(new BigDecimal("1.0000000000000001"), 1.0000000000000001),
                // Precision loss in negative large integer
                Arguments.of(new BigDecimal("-9007199254740993"), -9007199254740993.0),
                // Decimal with many significant digits
                Arguments.of(new BigDecimal("123456789.123456789"), 123456789.123456789),
                // Small value with precision beyond double's capability
                Arguments.of(new BigDecimal("0.10000000000000001"), 0.10000000000000001),
                // Precision beyond what Double.toString preserves
                Arguments.of(new BigDecimal("3.141592653589793238"), 3.141592653589793238),
                // Very small difference from an integer, not representable at stated precision
                Arguments.of(new BigDecimal("2.00000000000000005"), 2.00000000000000005),
                // Large number with fractional precision loss
                Arguments.of(new BigDecimal("1e308").add(new BigDecimal("1")), 1e308),
                // Negative high-precision decimal beyond double's digits
                Arguments.of(new BigDecimal("-0.123456789012345678"), -0.123456789012345678));
    }
}
