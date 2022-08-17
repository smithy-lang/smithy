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

package software.amazon.smithy.rulesengine.reterminus.lang.expr;

import static software.amazon.smithy.rulesengine.reterminus.error.RuleError.ctx;

import java.util.Objects;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.rulesengine.reterminus.eval.Scope;
import software.amazon.smithy.rulesengine.reterminus.eval.Type;
import software.amazon.smithy.rulesengine.reterminus.eval.Value;
import software.amazon.smithy.rulesengine.reterminus.lang.Identifier;
import software.amazon.smithy.rulesengine.reterminus.visit.ExprVisitor;

/**
 * A reference to a field.
 */
public class Ref extends Expr {
    private final SourceLocation sourceLocation;
    private final Identifier name;

    public Ref(Identifier name, FromSourceLocation sourceLocation) {
        this.name = name;
        this.sourceLocation = sourceLocation.getSourceLocation();
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public <R> R accept(ExprVisitor<R> visitor) {
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
        Ref ref = (Ref) o;
        return name.equals(ref.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public Type typecheckLocal(Scope<Type> scope) {
        return ctx(
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
    public Value eval(Scope<Value> scope) {
        return scope.getValue(this.name).orElse(new Value.None());
    }

    @Override
    public Node toNode() {
        return ObjectNode.builder()
                .withMember("ref", name.asString())
                .build();
    }
}
