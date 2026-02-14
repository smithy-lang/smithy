/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import java.util.List;

class EitherFunction<T> implements Function<T> {
    @Override
    public String name() {
        return "either";
    }

    @Override
    public T abstractApply(AbstractEvaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        JmespathAbstractRuntime<T> runtime = evaluator.runtime();
        checkArgumentCount(2, functionArguments);
        T left = functionArguments.get(0).expectArray();
        T right = functionArguments.get(1).expectValue();

        return runtime.either(left, right);
    }

    @Override
    public T concreteApply(Evaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        return abstractApply(evaluator, functionArguments);
    }
}
