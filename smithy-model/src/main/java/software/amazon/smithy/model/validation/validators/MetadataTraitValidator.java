/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.MetadataTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Validates applications of the {@code metadata} trait itself.
 *
 * <p>A metadata key MUST NOT have a type defined by more than one shape
 * carrying the {@code metadata} trait. Each shape participating in a
 * conflict receives an {@code ERROR} event.
 *
 * <p>Validation of the metadata values themselves is handled separately by
 * {@link TypedMetadataValidator}.
 */
@SmithyInternalApi
public final class MetadataTraitValidator extends AbstractValidator {
    // We will eventually add trait definitions for these, but for now
    // this ensures that nobody else can.
    private static final Set<String> RESERVED = SetUtils.of("severityOverrides", "suppressions", "validators");

    @Override
    public List<ValidationEvent> validate(Model model) {
        Map<String, List<Shape>> shapesByKey = new LinkedHashMap<>();
        for (Shape shape : model.getShapesWithTrait(MetadataTrait.class)) {
            String key = shape.expectTrait(MetadataTrait.class).getKey();
            shapesByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(shape);
        }

        List<ValidationEvent> events = new ArrayList<>();
        for (Map.Entry<String, List<Shape>> entry : shapesByKey.entrySet()) {
            if (RESERVED.contains(entry.getKey())) {
                events.addAll(emitReservedEvents(entry.getKey(), entry.getValue()));
            } else if (entry.getValue().size() > 1) {
                events.addAll(emitDuplicateEvents(entry.getKey(), entry.getValue()));
            }
        }
        return events;
    }

    private List<ValidationEvent> emitReservedEvents(String key, List<Shape> shapes) {
        List<ValidationEvent> events = new ArrayList<>();
        for (Shape shape : shapes) {
            events.add(error(shape,
                    shape.expectTrait(MetadataTrait.class),
                    String.format(
                            "The metadata key `%s` has been reserved by Smithy and may not be defined elsewhere.",
                            key)));
        }
        return events;
    }

    private List<ValidationEvent> emitDuplicateEvents(String key, List<Shape> shapes) {
        Set<String> allIds = shapes.stream().map(Shape::toShapeId).map(ShapeId::toString).collect(Collectors.toSet());
        List<ValidationEvent> events = new ArrayList<>();
        for (Shape shape : shapes) {
            Set<String> others = new TreeSet<>(allIds);
            others.remove(shape.getId().toString());
            events.add(error(shape,
                    shape.expectTrait(MetadataTrait.class),
                    String.format(
                            "A type has already been defined for the metadata key `%s`. "
                                    + "Conflicts with: [%s]",
                            key,
                            String.join(", ", others))));
        }
        return events;
    }
}
