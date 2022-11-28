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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.NodeValidationVisitor;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Validates the {@link EndpointTestsTrait}.
 */
@SmithyUnstableApi
public final class EndpointTestsTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {

        List<ValidationEvent> result = new ArrayList<>();
        for (ServiceShape service : model.getServiceShapesWithTrait(EndpointTestsTrait.class)) {
            result.addAll(
                    service.expectTrait(EndpointTestsTrait.class).getTestCases().stream()
                    .flatMap(endpointTestCase -> endpointTestCase.getOperationInputs().stream())
                    .flatMap(operationInput -> validateOperationInput(operationInput, service, model))
                    .collect(Collectors.toList()));
        }
        return result;
    }

    private Stream<ValidationEvent> validateOperationInput(EndpointTestOperationInput testOperationInput, ServiceShape serviceShape, Model model) {
        OperationIndex index = OperationIndex.of(model);
        StructureShape input = index.expectInputShape(ShapeId.from(testOperationInput.getOperationName()));
        NodeValidationVisitor validator = NodeValidationVisitor.builder()
                .model(model)
                .value(testOperationInput.getOperationParams())
                .eventId(getName())
                .eventShapeId(serviceShape.toShapeId())
                .startingContext(String.format("For operationInput test defined on line %s", testOperationInput.getSourceLocation().getLine()))
                .build();
        return input.accept(validator).stream();
    }
}
