/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.functions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

public class FloorFunction implements Function {
    @Override
    public String name() {
        return "floor";
    }

    @Override
    public <T> T apply(JmespathRuntime<T> runtime, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(1, functionArguments);
        T value = functionArguments.get(0).expectNumber();
        Number number = runtime.asNumber(value);

        switch (runtime.numberType(value)) {
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
            case BIG_INTEGER:
                return value;
            case BIG_DECIMAL:
                return runtime.createNumber(((BigDecimal)number).setScale(0, RoundingMode.FLOOR));
            case DOUBLE:
                return runtime.createNumber(Math.floor(number.doubleValue()));
            case FLOAT:
                return runtime.createNumber((long) Math.floor(number.floatValue()));
            default:
                throw new RuntimeException("Unknown number type: " + number.getClass().getName());
        }
    }
}