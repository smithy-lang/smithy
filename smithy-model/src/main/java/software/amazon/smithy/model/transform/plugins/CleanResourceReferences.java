/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.ReferencesTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.model.transform.ModelTransformerPlugin;
import software.amazon.smithy.utils.FunctionalUtils;

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
        return model.shapes(StructureShape.class)
                .flatMap(s -> Trait.flatMapStream(s, ReferencesTrait.class))
                .flatMap(pair -> {
                    // Subject is the structure shape that might be modified.
                    StructureShape subject = pair.getLeft();
                    ReferencesTrait trait = pair.getRight();
                    // Get the reference to a particular shape.
                    List<ReferencesTrait.Reference> references = trait.getResourceReferences(resource.getId());

                    if (references.isEmpty()) {
                        return Stream.empty();
                    }

                    // If the trait contains a reference to the resource, then create a new version of the
                    // trait and shape that no longer reference the resource.
                    ReferencesTrait.Builder traitBuilder = trait.toBuilder().clearReferences();
                    trait.getReferences().stream()
                            .filter(FunctionalUtils.not(references::contains))
                            .forEach(traitBuilder::addReference);
                    return Stream.of(subject.toBuilder().addTrait(traitBuilder.build()).build());
                })
                .collect(Collectors.toSet());
    }
}
