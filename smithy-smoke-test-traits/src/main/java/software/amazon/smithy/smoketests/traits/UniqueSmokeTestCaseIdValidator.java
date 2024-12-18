/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.smoketests.traits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.utils.SetUtils;

/**
 * Validates that smoke test cases have unique IDs within a service closure,
 * or within the trait if it is not connected to a service.
 */
public class UniqueSmokeTestCaseIdValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        Set<ServiceShape> serviceShapes = model.getServiceShapes();
        Set<OperationShape> serviceBoundOperations = new HashSet<>();
        TopDownIndex index = new TopDownIndex(model);
        // Validate test case ids within each service closure
        for (ServiceShape service : serviceShapes) {
            Set<OperationShape> boundOperations = index.getContainedOperations(service);
            addValidationEventsForShapes(boundOperations, events);
            serviceBoundOperations.addAll(boundOperations);
        }

        // Also validate ids are unique within each non-service bound operation
        List<OperationShape> shapes = model.getOperationShapesWithTrait(SmokeTestsTrait.class)
                .stream()
                .filter(shape -> !serviceBoundOperations.contains(shape))
                .collect(Collectors.toList());

        for (OperationShape shape : shapes) {
            addValidationEventsForShapes(SetUtils.of(shape), events);
        }
        return events;
    }

    private void addValidationEventsForShapes(Set<OperationShape> shapes, List<ValidationEvent> events) {
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
                    events.add(error(shape,
                            String.format(
                                    "Conflicting `%s` test case IDs found for ID `%s`: %s",
                                    SmokeTestsTrait.ID,
                                    entry.getKey(),
                                    ValidationUtils.tickedList(entry.getValue().stream().map(Shape::getId)))));
                }
            }
        }
    }
}
