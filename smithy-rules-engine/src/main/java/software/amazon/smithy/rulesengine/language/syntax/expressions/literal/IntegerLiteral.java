/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.syntax.expressions.literal;

import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NumberNode;

/**
 * An integer literal value, containing integer values greater than or equal to 0.
 */
public final class IntegerLiteral extends Literal {
    private final NumberNode value;

    IntegerLiteral(NumberNode value) {
        super(value);
        validateValue(value);
        this.value = value;
    }

    IntegerLiteral(NumberNode value, FromSourceLocation sourceLocation) {
        super(sourceLocation);
        validateValue(value);
        this.value = value;
    }

    private void validateValue(NumberNode node) {
        int nodeValue = node.getValue().intValue();
        if (node.isFloatingPointNumber() || nodeValue < 0) {
            throw new RuntimeException("Only integer values greater than or equal to 0 are supported.");
        }
    }

    @Override
    public <T> T accept(LiteralVisitor<T> visitor) {
        return visitor.visitInteger(value.getValue().intValue());
    }

    @Override
    public Optional<Integer> asIntegerLiteral() {
        return Optional.of(value.getValue().intValue());
    }

    @Override
    public Node toNode() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IntegerLiteral integerLiteral = (IntegerLiteral) o;
        return value.equals(integerLiteral.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return Integer.toString(value.getValue().intValue());
    }
}
