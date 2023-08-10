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

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.AuthDefinitionTrait;
import software.amazon.smithy.model.traits.AuthTrait;
import software.amazon.smithy.model.traits.ProtocolDefinitionTrait;
import software.amazon.smithy.model.traits.Trait;

/**
 * An index that resolves service protocols and auth schemes.
 *
 * <p>This index can be used to get all of the protocol traits applied to
 * a service, to get all of the auth defining traits applied to a service,
 * to get the effective authentication schemes of a service, and to get the
 * effective authentication schemes of an operation bound within the
 * closure of a service.
 */
public final class ServiceIndex implements KnowledgeIndex {

    private final WeakReference<Model> model;
    private final Set<ShapeId> protocolTraits = new HashSet<>();
    private final Set<ShapeId> authTraits = new HashSet<>();

    public ServiceIndex(Model model) {
        this.model = new WeakReference<>(model);

        for (Shape shape : model.getShapesWithTrait(ProtocolDefinitionTrait.class)) {
            protocolTraits.add(shape.getId());
        }

        for (Shape shape : model.getShapesWithTrait(AuthDefinitionTrait.class)) {
            authTraits.add(shape.getId());
        }
    }

    public static ServiceIndex of(Model model) {
        return model.getKnowledge(ServiceIndex.class, ServiceIndex::new);
    }

    /**
     * Get all protocol traits attached to a service.
     *
     * <p>A <em>protocol trait</em> is a trait that is marked with
     * the {@code smithy.api#protocolDefinition} trait.
     *
     * <p>An empty map is returned if {@code service} cannot be found in the
     * model or is not a service shape.
     *
     * @param service Service to get the protocols of.
     * @return Returns a map of the protocol trait ID to the trait.
     */
    public Map<ShapeId, Trait> getProtocols(ToShapeId service) {
        return getTraitMapInSet(service, protocolTraits);
    }

    private Map<ShapeId, Trait> getTraitMapInSet(ToShapeId service, Set<ShapeId> haystack) {
        return getModel()
                .getShape(service.toShapeId())
                .flatMap(Shape::asServiceShape)
                .map(shape -> {
                    Map<ShapeId, Trait> result = new TreeMap<>();
                    for (Trait trait : shape.getAllTraits().values()) {
                        if (haystack.contains(trait.toShapeId())) {
                            result.put(trait.toShapeId(), trait);
                        }
                    }
                    return result;
                })
                .orElse(Collections.emptyMap());
    }

    private Model getModel() {
        return Objects.requireNonNull(model.get(), "The dereferenced WeakReference<Model> is null");
    }

    /**
     * Get all auth defining traits attached to a service or operation.
     *
     * <p>An <em>auth defining trait</em> is a trait that is marked with
     * the {@code smithy.api#authDefinition} trait.
     *
     * <p>An empty map is returned if {@code id} cannot be found in the
     * model or is not a service shape.
     *
     * @param service Service to get the auth schemes of.
     * @return Returns a map of the trait shape ID to the auth trait itself.
     */
    public Map<ShapeId, Trait> getAuthSchemes(ToShapeId service) {
        return getTraitMapInSet(service, authTraits);
    }

    /**
     * Gets a list of effective authentication schemes applied to a service.
     *
     * <p>An <em>effective authentication</em> scheme is derived from the
     * {@code smithy.api#auth} trait and the auth defining traits applied
     * to a service. If the service has the {@code smithy.api#auth} trait,
     * then a map is returned that contains the traits applied to the service
     * that matches the values in the auth trait. If no auth trait is applied,
     * then all of the auth defining traits on the service are returned.
     *
     * <p>The returned map is provided in the same order as the values in the
     * {@code auth} trait if an auth trait is present, otherwise the result
     * returned is ordered alphabetically by absolute shape id.
     *
     * <p>An empty map is returned if {@code service} cannot be found in the
     * model or is not a service shape.
     *
     * @param service Service to get the effective authentication schemes of.
     * @return Returns a map of the trait shape ID to the auth trait itself.
     */
    public Map<ShapeId, Trait> getEffectiveAuthSchemes(ToShapeId service) {
        return getModel()
                .getShape(service.toShapeId())
                .flatMap(Shape::asServiceShape)
                .map(shape -> {
                    Map<ShapeId, Trait> result = getAuthTraitValues(shape, shape);
                    if (result == null) {
                        result = new TreeMap<>();
                        for (Map.Entry<ShapeId, Trait> traitEntry : shape.getAllTraits().entrySet()) {
                            if (authTraits.contains(traitEntry.getKey())) {
                                result.put(traitEntry.getKey(), traitEntry.getValue());
                            }
                        }
                    }
                    return result;
                })
                .orElse(Collections.emptyMap());
    }

    /**
     * Gets a list of effective authentication schemes applied to an operation
     * bound within a service.
     *
     * <p>If the given operation defines that {@code smithy.api#auth} trait,
     * then a map is returned that consists of the traits applied to the
     * service that match the values of the {@code smithy.api#auth} trait. If
     * the operation does not define an {@code smithy.api#auth} trait, then
     * the effective auth schemes of the service is returned (that is, the
     * return value of {@link #getEffectiveAuthSchemes(ToShapeId)}).
     *
     * <p>The returned map is provided in the same order as the values in the
     * {@code auth} trait if an auth trait is present, otherwise the result
     * returned is ordered alphabetically by absolute shape id.
     *
     * <p>An empty map is returned if {@code service} shape cannot be found
     * in the model or is not a service shape. An empty map is returned if
     * {@code operation} cannot be found in the model or is not an operation
     * shape.
     *
     * @param service Service the operation is within.
     * @param operation Operation to get the effective authentication schemes of.
     * @return Returns a map of the trait shape ID to the auth trait itself.
     */
    public Map<ShapeId, Trait> getEffectiveAuthSchemes(ToShapeId service, ToShapeId operation) {
        Shape serviceShape = getModel()
                .getShape(service.toShapeId())
                .flatMap(Shape::asServiceShape)
                .orElse(null);

        if (serviceShape == null) {
            return Collections.emptyMap();
        }

        return getModel()
                .getShape(operation.toShapeId())
                .flatMap(Shape::asOperationShape)
                .map(operationShape -> {
                    Map<ShapeId, Trait> result = getAuthTraitValues(serviceShape, operationShape);
                    return result != null ? result : getEffectiveAuthSchemes(service);
                })
                .orElse(Collections.emptyMap());
    }

    private Map<ShapeId, Trait> getAuthTraitValues(Shape service, Shape subject) {
        if (!subject.hasTrait(AuthTrait.class)) {
            return null;
        }

        AuthTrait authTrait = subject.expectTrait(AuthTrait.class);
        Map<ShapeId, Trait> result = new LinkedHashMap<>();
        for (ShapeId value : authTrait.getValueSet()) {
            service.findTrait(value).ifPresent(trait -> result.put(value, trait));
        }

        return result;
    }
}
