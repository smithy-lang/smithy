/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.directed;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

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
    private volatile Map<ShapeId, Shape> connectedShapes;
    private volatile Set<OperationShape> containedOperations;

    Directive(Model model, S settings, ServiceShape service) {
        this.model = model;
        this.settings = settings;
        this.service = service;
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
     * @return Gets the service being generated.
     */
    public final ServiceShape service() {
        return service;
    }

    /**
     * @return Returns a map of shapes connected to the service.
     */
    public final Map<ShapeId, Shape> connectedShapes() {
        Map<ShapeId, Shape> result = connectedShapes;
        if (result == null) {
            result = new TreeMap<>();
            Iterator<Shape> iterator = new Walker(model).iterateShapes(service);
            while (iterator.hasNext()) {
                Shape next = iterator.next();
                result.put(next.getId(), next);
            }
            connectedShapes = result;
        }
        return result;
    }

    /**
     * Gets a set of all operation shapes in the service, sorted by name.
     *
     * <p>This includes operations contained in resources in the closure of the service.
     *
     * @return Returns all sorted service operations.
     */
    public Set<OperationShape> operations() {
        Set<OperationShape> result = containedOperations;
        if (result == null) {
            result = containedOperations = TopDownIndex.of(model()).getContainedOperations(service());
        }
        return result;
    }
}
