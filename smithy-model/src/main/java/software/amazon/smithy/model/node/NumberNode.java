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
import java.util.Objects;
import java.util.Optional;
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
    private String stringCache;

    public NumberNode(Number value, SourceLocation sourceLocation) {
        super(sourceLocation);
        this.value = Objects.requireNonNull(value);
        stringCache = value.toString();
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
    public Optional<NumberNode> asNumberNode() {
        return Optional.of(this);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof NumberNode && stringCache.equals(((NumberNode) other).stringCache);
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
