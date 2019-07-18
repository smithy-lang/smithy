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

package software.amazon.smithy.build.transforms;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.utils.Pair;

/**
 * Removes trait definitions and traits from shapes when a trait name
 * matches any of the given arguments.
 *
 * <p>End an arguments with "#" to exclude the traits of an entire
 * namespace.
 */
public final class ExcludeTraits extends AbstractTraitRemoval {
    private static final Logger LOGGER = Logger.getLogger(ExcludeTraits.class.getName());

    @Override
    public String getName() {
        return "excludeTraits";
    }

    @Override
    public BiFunction<ModelTransformer, Model, Model> createTransformer(List<String> arguments) {
        Pair<Set<ShapeId>, Set<String>> namesAndNamespaces = parseTraits(arguments);
        Set<ShapeId> names = namesAndNamespaces.getLeft();
        Set<String> namespaces = namesAndNamespaces.getRight();
        LOGGER.info(() -> "Excluding traits by ID " + names + " and namespaces " + namespaces);

        return (transformer, model) -> {
            Set<Shape> removeTraits = model.getTraitShapes().stream()
                    .filter(trait -> matchesTraitDefinition(trait, names, namespaces))
                    .collect(Collectors.toSet());

            if (!removeTraits.isEmpty()) {
                LOGGER.info(() -> "Excluding traits: " + removeTraits);
            }

            return transformer.removeShapes(model, removeTraits);
        };
    }
}
