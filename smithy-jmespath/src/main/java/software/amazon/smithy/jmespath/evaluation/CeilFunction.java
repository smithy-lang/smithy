/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import software.amazon.smithy.jmespath.RuntimeType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

class CeilFunction<T> implements Function<T> {
    @Override
    public String name() {
        return "ceil";
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

        switch (evaluator.runtime().numberType(value)) {
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
            case BIG_INTEGER:
                return value;
            case BIG_DECIMAL:
                return evaluator.runtime().createNumber(((BigDecimal) number).setScale(0, RoundingMode.CEILING));
            case DOUBLE:
                return evaluator.runtime().createNumber(Math.ceil(number.doubleValue()));
            case FLOAT:
                return evaluator.runtime().createNumber(Math.ceil(number.floatValue()));
            default:
                throw new RuntimeException("Unknown number type: " + number.getClass().getName());
        }
    }
}
