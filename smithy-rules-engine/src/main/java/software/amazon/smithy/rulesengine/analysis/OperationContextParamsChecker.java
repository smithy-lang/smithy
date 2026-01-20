/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.analysis;

import java.util.Optional;
import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.jmespath.LinterResult;
import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.jmespath.node.ModelJmespathUtilities;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.rulesengine.language.syntax.parameters.ParameterType;
import software.amazon.smithy.rulesengine.traits.OperationContextParamDefinition;

public final class OperationContextParamsChecker {

    private OperationContextParamsChecker() {

    }

    public static LinterResult lint(
            OperationContextParamDefinition paramDefinition,
            OperationShape operationShape,
            Model model
    ) {
        JmespathExpression path = JmespathExpression.parse(paramDefinition.getPath());
        StructureShape input = OperationIndex.of(model).expectInputShape(operationShape);
        return ModelJmespathUtilities.lint(model, input, path);
    }

    public static Optional<ParameterType> inferParameterType(
            OperationContextParamDefinition paramDefinition,
            OperationShape operationShape,
            Model model
    ) {
        RuntimeType runtimeType = lint(paramDefinition, operationShape, model).getReturnType();
        switch (runtimeType) {
            case BOOLEAN:
                return Optional.of(ParameterType.BOOLEAN);
            case STRING:
                return Optional.of(ParameterType.STRING);
            case ARRAY:
                return Optional.of(ParameterType.STRING_ARRAY);
            default:
                return Optional.empty();
        }
    }
}
