/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.knowledge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.selector.PathFinder;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.EntityShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ToShapeId;

/**
 * Computes all of the parent shapes of resources and operations from the bottom-up.
 */
public final class BottomUpIndex implements KnowledgeIndex {
    private static final Selector SELECTOR = Selector.parse(":is(resource, operation)");
    private final Map<ShapeId, Map<ShapeId, List<EntityShape>>> parentBindings = new HashMap<>();

    public BottomUpIndex(Model model) {
        PathFinder pathFinder = PathFinder.create(model);

        for (ServiceShape service : model.getServiceShapes()) {
            Map<ShapeId, List<EntityShape>> serviceBindings = new HashMap<>();
            parentBindings.put(service.getId(), serviceBindings);
            for (PathFinder.Path path : pathFinder.search(service, SELECTOR)) {
                List<EntityShape> shapes = new ArrayList<>();
                // Get all of the path elements other than the last one in reverse order.
                for (int i = path.size() - 1; i >= 0; i--) {
                    Relationship rel = path.get(i);
                    // This should always be an EntityShape, but just in case new relationships are added...
                    if (rel.getShape() instanceof EntityShape) {
                        shapes.add((EntityShape) rel.getShape());
                    }
                }
                // Add the end shape (a resource or operation) to the service bindings.
                serviceBindings.put(path.getEndShape().getId(), shapes);
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
                service.toShapeId(), Collections.emptyMap());
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
