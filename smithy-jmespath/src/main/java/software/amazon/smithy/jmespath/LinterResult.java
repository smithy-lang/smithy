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
