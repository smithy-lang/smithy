/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import java.util.List;
import software.amazon.smithy.jmespath.JmespathException;
import software.amazon.smithy.jmespath.JmespathExceptionType;
import software.amazon.smithy.jmespath.RuntimeType;

class ContainsFunction<T> implements Function<T> {
    @Override
    public String name() {
        return "contains";
    }

    @Override
    public T abstractApply(AbstractEvaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        return evaluator.runtime().createAny(RuntimeType.BOOLEAN);
    }

    @Override
    public T concreteApply(Evaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        JmespathRuntime<T> runtime = evaluator.runtime();
        checkArgumentCount(2, functionArguments);
        T subject = functionArguments.get(0).expectValue();
        T search = functionArguments.get(1).expectValue();
        switch (runtime.typeOf(subject)) {
            case STRING:
                String searchString = runtime.asString(search);
                String subjectString = runtime.asString(subject);
                return runtime.createBoolean(subjectString.contains(searchString));
            case ARRAY:
                for (T item : runtime.asIterable(subject)) {
                    if (runtime.equal(item, search)) {
                        return runtime.createBoolean(true);
                    }
                }
                return runtime.createBoolean(false);
            default:
                throw new JmespathException(JmespathExceptionType.INVALID_TYPE,
                        "contains is not supported for " + runtime.typeOf(subject));
        }
    }
}
