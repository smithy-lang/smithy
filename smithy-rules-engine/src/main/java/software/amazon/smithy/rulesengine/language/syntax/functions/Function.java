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

package software.amazon.smithy.rulesengine.language.syntax.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.rulesengine.language.error.RuleError;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public abstract class Function extends Expression {
    protected final FunctionNode functionNode;

    public Function(FunctionNode functionNode) {
        super(functionNode.getSourceLocation());
        this.functionNode = functionNode;
    }

    /**
     * Convert this function into a condition.
     *
     * @return the function as a condition.
     */
    public Condition condition() {
        return new Condition.Builder().fn(this).build();
    }

    /**
     * Converts this function into a condition which stores the output in the named result.
     *
     * @param result the name of the result parameter.
     * @return the function as a condition.
     */
    public Condition condition(String result) {
        return new Condition.Builder().fn(this).result(result).build();
    }

    /**
     * Returns the name of this function, eg. {@code isSet}, {@code parseUrl}
     *
     * @return The name
     */
    public String getName() {
        return functionNode.getName();
    }

    /**
     * @return The arguments to this function
     */
    public List<Expression> getArguments() {
        return functionNode.getArguments();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(functionNode.getName()).append("(");
        List<String> arguments = new ArrayList<>();
        for (Expression expression : functionNode.getArguments()) {
            arguments.add(expression.toString());
        }
        builder.append(String.join(", ", arguments));
        return builder.append(")").toString();
    }

    protected Expression expectOneArgument() {
        List<Expression> argv = this.functionNode.getArguments();
        if (argv.size() == 1) {
            return argv.get(0);
        } else {
            throw new RuleError(
                    new SourceException("expected 1 argument but found " + argv.size(), this.functionNode));
        }
    }

    @Override
    public SourceLocation getSourceLocation() {
        return functionNode.getSourceLocation();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Function) {
            return ((Function) obj).functionNode.equals(this.functionNode);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(functionNode);
    }

    @Override
    public Node toNode() {
        return functionNode.toNode();
    }
}
