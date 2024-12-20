/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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
