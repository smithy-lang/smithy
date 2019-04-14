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

package software.amazon.smithy.model.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIdSyntaxException;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Validator suppression.
 */
public final class Suppression implements FromSourceLocation, ToSmithyBuilder<Suppression> {

    private final SourceLocation sourceLocation;
    private final String reason;
    private final Set<ShapeId> shapes;
    private final Set<String> validatorIds;
    private final Set<String> namespaceNames;
    private Predicate<ValidationEvent> predicate;

    private Suppression(Builder builder) {
        if (builder.validatorIds.isEmpty()) {
            throw new SourceException("Suppressions require at least one `id`", builder.sourceLocation);
        }

        builder.validatorIds.forEach(id -> {
            if (id.isEmpty()) {
                throw new SourceException("Suppression `ids` element must not be empty", builder.sourceLocation);
            } else if (id.indexOf('*') > 0) {
                throw new SourceException(
                        String.format("Invalid suppression `ids` wildcard: `%s`", id), builder.sourceLocation);
            }
        });
        validatorIds = Collections.unmodifiableSet(new LinkedHashSet<>(builder.validatorIds));

        // Load each shape string into a Shape.Id, wrapping exceptions.
        Set<ShapeId> shapeIds = new LinkedHashSet<>();
        builder.shapes.forEach(shape -> {
            try {
                shapeIds.add(ShapeId.from(shape));
            } catch (ShapeIdSyntaxException e) {
                throw new SourceException(e.getMessage(), builder.sourceLocation);
            }
        });
        shapes = Collections.unmodifiableSet(shapeIds);

        sourceLocation = builder.sourceLocation;
        reason = builder.reason;
        namespaceNames = new HashSet<>(builder.namespaceNames);
        predicate = createPredicate(validatorIds, shapes, namespaceNames);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        Builder builder = builder().sourceLocation(sourceLocation).reason(reason);
        namespaceNames.forEach(builder::addShape);
        shapes.forEach(builder::addShape);
        validatorIds.forEach(builder::addValidatorId);
        return builder;
    }

    /**
     * <p>Applies a list of suppressions to a validation event.
     *
     * <p>Suppressions are applied, in order, until the event is marked as
     * suppressed or there are no more suppressions to apply. The list of
     * suppressions is short-circuited as soon as the first suppression
     * suppresses the event.
     *
     * @param validationEvent Validation event to suppress if needed.
     * @param suppressions Ordered list of suppressions to apply.
     * @return Returns the event that is mapped over by the list of suppressions.
     */
    public static ValidationEvent suppressEvent(ValidationEvent validationEvent, List<Suppression> suppressions) {
        Iterator<Suppression> suppressionIterator = suppressions.iterator();
        while (validationEvent.getSeverity().canSuppress() && suppressionIterator.hasNext()) {
            validationEvent = suppressionIterator.next().suppress(validationEvent);
        }
        return validationEvent;
    }

    private static Predicate<ValidationEvent> createPredicate(
            Set<String> eventIds,
            Set<ShapeId> shapeIds,
            Set<String> namespaceNames
    ) {
        // Build up a list of predicates that all must match.
        List<Predicate<ValidationEvent>> predicates = new ArrayList<>();

        // Only suppress things that can be suppressed.
        predicates.add(event -> event.getSeverity().canSuppress());

        eventIds.stream()
                .filter(rule -> !rule.equals("*"))
                .forEach(rule -> predicates.add(event -> event.getEventId().equals(rule)));

        if (!shapeIds.isEmpty() || !namespaceNames.isEmpty()) {
            predicates.add(event -> event.getShapeId()
                    .filter(id -> namespaceNames.contains(id.getNamespace()) || shapeIds.contains(id))
                    .isPresent());
        }

        return event -> predicates.stream().allMatch(p -> p.test(event));
    }

    /**
     * @return Returns the optional reason string.
     */
    public Optional<String> getReason() {
        return Optional.ofNullable(reason);
    }

    /**
     * @return Returns the list of validator IDs.
     */
    public Set<String> getValidatorIds() {
        return validatorIds;
    }

    /**
     * @return Returns the possibly empty list of shapes to suppress.
     */
    public Set<ShapeId> getShapes() {
        return shapes;
    }

    /**
     * @return Returns the namespaces that are suppressed.
     */
    public Set<String> getNamespaceNames() {
        return namespaceNames;
    }

    /**
     * Given an event returns a mapped event that is either the same event
     * or a suppressed version of it.
     *
     * @param event Event to map over.
     * @return Returns the suppressed event or the original event.
     */
    public ValidationEvent suppress(ValidationEvent event) {
        if (!predicate.test(event)) {
            return event;
        }

        ValidationEvent.Builder builder = event.toBuilder().severity(Severity.SUPPRESSED);
        if (reason != null) {
            builder.suppressionReason(reason);
        }
        return builder.build();
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public String toString() {
        return "suppression of `" + validatorIds.stream().collect(Collectors.joining("`, `")) + "`";
    }

    /**
     * Builder used to create suppression definitions.
     */
    public static final class Builder implements SmithyBuilder<Suppression> {

        private SourceLocation sourceLocation = SourceLocation.none();
        private String reason;
        private final Set<String> shapes = new LinkedHashSet<>();
        private final Set<String> validatorIds = new LinkedHashSet<>();
        private final Set<String> namespaceNames = new HashSet<>();

        private Builder() {}

        public Builder sourceLocation(SourceLocation sourceLocation) {
            this.sourceLocation = Objects.requireNonNull(sourceLocation);
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder addShape(String shapeId) {
            Objects.requireNonNull(shapeId, "shapeId must not be null");
            if (shapeId.endsWith("#")) {
                namespaceNames.add(shapeId.substring(0, shapeId.length() - 1));
            } else {
                shapes.add(shapeId);
            }
            return this;
        }

        public Builder addShape(ShapeId shapeId) {
            shapes.add(Objects.requireNonNull(shapeId.toString(), "shapeId must not be null"));
            return this;
        }

        public Builder addValidatorId(String id) {
            this.validatorIds.add(Objects.requireNonNull(id, "validatorId must not be null"));
            return this;
        }

        @Override
        public Suppression build() {
            return new Suppression(this);
        }
    }
}
