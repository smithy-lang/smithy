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

package software.amazon.smithy.model.neighbor;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.TraitDefinition;

/**
 * Finds trait definitions that are not connected to a service shape.
 */
public final class UnreferencedTraitDefinitions {

    private final Predicate<TraitDefinition> keepFilter;

    public UnreferencedTraitDefinitions() {
        this(traitDefinition -> true);
    }

    /**
     * @param keepFilter Predicate that if matched keeps a trait definition from being unreferenced.
     */
    public UnreferencedTraitDefinitions(Predicate<TraitDefinition> keepFilter) {
        this.keepFilter = keepFilter;
    }

    public Set<TraitDefinition> compute(Model model) {
        Walker walker = new Walker(model.getKnowledge(NeighborProviderIndex.class).getProvider());

        // Begin with a mutable set of all trait definitions contained in the model
        Set<TraitDefinition> unused = model.getTraitDefinitions().stream()
                // Exclude prelude traits -- these are defined by Smithy, not by the model itself
                .filter(traitDef -> !Prelude.isPreludeTraitDefinition(traitDef.getFullyQualifiedName()))
                .collect(Collectors.toSet());

        // Find all traits used directly or indirectly by a service shape and remove
        // their definitions from the unused set.
        model.getShapeIndex().shapes(ServiceShape.class)
                .flatMap(service -> walker.walkShapes(service).stream())
                .distinct()
                .map(Shape::getAllTraits)
                .map(Map::keySet)
                .flatMap(Set::stream)
                .distinct()
                .flatMap(traitName -> model.getTraitDefinition(traitName).stream())
                .filter(keepFilter)
                .forEach(unused::remove);
        return unused;
    }
}
