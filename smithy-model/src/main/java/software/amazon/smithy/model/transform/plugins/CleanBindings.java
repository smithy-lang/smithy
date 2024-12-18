/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform.plugins;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.model.transform.ModelTransformerPlugin;

/**
 * Removes operation bindings from resources and services when operations
 * are removed, and removes resource bindings from services and resources when
 * resources are removed.
 */
public final class CleanBindings implements ModelTransformerPlugin {
    @Override
    public Model onRemove(ModelTransformer transformer, Collection<Shape> shapes, Model model) {
        Set<ShapeId> removedResources = shapes.stream()
                .filter(Shape::isResourceShape)
                .map(Shape::getId)
                .collect(Collectors.toSet());
        Set<ShapeId> removedOperations = shapes.stream()
                .filter(Shape::isOperationShape)
                .map(Shape::getId)
                .collect(Collectors.toSet());

        Set<Shape> toReplace = getServicesToUpdate(model, removedResources, removedOperations);
        toReplace.addAll(getResourcesToUpdate(model, removedResources, removedOperations));
        return transformer.replaceShapes(model, toReplace);
    }

    private Set<Shape> getServicesToUpdate(Model model, Set<ShapeId> resources, Set<ShapeId> operations) {
        return model.shapes(ServiceShape.class)
                .filter(service -> containsAny(service.getResources(), resources)
                        || containsAny(service.getOperations(), operations))
                .map(service -> {
                    ServiceShape.Builder builder = service.toBuilder();
                    resources.forEach(builder::removeResource);
                    operations.forEach(builder::removeOperation);
                    return builder.build();
                })
                .collect(Collectors.toSet());
    }

    private Set<Shape> getResourcesToUpdate(Model model, Set<ShapeId> resources, Set<ShapeId> operations) {
        return model.shapes(ResourceShape.class)
                .filter(resource -> containsAny(resource.getAllOperations(), operations)
                        || containsAny(resource.getResources(), resources))
                .map(resource -> {
                    ResourceShape.Builder builder = resource.toBuilder();
                    resources.forEach(builder::removeResource);
                    operations.forEach(builder::removeFromAllOperationBindings);
                    return builder.build();
                })
                .collect(Collectors.toSet());
    }

    private boolean containsAny(Set<ShapeId> haystack, Set<ShapeId> needles) {
        Set<ShapeId> container = new HashSet<>(haystack);
        container.retainAll(needles);
        return !container.isEmpty();
    }
}
