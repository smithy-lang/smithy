/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.smoketests.traits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.utils.ListUtils;

/**
 * Validates that smoke test cases have unique IDs within a service closure,
 * or within the trait if it is not connected to a service.
 */
public class UniqueSmokeTestCaseIdValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        Set<ServiceShape> serviceShapes = model.getServiceShapes();
        Set<ShapeId> serviceBoundOperationIds = new HashSet<>();
        // Validate test case ids within each service closure
        for (ServiceShape service : serviceShapes) {
            Set<ShapeId> operationIds = service.getAllOperations();
            serviceBoundOperationIds.addAll(operationIds);
            List<Shape> shapes = operationIds.stream()
                    .map(model::getShape)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
            addValidationEventsForShapes(shapes, events);
        }

        // Also validate ids are unique within each non-service bound operation
        List<OperationShape> shapes = model.getOperationShapesWithTrait(SmokeTestsTrait.class).stream()
                .filter(shape -> !serviceBoundOperationIds.contains(shape.getId()))
                .collect(Collectors.toList());
        for (OperationShape shape : shapes) {
            addValidationEventsForShapes(ListUtils.of(shape), events);
        }
        return events;
    }

    private void addValidationEventsForShapes(List<? extends Shape> shapes, List<ValidationEvent> events) {
        Map<String, List<Shape>> testCaseIdsToOperations = new HashMap<>();
        for (Shape shape : shapes) {
            if (!shape.isOperationShape() || !shape.hasTrait(SmokeTestsTrait.class)) {
                continue;
            }

            SmokeTestsTrait trait = shape.expectTrait(SmokeTestsTrait.class);
            for (SmokeTestCase testCase : trait.getTestCases()) {
                testCaseIdsToOperations.computeIfAbsent(testCase.getId(), id -> new ArrayList<>()).add(shape);
            }
        }

        for (Map.Entry<String, List<Shape>> entry : testCaseIdsToOperations.entrySet()) {
            if (entry.getValue().size() > 1) {
                for (Shape shape : entry.getValue()) {
                    events.add(error(shape, String.format(
                            "Conflicting `%s` test case IDs found for ID `%s`: %s",
                            SmokeTestsTrait.ID, entry.getKey(),
                            ValidationUtils.tickedList(entry.getValue().stream().map(Shape::getId))
                    )));
                }
            }
        }
    }


}
