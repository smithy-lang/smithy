/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.smithy.model.SourceLocation;

/**
 * Represents a number node. Number nodes contain a {@code Number} value.
 *
 * <p>Number nodes contain a value. You can inspect its type by calling
 * {@code isNaturalNumber()} or {@code isFloatingPointNumber()}. Natural
 * numbers are positive, or negative numbers without a decimal part.
 */
public final class NumberNode extends Node {

    private final BigDecimal value;
    private final Number originalValue;
    private final String stringCache;
    private boolean isNaN;
    private boolean isPositiveInfinity;
    private boolean isNegativeInfinity;

    public NumberNode(Number value, SourceLocation sourceLocation) {
        super(sourceLocation);
        originalValue = value;
        stringCache = value.toString();
        this.value = toBigDecimal(originalValue);
    }

    private BigDecimal toBigDecimal(Number value) {
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        } else if (value instanceof Integer || value instanceof Long || value instanceof Short
                   || value instanceof Byte) {
            return BigDecimal.valueOf(value.longValue());
        } else if (value instanceof Float || value instanceof Double) {
            double d = value.doubleValue();
            if (Double.isNaN(d)) {
                isNaN = true;
                return null;
            } else if (Double.isInfinite(d)) {
                if (stringCache.startsWith("-")) {
                    isNegativeInfinity = true;
                } else {
                    isPositiveInfinity = true;
                }
                return null;
            } else {
                return BigDecimal.valueOf(d);
            }
        } else if (value instanceof BigInteger) {
            return new BigDecimal((BigInteger) value);
        } else {
            return new BigDecimal(stringCache);
        }
    }

    /**
     * Gets the number value.
     *
     * @return Returns a number.
     */
    public Number getValue() {
        return originalValue;
    }

    /**
     * Gets the number value as a BigDecimal if possible.
     *
     * <p>NaN and infinite numbers will return an empty Optional.
     *
     * @return Returns the BigDecimal value of the wrapped number.
     */
    public Optional<BigDecimal> asBigDecimal() {
        return Optional.ofNullable(value);
    }

    @Deprecated
    public boolean isNaturalNumber() {
        return !isFloatingPointNumber();
    }

    /**
     * Check the value is negative, including negative infinity.
     *
     * <p>Any number >= 0, +Infinity, and NaN return false.
     *
     * @return Return true if negative.
     */
    public boolean isNegative() {
        return isNegativeInfinity || (value != null && value.compareTo(BigDecimal.ZERO) < 0);
    }

    /**
     * Returns true if the node contains a floating point number.
     *
     * @return Returns true if the node contains a floating point number.
     */
    public boolean isFloatingPointNumber() {
        return value == null || value.scale() > 0 || toString().contains(".");
    }

    /**
     * Returns true if the number is a floating point NaN.
     *
     * @return Return true if NaN.
     */
    public boolean isNaN() {
        return isNaN;
    }

    /**
     * Returns true if the number is infinite.
     *
     * @return Return true if infinite.
     */
    public boolean isInfinite() {
        return isPositiveInfinity || isNegativeInfinity;
    }

    @Override
    public NodeType getType() {
        return NodeType.NUMBER;
    }

    @Override
    public <R> R accept(NodeVisitor<R> visitor) {
        return visitor.numberNode(this);
    }

    @Override
    public NumberNode expectNumberNode(String errorMessage) {
        return this;
    }

    @Override
    public NumberNode expectNumberNode(Supplier<String> errorMessage) {
        return this;
    }

    @Override
    public Optional<NumberNode> asNumberNode() {
        return Optional.of(this);
    }

    /**
     * Returns true if the value of the number contained in the number node is zero,
     * accounting for float, double, bigInteger, bigDecimal, and other numeric types
     * (e.g., 0, 0.0, etc).
     *
     * <p>Note that -0 and +0 are considered 0. However, NaN is not considered zero.
     * When unknown number types are encountered, this method will return true if the
     * toString of the given number returns "0", or "0.0". Other kinds of unknown
     * number types will be treated like a double.
     *
     * <p>Double and float comparisons to zero are exact and use no rounding. The majority
     * of values seen by this method come from models that use "0" or "0.0". However,
     * we can improve this in the future with some kind of epsilon if the need arises.
     *
     * @return Returns true if set to zero.
     */
    public boolean isZero() {
        return value != null && value.compareTo(BigDecimal.ZERO) == 0;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof NumberNode)) {
            return false;
        } else if (other == this) {
            return true;
        } else {
            NumberNode o = (NumberNode) other;
            return isNaN == o.isNaN
                   && isPositiveInfinity == o.isPositiveInfinity
                   && isNegativeInfinity == o.isNegativeInfinity
                   && Objects.equals(value, o.value);
        }
    }

    @Override
    public int hashCode() {
        return getType().hashCode() * 7 + stringCache.hashCode();
    }

    @Override
    public String toString() {
        return stringCache;
    }
}
