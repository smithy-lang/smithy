/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import software.amazon.smithy.jmespath.RuntimeType;

class LengthFunction<T> implements Function<T> {

    private static final Set<RuntimeType> PARAMETER_TYPES = new HashSet<>();
    static {
        PARAMETER_TYPES.add(RuntimeType.STRING);
        PARAMETER_TYPES.add(RuntimeType.ARRAY);
        PARAMETER_TYPES.add(RuntimeType.OBJECT);
    }

    @Override
    public String name() {
        return "length";
    }


    @Override
    public T abstractApply(AbstractEvaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        return evaluator.runtime().createAny(RuntimeType.NUMBER);
    }

    @Override
    public T concreteApply(Evaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(1, functionArguments);
        T value = functionArguments.get(0).expectAnyOf(PARAMETER_TYPES);

        return evaluator.runtime().abstractLength(value);
    }
}
