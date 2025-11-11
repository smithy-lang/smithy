/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.knowledge;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
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

/**
 * Provides top-down access to all resources and operations contained within a
 * service or resource closure.
 */
public final class TopDownIndex implements KnowledgeIndex {
    private final Map<ShapeId, Set<ResourceShape>> resources = new HashMap<>();
    private final Map<ShapeId, Set<OperationShape>> operations = new HashMap<>();
    private final Map<ShapeId, Set<ResourceShape>> sortedResources = new HashMap<>();
    private final Map<ShapeId, Set<OperationShape>> sortedOperations = new HashMap<>();

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
        Set<ResourceShape> containedResources = new LinkedHashSet<>();
        Set<OperationShape> containedOperations = new LinkedHashSet<>();

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

        // Store sorted copies of the sets separately. These *could* just be produced on the fly
        // by wrapping the un-sorted set when getContainedX is called, but that defeats the
        // spirit of knowledge indexes - all the work should be done up front and not repeated.
        sortedOperations.put(container, Collections.unmodifiableSortedSet(new TreeSet<>(containedOperations)));
        sortedResources.put(container, Collections.unmodifiableSortedSet(new TreeSet<>(containedResources)));
    }

    /**
     * Get all operations in service or resource closure.
     *
     * <p>Operations returned are be sorted by id.
     *
     * @param entity Service or resource shape ID.
     * @return Returns all operations in the service closure.
     */
    public Set<OperationShape> getContainedOperations(ToShapeId entity) {
        return getContainedOperations(entity, true);
    }

    /**
     * Get all operations in service or resource closure.
     *
     * @param entity Service or resource shape ID.
     * @param sorted Whether to return the operations sorted by id.
     * @return Returns all operations in the service closure.
     */
    public Set<OperationShape> getContainedOperations(ToShapeId entity, boolean sorted) {
        if (sorted) {
            return sortedOperations.getOrDefault(entity.toShapeId(), Collections.emptySet());
        } else {
            return operations.getOrDefault(entity.toShapeId(), Collections.emptySet());
        }
    }

    /**
     * Get all resources in a service or resource closure.
     *
     * <p>Resources returned are be sorted by id.
     *
     * @param entity Service or resource shape ID.
     * @return Returns all resources in the service closure.
     */
    public Set<ResourceShape> getContainedResources(ToShapeId entity) {
        return getContainedResources(entity, true);
    }

    /**
     * Get all resources in a service or resource closure.
     *
     * @param entity Service or resource shape ID.
     * @param sorted Whether to return the resources sorted by id.
     * @return Returns all resources in the service closure.
     */
    public Set<ResourceShape> getContainedResources(ToShapeId entity, boolean sorted) {
        if (sorted) {
            return sortedResources.getOrDefault(entity.toShapeId(), Collections.emptySet());
        } else {
            return resources.getOrDefault(entity.toShapeId(), Collections.emptySet());
        }
    }
}
