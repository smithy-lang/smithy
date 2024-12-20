/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.syntax.expressions.literal;

import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;

/**
 * A boolean literal value, containing true or false.
 */
public final class BooleanLiteral extends Literal {
    private final BooleanNode value;

    BooleanLiteral(BooleanNode value) {
        super(value);
        this.value = value;
    }

    BooleanLiteral(BooleanNode value, FromSourceLocation sourceLocation) {
        super(sourceLocation);
        this.value = value;
    }

    /**
     * Gets the {@link BooleanNode} value of this literal.
     *
     * @return the literal's value node.
     */
    public BooleanNode value() {
        return value;
    }

    @Override
    public <T> T accept(LiteralVisitor<T> visitor) {
        return visitor.visitBoolean(value.getValue());
    }

    @Override
    public Optional<Boolean> asBooleanLiteral() {
        return Optional.of(value.getValue());
    }

    @Override
    public Node toNode() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        BooleanLiteral that = (BooleanLiteral) obj;
        return Objects.equals(this.value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
