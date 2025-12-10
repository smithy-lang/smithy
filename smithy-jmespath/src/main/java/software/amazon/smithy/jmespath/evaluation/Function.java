/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import java.util.List;
import software.amazon.smithy.jmespath.JmespathException;
import software.amazon.smithy.jmespath.JmespathExceptionType;

interface Function {

    String name();

    <T> T apply(JmespathRuntime<T> runtime, List<FunctionArgument<T>> arguments);

    // Helpers

    default <T> void checkArgumentCount(int n, List<FunctionArgument<T>> arguments) {
        if (arguments.size() != n) {
            throw new JmespathException(JmespathExceptionType.INVALID_ARITY,
                    String.format("Expected %d arguments, got %d", n, arguments.size()));
        }
    }
}
