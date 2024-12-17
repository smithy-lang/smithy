/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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
        return transformer.removeShapes(model,
                model.shapes()
                        .filter(shape -> canFilterShape(model, shape))
                        .filter(FunctionalUtils.not(predicate))
                        .collect(Collectors.toSet()));
    }

    private static boolean canFilterShape(Model model, Shape shape) {
        return !shape.isMemberShape() || model.getShape(shape.asMemberShape().get().getContainer())
                .filter(container -> container.isStructureShape()
                        || container.isUnionShape()
                        || container.isEnumShape()
                        || container.isIntEnumShape())
                .isPresent();
    }
}
