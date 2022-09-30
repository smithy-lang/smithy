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

package software.amazon.smithy.rulesengine.language.syntax.expr;

import static software.amazon.smithy.rulesengine.language.error.RuleError.context;

import java.util.Objects;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.rulesengine.language.eval.Scope;
import software.amazon.smithy.rulesengine.language.eval.Type;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.visit.ExpressionVisitor;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A reference to a field.
 */
@SmithyUnstableApi
public final class Reference extends Expression {
    private final Identifier name;

    public Reference(Identifier name, FromSourceLocation sourceLocation) {
        super(sourceLocation.getSourceLocation());
        this.name = name;
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visitRef(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Reference reference = (Reference) o;
        return name.equals(reference.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public Type typeCheckLocal(Scope<Type> scope) {
        return context(
                "while resolving the type of reference " + name,
                this,
                () -> {
                    Type baseType = scope.expectValue(this.name);
                    if (scope.isNonNull(this)) {
                        return baseType.expectOptional().inner();
                    } else {
                        return baseType;
                    }
                }
        );
    }

    @Override
    public String template() {
        return String.format("{%s}", name);
    }

    public Identifier getName() {
        return name;
    }

    @Override
    public String toString() {
        return name.asString();
    }

    @Override
    public Node toNode() {
        return ObjectNode.builder()
                .withMember("ref", name.asString())
                .build();
    }
}
