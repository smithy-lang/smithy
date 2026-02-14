/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import java.util.List;

class TypeFunction<T> implements Function<T> {
    @Override
    public String name() {
        return "type";
    }

    @Override
    public T abstractApply(AbstractEvaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(1, functionArguments);
        return evaluator.runtime().abstractTypeOf(functionArguments.get(0).expectValue());
    }
}
