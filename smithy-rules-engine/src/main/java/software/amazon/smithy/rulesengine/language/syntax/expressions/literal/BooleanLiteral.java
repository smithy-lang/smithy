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
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;

public final class BooleanLiteral extends Literal {
    private final BooleanNode value;

    BooleanLiteral(BooleanNode value, FromSourceLocation sourceLocation) {
        super(sourceLocation);
        this.value = value;
    }

    public BooleanNode value() {
        return value;
    }

    @Override
    public <T> T accept(LiteralVisitor<T> visitor) {
        return visitor.visitBoolean(value.getValue());
    }

    @Override
    public Optional<Boolean> asBoolean() {
        return Optional.of(value.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
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
    public String toString() {
        return value.toString();
    }

    @Override
    public Node toNode() {
        return value;
    }
}
