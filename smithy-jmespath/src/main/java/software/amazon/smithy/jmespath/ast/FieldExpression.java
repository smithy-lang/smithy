/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.ast;

import java.util.Objects;
import software.amazon.smithy.jmespath.ExpressionVisitor;
import software.amazon.smithy.jmespath.JmespathExpression;

/**
 * Gets a field by name from an object.
 *
 * <p>This AST node is created for identifiers. For example,
 * {@code foo} creates a {@code FieldExpression}.
 *
 * @see <a href="https://jmespath.org/specification.html#identifiers">Identifiers</a>
 */
public final class FieldExpression extends JmespathExpression {

    private final String name;

    public FieldExpression(String name) {
        this(name, 1, 1);
    }

    public FieldExpression(String name, int line, int column) {
        super(line, column);
        this.name = name;
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitField(this);
    }

    /**
     * Get the name of the field to retrieve.
     *
     * @return Returns the name of the field.
     */
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof FieldExpression)) {
            return false;
        } else {
            return getName().equals(((FieldExpression) o).getName());
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }

    @Override
    public String toString() {
        return "FieldExpression{name='" + name + '\'' + '}';
    }
}
