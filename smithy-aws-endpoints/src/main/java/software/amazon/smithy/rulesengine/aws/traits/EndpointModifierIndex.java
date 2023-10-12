/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.aws.traits;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
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

    private final Map<ShapeId, Map<ShapeId, Trait>> endpointModifierTraits = new HashMap<>();

    public EndpointModifierIndex(Model model) {
        for (ServiceShape serviceShape : model.getServiceShapes()) {
            Map<ShapeId, Trait> result = new TreeMap<>();
            for (Trait trait : serviceShape.getAllTraits().values()) {
                Shape traitShape = model.getShape(trait.toShapeId()).get();
                if (traitShape.hasTrait(EndpointModifierTrait.ID)) {
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
        return endpointModifierTraits.get(toShapeId);
    }
}
