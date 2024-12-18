/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform.plugins;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.AuthDefinitionTrait;
import software.amazon.smithy.model.traits.ProtocolDefinitionTrait;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.model.transform.ModelTransformerPlugin;

/**
 * Removes traits from {@link AuthDefinitionTrait} and
 * {@link ProtocolDefinitionTrait} traits that refer to removed shapes.
 */
public final class CleanTraitDefinitions implements ModelTransformerPlugin {
    @Override
    public Model onRemove(ModelTransformer transformer, Collection<Shape> removed, Model model) {
        Set<ShapeId> removedShapeIds = removed.stream().map(Shape::getId).collect(Collectors.toSet());
        model = transformer.replaceShapes(model, getAuthDefShapesToReplace(model, removedShapeIds));

        return transformer.replaceShapes(model, getProtocolDefShapesToReplace(model, removedShapeIds));
    }

    private Set<Shape> getAuthDefShapesToReplace(Model model, Set<ShapeId> removedShapeIds) {
        Set<Shape> shapes = new HashSet<>();
        for (StructureShape structure : model.getStructureShapesWithTrait(AuthDefinitionTrait.class)) {
            AuthDefinitionTrait authDefTrait = structure.expectTrait(AuthDefinitionTrait.class);
            List<ShapeId> traits = authDefTrait.getTraits();
            List<ShapeId> newTraits = excludeTraitsInSet(traits, removedShapeIds);
            // Return early if re-built list of traits is the same as existing list.
            if (!traits.equals(newTraits)) {
                // If the list of traits on the AuthDefinitionTrait has changed due to a trait shape being
                // removed from the model, return a new version of the shape with a new version of the trait.
                shapes.add(structure.toBuilder()
                        .addTrait(authDefTrait.toBuilder().traits(newTraits).build())
                        .build());
            }
        }
        return shapes;
    }

    private Set<Shape> getProtocolDefShapesToReplace(Model model, Set<ShapeId> removedShapeIds) {
        Set<Shape> shapes = new HashSet<>();
        for (StructureShape structure : model.getStructureShapesWithTrait(ProtocolDefinitionTrait.class)) {
            ProtocolDefinitionTrait protocolDefinitionTrait = structure.expectTrait(ProtocolDefinitionTrait.class);
            List<ShapeId> traits = protocolDefinitionTrait.getTraits();
            List<ShapeId> newTraits = excludeTraitsInSet(traits, removedShapeIds);

            // Return early if re-built list of traits is the same as existing list.
            if (!traits.equals(newTraits)) {
                // If the list of traits on the ProtocolDefinitionTrait has changed due to a trait shape
                // being removed from the model, return a new version of the shape with a new version of
                // the trait.
                shapes.add(structure.toBuilder()
                        .addTrait(protocolDefinitionTrait.toBuilder().traits(newTraits).build())
                        .build());
            }
        }
        return shapes;
    }

    private List<ShapeId> excludeTraitsInSet(List<ShapeId> traits, Set<ShapeId> shapeIds) {
        return traits.stream()
                .filter(trait -> !shapeIds.contains(trait))
                .collect(Collectors.toList());
    }
}
