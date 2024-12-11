/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.ast;

import java.util.List;
import java.util.Objects;
import software.amazon.smithy.jmespath.ExpressionVisitor;
import software.amazon.smithy.jmespath.JmespathExpression;

/**
 * Executes a function by name using a list of argument expressions.
 *
 * @see <a href="https://jmespath.org/specification.html#functions-expressions">Function Expressions</a>
 */
public final class FunctionExpression extends JmespathExpression {

    public String name;
    public List<JmespathExpression> arguments;

    public FunctionExpression(String name, List<JmespathExpression> arguments) {
        this(name, arguments, 1, 1);
    }

    public FunctionExpression(String name, List<JmespathExpression> arguments, int line, int column) {
        super(line, column);
        this.name = name;
        this.arguments = arguments;
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitFunction(this);
    }

    /**
     * Gets the function name.
     *
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the function arguments.
     *
     * @return Returns the argument expressions.
     */
    public List<JmespathExpression> getArguments() {
        return arguments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FunctionExpression that = (FunctionExpression) o;
        return getName().equals(that.getName()) && getArguments().equals(that.getArguments());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getArguments());
    }

    @Override
    public String toString() {
        return "FunctionExpression{name='" + name + '\'' + ", arguments=" + arguments + '}';
    }
}
