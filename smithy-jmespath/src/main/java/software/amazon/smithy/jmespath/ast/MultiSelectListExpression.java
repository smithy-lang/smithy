/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.ast;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import software.amazon.smithy.jmespath.ExpressionVisitor;
import software.amazon.smithy.jmespath.JmespathExpression;

/**
 * Selects one or more values into a created array.
 *
 * @see <a href="https://jmespath.org/specification.html#multiselect-list">MultiSelect List</a>
 */
public final class MultiSelectListExpression extends JmespathExpression {

    private final List<JmespathExpression> expressions;

    public MultiSelectListExpression(List<JmespathExpression> expressions) {
        this(expressions, 1, 1);
    }

    public MultiSelectListExpression(List<JmespathExpression> expressions, int line, int column) {
        super(line, column);
        this.expressions = Collections.unmodifiableList(expressions);
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitMultiSelectList(this);
    }

    /**
     * Gets the ordered list of expressions to add to the list.
     *
     * @return Returns the expressions.
     */
    public List<JmespathExpression> getExpressions() {
        return expressions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof MultiSelectListExpression)) {
            return false;
        }
        MultiSelectListExpression that = (MultiSelectListExpression) o;
        return expressions.equals(that.expressions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expressions);
    }

    @Override
    public String toString() {
        return "MultiSelectListExpression{expressions=" + expressions + '}';
    }
}
