/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.traits;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.KnowledgeIndex;
import software.amazon.smithy.model.shapes.EntityShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.MapUtils;

/**
 * Computes the API Gateway integration for each operation,
 * resource, and service shape in a model.
 */
public final class IntegrationTraitIndex implements KnowledgeIndex {
    private final Map<ShapeId, Map<ShapeId, Trait>> traits = new HashMap<>();

    public IntegrationTraitIndex(Model model) {
        model.shapes(ServiceShape.class).forEach(service -> {
            Map<ShapeId, Trait> serviceMap = new HashMap<>();
            traits.put(service.getId(), serviceMap);
            walk(model, service.getId(), service, null);
        });
    }

    public static IntegrationTraitIndex of(Model model) {
        return model.getKnowledge(IntegrationTraitIndex.class, IntegrationTraitIndex::new);
    }

    /**
     * Get the integration trait for a particular operation, resource, or
     * service bound within a service.
     *
     * @param service Service shape or ShapeId thereof.
     * @param shape Operation, service, or resource shape in the service.
     *  When the service shape ID is provided, the integration trait of the
     *  service is returned if present.
     *
     * @return The integration trait or an empty optional if none set
     */
    public Optional<Trait> getIntegrationTrait(ToShapeId service, ToShapeId shape) {
        return Optional.ofNullable(traits.getOrDefault(service.toShapeId(), MapUtils.of())
                .get(shape.toShapeId()));
    }

    /**
     * Get the integration trait for a particular operation, resource, or
     * service bound within a service of a specific type.
     *
     * @param service Service shape or ShapeId thereof.
     * @param shape Operation, service, or resource shape in the service.
     *  When the service shape ID is provided, the integration trait of the
     *  service is returned if present.
     * @param type Integration trait type.
     * @param <T> Type of trait to retrieve.
     *
     * @return The integration trait or an empty optional if none set or
     *  if not of the expected type.
     */
    public <T extends Trait> Optional<T> getIntegrationTrait(ToShapeId service, ToShapeId shape, Class<T> type) {
        return getIntegrationTrait(service, shape).filter(type::isInstance).map(type::cast);
    }

    private void walk(Model model, ShapeId service, EntityShape current, Trait trait) {
        Trait updatedTrait = extractTrait(current, trait);
        Map<ShapeId, Trait> serviceMapping = traits.get(service);
        serviceMapping.put(current.getId(), updatedTrait);

        for (ShapeId resource : current.getResources()) {
            model.getShape(resource)
                    .flatMap(Shape::asResourceShape)
                    .ifPresent(resourceShape -> walk(model, service, resourceShape, updatedTrait));
        }

        for (ShapeId operation : current.getAllOperations()) {
            model.getShape(operation).ifPresent(op -> serviceMapping.put(operation, extractTrait(op, updatedTrait)));
        }
    }

    private static Trait extractTrait(Shape shape, Trait defaultValue) {
        return shape.getTrait(MockIntegrationTrait.class)
                .map(trait -> (Trait) trait)
                .orElseGet(() -> shape.getTrait(IntegrationTrait.class)
                        .map(trait -> (Trait) trait)
                        .orElse(defaultValue));
    }
}
