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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.neighbor.RelationshipType;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.utils.SetUtils;

/**
 * Provides top-down access to all resources and operations contained within a
 * service or resource closure.
 */
public final class TopDownIndex implements KnowledgeIndex {
    private final Map<ShapeId, Set<ResourceShape>> resources = new HashMap<>();
    private final Map<ShapeId, Set<OperationShape>> operations = new HashMap<>();

    public TopDownIndex(Model model) {
        NeighborProvider provider = NeighborProviderIndex.of(model).getProvider();
        Walker walker = new Walker(provider);

        // Only traverse resource and operation bindings.
        Predicate<Relationship> filter = rel -> {
            RelationshipType type = rel.getRelationshipType();
            return type == RelationshipType.RESOURCE || type.isOperationBinding();
        };

        for (ResourceShape resource : model.getResourceShapes()) {
            findContained(resource.getId(), walker.walkShapes(resource, filter));
        }

        for (ServiceShape service : model.getServiceShapes()) {
            findContained(service.getId(), walker.walkShapes(service, filter));
        }
    }

    public static TopDownIndex of(Model model) {
        return model.getKnowledge(TopDownIndex.class, TopDownIndex::new);
    }

    private void findContained(ShapeId container, Collection<Shape> shapes) {
        Set<ResourceShape> containedResources = new TreeSet<>();
        Set<OperationShape> containedOperations = new TreeSet<>();

        for (Shape shape : shapes) {
            if (!shape.getId().equals(container)) {
                if (shape instanceof ResourceShape) {
                    containedResources.add((ResourceShape) shape);
                } else if (shape instanceof OperationShape) {
                    containedOperations.add((OperationShape) shape);
                }
            }
        }

        operations.put(container, Collections.unmodifiableSet(containedOperations));
        resources.put(container, Collections.unmodifiableSet(containedResources));
    }

    /**
     * Get all operations in service or resource closure.
     *
     * @param entity Service or resource shape ID.
     * @return Returns all operations in the service closure.
     */
    public Set<OperationShape> getContainedOperations(ToShapeId entity) {
        return operations.getOrDefault(entity.toShapeId(), SetUtils.of());
    }

    /**
     * Get all resources in a service or resource closure.
     *
     * @param entity Service or resource shape ID.
     * @return Returns all resources in the service closure.
     */
    public Set<ResourceShape> getContainedResources(ToShapeId entity) {
        return resources.getOrDefault(entity.toShapeId(), SetUtils.of());
    }
}
