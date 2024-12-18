/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;

/**
 * Validates if operations that are bound directly to a service may
 * be more accurately bound to a resource bound to the same service.
 */
public final class ServiceBoundResourceOperationValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        OperationIndex operationIndex = OperationIndex.of(model);
        TopDownIndex topDownIndex = TopDownIndex.of(model);

        // Check every service operation to see if it should be bound to a resource instead.
        for (ServiceShape service : model.getServiceShapes()) {
            // Store potential targets to emit one event per operation.
            Map<OperationShape, Set<ShapeId>> potentiallyBetterBindings = new HashMap<>();
            for (ShapeId operationId : service.getOperations()) {
                OperationShape operation = model.expectShape(operationId, OperationShape.class);
                // Check the resources of the containing service to test for input/output attachment.
                for (ResourceShape resource : topDownIndex.getContainedResources(service)) {
                    // Check the operation members to see if they are implicit matches for resource identifiers.
                    for (MemberShape member : operationIndex.getInputMembers(operation).values()) {
                        if (isImplicitIdentifierBinding(member, resource)) {
                            potentiallyBetterBindings.computeIfAbsent(operation, k -> new HashSet<>())
                                    .add(resource.getId());
                        }
                    }
                    for (MemberShape member : operationIndex.getOutputMembers(operation).values()) {
                        if (isImplicitIdentifierBinding(member, resource)) {
                            potentiallyBetterBindings.computeIfAbsent(operation, k -> new HashSet<>())
                                    .add(resource.getId());
                        }
                    }
                }
            }

            // Emit events per service that's present with a potentially bad binding.
            for (Map.Entry<OperationShape, Set<ShapeId>> entry : potentiallyBetterBindings.entrySet()) {
                events.add(warning(entry.getKey(),
                        service,
                        format(
                                "The `%s` operation is bound to the `%s` service but has members that match identifiers "
                                        + "of the following resource shapes: [%s]. It may be more accurately bound to one "
                                        + "of them than directly to the service.",
                                entry.getKey().getId(),
                                service.getId(),
                                ValidationUtils.tickedList(entry.getValue())),
                        service.getId().toString(),
                        entry.getKey().getId().getName()));
            }
        }

        return events;
    }

    private boolean isImplicitIdentifierBinding(MemberShape member, ResourceShape resource) {
        return resource.getIdentifiers().containsKey(member.getMemberName())
                && member.getTrait(RequiredTrait.class).isPresent()
                && member.getTarget().equals(resource.getIdentifiers().get(member.getMemberName()));
    }
}
