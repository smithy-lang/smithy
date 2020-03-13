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

package software.amazon.smithy.model.transform;

import java.util.function.Predicate;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.utils.FunctionalUtils;

/**
 * Filters shapes out of a model that do not match a predicate.
 *
 * <p>Members of lists, sets, and maps are not passed to the filter
 * function or eligible for removal.
 *
 * <p>If a shape is removed from the model that is a trait definition,
 * all instances of that trait are automatically removed.
 *
 * @see ModelTransformer#filterShapes
 */
final class FilterShapes {
    private final Predicate<Shape> predicate;

    FilterShapes(Predicate<Shape> predicate) {
        this.predicate = predicate
                // Don't ever filter out prelude shapes.
                .or(shape -> Prelude.isPreludeShape(shape.getId()));
    }

    Model transform(ModelTransformer transformer, Model model) {
        return transformer.removeShapes(model, model.shapes()
                .filter(shape -> canFilterShape(model, shape))
                .filter(FunctionalUtils.not(predicate))
                .collect(Collectors.toSet()));
    }

    private static boolean canFilterShape(Model model, Shape shape) {
        return !shape.isMemberShape() || model.getShape(shape.expectMemberShape().getContainer())
                .filter(container -> container.isStructureShape() || container.isUnionShape())
                .isPresent();
    }
}
