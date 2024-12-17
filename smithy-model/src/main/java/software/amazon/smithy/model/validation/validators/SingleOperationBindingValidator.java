/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates that an operation is only bound to once in an entire
 * service closure.
 */
public final class SingleOperationBindingValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        TopDownIndex topDownIndex = TopDownIndex.of(model);
        return model.shapes(ServiceShape.class)
                .flatMap(shape -> validateService(topDownIndex, shape).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateService(TopDownIndex topDownIndex, ServiceShape service) {
        List<ValidationEvent> events = new ArrayList<>();

        for (OperationShape operation : topDownIndex.getContainedOperations(service)) {
            ShapeId id = operation.getId();
            Set<Shape> containers = new HashSet<>();
            if (service.getOperations().contains(id)) {
                containers.add(service);
            }

            for (ResourceShape resource : topDownIndex.getContainedResources(service.getId())) {
                if (resource.getAllOperations().contains(id)) {
                    containers.add(resource);
                }
            }

            if (containers.size() > 1) {
                events.add(error(operation,
                        String.format(
                                "An operation can appear only once in an entire service closure. This operation is "
                                        + "illegally bound into the `%s` service closure from multiple shapes: %s",
                                service.getId(),
                                containers.stream().map(Object::toString).sorted().collect(Collectors.joining(", ")))));
            }
        }

        return events;
    }
}
