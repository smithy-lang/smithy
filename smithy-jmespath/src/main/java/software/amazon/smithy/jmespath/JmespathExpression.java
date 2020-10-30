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

package software.amazon.smithy.jmespath;

import java.util.Set;
import java.util.TreeSet;
import software.amazon.smithy.jmespath.ast.LiteralExpression;

/**
 * Represents a JMESPath AST node.
 */
public abstract class JmespathExpression {

    private final int line;
    private final int column;

    protected JmespathExpression(int line, int column) {
        this.line = line;
        this.column = column;
    }

    /**
     * Parse a JMESPath expression.
     *
     * @param text Expression to parse.
     * @return Returns the parsed expression.
     * @throws JmespathException if the expression is invalid.
     */
    public static JmespathExpression parse(String text) {
        return Parser.parse(text);
    }

    /**
     * Get the approximate line where the node was defined.
     *
     * @return Returns the line.
     */
    public final int getLine() {
        return line;
    }

    /**
     * Get the approximate column where the node was defined.
     *
     * @return Returns the column.
     */
    public final int getColumn() {
        return column;
    }

    /**
     * Visits a node using a double-dispatch visitor.
     *
     * @param visitor Visitor to accept on the node.
     * @param <T> Type of value the visitor returns.
     * @return Returns the result of applying the visitor.
     */
    public abstract <T> T accept(ExpressionVisitor<T> visitor);

    /**
     * Lint the expression using static analysis using "any" as the
     * current node.
     *
     * @return Returns the linter result.
     */
    public LinterResult lint() {
        return lint(LiteralExpression.ANY);
    }

    /**
     * Lint the expression using static analysis.
     *
     * @param currentNode The value to set as the current node.
     * @return Returns the problems that were detected.
     */
    public LinterResult lint(LiteralExpression currentNode) {
        Set<ExpressionProblem> problems = new TreeSet<>();
        TypeChecker typeChecker = new TypeChecker(currentNode, problems);
        LiteralExpression result = this.accept(typeChecker);
        return new LinterResult(result.getType(), problems);
    }
}
