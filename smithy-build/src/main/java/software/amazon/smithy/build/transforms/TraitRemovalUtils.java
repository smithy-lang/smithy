/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import java.util.HashSet;
import java.util.Set;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.Pair;

/**
 * Utilities for {@link IncludeTraits} and {@link ExcludeTraits}.
 */
final class TraitRemovalUtils {

    private TraitRemovalUtils() {}

    static Pair<Set<ShapeId>, Set<String>> parseTraits(Set<String> ids) {
        Set<ShapeId> traitNames = new HashSet<>();
        Set<String> traitNamespaces = new HashSet<>();

        for (String arg : ids) {
            if (arg.endsWith("#")) {
                traitNamespaces.add(arg.substring(0, arg.length() - 1));
            } else {
                traitNames.add(ShapeId.from(Trait.makeAbsoluteName(arg)));
            }
        }

        return Pair.of(traitNames, traitNamespaces);
    }

    static boolean matchesTraitDefinition(Shape traitShape, Set<ShapeId> traitNames, Set<String> traitNamespaces) {
        return traitNames.contains(traitShape.getId()) || traitNamespaces.contains(traitShape.getId().getNamespace());
    }
}
