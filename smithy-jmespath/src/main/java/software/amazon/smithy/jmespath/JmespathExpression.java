/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath;

import java.util.Set;
import java.util.TreeSet;
import software.amazon.smithy.jmespath.ast.LiteralExpression;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;
import software.amazon.smithy.jmespath.evaluation.Evaluator;

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
        return Parser.parse(text, LiteralExpressionJmespathRuntime.INSTANCE);
    }

    /**
     * Parse a JMESPath expression.
     *
     * @param text Expression to parse.
     * @param runtime JmespathRuntime used to instantiate literal values.
     * @return Returns the parsed expression.
     * @throws JmespathException if the expression is invalid.
     */
    public static <T> JmespathExpression parse(String text, JmespathRuntime<T> runtime) {
        return Parser.parse(text, runtime);
    }

    /**
     * Parse a JSON value.
     *
     * @param text JSON value to parse.
     * @param runtime JmespathRuntime used to instantiate the parsed JSON value.
     * @return Returns the parsed JSON value.
     * @throws JmespathException if the text is invalid.
     */
    public static <T> T parseJson(String text, JmespathRuntime<T> runtime) {
        Lexer<T> lexer = new Lexer<T>(text, runtime);
        return lexer.parseJsonValue();
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

    public LiteralExpression evaluate(LiteralExpression currentNode) {
        return evaluate(currentNode, new LiteralExpressionJmespathRuntime());
    }

    public <T> T evaluate(T currentNode, JmespathRuntime<T> runtime) {
        return new Evaluator<>(currentNode, runtime).visit(this);
    }
}
