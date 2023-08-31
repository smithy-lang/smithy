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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.knowledge.NullableIndex;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeType;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.BigDecimalShape;
import software.amazon.smithy.model.shapes.BigIntegerShape;
import software.amazon.smithy.model.shapes.BlobShape;
import software.amazon.smithy.model.shapes.BooleanShape;
import software.amazon.smithy.model.shapes.ByteShape;
import software.amazon.smithy.model.shapes.DocumentShape;
import software.amazon.smithy.model.shapes.DoubleShape;
import software.amazon.smithy.model.shapes.FloatShape;
import software.amazon.smithy.model.shapes.IntegerShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.LongShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.ShortShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.validation.node.NodeValidatorPlugin;
import software.amazon.smithy.model.validation.node.TimestampValidationStrategy;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Validates {@link Node} values provided for {@link Shape} definitions.
 *
 * <p>This visitor validator is used to ensure that values provided for custom
 * traits and examples are correct for their schema definitions. A map of
 * shape types to a list of additional validators can be provided to perform
 * additional, non-standard, validation of these values. For example, this can
 * be used to provide additional validation needed for custom traits that are
 * applied to the shape of the data.
 */
public final class NodeValidationVisitor implements ShapeVisitor<List<ValidationEvent>> {

    private static final List<NodeValidatorPlugin> BUILTIN = NodeValidatorPlugin.getBuiltins();

    private final Model model;
    private final TimestampValidationStrategy timestampValidationStrategy;
    private final boolean allowOptionalNull;
    private String eventId;
    private Node value;
    private ShapeId eventShapeId;
    private String startingContext;
    private NodeValidatorPlugin.Context validationContext;
    private final NullableIndex nullableIndex;

    private NodeValidationVisitor(Builder builder) {
        this.model = SmithyBuilder.requiredState("model", builder.model);
        this.nullableIndex = NullableIndex.of(model);
        this.validationContext = new NodeValidatorPlugin.Context(model, builder.features.copy());
        this.timestampValidationStrategy = builder.timestampValidationStrategy;
        this.allowOptionalNull = builder.allowOptionalNull;
        setValue(SmithyBuilder.requiredState("value", builder.value));
        setStartingContext(builder.contextText);
        setValue(builder.value);
        setEventShapeId(builder.eventShapeId);
        setEventId(builder.eventId);
    }

    /**
     * Features to use when validating.
     */
    // TODO Move other features here like allowOptionalNull.
    public enum Feature {
        /**
         * Emit a warning when a range trait is incompatible with a default value of 0.
         *
         * <p>This was a common pattern in Smithy 1.0 and earlier. It implies that the value is effectively
         * required. However, changing the type of the value by un-boxing it or adjusting the range trait would
         * be a lossy transformation when migrating a model from 1.0 to 2.0.
         */
        RANGE_TRAIT_ZERO_VALUE_WARNING,

        // Lowers severity of constraint trait validations to WARNING.
        ALLOW_CONSTRAINT_ERRORS;

        public static Feature fromNode(Node node) {
            return Feature.valueOf(node.expectStringNode().getValue());
        }

