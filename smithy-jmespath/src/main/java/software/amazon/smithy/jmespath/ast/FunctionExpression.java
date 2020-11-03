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
