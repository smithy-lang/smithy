/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import software.amazon.smithy.jmespath.JmespathException;
import software.amazon.smithy.jmespath.JmespathExceptionType;
import software.amazon.smithy.jmespath.type.Type;

public interface Function<T> {

    String name();

    default T apply(AbstractEvaluator<T> evaluator, List<FunctionArgument<T>> arguments) {
        if (evaluator instanceof Evaluator) {
            return concreteApply((Evaluator<T>)evaluator, arguments);
        } else {
            return abstractApply(evaluator, arguments);
        }
    }

    T abstractApply(AbstractEvaluator<T> evaluator, List<FunctionArgument<T>> arguments);

    default T concreteApply(Evaluator<T> evaluator, List<FunctionArgument<T>> arguments) {
        return abstractApply(evaluator, arguments);
    }

    // Helpers

    default void checkArgumentCount(int n, List<FunctionArgument<T>> arguments) {
        if (arguments.size() != n) {
            throw new JmespathException(JmespathExceptionType.INVALID_ARITY,
                    String.format("Expected %d arguments, got %d", n, arguments.size()));
        }
    }

    default T apply(AbstractEvaluator<T> evaluator, T arg0) {
        return apply(evaluator, Collections.singletonList(evaluator.runtime().createFunctionArgument(arg0)));
    }

    default T apply(AbstractEvaluator<T> evaluator, T arg0, T arg1) {
        return apply(evaluator, Arrays.asList(
                evaluator.runtime().createFunctionArgument(arg0),
                evaluator.runtime().createFunctionArgument(arg1)));
    }

    default T apply(AbstractEvaluator<T> evaluator, T arg0, T arg1, T arg2) {
        return apply(evaluator, Arrays.asList(
                evaluator.runtime().createFunctionArgument(arg0),
                evaluator.runtime().createFunctionArgument(arg1),
                evaluator.runtime().createFunctionArgument(arg2)));
    }
}
