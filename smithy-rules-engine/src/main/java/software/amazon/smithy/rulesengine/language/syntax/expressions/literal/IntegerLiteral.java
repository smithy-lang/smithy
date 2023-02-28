/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.rulesengine.language.syntax.expressions.literal;

import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NumberNode;

public final class IntegerLiteral extends Literal {
    private final NumberNode value;

    IntegerLiteral(NumberNode value, FromSourceLocation sourceLocation) {
        super(sourceLocation);
        if (!value.isNaturalNumber()) {
            throw new RuntimeException("only integers >= 0 are supported");
        }
        this.value = value;
    }

    @Override
    public <T> T accept(LiteralVisitor<T> visitor) {
        return visitor.visitInteger(value.getValue().intValue());
    }

    @Override
    public Optional<Integer> asInteger() {
        return Optional.of(this.value.getValue().intValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
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
    public String toString() {
        return Integer.toString(value.getValue().intValue());
    }

    @Override
    public Node toNode() {
        return value;
    }
}
