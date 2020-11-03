/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