        public static Node toNode(Feature feature) {
            return StringNode.from(feature.toString());
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Changes the Node value the visitor will evaluate.
     *
     * @param value Value to set.
     */
    public void setValue(Node value) {
        this.value = Objects.requireNonNull(value);
    }

    /**
     * Changes the shape ID that emitted events are associated with.
     *
     * @param eventShapeId Shape ID to set.
     */
    public void setEventShapeId(ShapeId eventShapeId) {
        this.eventShapeId = eventShapeId;
    }

    /**
     * Changes the starting context of the messages emitted by events.
     *
     * @param startingContext Starting context message to set.
     */
    public void setStartingContext(String startingContext) {
        this.startingContext = startingContext == null ? "" : startingContext;
    }

    /**
     * Changes the event ID emitted for events created by this validator.
     *
     * @param eventId Event ID to set.
     */
    public void setEventId(String eventId) {
        this.eventId = eventId == null ? Validator.MODEL_ERROR : eventId;
    }

    private NodeValidationVisitor traverse(String segment, Node node) {
        Builder builder = builder();
        builder.eventShapeId(eventShapeId);
        builder.eventId(eventId);
        builder.value(node);
        builder.model(model);
        builder.startingContext(startingContext.isEmpty() ? segment : (startingContext + "." + segment));
        builder.timestampValidationStrategy(timestampValidationStrategy);
        builder.allowOptionalNull(allowOptionalNull);
        NodeValidationVisitor visitor = new NodeValidationVisitor(builder);
        // Use the same validation context.
        visitor.validationContext = this.validationContext;
        return visitor;
    }

    @Override
    public List<ValidationEvent> blobShape(BlobShape shape) {
        return value.asStringNode()
                .map(stringNode -> applyPlugins(shape))
                .orElseGet(() -> invalidShape(shape, NodeType.STRING));
    }

    @Override
    public List<ValidationEvent> booleanShape(BooleanShape shape) {
        return value.isBooleanNode()
               ? applyPlugins(shape)
               : invalidShape(shape, NodeType.BOOLEAN);
    }

    @Override
    public List<ValidationEvent> byteShape(ByteShape shape) {
        return validateNaturalNumber(shape, Long.valueOf(Byte.MIN_VALUE), Long.valueOf(Byte.MAX_VALUE));
    }

    @Override
    public List<ValidationEvent> shortShape(ShortShape shape) {
        return validateNaturalNumber(shape, Long.valueOf(Short.MIN_VALUE), Long.valueOf(Short.MAX_VALUE));
    }

    @Override
    public List<ValidationEvent> integerShape(IntegerShape shape) {
        return validateNaturalNumber(shape, Long.valueOf(Integer.MIN_VALUE), Long.valueOf(Integer.MAX_VALUE));
    }

    @Override
    public List<ValidationEvent> longShape(LongShape shape) {
        return validateNaturalNumber(shape, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    @Override
    public List<ValidationEvent> bigIntegerShape(BigIntegerShape shape) {
        return validateNaturalNumber(shape, null, null);
    }

    private List<ValidationEvent> validateNaturalNumber(Shape shape, Long min, Long max) {
        return value.asNumberNode()
                .map(number -> {
                    if (!number.isNaturalNumber()) {
                        return ListUtils.of(event(String.format(
                                "%s shapes must not have floating point values, but found `%s` provided for `%s`",
                                shape.getType(), number.getValue(), shape.getId())));
                    }

                    Long numberValue = number.getValue().longValue();
                    if (min != null && numberValue < min) {
                        return ListUtils.of(event(String.format(
                                "%s value must be > %d, but found %d", shape.getType(), min, numberValue)));
                    } else if (max != null && numberValue > max) {
                        return ListUtils.of(event(String.format(
                                "%s value must be < %d, but found %d", shape.getType(), max, numberValue)));
                    } else {
                        return applyPlugins(shape);
                    }
                })
                .orElseGet(() -> invalidShape(shape, NodeType.NUMBER));
    }

    @Override
    public List<ValidationEvent> floatShape(FloatShape shape) {
        return value.isNumberNode() || value.isStringNode()
               ? applyPlugins(shape)
               : invalidShape(shape, NodeType.NUMBER);
    }

    @Override
    public List<ValidationEvent> documentShape(DocumentShape shape) {
        // Document values are always valid.
        return Collections.emptyList();
    }

    @Override
    public List<ValidationEvent> doubleShape(DoubleShape shape) {
        return value.isNumberNode() || value.isStringNode()
               ? applyPlugins(shape)
               : invalidShape(shape, NodeType.NUMBER);
    }

    @Override
    public List<ValidationEvent> bigDecimalShape(BigDecimalShape shape) {
        return value.isNumberNode()
               ? applyPlugins(shape)
               : invalidShape(shape, NodeType.NUMBER);
    }

    @Override
    public List<ValidationEvent> stringShape(StringShape shape) {
        return value.asStringNode()
                .map(string -> applyPlugins(shape))
                .orElseGet(() -> invalidShape(shape, NodeType.STRING));
    }

    @Override
    public List<ValidationEvent> timestampShape(TimestampShape shape) {
        return applyPlugins(shape);
    }

    @Override
    public List<ValidationEvent> listShape(ListShape shape) {
        return value.asArrayNode()
                .map(array -> {
                    MemberShape member = shape.getMember();
                    List<ValidationEvent> events = applyPlugins(shape);
                    // Each element creates a context with a numeric index (e.g., "foo.0.baz", "foo.1.baz", etc.).
                    for (int i = 0; i < array.getElements().size(); i++) {
                        events.addAll(member.accept(traverse(String.valueOf(i), array.getElements().get(i))));
                    }
                    return events;
                })
                .orElseGet(() -> invalidShape(shape, NodeType.ARRAY));
    }

    @Override
    public List<ValidationEvent> mapShape(MapShape shape) {
        return value.asObjectNode()
                .map(object -> {
                    List<ValidationEvent> events = applyPlugins(shape);
                    for (Map.Entry<StringNode, Node> entry : object.getMembers().entrySet()) {
                        String key = entry.getKey().getValue();
                        events.addAll(traverse(key + " (map-key)", entry.getKey()).memberShape(shape.getKey()));
                        events.addAll(traverse(key, entry.getValue()).memberShape(shape.getValue()));
                    }
                    return events;
                })
                .orElseGet(() -> invalidShape(shape, NodeType.OBJECT));
    }

    @Override
    public List<ValidationEvent> structureShape(StructureShape shape) {
        return value.asObjectNode()
                .map(object -> {
                    List<ValidationEvent> events = applyPlugins(shape);
                    Map<String, MemberShape> members = shape.getAllMembers();

                    for (Map.Entry<String, Node> entry : object.getStringMap().entrySet()) {
                        String entryKey = entry.getKey();
                        Node entryValue = entry.getValue();
                        if (!members.containsKey(entryKey)) {
                            String message = String.format(
                                    "Invalid structure member `%s` found for `%s`", entryKey, shape.getId());
                            events.add(event(message, Severity.WARNING, shape.getId().toString(), entryKey));
                        } else {
                            events.addAll(traverse(entryKey, entryValue).memberShape(members.get(entryKey)));
                        }
                    }

                    for (MemberShape member : members.values()) {
                        if (member.isRequired() && !object.getMember(member.getMemberName()).isPresent()) {
                            Severity severity = this.validationContext.hasFeature(Feature.ALLOW_CONSTRAINT_ERRORS)
                                    ? Severity.WARNING : Severity.ERROR;
                            events.add(event(String.format(
                                    "Missing required structure member `%s` for `%s`",
                                    member.getMemberName(), shape.getId()), severity));
                        }
                    }
                    return events;
                })
                .orElseGet(() -> invalidShape(shape, NodeType.OBJECT));
    }

    @Override
    public List<ValidationEvent> unionShape(UnionShape shape) {
        return value.asObjectNode()
                .map(object -> {
                    List<ValidationEvent> events = applyPlugins(shape);
                    if (object.size() > 1) {
                        events.add(event("union values can contain a value for only a single member"));
                    } else {
                        Map<String, MemberShape> members = shape.getAllMembers();
                        for (Map.Entry<String, Node> entry : object.getStringMap().entrySet()) {
                            String entryKey = entry.getKey();
                            Node entryValue = entry.getValue();
                            if (!members.containsKey(entryKey)) {
                                events.add(event(String.format(
                                        "Invalid union member `%s` found for `%s`", entryKey, shape.getId())));
                            } else {
                                events.addAll(traverse(entryKey, entryValue).memberShape(members.get(entryKey)));
                            }
                        }
                    }
                    return events;
                })
                .orElseGet(() -> invalidShape(shape, NodeType.OBJECT));
    }

    @Override
    public List<ValidationEvent> memberShape(MemberShape shape) {
        List<ValidationEvent> events = applyPlugins(shape);
        model.getShape(shape.getTarget()).ifPresent(target -> {
            // We only need to keep track of a single referring member, so a stack of members or anything like that
            // isn't needed here.
            validationContext.setReferringMember(shape);
            events.addAll(target.accept(this));
            validationContext.setReferringMember(null);
        });
        return events;
    }

    @Override
    public List<ValidationEvent> operationShape(OperationShape shape) {
        return invalidSchema(shape);
    }

    @Override
    public List<ValidationEvent> resourceShape(ResourceShape shape) {
        return invalidSchema(shape);
    }

    @Override
    public List<ValidationEvent> serviceShape(ServiceShape shape) {
        return invalidSchema(shape);
    }

    private List<ValidationEvent> invalidShape(Shape shape, NodeType expectedType) {
        // Nullable shapes allow null values.
        if (allowOptionalNull && value.isNullNode()) {
            // Non-members are nullable. Members are nullable based on context.
            if (!shape.isMemberShape() || shape.asMemberShape().filter(nullableIndex::isMemberNullable).isPresent()) {
                return Collections.emptyList();
            }
        }

        String message = String.format(
                "Expected %s value for %s shape, `%s`; found %s value",
                expectedType, shape.getType(), shape.getId(), value.getType());
        if (value.isStringNode()) {
            message += ", `" + value.expectStringNode().getValue() + "`";
        } else if (value.isNumberNode()) {
            message += ", `" + value.expectNumberNode().getValue() + "`";
        } else if (value.isBooleanNode()) {
            message += ", `" + value.expectBooleanNode().getValue() + "`";
        }
        return ListUtils.of(event(message));
    }

    private List<ValidationEvent> invalidSchema(Shape shape) {
        return ListUtils.of(event("Encountered invalid shape type: " + shape.getType()));
    }

    private ValidationEvent event(String message, String... additionalEventIdParts) {
        return event(message, Severity.ERROR, additionalEventIdParts);
    }

    private ValidationEvent event(String message, Severity severity, String... additionalEventIdParts) {
        return event(message, severity, value.getSourceLocation(), additionalEventIdParts);
    }

    private ValidationEvent event(
            String message,
            Severity severity,
            SourceLocation sourceLocation,
            String... additionalEventIdParts
    ) {
        return ValidationEvent.builder()
                .id(additionalEventIdParts.length > 0
                        ? eventId + "." + String.join(".", additionalEventIdParts) : eventId)
                .severity(severity)
                .sourceLocation(sourceLocation)
                .shapeId(eventShapeId)
                .message(startingContext.isEmpty() ? message : startingContext + ": " + message)
                .build();
    }

    private List<ValidationEvent> applyPlugins(Shape shape) {
        List<ValidationEvent> events = new ArrayList<>();
        timestampValidationStrategy.apply(shape, value, validationContext,
                (location, severity, message, additionalEventIdParts) ->
                        events.add(event(message, severity, location.getSourceLocation(), additionalEventIdParts)));

        for (NodeValidatorPlugin plugin : BUILTIN) {
            plugin.apply(shape, value, validationContext,
                    (location, severity, message, additionalEventIdParts) ->
                            events.add(event(message, severity, location.getSourceLocation(), additionalEventIdParts)));
        }

        return events;
    }

    /**
     * Builds a {@link NodeValidationVisitor}.
     */
    public static final class Builder implements SmithyBuilder<NodeValidationVisitor> {
        private String eventId;
        private String contextText;
        private ShapeId eventShapeId;
        private Node value;
        private Model model;
        private TimestampValidationStrategy timestampValidationStrategy = TimestampValidationStrategy.FORMAT;
        private boolean allowOptionalNull;
        private final BuilderRef<Set<Feature>> features = BuilderRef.forUnorderedSet();

        Builder() {}

        /**
         * Sets the <strong>required</strong> model to use when traversing
         * walking shapes during validation.
         *
         * @param model Model that contains shapes to validate.
         * @return Returns the builder.
         */
        public Builder model(Model model) {
            this.model = model;
            return this;
        }

        /**
         * Sets the <strong>required</strong> node value to validate.
         *
         * @param value Value to validate.
         * @return Returns the builder.
         */
        public Builder value(Node value) {
            this.value = Objects.requireNonNull(value);
            return this;
        }

        /**
         * Sets an optional custom event ID to use for created validation events.
         *
         * @param id Custom event ID.
         * @return Returns the builder.
         */
        public Builder eventId(String id) {
            this.eventId = Objects.requireNonNull(id);
            return this;
        }

        /**
         * Sets an optional starting context of the validator that is prepended
         * to each emitted validation event message.
         *
         * @param contextText Starting event message content.
         * @return Returns the builder.
         */
        public Builder startingContext(String contextText) {
            this.contextText = Objects.requireNonNull(contextText);
            return this;
        }

        /**
         * Sets an optional shape ID that is used as the shape ID in each
         * validation event emitted by the validator.
         *
         * @param eventShapeId Shape ID to set on every validation event.
         * @return Returns the builder.
         */
        public Builder eventShapeId(ShapeId eventShapeId) {
            this.eventShapeId = eventShapeId;
            return this;
        }

        /**
         * Sets the strategy used to validate timestamps.
         *
         * <p>By default, timestamps are validated using
         * {@link TimestampValidationStrategy#FORMAT}.
         *
         * @param timestampValidationStrategy Timestamp validation strategy.
         * @return Returns the builder.
         */
        public Builder timestampValidationStrategy(TimestampValidationStrategy timestampValidationStrategy) {
            this.timestampValidationStrategy = timestampValidationStrategy;
            return this;
        }

        @Deprecated
        public Builder allowBoxedNull(boolean allowBoxedNull) {
            return allowOptionalNull(allowBoxedNull);
        }

        /**
         * Configure how null values are handled when they are provided for
         * optional types.
         *
         * <p>By default, null values are not allowed for optional types.
         *
         * @param allowOptionalNull Set to true to allow null values for optional shapes.
         * @return Returns the builder.
         */
        public Builder allowOptionalNull(boolean allowOptionalNull) {
            this.allowOptionalNull = allowOptionalNull;
            return this;
        }

        /**
         * Adds a feature flag to the validator.
         *
         * @param feature Feature to set.
         * @return Returns the builder.
         */
        @SmithyInternalApi
        public Builder addFeature(Feature feature) {
            this.features.get().add(feature);
            return this;
        }

        @Override
        public NodeValidationVisitor build() {
            return new NodeValidationVisitor(this);
        }
    }
}
