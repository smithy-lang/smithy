/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Contains the result of {@link JmespathExpression#lint}.
 */
public final class LinterResult {

    private final RuntimeType returnType;
    private final Set<ExpressionProblem> problems;

    public LinterResult(RuntimeType returnType, Set<ExpressionProblem> problems) {
        this.returnType = returnType;
        this.problems = Collections.unmodifiableSet(problems);
    }

    /**
     * Gets the statically known return type of the expression.
     *
     * @return Returns the return type of the expression.
     */
    public RuntimeType getReturnType() {
        return returnType;
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
        } else if (!(o instanceof LinterResult)) {
            return false;
        }
        LinterResult that = (LinterResult) o;
        return returnType == that.returnType && problems.equals(that.problems);
    }

    @Override
    public int hashCode() {
        return Objects.hash(returnType, problems);
    }
}
