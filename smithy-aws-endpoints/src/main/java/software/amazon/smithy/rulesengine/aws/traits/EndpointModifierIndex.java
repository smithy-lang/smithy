/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.aws.traits;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.logging.Logger;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.KnowledgeIndex;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.Trait;

/**
 * Locates the endpoint modifier traits applied to services.
 *
 * Endpoint modifier traits are traits that are marked by {@link EndpointModifierTrait}
 */
public final class EndpointModifierIndex implements KnowledgeIndex {
    private static final Logger LOGGER = Logger.getLogger(EndpointModifierIndex.class.getName());

    private final Map<ShapeId, Map<ShapeId, Trait>> endpointModifierTraits = new HashMap<>();

    public EndpointModifierIndex(Model model) {
        for (ServiceShape serviceShape : model.getServiceShapes()) {
            Map<ShapeId, Trait> result = new TreeMap<>();
            for (Trait trait : serviceShape.getAllTraits().values()) {
                Optional<Shape> traitShape = model.getShape(trait.toShapeId());
                if (!traitShape.isPresent()) {
                    LOGGER.warning(String.format(
                            "`%s` trait found in service `%s`, but the trait definition is missing",
                            trait.toShapeId(),
                            serviceShape.toShapeId()));
                    continue;
                }
                if (traitShape.get().hasTrait(EndpointModifierTrait.ID)) {
                    result.put(trait.toShapeId(), trait);
                }
            }
            endpointModifierTraits.put(serviceShape.toShapeId(), result);
        }
    }

    public static EndpointModifierIndex of(Model model) {
        return model.getKnowledge(EndpointModifierIndex.class, EndpointModifierIndex::new);
    }

    /**
     * Gets all endpoint modifier traits applied to a service.
     *
     * @param toShapeId Service shape to query
     * @return Map of endpoint modifier trait ID to the trait
     */
    public Map<ShapeId, Trait> getEndpointModifierTraits(ToShapeId toShapeId) {
        return endpointModifierTraits.get(toShapeId.toShapeId());
    }
}
