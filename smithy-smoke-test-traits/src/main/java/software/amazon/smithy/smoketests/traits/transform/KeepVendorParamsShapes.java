/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.smoketests.traits.transform;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.model.transform.ModelTransformerPlugin;
import software.amazon.smithy.smoketests.traits.SmokeTestCase;
import software.amazon.smithy.smoketests.traits.SmokeTestsTrait;

/**
 * Runs after all other {@link ModelTransformerPlugin}s, adding back any shapes referenced by a
 * {@code SmokeTestCase.vendorParamsShape} and all connected shapes, that were removed by previous transforms.
 *
 * <p>Since these shapes are referenced from within trait
 * values, they don't create an edge in the model graph. This means transforms like
 * <a href="https://smithy.io/2.0/guides/building-models/build-config.html#removeunusedshapes">removeUnusedShapes</a>
 * will remove vendor params shapes, causing {@link software.amazon.smithy.smoketests.traits.SmokeTestCaseValidator}
 * to fail.
 */
public class KeepVendorParamsShapes implements ModelTransformerPlugin {
    @Override
    public byte order() {
        // This plugin has to run last, in case previous plugins removed any of the vendor params shapes.
        return Byte.MAX_VALUE;
    }

    @Override
    public Model onRemove(ModelTransformer transformer, Collection<Shape> removed, Model model) {
        Set<ShapeId> vendorParamsShapeIds = model.getShapesWithTrait(SmokeTestsTrait.class).stream()
                .flatMap(shape -> shape.expectTrait(SmokeTestsTrait.class).getTestCases().stream())
                .map(SmokeTestCase::getVendorParamsShape)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());

        // Only consider vendor params shapes that were removed.
        vendorParamsShapeIds.removeAll(model.getShapeIds());
        if (vendorParamsShapeIds.isEmpty()) {
            return model;
        }

        Model.Builder builder = model.toBuilder();

        // Need to add back all the shapes connected to the vendor params shape as well.
        Model removedShapesModel = Model.builder().addShapes(removed).build();
        Walker removedShapesWalker = new Walker(removedShapesModel);
        for (ShapeId removedVendorParamsShapeId : vendorParamsShapeIds) {
            Shape removedShape = removedShapesModel.expectShape(removedVendorParamsShapeId);
            Set<Shape> connected = removedShapesWalker.walkShapes(removedShape);
            builder.addShapes(connected);
        }

        return builder.build();
    }
}
