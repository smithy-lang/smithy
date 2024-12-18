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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.CollectionShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.SimpleShape;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.Pair;

/**
 * Validates that service closures do not contain duplicate case-insensitive
 * shape names. The rename property of the service is used to deconflict
 * shapes.
 *
 * <p>This validator allows some kinds of conflicts when they are likely
 * inconsequential. Some classes of conflicts are permitted, and in those
 * cases a WARNING or NOTE is emitted. A conflict is permitted if the shapes
 * are the same type; the two shapes are either a simple shape, list, or set;
 * both shapes have the same exact traits; and both shapes have equivalent
 * members (that is, the members follow these same rules). Permitted conflicts
 * detected between simple shapes are emitted as a NOTE, permitted conflicts
 * detected on other shapes are emitted as a WARNING, and other conflicts are
 * emitted as ERROR.
 */
public final class ServiceValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (ServiceShape shape : model.getServiceShapes()) {
            validateService(model, shape, events);
        }
        return events;
    }

    private void validateService(Model model, ServiceShape service, List<ValidationEvent> events) {
        // Ensure that shapes bound to the service have unique shape names.
        Walker walker = new Walker(NeighborProviderIndex.of(model).getProvider());
        Map<ShapeId, Shape> serviceClosure = new HashMap<>();
        walker.iterateShapes(service).forEachRemaining(shape -> serviceClosure.put(shape.getId(), shape));

        // Create a mapping of lowercase contextual shape names to shape IDs.
        Map<String, Set<ShapeId>> normalizedNamesToIds = new HashMap<>();
        for (ShapeId id : serviceClosure.keySet()) {
            if (!id.hasMember()) {
                String possiblyRename = service.getContextualName(id);
                normalizedNamesToIds
                        .computeIfAbsent(possiblyRename.toLowerCase(Locale.ENGLISH), name -> new TreeSet<>())
                        .add(id);
            }
        }

        // Determine the severity of each conflict.
        ConflictDetector detector = new ConflictDetector(model);

        // Figure out if each conflict can be ignored, and then emit events for
        // both sides of the conflict using the appropriate severity.
        for (Map.Entry<String, Set<ShapeId>> entry : normalizedNamesToIds.entrySet()) {
            Set<ShapeId> ids = entry.getValue();
            // Only look at groupings that contain conflicts.
            if (ids.size() <= 1) {
                continue;
            }
            for (ShapeId subjectId : ids) {
                model.getShape(subjectId).ifPresent(subject -> {
                    for (ShapeId otherId : ids) {
                        if (!otherId.equals(subjectId)) {
                            model.getShape(otherId).ifPresent(other -> {
                                Severity severity = detector.detect(subject, other);
                                if (severity != null) {
                                    events.add(conflictingNames(severity, service, subject, other));
                                }
                            });
                        }
                    }
                });
            }
        }

        events.addAll(validateRenames(service, serviceClosure));
    }

    private List<ValidationEvent> validateRenames(ServiceShape service, Map<ShapeId, Shape> closure) {
        if (service.getRename().isEmpty()) {
            return Collections.emptyList();
        }

        List<ValidationEvent> events = new ArrayList<>();

        Map<String, Set<ShapeId>> renameMappings = new HashMap<>();
        for (Map.Entry<ShapeId, String> rename : service.getRename().entrySet()) {
            ShapeId from = rename.getKey();
            String to = rename.getValue();
            renameMappings.computeIfAbsent(to.toLowerCase(Locale.ENGLISH), t -> new HashSet<>()).add(from);

            if (!ShapeId.isValidIdentifier(to)) {
                events.add(error(service,
                        String.format(
                                "Service attempts to rename `%s` to an invalid identifier, \"%s\"",
                                from,
                                to)));
            } else if (to.equals(from.getName())) {
                events.add(error(service,
                        String.format(
                                "Service rename for `%s` does not actually change the name from `%s`",
                                from,
                                to)));
            }

            // Each renamed shape ID must actually exist in the closure.
            if (!closure.containsKey(from)) {
                events.add(error(service, "Service attempts to rename a shape not in the service: " + from));
            } else {
                getInvalidRenameReason(closure.get(from)).ifPresent(reason -> {
                    events.add(error(service,
                            String.format(
                                    "Service attempts to rename a %s shape from `%s` to \"%s\"; %s",
                                    closure.get(from).getType(),
                                    from,
                                    to,
                                    reason)));
                });
            }
        }

        return events;
    }

    private Optional<String> getInvalidRenameReason(Shape shape) {
        if (shape.isMemberShape() || shape.isResourceShape() || shape.isOperationShape()) {
            return Optional.of(shape.getType() + "s cannot be renamed");
        } else {
            return Optional.empty();
        }
    }

    private ValidationEvent conflictingNames(Severity severity, ServiceShape service, Shape subject, Shape other) {
        StringBuilder message = new StringBuilder();

        if (service.getRename().get(subject.getId()) != null) {
            message.append("Renamed shape name \"")
                    .append(service.getRename().get(subject.getId()))
                    .append('"');
        } else {
            message.append("Shape name `").append(subject.getId()).append('`');
        }

        message.append(" conflicts with `").append(other.getId()).append("` ");

        if (service.getRename().get(other.getId()) != null) {
            message.append("(renamed to \"")
                    .append(service.getRename().get(other.getId()))
                    .append("\") ");
        }

        message.append("in the `")
                .append(service.getId())
                .append("` service closure. ")
                .append("Shapes in the closure of a service ")
                .append(severity.ordinal() >= Severity.DANGER.ordinal() ? "must " : "should ")
                .append("have case-insensitively unique names regardless of their namespaces. ")
                .append("Use the `rename` property of the service to disambiguate shape names.");

        return ValidationEvent.builder()
                .id(getName())
                .severity(severity)
                .shape(subject)
                .message(message.toString())
                .build();
    }

    private static final class ConflictDetector {

        private final Model model;
        private final Map<Pair<ShapeId, ShapeId>, Severity> cache = new HashMap<>();

        ConflictDetector(Model model) {
            this.model = model;
        }

        Severity detect(Shape a, Shape b) {
            // Treat null values as allowed so that this validator just
            // ignores cases where a member target is broken.
            if (a == null || b == null) {
                return null;
            }

            // Create a normalized cache key since the comparison of a to b
            // and b to a is the same result.
            Pair<ShapeId, ShapeId> cacheKey = a.getId().compareTo(b.getId()) < 0
                    ? Pair.of(a.getId(), b.getId())
                    : Pair.of(b.getId(), a.getId());

            // Don't use computeIfAbsent here since we don't want to lock the HashMap.
            // Computing if there is a conflict for aggregate shapes requires that
            // both the aggregate and its members are checked recursively.
            if (cache.containsKey(cacheKey)) {
                return cache.get(cacheKey);
            }

            Severity result = detectConflicts(a, b);
            cache.put(cacheKey, result);
            return result;
        }

        private Severity detectConflicts(Shape a, Shape b) {
            // 1. Check for conflicts that are not allowed.
            // 2. Conflicting shapes must have the same types.
            // 3. Conflicting shapes must have the same traits.
            if (isShapeTypeConflictForbidden(a)
                    || isShapeTypeConflictForbidden(b)
                    || a.getType() != b.getType()
                    || !equivalentTraits(a.getAllTraits(), b.getAllTraits())) {
                return Severity.ERROR;
            }

            // Return early if WARNING or greater member conflicts are detected.
            Severity memberConflict = detectMemberConflicts(a, b);
            if (memberConflict != null && memberConflict.ordinal() >= Severity.WARNING.ordinal()) {
                return memberConflict;
            }

            // Simple shape conflicts are almost always benign and can be
            // ignored, so issue a NOTE instead of a WARNING.
            if (a instanceof SimpleShape) {
                return Severity.NOTE;
            }

            // The conflict occurred on a list or set.
            return Severity.WARNING;
        }

        // Check if the traits are equal, disregarding synthetic traits.
        private boolean equivalentTraits(Map<ShapeId, Trait> left, Map<ShapeId, Trait> right) {
            for (Map.Entry<ShapeId, Trait> entry : left.entrySet()) {
                if (!entry.getValue().isSynthetic()) {
                    if (!Objects.equals(entry.getValue(), right.get(entry.getKey()))) {
                        return false;
                    }
                }
            }
            // Only thing to check here is if the right map has traits the left map doesn't.
            for (Map.Entry<ShapeId, Trait> entry : right.entrySet()) {
                if (!entry.getValue().isSynthetic() && !left.containsKey(entry.getKey())) {
                    return false;
                }
            }
            return true;
        }

        private boolean isShapeTypeConflictForbidden(Shape shape) {
            return !(shape instanceof SimpleShape || shape instanceof CollectionShape || shape.isMemberShape());
        }

        private Severity detectMemberConflicts(Shape a, Shape b) {
            if (a instanceof MemberShape) {
                // Member shapes must have the same traits and they must
                // target the same kind of shape. The target can be different
                // as long as the targets are effectively the same.
                MemberShape aMember = (MemberShape) a;
                MemberShape bMember = (MemberShape) b;
                Shape aTarget = model.getShape(aMember.getTarget()).orElse(null);
                Shape bTarget = model.getShape(bMember.getTarget()).orElse(null);
                return detect(aTarget, bTarget);
            } else if (a instanceof CollectionShape) {
                // Collections/map shapes can conflict if they have the same traits and members.
                CollectionShape aCollection = (CollectionShape) a;
                CollectionShape bCollection = (CollectionShape) b;
                return detect(aCollection.getMember(), bCollection.getMember());
            } else {
                return null;
            }
        }
    }
}
