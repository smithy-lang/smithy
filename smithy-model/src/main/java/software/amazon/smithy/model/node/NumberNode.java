/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

    private final Number value;
    private final String stringCache;
    private volatile BigDecimal equality;

    public NumberNode(Number value, SourceLocation sourceLocation) {
        super(sourceLocation);
        this.value = Objects.requireNonNull(value);
        stringCache = value.toString();

        if (value instanceof BigDecimal) {
            equality = (BigDecimal) value;
        }
    }

    /**
     * Gets the number value.
     *
     * @return Returns a number.
     */
    public Number getValue() {
        return value;
    }

    /**
     * Returns true if the node contains a natural number.
     *
     * @return Returns true if the node contains a natural number.
     */
    public boolean isNaturalNumber() {
        return !isFloatingPointNumber();
    }

    /**
     * Returns true if the node contains a floating point number.
     *
     * @return Returns true if the node contains a floating point number.
     */
    public boolean isFloatingPointNumber() {
        return value instanceof Float
                || value instanceof Double
                || value instanceof BigDecimal
                || stringCache.contains(".");
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
        // Do a cheap test based on the serialized value of the number first.
        // This test covers byte, short, integer, and long.
        if (toString().equals("0") || toString().equals("0.0")) {
            return true;
        } else if (value instanceof BigDecimal) {
            return value.equals(BigDecimal.ZERO);
        } else if (value instanceof BigInteger) {
            return value.equals(BigInteger.ZERO);
        } else if (value instanceof Float) {
            return value.floatValue() == 0f;
        } else {
            return value.doubleValue() == 0d;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof NumberNode)) {
            return false;
        } else if (other == this) {
            return true;
        } else {
            NumberNode otherNode = (NumberNode) other;

            // This only works if both values are the same type.
            if (value.equals(otherNode.value)) {
                return true;
            }

            // Attempt a cheap check based on the string cache.
            if (stringCache.equals(otherNode.stringCache)) {
                return true;
            }

            // Convert both to BigDecimal and compare equality.
            return getEquality().equals(otherNode.getEquality());
        }
    }

    private BigDecimal getEquality() {
        BigDecimal e = equality;

        if (e == null) {
            if (value instanceof Integer) {
                e = BigDecimal.valueOf(value.intValue());
            } else if (value instanceof Short) {
                e = BigDecimal.valueOf(value.shortValue());
            } else if (value instanceof Byte) {
                e = BigDecimal.valueOf(value.byteValue());
            } else if (value instanceof Long) {
                e = BigDecimal.valueOf(value.longValue());
            } else {
                e = new BigDecimal(stringCache);
            }
            equality = e;
        }

        return e;
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
