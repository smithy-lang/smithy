/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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
                if (node.isStringNode() || node.isBooleanNode()) {
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
}
