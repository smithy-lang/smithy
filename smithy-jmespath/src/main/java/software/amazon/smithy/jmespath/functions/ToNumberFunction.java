/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.functions;

import java.util.List;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

public class ToNumberFunction implements Function {
    @Override
    public String name() {
        return "to_number";
    }

    @Override
    public <T> T apply(JmespathRuntime<T> runtime, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(1, functionArguments);
        T value = functionArguments.get(0).expectValue();
        
        switch (runtime.typeOf(value)) {
            case NUMBER:
                return value;
            case STRING:
                try {
                    String str = runtime.asString(value);
                    if (str.contains(".")) {
                        return runtime.createNumber(Double.parseDouble(str));
                    } else {
                        return runtime.createNumber(Long.parseLong(str));
                    }
                } catch (NumberFormatException e) {
                    return runtime.createNull();
                }
            default:
                return runtime.createNull();
        }
    }
}