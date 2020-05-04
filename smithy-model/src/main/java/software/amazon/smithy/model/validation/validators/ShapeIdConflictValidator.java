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

package software.amazon.smithy.model.validation.validators;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Ensures that no two shapes in the model have the same case-insensitive
 * shape ID.
 */
public final class ShapeIdConflictValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        Map<String, List<Shape>> conflicts = model.shapes()
                .collect(Collectors.groupingBy(shape -> shape.getId().toString().toLowerCase(Locale.US)))
                .entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return conflicts.values().stream()
                .flatMap(this::emitEvents)
                .collect(Collectors.toList());
    }

    private Stream<ValidationEvent> emitEvents(List<Shape> collidingShapes) {
        List<ShapeId> collidingIds = collidingShapes.stream()
                .map(Shape::getId)
                .sorted()
                .collect(Collectors.toList());
        return collidingShapes.stream().map(value -> {
            String collideString = collidingIds.stream()
                    .filter(id -> !id.equals(value.getId()))
                    .map(id -> "`" + id + "`")
                    .collect(Collectors.joining(", "));
            return error(value, String.format(
                    "Shape ID `%s` conflicts with other shape IDs in the model: [%s]",
                    value.getId(), collideString));
        });
    }
}
