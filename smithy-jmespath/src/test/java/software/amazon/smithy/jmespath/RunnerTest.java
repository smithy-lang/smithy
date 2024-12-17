/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * This test loads invalid and valid files, to ensure that they
 * are either able to be parsed or not able to be parsed.
 */
public class RunnerTest {
    @Test
    public void validTests() {
        new NewLineExpressionsDataSource().validTests().forEach(line -> {
            try {
                JmespathExpression expression = JmespathExpression.parse(line);
                for (ExpressionProblem problem : expression.lint().getProblems()) {
                    if (problem.severity == ExpressionProblem.Severity.ERROR) {
                        Assertions.fail("Did not expect an ERROR for line: " + line + "\n" + problem);
                    }
                }
            } catch (JmespathException e) {
                Assertions.fail("Error loading line:\n" + line + "\n" + e.getMessage(), e);
            }
        });
    }

    @Test
    public void invalidTests() {
        new NewLineExpressionsDataSource().invalidTests().forEach(line -> {
            try {
                JmespathExpression.parse(line);
                Assertions.fail("Expected line to fail: " + line);
            } catch (JmespathException e) {
                // pass
            }
        });
    }
}
