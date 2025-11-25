/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath;

import software.amazon.smithy.jmespath.ast.LiteralExpression;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Contains the result of {@link JmespathExpression#lint}.
 */
public final class ExpressionResult {

    private final LiteralExpression value;
    private final Set<ExpressionProblem> problems;

    public ExpressionResult(LiteralExpression value, Set<ExpressionProblem> problems) {
        this.value = value;
        this.problems = Collections.unmodifiableSet(problems);
    }

    /**
     * Gets the statically known return type of the expression.
     *
     * @return Returns the return type of the expression.
     */
    public LiteralExpression getValue() {
        return value;
    }

    /**
     * Gets the set of problems in the expression.
     *
     * @return Returns the detected problems.
     */
    public Set<ExpressionProblem> getProblems() {
        return problems;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof ExpressionResult)) {
            return false;
        }
        ExpressionResult that = (ExpressionResult) o;
        return Objects.equals(value, that.value) && problems.equals(that.problems);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, problems);
    }
}
