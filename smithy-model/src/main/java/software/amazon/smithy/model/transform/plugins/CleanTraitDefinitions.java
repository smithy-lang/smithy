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

package software.amazon.smithy.model.transform.plugins;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.AuthDefinitionTrait;
import software.amazon.smithy.model.traits.ProtocolDefinitionTrait;
import software.amazon.smithy.model.traits.Trait;
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
        return model.shapes(StructureShape.class)
                .flatMap(s -> Trait.flatMapStream(s, AuthDefinitionTrait.class))
                .flatMap(pair -> {
                    AuthDefinitionTrait authDefTrait = pair.getRight();
                    List<ShapeId> traits = authDefTrait.getTraits();
                    List<ShapeId> newTraits = excludeTraitsInSet(traits, removedShapeIds);

                    // Return early if re-built list of traits is the same as existing list.
                    if (traits.equals(newTraits)) {
                        return Stream.empty();
                    }

                    return rebuildShapeWithAuthDef(pair.getLeft(), authDefTrait.getSourceLocation(), newTraits);
                })
                .collect(Collectors.toSet());
    }

    private Stream<Shape> rebuildShapeWithAuthDef(
            StructureShape shape,
            SourceLocation location,
            List<ShapeId> traits
    ) {
        AuthDefinitionTrait.Builder traitBuilder = AuthDefinitionTrait.builder();
        traitBuilder.sourceLocation(location);
        traits.forEach(traitBuilder::addTrait);

        return Stream.of(shape.toBuilder().addTrait(traitBuilder.build()).build());
    }

    private Set<Shape> getProtocolDefShapesToReplace(Model model, Set<ShapeId> removedShapeIds) {
        return model.shapes(StructureShape.class)
                .flatMap(s -> Trait.flatMapStream(s, ProtocolDefinitionTrait.class))
                .flatMap(pair -> {
                    ProtocolDefinitionTrait protocolDefinitionTrait = pair.getRight();
                    List<ShapeId> traits = protocolDefinitionTrait.getTraits();
                    List<ShapeId> newTraits = excludeTraitsInSet(traits, removedShapeIds);

                    // Return early if re-built list of traits is the same as existing list.
                    if (traits.equals(newTraits)) {
                        return Stream.empty();
                    }

                    return rebuildShapeWithProtocolDef(
                            pair.getLeft(),
                            protocolDefinitionTrait.getSourceLocation(),
                            newTraits);
                })
                .collect(Collectors.toSet());
    }

    private Stream<Shape> rebuildShapeWithProtocolDef(
            StructureShape shape,
            SourceLocation location,
            List<ShapeId> traits
    ) {
        ProtocolDefinitionTrait.Builder traitBuilder = ProtocolDefinitionTrait.builder();
        traitBuilder.sourceLocation(location);
        traits.forEach(traitBuilder::addTrait);

        return Stream.of(shape.toBuilder().addTrait(traitBuilder.build()).build());
    }

    private List<ShapeId> excludeTraitsInSet(List<ShapeId> traits, Set<ShapeId> shapeIds) {
        return traits.stream()
                .filter(trait -> !shapeIds.contains(trait))
                .collect(Collectors.toList());
    }
}
