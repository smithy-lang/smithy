/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation;

import static software.amazon.smithy.model.validation.Severity.ERROR;
import static software.amazon.smithy.model.validation.Validator.MODEL_ERROR;

import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * A validation event created when validating a model.
 *
 * <p>Validation events are collection while assembling and validating a model.
 * Events with a severity less than ERROR can be suppressed. All events contain
 * a message, severity, and eventId.
 */
public final class ValidationEvent
        implements FromSourceLocation, Comparable<ValidationEvent>, ToNode, ToSmithyBuilder<ValidationEvent> {
    private static final ValidationEventFormatter DEFAULT_FORMATTER = new LineValidationEventFormatter();
    private final SourceLocation sourceLocation;
    private final String message;
    private final String eventId;
    private final Severity severity;
    private final ShapeId shapeId;
    private final String suppressionReason;
    private final String hint;
    private int hash;

    private ValidationEvent(Builder builder) {
        if (builder.suppressionReason != null && builder.severity != Severity.SUPPRESSED) {
            throw new IllegalStateException("A suppression reason must only be provided for SUPPRESSED events");
        }

        this.sourceLocation = SmithyBuilder.requiredState("sourceLocation", builder.sourceLocation);
        this.message = SmithyBuilder.requiredState("message", builder.message);
        this.severity = SmithyBuilder.requiredState("severity", builder.severity);
        this.eventId = SmithyBuilder.requiredState("id", builder.eventId);
        this.shapeId = builder.shapeId;
        this.suppressionReason = builder.suppressionReason;
        this.hint = builder.hint;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new ValidationEvent from a {@link SourceException}.
     *
     * @param exception Exception to use to create the event.
     * @return Returns a created validation event with an ID of Model.
     */
    public static ValidationEvent fromSourceException(SourceException exception) {
        return fromSourceException(exception, "");
    }

    /**
     * Creates a new ValidationEvent from a {@link SourceException}.
     *
     * @param exception Exception to use to create the event.
     * @param prefix Prefix string to add to the message.
     * @return Returns a created validation event with an ID of Model.
     */
    public static ValidationEvent fromSourceException(SourceException exception, String prefix) {
        // Extract shape IDs from exceptions that implement ToShapeId.
        ShapeId id = (exception instanceof ToShapeId)
                ? ((ToShapeId) exception).toShapeId()
                : null;
        return fromSourceException(exception, prefix, id);
    }

    /**
     * Creates a new ValidationEvent from a {@link SourceException}.
     *
     * @param exception Exception to use to create the event.
     * @param prefix Prefix string to add to the message.
     * @param shapeId ShapeId to associate with the event.
     * @return Returns a created validation event with an ID of Model.
     */
    public static ValidationEvent fromSourceException(SourceException exception, String prefix, ShapeId shapeId) {
        // Get the message without source location since it's in the event.
        return ValidationEvent.builder()
                .id(MODEL_ERROR)
                .severity(ERROR)
                .message(prefix + exception.getMessageWithoutLocation())
                .sourceLocation(exception.getSourceLocation())
                .shapeId(shapeId)
                .build();
    }

    @Override
    public int compareTo(ValidationEvent other) {
        int comparison = getSourceLocation().getFilename().compareTo(other.getSourceLocation().getFilename());
        if (comparison != 0) {
            return comparison;
        }

        comparison = Integer.compare(getSourceLocation().getLine(), other.getSourceLocation().getLine());
        if (comparison != 0) {
            return comparison;
        }

        comparison = Integer.compare(getSourceLocation().getColumn(), other.getSourceLocation().getColumn());
        if (comparison != 0) {
            return comparison;
        }

        // Fall back to a comparison that favors by severity, followed, by shape ID, etc...
        return toString().compareTo(other.toString());
    }

    @Override
    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.sourceLocation = sourceLocation;
        builder.message = message;
        builder.severity = severity;
        builder.eventId = eventId;
        builder.shapeId = shapeId;
        builder.suppressionReason = suppressionReason;
        builder.hint = hint;
        return builder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof ValidationEvent)) {
            return false;
        }

        ValidationEvent other = (ValidationEvent) o;
        return sourceLocation.equals(other.sourceLocation)
                && message.equals(other.message)
                && severity.equals(other.severity)
                && eventId.equals(other.eventId)
                && getShapeId().equals(other.getShapeId())
                && getSuppressionReason().equals(other.getSuppressionReason())
                && getHint().equals(other.getHint());
    }

    @Override
    public int hashCode() {
        int result = hash;
        if (result == 0) {
            result = Objects.hash(eventId, shapeId, severity, sourceLocation, message, suppressionReason, hint);
            hash = result;
        }
        return result;
    }

    @Override
    public String toString() {
        return DEFAULT_FORMATTER.format(this);
    }

    @Override
    public Node toNode() {
        return Node.objectNodeBuilder()
                .withMember("id", Node.from(getId()))
                .withMember("severity", Node.from(getSeverity().toString()))
                .withOptionalMember("shapeId", getShapeId().map(Object::toString).map(Node::from))
                .withMember("message", Node.from(getMessage()))
                .withOptionalMember("suppressionReason", getSuppressionReason().map(Node::from))
                .withOptionalMember("hint", getHint().map(Node::from))
                .withMember("filename", Node.from(getSourceLocation().getFilename()))
                .withMember("line", Node.from(getSourceLocation().getLine()))
                .withMember("column", Node.from(getSourceLocation().getColumn()))
                .build();
    }

    public static ValidationEvent fromNode(Node node) {
        ObjectNode objectNode = node.expectObjectNode();

        // A source location should always have at least a filename in the node
        // representation of a ValidationEvent. Expect that and default the
        // other properties.
        SourceLocation location = new SourceLocation(
                objectNode.expectStringMember("filename").getValue(),
                objectNode.getNumberMemberOrDefault("line", 0).intValue(),
                objectNode.getNumberMemberOrDefault("column", 0).intValue());
        Builder builder = builder().sourceLocation(location);
        objectNode.expectStringMember("id", builder::id)
                .expectMember("severity", Severity::fromNode, builder::severity)
                .expectStringMember("message", builder::message)
                .getStringMember("suppressionReason", builder::suppressionReason)
                .getStringMember("hint", builder::hint)
                .getMember("shapeId", ShapeId::fromNode, builder::shapeId);
        return builder.build();
    }

    /**
     * @return The location at which the event occurred.
     */
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    /**
     * @return The human-readable event message.
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return The severity level of the event.
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * Returns the identifier of the validation event.
     *
     * <p>The validation event identifier can be used to suppress events.
     *
     * @return Returns the event ID.
     * @deprecated Use the {@code getId()} method to match the node format.
     */
    public String getEventId() {
        return getId();
    }

    /**
     * Tests if the event ID hierarchically contains the given ID.
     *
     * <p>Event IDs that contain dots (.) are hierarchical. An event ID of
     * {@code "Foo.Bar"} contains the ID {@code "Foo"} and {@code "Foo.Bar"}.
     * However, an event ID of {@code "Foo"} does not contain the ID
     * {@code "Foo.Bar"} as {@code "Foo.Bar"} is more specific than {@code "Foo"}.
     * If an event ID exactly matches the given {@code id}, then it also contains
     * the ID (for example, {@code "Foo.Bar."} contains {@code "Foo.Bar."}.
     *
     * @param id ID to test.
     * @return Returns true if the event's event ID contains the given {@code id}.
     */
    public boolean containsId(String id) {
        int eventLength = eventId.length();
        int suppressionLength = id.length();
        if (suppressionLength == eventLength) {
            return id.equals(eventId);
        } else if (suppressionLength > eventLength) {
            return false;
        } else {
            return eventId.startsWith(id) && eventId.charAt(id.length()) == '.';
        }
    }

    /**
     * Returns the identifier of the validation event.
     *
     * <p>The validation event identifier can be used to suppress events.
     *
     * @return Returns the event ID.
     */
    public String getId() {
        return eventId;
    }

    /**
     * @return The shape ID that is associated with the event.
     */
    public Optional<ShapeId> getShapeId() {
        return Optional.ofNullable(shapeId);
    }

    /**
     * Get the reason that the event was suppressed.
     *
     * @return Returns the suppression reason if available.
     */
    public Optional<String> getSuppressionReason() {
        return Optional.ofNullable(suppressionReason);
    }

    /**
     * Get an optional hint that adds more detail about how to fix a specific issue.
     *
     * @return Returns the hint if available.
     */
    public Optional<String> getHint() {
        return Optional.ofNullable(hint);
    }

    /**
     * Builds ValidationEvent values.
     */
    public static final class Builder implements SmithyBuilder<ValidationEvent> {

        private SourceLocation sourceLocation = SourceLocation.none();
        private String message;
        private Severity severity;
        private String eventId;
        private ShapeId shapeId;
        private String suppressionReason;
        private String hint;

        private Builder() {}

        /**
         * Sets the required message of the event.
         *
         * @param eventMessage Message to set.
         * @return Returns the builder.
         */
        public Builder message(String eventMessage) {
            message = Objects.requireNonNull(eventMessage);
            return this;
        }

        public Builder message(String eventMessage, Object... placeholders) {
            return message(String.format(eventMessage, placeholders));
        }

        /**
         * Sets the required severity of the event.
         * @param severity Event severity.
         * @return Returns the builder.
         */
        public Builder severity(Severity severity) {
            this.severity = Objects.requireNonNull(severity);
            return this;
        }

        /**
         * Sets the required event ID of the event.
         *
         * @param eventId Event ID.
         * @return Returns the builder.
         * @deprecated Use the {@code id(String eventId)} setter to match the node format.
         */
        public Builder eventId(final String eventId) {
            return id(eventId);
        }

        /**
         * Sets the required event ID of the event.
         *
         * @param eventId Event ID.
         * @return Returns the builder.
         */
        public Builder id(final String eventId) {
            this.eventId = Objects.requireNonNull(eventId);
            return this;
        }

        /**
         * Sets the source location of where the event occurred.
         *
         * @param sourceLocation Event source location.
         * @return Returns the builder.
         */
        public Builder sourceLocation(FromSourceLocation sourceLocation) {
            this.sourceLocation = Objects.requireNonNull(sourceLocation.getSourceLocation());
            return this;
        }

        /**
         * Sets the shape ID related to the event.
         *
         * @param toShapeId Shape ID.
         * @param <T> Value to convert to a shape ID.
         * @return Returns the builder.
         */
        public <T extends ToShapeId> Builder shapeId(T toShapeId) {
            this.shapeId = toShapeId == null ? null : toShapeId.toShapeId();
            return this;
        }

        /**
         * Sets the shape ID and source location based on a shape.
         *
         * @param encounteredShape Shape.
         * @return Returns the builder.
         */
        public Builder shape(Shape encounteredShape) {
            return sourceLocation(Objects.requireNonNull(encounteredShape).getSourceLocation())
                    .shapeId(encounteredShape.getId());
        }

        /**
         * Sets a reason for suppressing the event.
         *
         * <p>This is only relevant if the severity is SUPPRESSED.
         *
         * @param eventSuppressionReason Event suppression reason.
         * @return Returns the builder.
         */
        public Builder suppressionReason(String eventSuppressionReason) {
            suppressionReason = eventSuppressionReason;
            return this;
        }

        /**
         * Sets an optional hint adding more detail about how to fix a specific issue.
         *
         * @param hint Hint to set
         * @return Returns the builder.
         */
        public Builder hint(String hint) {
            this.hint = hint;
            return this;
        }

        @Override
        public ValidationEvent build() {
            return new ValidationEvent(this);
        }
    }
}
