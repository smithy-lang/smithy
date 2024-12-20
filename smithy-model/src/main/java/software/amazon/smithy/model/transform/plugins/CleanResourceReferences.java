/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform.plugins;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.ReferencesTrait;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.model.transform.ModelTransformerPlugin;

/**
 * Removes references to resources that are removed from
 * {@link ReferencesTrait}s.
 */
public final class CleanResourceReferences implements ModelTransformerPlugin {
    @Override
    public Model onRemove(ModelTransformer transformer, Collection<Shape> shapes, Model model) {
        Set<Shape> toReplace = new HashSet<>();
        shapes.forEach(shape -> toReplace.addAll(getAffectedStructures(model, shape)));
        return transformer.replaceShapes(model, toReplace);
    }

    private Set<Shape> getAffectedStructures(Model model, Shape resource) {
        Set<Shape> result = new HashSet<>();

        for (StructureShape struct : model.getStructureShapesWithTrait(ReferencesTrait.class)) {
            // References can also be on strings, but we only care about structs.
            ReferencesTrait trait = struct.expectTrait(ReferencesTrait.class);
            // Get the reference to a particular shape.
            List<ReferencesTrait.Reference> references = trait.getResourceReferences(resource.getId());

            if (!references.isEmpty()) {
                // If the trait contains a reference to the resource, then create a new version of the
                // trait and shape that no longer reference the resource.
                ReferencesTrait.Builder traitBuilder = trait.toBuilder();
                traitBuilder.clearReferences();

                for (ReferencesTrait.Reference ref : trait.getReferences()) {
                    if (!references.contains(ref)) {
                        traitBuilder.addReference(ref);
                    }
                }

                result.add(struct.toBuilder().addTrait(traitBuilder.build()).build());
            }
        }

        return result;
    }
}
