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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import software.amazon.smithy.build.ProjectionTransformer;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * Removes shapes from the model that are not connected to any service shape.
 *
 * <p>You can export shapes that are not connected to any service shape by
 * applying specific tags to the shape and adding the list of export tags as
 * arguments to the treeShaker.
 *
 * <p>Shapes from the prelude *are* removed if they are not referenced as
 * part of a model.
 */
public final class RemoveUnusedShapes implements ProjectionTransformer {
    @Override
    public String getName() {
        return "removeUnusedShapes";
    }

    @Override
    public Collection<String> getAliases() {
        return Collections.singleton("treeShaker");
    }

    @Override
    public BiFunction<ModelTransformer, Model, Model> createTransformer(List<String> arguments) {
        Set<String> includeTags = new HashSet<>(arguments);
        Predicate<Shape> keepShapesByTag = shape -> includeTags.stream().noneMatch(shape::hasTag);
        Predicate<Shape> keepTraitDefsByTag = trait -> includeTags.stream().noneMatch(trait::hasTag);

        return (transformer, model) -> {
            int currentShapeCount;

            do {
                currentShapeCount = model.toSet().size();
                model = transformer.removeUnreferencedShapes(model, keepShapesByTag);
                model = transformer.removeUnreferencedTraitDefinitions(model, keepTraitDefsByTag);
            } while (currentShapeCount != model.toSet().size());

            return model;
        };
    }
}
