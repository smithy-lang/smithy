/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.traits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.NodeValidationVisitor;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Validates the {@link EndpointTestsTrait}.
 */
@SmithyUnstableApi
public final class EndpointTestsTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        TopDownIndex topDownIndex = TopDownIndex.of(model);

        for (ServiceShape serviceShape : model.getServiceShapesWithTrait(EndpointTestsTrait.class)) {
            // Precompute shape ids to operations in the service.
            Map<String, OperationShape> operationNameMap = new HashMap<>();
            for (OperationShape operationShape : topDownIndex.getContainedOperations(serviceShape)) {
                operationNameMap.put(operationShape.getId().getName(), operationShape);
            }

            for (EndpointTestCase testCase : serviceShape.expectTrait(EndpointTestsTrait.class).getTestCases()) {
                for (EndpointTestOperationInput testOperationInput : testCase.getOperationInputs()) {
                    String operationName = testOperationInput.getOperationName();

                    // It's possible for an operation defined to not be in the service closure.
                    if (!operationNameMap.containsKey(operationName)) {
                        events.add(error(serviceShape, testOperationInput,
                                String.format("Test case operation `%s` does not exist in service `%s`",
                                        operationName, serviceShape.getId())));
                    }

                    StructureShape inputShape = model.expectShape(
                            operationNameMap.get(operationName).getInputShape(), StructureShape.class);
                    events.addAll(validateOperationInput(model, serviceShape, inputShape, testOperationInput));
                }
            }
        }

        return events;
    }

    private List<ValidationEvent> validateOperationInput(
            Model model,
            ServiceShape serviceShape,
            StructureShape inputShape,
            EndpointTestOperationInput testOperationInput
    ) {
        NodeValidationVisitor validator = NodeValidationVisitor.builder()
                .model(model)
                .value(testOperationInput.getOperationParams())
                .eventId(getName())
                .eventShapeId(serviceShape.toShapeId())
                .startingContext("The operationInput value for an endpoint test "
                        + "does not match the operation's input shape")
                .build();
        return inputShape.accept(validator);
    }
}
