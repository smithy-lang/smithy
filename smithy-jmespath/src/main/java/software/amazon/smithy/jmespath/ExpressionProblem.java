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

import java.util.Objects;

/**
 * Represents a problem detected by static analysis.
 */
public final class ExpressionProblem implements Comparable<ExpressionProblem> {

    /**
     * The severity of the problem.
     */
    public enum Severity {
        /** The problem is an unrecoverable error. */
        ERROR,

        /** The problem is a warning that you might be able to ignore depending on the input. */
        DANGER,

        /** The problem points out a potential issue that may be intentional. */
        WARNING
    }

    /** The description of the problem. */
    public final String message;

    /** The line where the problem occurred. */
    public final int line;

    /** The column where the problem occurred. */
    public final int column;

    /** The severity of the problem. */
    public final Severity severity;

    ExpressionProblem(Severity severity, int line, int column, String message) {
        this.severity = severity;
        this.line = line;
        this.column = column;
        this.message = message;
    }

    @Override
    public String toString() {
        return "[" + severity + "] " + message + " (" + line + ":" + column + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof ExpressionProblem)) {
            return false;
        }
        ExpressionProblem problem = (ExpressionProblem) o;
        return severity == problem.severity
               && line == problem.line
               && column == problem.column
               && message.equals(problem.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(severity, message, line, column);
    }

    @Override
    public int compareTo(ExpressionProblem o) {
        return toString().compareTo(o.toString());
    }
}
