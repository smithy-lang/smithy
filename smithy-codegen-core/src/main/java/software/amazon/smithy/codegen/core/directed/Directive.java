/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.directed;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import software.amazon.smithy.codegen.core.CodegenException;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.ShapeClosureIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SetUtils;

/**
 * Directive classes contain all of the context needed in order to perform
 * the tasks defined in a {@link DirectedCodegen} implementation.
 *
 * @param <S> Settings object used to configure code generation.
 */
public abstract class Directive<S> {

    private final Model model;
    private final S settings;
    private final ServiceShape service;
    private final String shapeClosureId;
    private final boolean generateDataShapesOnly;
    private volatile Map<ShapeId, Shape> connectedShapes;
    private volatile Set<OperationShape> containedOperations;

    Directive(Model model, S settings, ServiceShape service, String shapeClosureId, boolean generateDataShapesOnly) {
        this.model = model;
        this.settings = settings;
        this.service = service;
        this.shapeClosureId = shapeClosureId;
        this.generateDataShapesOnly = generateDataShapesOnly;
    }

    /**
     * @return Gets the model being code generated.
     */
    public final Model model() {
        return model;
    }

    /**
     * @return Gets code generation settings.
     */
    public final S settings() {
        return settings;
    }

    /**
     * Gets the service being generated.
     *
     * @return Gets the service being generated.
     * @throws CodegenException if there is no service because code generation is
     *  driven by a shape closure rather than a service.
     * @deprecated Use {@link #getService()} to safely handle the absence of a
     *  service when code generation is driven by a shape closure.
     */
    @Deprecated
    public final ServiceShape service() {
        if (service == null) {
            throw new CodegenException(
                    "Attempted to get a service from a directive that is generating the shape closure '"
                            + shapeClosureId + "'. Use getService() to safely handle the absence of a service.");
        }
        return service;
    }

    /**
     * Gets the service being generated, if code generation is driven by a service.
     *
     * @return Returns the optional service being generated.
     */
    public final Optional<ServiceShape> getService() {
        return Optional.ofNullable(service);
    }

    /**
     * Gets the ID of the shape closure being generated, if code generation is
     * driven by a shape closure.
     *
     * @return Returns the optional shape closure ID being generated.
     */
    public final Optional<String> getShapeClosureId() {
        return Optional.ofNullable(shapeClosureId);
    }

    /**
     * Returns a map of the shapes being generated.
     *
     * <p>When driven by a service, these are the shapes connected to the service.
     * When driven by a shape closure, these are the shapes in that closure. When only
     * data shapes are being generated, service, resource, and operation shapes are
     * excluded since they are not generated.
     *
     * @return Returns a map of shapes being generated.
     */
    public final Map<ShapeId, Shape> connectedShapes() {
        Map<ShapeId, Shape> result = connectedShapes;
        if (result == null) {
            result = new TreeMap<>();
            for (Shape next : resolveShapes()) {
                result.put(next.getId(), next);
            }
            result = MapUtils.orderedCopyOf(result);
            connectedShapes = result;
        }
        return result;
    }

    private Set<Shape> resolveShapes() {
        Set<Shape> result = new LinkedHashSet<>();
        Iterator<Shape> iterator;
        if (service != null) {
            iterator = new Walker(model).iterateShapes(service);
        } else {
            iterator = ShapeClosureIndex.of(model).getShapesInClosure(shapeClosureId).iterator();
        }

        while (iterator.hasNext()) {
            Shape next = iterator.next();
            // A shape closure may include prelude shapes reached through members, but those
            // are not generated. This matches how a service closure is resolved for codegen.
            if (service == null && Prelude.isPreludeShape(next)) {
                continue;
            }

            // Service, resource, and operation shapes are not generated when generating only
            // data shapes, so they are not part of the generated set.
            if (generateDataShapesOnly && next.getType().getCategory() == ShapeType.Category.SERVICE) {
                continue;
            }
            result.add(next);
        }

        return result;
    }

    /**
     * Gets a set of all operation shapes being generated, sorted by name.
     *
     * <p>When driven by a service, this includes operations contained in resources
     * in the closure of the service. When driven by a shape closure, this is the
     * set of operation shapes in the closure. When only data shapes are being
     * generated, no operations are generated and this is always empty.
     *
     * @return Returns all sorted operations being generated.
     */
    public Set<OperationShape> operations() {
        Set<OperationShape> result = containedOperations;
        if (result == null) {
            if (generateDataShapesOnly) {
                result = Collections.emptySet();
            } else if (service != null) {
                result = TopDownIndex.of(model()).getContainedOperations(service);
            } else {
                result = new LinkedHashSet<>();
                for (Shape shape : connectedShapes().values()) {
                    shape.asOperationShape().ifPresent(result::add);
                }
                result = SetUtils.orderedCopyOf(result);
            }
            containedOperations = result;
        }
        return result;
    }
}
