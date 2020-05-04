/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.CollectionShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.SimpleShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.utils.Pair;

/**
 * Validates that service closures do not contain duplicate case-insensitive
 * shape names.
 *
 * <p>This validator allows some kinds of conflicts when they are likely
 * inconsequential. Some classes of conflicts are permitted, and in those
 * cases a WARNING or NOTE is emitted. A conflict is permitted if the shapes
 * are the same type; the two shapes are either a simple shape, list, or set;
 * both shapes have the same exact traits; and both shapes have equivalent
 * members (that is, the members follow these same rules). Permitted conflicts
 * detected between simple shapes are emitted as a NOTE, permitted conflicts
 * detected on other shapes are emitted as a WARNING, forbidden conflicts
 * detected for an operation or resource are emitted as an ERROR, and all
 * other forbidden conflicts are emitted as DANGER.
 */
public final class ServiceValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        return model.shapes(ServiceShape.class)
                .flatMap(shape -> validateService(model, shape).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateService(Model model, ServiceShape service) {
        // Ensure that shapes bound to the service have unique shape names.
        Walker walker = new Walker(model.getKnowledge(NeighborProviderIndex.class).getProvider());
        Set<Shape> serviceClosure = walker.walkShapes(service);
        Map<String, List<ShapeId>> conflicts = ValidationUtils.findDuplicateShapeNames(serviceClosure);

        if (conflicts.isEmpty()) {
            return Collections.emptyList();
        }

        // Determine the severity of each conflict.
        ConflictDetector detector = new ConflictDetector(model);
        List<ValidationEvent> events = new ArrayList<>();

        // Figure out if each conflict can be ignored, and then emit events for
        // both sides of the conflict using the appropriate severity.
        for (Map.Entry<String, List<ShapeId>> entry : conflicts.entrySet()) {
            List<ShapeId> ids = entry.getValue();
            for (int i = 0; i < ids.size(); i++) {
                Shape subject = model.expectShape(ids.get(i));
                for (int j = 0; j < ids.size(); j++) {
                    if (i != j) {
                        Shape other = model.expectShape(ids.get(j));
                        Severity severity = detector.detect(subject, other);
                        if (severity != null) {
                            events.add(conflictingNames(severity, service, subject, other));
                            events.add(conflictingNames(severity, service, other, subject));
                        }
                    }
                }
            }
        }

        return events;
    }

    private ValidationEvent conflictingNames(Severity severity, ServiceShape shape, Shape subject, Shape other) {
        // Whether it's a should or a must based on the severity.
        String declaration = severity == Severity.DANGER || severity == Severity.ERROR ? "must" : "should";

        return ValidationEvent.builder()
                .eventId(getName())
                .severity(severity)
                .shape(subject)
                .message(String.format(
                        "Shape name `%s` conflicts with `%s` in the `%s` service closure. These shapes in the closure "
                        + "of a service %s have case-insensitively unique names regardless of their namespaces.",
                        subject.getId().getName(),
                        other.getId(),
                        shape.getId(),
                        declaration))
                .build();
    }

    private static final class ConflictDetector {

        private static final EnumMap<ShapeType, Severity> FORBIDDEN = new EnumMap<>(ShapeType.class);

        static {
            // Service types are never allowed to conflict.
            FORBIDDEN.put(ShapeType.RESOURCE, Severity.ERROR);
            FORBIDDEN.put(ShapeType.OPERATION, Severity.ERROR);
            FORBIDDEN.put(ShapeType.SERVICE, Severity.ERROR);
            // These aggregate types are never allowed to conflict either, but
            // we will present the ability to suppress the violation if needed.
            FORBIDDEN.put(ShapeType.MAP, Severity.DANGER);
            FORBIDDEN.put(ShapeType.STRUCTURE, Severity.DANGER);
            FORBIDDEN.put(ShapeType.UNION, Severity.DANGER);
        }

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
            // Some types can never have conflicts since they're almost
            // universally code generated as named types.
            if (FORBIDDEN.containsKey(a.getType())) {
                return FORBIDDEN.get(a.getType());
            } else if (FORBIDDEN.containsKey(b.getType())) {
                return FORBIDDEN.get(b.getType());
            }

            // Conflicting shapes must have the same types.
            if (a.getType() != b.getType()) {
                return Severity.DANGER;
            }

            // When shapes conflict, they must have the same traits.
            if (!a.getAllTraits().equals(b.getAllTraits())) {
                return Severity.DANGER;
            }

            // Detect type-specific member conflicts. Return early if the
            // severity is a greater violation than the remaining checks.
            Severity memberConflict = detectMemberConflicts(a, b);
            if (memberConflict == Severity.WARNING
                    || memberConflict == Severity.DANGER
                    || memberConflict == Severity.ERROR) {
                return memberConflict;
            }

            // Simple shape conflicts are almost always benign and can be
            // ignored, so issue a NOTE instead of a WARNING.
            if (a instanceof SimpleShape) {
                return Severity.NOTE;
            }

            return Severity.WARNING;
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
