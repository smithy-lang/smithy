/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Maps over shapes in the model using a mapping function.
 *
 * <p>Only shapes that have changed cause the shape the be replaced in
 * the Model. The mapping function MUST return a shape with the same
 * ID and the same type (e.g., a string cannot become an integer).
 *
 * @see ModelTransformer#mapShapes
 */
final class MapShapes {
    private final Function<Shape, Shape> mapper;

    MapShapes(Function<Shape, Shape> mapper) {
        this.mapper = mapper;
    }

    Model transform(ModelTransformer transformer, Model model) {
        return transformer.replaceShapes(model,
                model.shapes()
                        .flatMap(shape -> {
                            Shape mapped =
                                    Objects.requireNonNull(mapper.apply(shape), "Shape mapper must not return null");
                            if (mapped.equals(shape)) {
                                return Stream.empty();
                            } else if (!mapped.getId().equals(shape.getId())) {
                                throw new ModelTransformException(String.format(
                                        "Mapped shapes must have the same shape ID. Expected %s, but found %s",
                                        shape.getId(),
                                        mapped.getId()));
                            } else {
                                return Stream.of(mapped);
                            }
                        })
                        .collect(Collectors.toSet()));
    }
}
