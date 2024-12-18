/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits;

import java.util.HashMap;
import java.util.Map;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.KnowledgeIndex;
import software.amazon.smithy.model.selector.PathFinder;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ToShapeId;

/**
 * Determines if a service, resource, or operation are considered
 * part of the data plane or control plane.
 *
 * <p>The plane is inherited from the top-down and can be overridden
 * per shape. For example, if a service shape has the
 * {@code aws.api#controlPlane} shape, then every shape within the closure
 * of the service inherits this property. If a resource shape defines a
 * {@code aws.api#controlPlane} or {@code aws.api#dataPlane} trait, then all
 * shapes within the closure of the resource inherit it. If an operation is
 * marked with the {@code aws.api#dataPlane} trait, it overrides any plane
 * traits of the service or resource its bound within.
 */
public final class PlaneIndex implements KnowledgeIndex {
    private static final Selector SELECTOR = Selector.parse(":test(operation, resource)");

    private final Map<ShapeId, Plane> servicePlanes = new HashMap<>();
    private final PathFinder pathFinder;

    private enum Plane {
        CONTROL, DATA
    }

    public PlaneIndex(Model model) {
        pathFinder = PathFinder.create(model);

        model.shapes(ServiceShape.class).forEach(service -> {
            Plane plane = extractPlane(service);
            if (plane != null) {
                servicePlanes.put(service.getId(), plane);
            }
        });
    }

    public static PlaneIndex of(Model model) {
        return model.getKnowledge(PlaneIndex.class, PlaneIndex::new);
    }

    /**
     * Checks if the given service shape is part of the control plane.
     *
     * @param service Service to check.
     * @return Returns true if the service is part of the control plane.
     */
    public boolean isControlPlane(ToShapeId service) {
        return servicePlanes.getOrDefault(service.toShapeId(), null) == Plane.CONTROL;
    }

    /**
     * Checks if the given shape within a service is part of the control plane.
     *
     * @param service Service to check.
     * @param operationOrResource Operation or resource within the service to check.
     * @return Returns true if the shape is part of the control plane.
     */
    public boolean isControlPlane(ToShapeId service, ToShapeId operationOrResource) {
        return resolvePlane(service, operationOrResource.toShapeId()) == Plane.CONTROL;
    }

    /**
     * Checks if the given service shape is part of the data plane.
     *
     * @param service Service to check.
     * @return Returns true if the service is part of the data plane.
     */
    public boolean isDataPlane(ToShapeId service) {
        return servicePlanes.getOrDefault(service.toShapeId(), null) == Plane.DATA;
    }

    /**
     * Checks if the given shape within a service is part of the data plane.
     *
     * @param service Service to check.
     * @param operationOrResource Operation or resource within the service to check.
     * @return Returns true if the shape is part of the data plane.
     */
    public boolean isDataPlane(ToShapeId service, ToShapeId operationOrResource) {
        return resolvePlane(service, operationOrResource.toShapeId()) == Plane.DATA;
    }

    /**
     * Checks if the given service shape has defined its plane.
     *
     * @param service Service to check.
     * @return Returns true if the service has defined its plane.
     */
    public boolean isPlaneDefined(ToShapeId service) {
        return servicePlanes.containsKey(service.toShapeId());
    }

    /**
     * Checks if the given shape within a service has a resolvable plane.
     *
     * @param service Service to check.
     * @param operationOrResource Operation or resource within the service to check.
     * @return Returns true if the shape has a resolvable plane.
     */
    public boolean isPlaneDefined(ToShapeId service, ToShapeId operationOrResource) {
        return resolvePlane(service, operationOrResource.toShapeId()) != null;
    }

    private Plane resolvePlane(ToShapeId service, ShapeId operationOrResource) {
        Plane result = null;

        for (PathFinder.Path path : pathFinder.search(service, SELECTOR)) {
            for (Shape shape : path.getShapes()) {
                Plane nextPlane = extractPlane(shape);
                if (nextPlane != null) {
                    result = nextPlane;
                }
                if (shape.getId().equals(operationOrResource)) {
                    return result;
                }
            }
        }

        return null;
    }

    private Plane extractPlane(Shape shape) {
        if (shape.hasTrait(ControlPlaneTrait.class)) {
            return Plane.CONTROL;
        } else if (shape.hasTrait(DataPlaneTrait.class)) {
            return Plane.DATA;
        } else {
            return null;
        }
    }
}
