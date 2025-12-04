/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.functions;

import java.util.List;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

public class EndsWithFunction implements Function {
    @Override
    public String name() {
        return "ends_with";
    }

    @Override
    public <T> T apply(JmespathRuntime<T> runtime, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(2, functionArguments);
        T subject = functionArguments.get(0).expectString();
        T suffix = functionArguments.get(1).expectString();

        String subjectStr = runtime.asString(subject);
        String suffixStr = runtime.asString(suffix);

        return runtime.createBoolean(subjectStr.endsWith(suffixStr));
    }
}
