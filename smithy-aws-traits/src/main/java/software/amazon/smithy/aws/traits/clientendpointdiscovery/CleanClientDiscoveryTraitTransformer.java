/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.clientendpointdiscovery;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.model.transform.ModelTransformerPlugin;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Removes the endpoint discovery trait from a service if the referenced operation or error are removed.
 */
@SmithyInternalApi
public final class CleanClientDiscoveryTraitTransformer implements ModelTransformerPlugin {
    @Override
    public Model onRemove(ModelTransformer transformer, Collection<Shape> shapes, Model model) {
        Set<ShapeId> removedOperations = shapes.stream()
                .filter(Shape::isOperationShape)
                .map(Shape::getId)
                .collect(Collectors.toSet());
        Set<ShapeId> removedErrors = shapes.stream()
                .filter(shape -> shape.hasTrait(ErrorTrait.class))
                .map(Shape::getId)
                .collect(Collectors.toSet());

        Set<Shape> servicesToUpdate = getServicesToUpdate(model, removedOperations, removedErrors);
        Set<Shape> shapesToUpdate = new HashSet<>(servicesToUpdate);

        Set<Shape> operationsToUpdate = getOperationsToUpdate(
                model,
                servicesToUpdate.stream().map(Shape::getId).collect(Collectors.toSet()));
        shapesToUpdate.addAll(operationsToUpdate);

        Set<Shape> membersToUpdate = getMembersToUpdate(
                model,
                operationsToUpdate.stream().map(Shape::getId).collect(Collectors.toSet()));
        shapesToUpdate.addAll(membersToUpdate);
        return transformer.replaceShapes(model, shapesToUpdate);
    }

    private Set<Shape> getServicesToUpdate(Model model, Set<ShapeId> removedOperations, Set<ShapeId> removedErrors) {
        Set<Shape> result = new HashSet<>();
        for (ServiceShape service : model.getServiceShapesWithTrait(ClientEndpointDiscoveryTrait.class)) {
            ClientEndpointDiscoveryTrait trait = service.expectTrait(ClientEndpointDiscoveryTrait.class);
            if (removedOperations.contains(trait.getOperation())) {
                ServiceShape.Builder builder = service.toBuilder();
                builder.removeTrait(ClientEndpointDiscoveryTrait.ID);
                result.add(builder.build());
            }
        }
        return result;
    }

    private Set<Shape> getOperationsToUpdate(
            Model model,
            Set<ShapeId> updatedServices
    ) {
        ClientEndpointDiscoveryIndex discoveryIndex = ClientEndpointDiscoveryIndex.of(model);
        Set<ShapeId> stillBoundOperations = model.shapes(ServiceShape.class)
                // Get all endpoint discovery services
                .filter(service -> service.hasTrait(ClientEndpointDiscoveryTrait.class))
                .map(Shape::getId)
                // Get those services who aren't having their discovery traits removed
                .filter(service -> !updatedServices.contains(service))
                // Get all the discovery operations bound to those services
                .flatMap(service -> discoveryIndex.getEndpointDiscoveryOperations(service).stream())
                .collect(Collectors.toSet());

        // Get all endpoint discovery operations
        Set<Shape> result = new HashSet<>();
        for (OperationShape operation : model.getOperationShapesWithTrait(ClientDiscoveredEndpointTrait.class)) {
            ClientDiscoveredEndpointTrait trait = operation.expectTrait(ClientDiscoveredEndpointTrait.class);
            // Only get the ones where discovery is optional, as it is safe to remove in that case.
            // Only get the ones that aren't still bound to a service that requires endpoint discovery.
            if (!trait.isRequired() && !stillBoundOperations.contains(operation.getId())) {
                result.add(operation.toBuilder().removeTrait(ClientDiscoveredEndpointTrait.ID).build());
            }
        }
        return result;
    }

    private Set<Shape> getMembersToUpdate(Model model, Set<ShapeId> updatedOperations) {
        Set<ShapeId> stillBoundMembers = model.shapes(OperationShape.class)
                // Get all endpoint discovery operations
                .filter(operation -> operation.hasTrait(ClientDiscoveredEndpointTrait.class))
                // Filter out the ones which are having their endpoint discovery traits removed
                .filter(operation -> !updatedOperations.contains(operation.getId()))
                // Get the input shapes of those operations
                .map(operation -> model.getShape(operation.getInputShape()).flatMap(Shape::asStructureShape))
                .filter(Optional::isPresent)
                // Get the input members
                .flatMap(input -> input.get().getAllMembers().values().stream())
                .map(Shape::getId)
                .collect(Collectors.toSet());

        return model.shapes(MemberShape.class)
                // Get all members which have the endpoint discovery id trait
                .filter(member -> member.hasTrait(ClientEndpointDiscoveryIdTrait.class))
                // Get those which are on structures that aren't still bound to endpoint discovery operations
                .filter(member -> !stillBoundMembers.contains(member.getId()))
                // Remove the trait
                .map(member -> member.toBuilder().removeTrait(ClientEndpointDiscoveryIdTrait.ID).build())
                .collect(Collectors.toSet());
    }
}
