/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.functions;

import java.util.List;
import software.amazon.smithy.jmespath.evaluation.EvaluationUtils;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

class AvgFunction implements Function {
    @Override
    public String name() {
        return "avg";
    }

    @Override
    public <T> T apply(JmespathRuntime<T> runtime, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(1, functionArguments);
        T array = functionArguments.get(0).expectArray();
        Number length = runtime.length(array);
        if (length.intValue() == 0) {
            return runtime.createNull();
        }
        Number sum = 0D;
        for (T element : runtime.toIterable(array)) {
            sum = EvaluationUtils.addNumbers(sum, runtime.asNumber(element));
        }
        return runtime.createNumber(EvaluationUtils.divideNumbers(sum, length));
    }
}
