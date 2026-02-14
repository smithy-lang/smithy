/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import software.amazon.smithy.jmespath.RuntimeType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

class AbsFunction<T> implements Function<T> {
    @Override
    public String name() {
        return "abs";
    }

    @Override
    public T abstractApply(AbstractEvaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        return evaluator.runtime().createAny(RuntimeType.NUMBER);
    }

    @Override
    public T concreteApply(Evaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(1, functionArguments);
        T value = functionArguments.get(0).expectNumber();
        Number number = evaluator.runtime().asNumber(value);

        JmespathRuntime<T> runtime = evaluator.runtime();
        switch (evaluator.runtime().numberType(value)) {
            case BYTE:
            case SHORT:
            case INTEGER:
                return runtime.createNumber(Math.abs(number.intValue()));
            case LONG:
                return runtime.createNumber(Math.abs(number.longValue()));
            case FLOAT:
                return runtime.createNumber(Math.abs(number.floatValue()));
            case DOUBLE:
                return runtime.createNumber(Math.abs(number.doubleValue()));
            case BIG_INTEGER:
                return runtime.createNumber(((BigInteger) number).abs());
            case BIG_DECIMAL:
                return runtime.createNumber(((BigDecimal) number).abs());
            default:
                throw new IllegalArgumentException("`abs` only supports numeric arguments");
        }
    }
}
