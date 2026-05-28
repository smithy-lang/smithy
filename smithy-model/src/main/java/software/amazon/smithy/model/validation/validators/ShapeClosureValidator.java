/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.knowledge.ShapeClosureIndex;
import software.amazon.smithy.model.metadata.ShapeClosure;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.selector.SelectorException;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIdSyntaxException;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates {@code shapeClosures} metadata: closure id uniqueness, that
 * closure ids do not collide with shape ids in the model, and that
 * declared renames follow the rules documented on the {@code rename}
 * member of {@code ShapeClosure}.
 *
 * <p>It also emits a {@code DANGER} when an {@code includeNamespaces} entry
 * or an {@code includeBySelector} matches no shapes in the model, and a
 * {@code WARNING} when non-renamed shapes in the closure have
 * case-insensitively conflicting names.
 *
 * <p>Like trait validation, problems are reported at the source location
 * of the offending closure.
 */
public final class ShapeClosureValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        Node metadata = model.getMetadata().get(ShapeClosure.METADATA_KEY);
        if (metadata == null || !metadata.isArrayNode()) {
            return Collections.emptyList();
        }

        List<ValidationEvent> events = new ArrayList<>();
        Map<String, SourceLocation> seenIds = new HashMap<>();
        Set<String> namespacesInModel = new HashSet<>();
        for (ShapeId shapeId : model.getShapeIds()) {
            namespacesInModel.add(shapeId.getNamespace());
        }

        for (Node element : metadata.expectArrayNode().getElements()) {
            // Skip anything ShapeClosure can't parse, structural validation is
            // handled by TypedMetadataValidator.
            ShapeClosure closure;
            try {
                closure = ShapeClosure.fromNode(element);
            } catch (ExpectationNotMetException | ShapeIdSyntaxException e) {
                continue;
            }
            SourceLocation location = closure.getSourceLocation();
            String id = closure.getId();

            SourceLocation previous = seenIds.put(id, location);
            if (previous != null) {
                events.add(error(location,
                        String.format(
                                "Shape closure id `%s` is declared more than once. The previous "
                                        + "declaration is at %s.",
                                id,
                                previous)));
                // Remaining checks resolve the closure by id, which the index
                // keys to only one of the duplicate entries, so skip them.
                continue;
            }

            // A malformed id cannot collide with a real shape; its format is
            // reported by the @idRef trait on ClosureId.
            try {
                if (model.getShape(ShapeId.from(id)).isPresent()) {
                    events.add(error(location,
                            String.format(
                                    "Shape closure ids must not match the ids of shapes in the model, but `%s` does.",
                                    id)));
                }
            } catch (ShapeIdSyntaxException e) {
                // Reported by the @idRef trait on ClosureId.
            }

            if (closure.getIncludeNamespaces().isEmpty() && !closure.getIncludeBySelector().isPresent()) {
                events.add(error(location,
                        String.format(
                                "Shape closure `%s` must define at least one of `includeNamespaces` "
                                        + "or `includeBySelector`.",
                                id)));
            }

            for (String namespace : closure.getIncludeNamespaces()) {
                if (!namespacesInModel.contains(namespace)) {
                    events.add(event(getName() + ".EmptyNamespace." + id,
                            Severity.DANGER,
                            location,
                            String.format(
                                    "Shape closure `%s` includes namespace `%s`, which has no shapes in the model.",
                                    id,
                                    namespace)));
                }
            }

            // Rename validation resolves the closure, which is unreliable when
            // the selector can't be parsed, so skip renames in that case.
            boolean validSelector = true;
            String selector = closure.getIncludeBySelector().orElse(null);
            if (selector != null && !selector.isEmpty()) {
                try {
                    if (Selector.parse(selector).select(model).isEmpty()) {
                        events.add(event(getName() + ".EmptySelector." + id,
                                Severity.DANGER,
                                location,
                                String.format(
                                        "Shape closure `%s` has an `includeBySelector` that matches no "
                                                + "shapes in the model.",
                                        id)));
                    }
                } catch (SelectorException e) {
                    events.add(error(location,
                            String.format(
                                    "Shape closure `%s` has an invalid `includeBySelector`: %s",
                                    id,
                                    e.getMessage())));
                    validSelector = false;
                }
            }

            if (validSelector) {
                events.addAll(validateRenames(model, closure));
                events.addAll(validateNameConflicts(model, closure));
            }
        }

        return events;
    }

    private List<ValidationEvent> validateRenames(Model model, ShapeClosure closure) {
        Map<ShapeId, String> renames = closure.getRename();
        if (renames.isEmpty()) {
            return Collections.emptyList();
        }
        String closureId = closure.getId();
        SourceLocation location = closure.getSourceLocation();

        Set<Shape> closureShapes = ShapeClosureIndex.of(model).getShapesInClosure(closureId);
        Set<ShapeId> closureIds = new HashSet<>();
        Map<String, ShapeId> nonRenamedNames = new HashMap<>();
        for (Shape shape : closureShapes) {
            closureIds.add(shape.getId());
            if (!shape.isMemberShape() && !renames.containsKey(shape.getId())) {
                nonRenamedNames.putIfAbsent(shape.getId().getName().toLowerCase(Locale.ENGLISH),
                        shape.getId());
            }
        }

        List<ValidationEvent> events = new ArrayList<>();
        Map<String, ShapeId> renamedNames = new HashMap<>();
        for (Map.Entry<ShapeId, String> entry : renames.entrySet()) {
            ShapeId fromId = entry.getKey();
            String to = entry.getValue();

            if (to.equals(fromId.getName())) {
                events.add(error(location,
                        String.format(
                                "Shape closure `%s` rename for `%s` does not change the name from `%s`.",
                                closureId,
                                fromId,
                                to)));
            }

            if (!closureIds.contains(fromId)) {
                events.add(error(location,
                        String.format(
                                "Shape closure `%s` attempts to rename `%s`, which is not in the closure.",
                                closureId,
                                fromId)));
                continue;
            }

            String normalized = to.toLowerCase(Locale.ENGLISH);
            ShapeId previousRename = renamedNames.put(normalized, fromId);
            if (previousRename != null) {
                events.add(error(location,
                        String.format(
                                "Shape closure `%s` rename for `%s` (\"%s\") case-insensitively conflicts "
                                        + "with the rename for `%s`.",
                                closureId,
                                fromId,
                                to,
                                previousRename)));
            }
            ShapeId conflictingNonRenamed = nonRenamedNames.get(normalized);
            if (conflictingNonRenamed != null) {
                events.add(error(location,
                        String.format(
                                "Shape closure `%s` rename for `%s` (\"%s\") case-insensitively conflicts "
                                        + "with non-renamed shape `%s`.",
                                closureId,
                                fromId,
                                to,
                                conflictingNonRenamed)));
            }
        }

        return events;
    }

    private List<ValidationEvent> validateNameConflicts(Model model, ShapeClosure closure) {
        Map<ShapeId, String> renames = closure.getRename();
        Map<String, Set<ShapeId>> namesToIds = new HashMap<>();
        for (Shape shape : ShapeClosureIndex.of(model).getShapesInClosure(closure.getId())) {
            ShapeId shapeId = shape.getId();
            // Conflicts involving a rename are reported as errors above.
            if (shape.isMemberShape() || renames.containsKey(shapeId)) {
                continue;
            }
            namesToIds.computeIfAbsent(shapeId.getName().toLowerCase(Locale.ENGLISH), n -> new TreeSet<>())
                    .add(shapeId);
        }

        List<ValidationEvent> events = new ArrayList<>();
        for (Set<ShapeId> conflicting : namesToIds.values()) {
            if (conflicting.size() > 1) {
                events.add(event(getName() + ".NameConflicts." + closure.getId(),
                        Severity.WARNING,
                        closure.getSourceLocation(),
                        String.format(
                                "Shape closure `%s` contains shapes whose names conflict case-insensitively: %s. "
                                        + "Shapes in a closure should have case-insensitively unique names regardless "
                                        + "of their namespaces. Some code generators or other tools may require this "
                                        + "in order to work. The `rename` property may be used to disambiguate them.",
                                closure.getId(),
                                conflicting)));
            }
        }
        return events;
    }

    private ValidationEvent error(SourceLocation location, String message) {
        return event(getName(), Severity.ERROR, location, message);
    }

    private ValidationEvent event(String id, Severity severity, SourceLocation location, String message) {
        return ValidationEvent.builder()
                .id(id)
                .severity(severity)
                .sourceLocation(location)
                .message(message)
                .build();
    }
}
