/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jsonschema;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.shapes.CollectionShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.SimpleShape;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.PrivateTrait;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.utils.FunctionalUtils;
import software.amazon.smithy.utils.StringUtils;

/**
 * Automatically de-conflicts map shapes, list shapes, and set shapes
 * by sorting conflicting shapes by ID and then appending a formatted
 * version of the shape ID namespace to the colliding shape.
 *
 * <p>Simple types are never generated at the top level because they
 * are always inlined into complex shapes; however, string shapes
 * marked with the enum trait are never allowed to conflict since
 * they can easily drift away from compatibility over time.
 * Structures and unions are not allowed to conflict either.
 */
final class DeconflictingStrategy implements RefStrategy {

    private static final Logger LOGGER = Logger.getLogger(DeconflictingStrategy.class.getName());
    private static final Pattern SPLIT_PATTERN = Pattern.compile("\\.");

    private final RefStrategy delegate;
    private final Map<ShapeId, String> pointers = new HashMap<>();
    private final Map<String, ShapeId> reversePointers = new HashMap<>();

    DeconflictingStrategy(Model model, RefStrategy delegate, Predicate<Shape> shapePredicate) {
        this.delegate = delegate;

        // Pre-compute a map of all converted shape refs. Sort the shapes
        // to make the result deterministic. Eliminate trait definitions
        // ahead of that computation to eliminate potential conflicts with
        // shapes that won't go into JSON Schema anyway.
        Model scrubbedModel = null;
        for (Shape shape : new TreeSet<>(model.toSet())) {
            if (!shapePredicate.and(FunctionalUtils.not(this::isIgnoredShape)).test(shape)) {
                continue;
            }

            String pointer = delegate.toPointer(shape.getId());
            if (!reversePointers.containsKey(pointer)) {
                pointers.put(shape.getId(), pointer);
                reversePointers.put(pointer, shape.getId());
            } else {
                if (scrubbedModel == null) {
                    scrubbedModel = ModelTransformer.create().scrubTraitDefinitions(model);
                }
                if (scrubbedModel.getShape(reversePointers.get(pointer)).isPresent()) {
                    String deconflictedPointer = deconflict(shape, pointer, reversePointers);
                    LOGGER.info(() -> String.format(
                            "De-conflicted `%s` JSON schema pointer from `%s` to `%s`",
                            shape.getId(),
                            pointer,
                            deconflictedPointer));
                    pointers.put(shape.getId(), deconflictedPointer);
                    reversePointers.put(deconflictedPointer, shape.getId());
                }
            }
        }
    }

    // Some shapes aren't converted to JSON schema at all because they
    // don't have a corresponding definition.
    private boolean isIgnoredShape(Shape shape) {
        return (shape instanceof SimpleShape && !shape.hasTrait(EnumTrait.class))
                || shape.isResourceShape()
                || shape.isServiceShape()
                || shape.isOperationShape()
                || shape.isMemberShape()
                || (Prelude.isPreludeShape(shape) && shape.hasTrait(PrivateTrait.class));
    }

    private String deconflict(Shape shape, String pointer, Map<String, ShapeId> reversePointers) {
        LOGGER.info(() -> String.format(
                "Attempting to de-conflict `%s` JSON schema pointer `%s` that conflicts with `%s`",
                shape.getId(),
                pointer,
                reversePointers.get(pointer)));

        if (!isSafeToDeconflict(shape)) {
            throw new ConflictingShapeNameException(String.format(
                    "Shape %s conflicts with %s using a JSON schema pointer of %s",
                    shape,
                    reversePointers.get(pointer),
                    pointer));
        }

        // Create a de-conflicted JSON schema pointer that just appends
        // the PascalCase formatted version of the shape's namespace to the
        // resulting pointer.
        StringBuilder builder = new StringBuilder(pointer);
        for (String part : SPLIT_PATTERN.split(shape.getId().getNamespace())) {
            builder.append(StringUtils.capitalize(part));
        }

        String updatedPointer = builder.toString();

        if (reversePointers.containsKey(updatedPointer)) {
            // Note: I don't know if this can ever actually happen... but just in case.
            throw new ConflictingShapeNameException(String.format(
                    "Unable to de-conflict shape %s because the de-conflicted name resolves "
                            + "to another generated name: %s",
                    shape,
                    updatedPointer));
        }

        return updatedPointer;
    }

    // We only want to de-conflict shapes that are generally not code-generated
    // because the de-conflicted names can potentially change over time as shapes
    // are added and removed. Things like structures, unions, and enums should
    // never be de-conflicted from this class.
    private boolean isSafeToDeconflict(Shape shape) {
        return shape instanceof CollectionShape || shape instanceof MapShape;
    }

    @Override
    public String toPointer(ShapeId id) {
        return pointers.computeIfAbsent(id, delegate::toPointer);
    }

    @Override
    public boolean isInlined(Shape shape) {
        return delegate.isInlined(shape);
    }
}
