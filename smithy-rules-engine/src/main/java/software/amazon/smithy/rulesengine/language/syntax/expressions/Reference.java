/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.syntax.expressions;

import static software.amazon.smithy.rulesengine.language.error.RuleError.context;

import java.util.Objects;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.rulesengine.language.evaluation.Scope;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A reference to a field.
 */
@SmithyUnstableApi
public final class Reference extends Expression {
    private final Identifier name;

    /**
     * Creates a Reference for the identifier from the given source location.
     *
     * @param name the identifier being referenced.
     * @param sourceLocation the source location for the reference.
     */
    public Reference(Identifier name, FromSourceLocation sourceLocation) {
        super(sourceLocation.getSourceLocation());
        this.name = name;
    }

    /**
     * Gets the name of the field being referenced.
     *
     * @return the name of the referenced field.
     */
    public Identifier getName() {
        return name;
    }

    @Override
    public String template() {
        return String.format("{%s}", name);
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visitRef(this);
    }

    @Override
    protected Type typeCheckLocal(Scope<Type> scope) {
        return context("while resolving the type of reference " + name, this, () -> {
            Type baseType = scope.expectValue(name);
            if (scope.isNonNull(this)) {
                return baseType.expectOptionalType().inner();
            } else {
                return baseType;
            }
        });
    }

    @Override
    public Node toNode() {
        return ObjectNode.builder()
                .withMember("ref", name.toString())
                .build();
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
    public String toString() {
        return name.toString();
    }
}
