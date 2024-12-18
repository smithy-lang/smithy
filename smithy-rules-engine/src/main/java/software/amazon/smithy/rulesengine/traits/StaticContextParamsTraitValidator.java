/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.traits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Validates {@link StaticContextParamsTrait} traits.
 */
@SmithyUnstableApi
public final class StaticContextParamsTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        ContextIndex index = ContextIndex.of(model);
        List<ValidationEvent> events = new ArrayList<>();
        for (OperationShape operationShape : model.getOperationShapes()) {
            Map<String, StaticContextParamDefinition> definitionMap = index.getStaticContextParams(operationShape)
                    .map(StaticContextParamsTrait::getParameters)
                    .orElse(Collections.emptyMap());
            for (Map.Entry<String, StaticContextParamDefinition> entry : definitionMap.entrySet()) {
                Node node = entry.getValue().getValue();
                if (supportedType(node)) {
                    continue;
                }
                events.add(error(operationShape,
                        String.format("The operation `%s` is marked with `%s` which contains a "
                                + "key `%s` with an unsupported document type value `%s`.",
                                operationShape.getId(),
                                StaticContextParamsTrait.ID.toString(),
                                entry.getKey(),
                                entry.getValue().getValue().getType().toString())));
            }
        }
        return events;
    }

    private static boolean supportedType(Node node) {
        if (node.isStringNode() || node.isBooleanNode()) {
            return true;
        }

        if (node.isArrayNode()) {
            // all elements must be strings
            return node.expectArrayNode().getElements().stream().allMatch(e -> e.isStringNode());
        }

        return false;
    }
}
