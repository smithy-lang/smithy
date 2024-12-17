/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.knowledge;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.shapes.EntityShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.utils.ListUtils;

/**
 * Computes all of the parent shapes of resources and operations from the bottom-up.
 */
public final class BottomUpIndex implements KnowledgeIndex {
    private final Map<ShapeId, Map<ShapeId, List<EntityShape>>> parentBindings = new HashMap<>();

    public BottomUpIndex(Model model) {
        NeighborProvider provider = NeighborProviderIndex.of(model).getProvider();
        for (ServiceShape service : model.getServiceShapes()) {
            Map<ShapeId, List<EntityShape>> paths = new HashMap<>();
            parentBindings.put(service.getId(), paths);

            Deque<EntityShape> path = new ArrayDeque<>();
            path.push(service);
            collectPaths(paths, path, service, provider);
        }
    }

    private void collectPaths(
            Map<ShapeId, List<EntityShape>> paths,
            Deque<EntityShape> path,
            Shape current,
            NeighborProvider neighborProvider
    ) {
        for (Relationship relationship : neighborProvider.getNeighbors(current)) {
            Shape neighbor = relationship.expectNeighborShape();
            if (!neighbor.isOperationShape() && !neighbor.isResourceShape()) {
                continue;
            }

            // Note: The path does not include the neighbor shape
            paths.put(neighbor.getId(), ListUtils.copyOf(path));

            // Recurse through neighbors of an entity (resource) shape
            if (neighbor instanceof EntityShape) {
                path.push((EntityShape) neighbor);
                collectPaths(paths, path, neighbor, neighborProvider);
                path.pop();
            }
        }
    }

    public static BottomUpIndex of(Model model) {
        return model.getKnowledge(BottomUpIndex.class, BottomUpIndex::new);
    }

    /**
     * Gets all of the parents of an operation or resource within a service.
     *
     * <p>The returned list contains each {@link EntityShape} parent (a resource or
     * service shape). The first element in the list is the direct parent of the
     * resource/operation, followed by that resource's parent, until finally the last
     * element in the list is always a {@code ServiceShape}.
     *
     * @param service Service to query.
     * @param operationOrResource The operation or resource for which to find the parents.
     * @return Returns the parents of the resource.
     */
    public List<EntityShape> getAllParents(ToShapeId service, ToShapeId operationOrResource) {
        Map<ShapeId, List<EntityShape>> serviceBindings = parentBindings.getOrDefault(
                service.toShapeId(),
                Collections.emptyMap());
        List<EntityShape> entities = serviceBindings.get(operationOrResource.toShapeId());
        return entities == null ? Collections.emptyList() : Collections.unmodifiableList(entities);
    }

    /**
     * Gets the direct parent of an operation or resource.
     *
     * @param service Service closure to query.
     * @param operationOrResource The operation or resource to query.
     * @return Returns the optionally found parent, a resource or service shape.
     */
    public Optional<EntityShape> getEntityBinding(ToShapeId service, ToShapeId operationOrResource) {
        List<EntityShape> entities = getAllParents(service, operationOrResource);
        return entities.isEmpty() ? Optional.empty() : Optional.of(entities.get(0));
    }

    /**
     * Gets the direct parent resource of an operation or resource if and only if
     * the given resource or operation is bound to a resource and not bound to a
     * service.
     *
     * @param service Service closure to query.
     * @param operationOrResource The operation or resource to query.
     * @return Returns the optionally found parent resource.
     */
    public Optional<ResourceShape> getResourceBinding(ToShapeId service, ToShapeId operationOrResource) {
        return getEntityBinding(service, operationOrResource)
                .filter(ResourceShape.class::isInstance)
                .map(ResourceShape.class::cast);
    }
}
