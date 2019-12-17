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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.BoxIndex;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeType;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.BigDecimalShape;
import software.amazon.smithy.model.shapes.BigIntegerShape;
import software.amazon.smithy.model.shapes.BlobShape;
import software.amazon.smithy.model.shapes.BooleanShape;
import software.amazon.smithy.model.shapes.ByteShape;
import software.amazon.smithy.model.shapes.CollectionShape;
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
import software.amazon.smithy.model.shapes.SetShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.ShortShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.validation.node.BlobLengthPlugin;
import software.amazon.smithy.model.validation.node.CollectionLengthPlugin;
import software.amazon.smithy.model.validation.node.IdRefPlugin;
import software.amazon.smithy.model.validation.node.MapLengthPlugin;
import software.amazon.smithy.model.validation.node.NodeValidatorPlugin;
import software.amazon.smithy.model.validation.node.PatternTraitPlugin;
import software.amazon.smithy.model.validation.node.RangeTraitPlugin;
import software.amazon.smithy.model.validation.node.StringEnumPlugin;
import software.amazon.smithy.model.validation.node.StringLengthPlugin;
import software.amazon.smithy.model.validation.node.TimestampValidationStrategy;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyBuilder;

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

    private final String eventId;
    private final Node value;
    private final Model model;
    private final String context;
    private final ShapeId eventShapeId;
    private final List<NodeValidatorPlugin> plugins;
    private final TimestampValidationStrategy timestampValidationStrategy;
    private final boolean allowBoxedNull;

    private NodeValidationVisitor(Builder builder) {
        this.value = SmithyBuilder.requiredState("value", builder.value);
        this.model = SmithyBuilder.requiredState("model", builder.model);
        this.context = builder.context;
        this.eventId = builder.eventId;
        this.eventShapeId = builder.eventShapeId;
        this.timestampValidationStrategy = builder.timestampValidationStrategy;
        this.allowBoxedNull = builder.allowBoxedNull;

        plugins = Arrays.asList(
                new BlobLengthPlugin(),
                new CollectionLengthPlugin(),
                new IdRefPlugin(),
                new MapLengthPlugin(),
                new PatternTraitPlugin(),
                new RangeTraitPlugin(),
                new StringEnumPlugin(),
                new StringLengthPlugin(),
                timestampValidationStrategy
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    private NodeValidationVisitor withNode(String segment, Node node) {
        Builder builder = builder();
        builder.eventShapeId(eventShapeId);
        builder.eventId(eventId);
        builder.value(node);
        builder.model(model);
        builder.startingContext(context.isEmpty() ? segment : (context + "." + segment));
        builder.timestampValidationStrategy(timestampValidationStrategy);
        builder.allowBoxedNull(allowBoxedNull);
        return new NodeValidationVisitor(builder);
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
                        return ListUtils.of(event(
                                shape.getType() + " shapes must not have floating point values, but found `"
                                + number.getValue() + "` provided for `" + shape.getId() + "`"));
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
        return value.isNumberNode()
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
        return value.isNumberNode()
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
        return processCollection(shape, shape.getMember());
    }

    @Override
    public List<ValidationEvent> setShape(SetShape shape) {
        return processCollection(shape, shape.getMember());
    }

    private List<ValidationEvent> processCollection(CollectionShape shape, MemberShape member) {
        return value.asArrayNode()
                .map(array -> {
                    List<ValidationEvent> events = applyPlugins(shape);
                    // Each element creates a context with a numeric index (e.g., "foo.0.baz", "foo.1.baz", etc.).
                    for (int i = 0; i < array.getElements().size(); i++) {
                        events.addAll(member.accept(withNode(String.valueOf(i), array.getElements().get(i))));
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
                        events.addAll(withNode(key + " (map-key)", entry.getKey()).memberShape(shape.getKey()));
                        events.addAll(withNode(key, entry.getValue()).memberShape(shape.getValue()));
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
                    object.getMembers().forEach((keyNode, value) -> {
                        String key = keyNode.getValue();
                        if (!members.containsKey(key)) {
                            events.add(event("Invalid structure member `" + key + "` found for `"
                                             + shape.getId() + "`"));
                        } else {
                            events.addAll(withNode(key, value).memberShape(members.get(key)));
                        }
                    });
                    members.forEach((memberName, member) -> {
                        if (member.isRequired()
                                && !object.getMember(memberName).isPresent()
                                // Ignore missing required primitive members because they have a default value.
                                && !isMemberPrimitive(member)) {
                            events.add(event("Missing required structure member `" + memberName + "` for `"
                                             + shape.getId() + "`"));
                        }
                    });
                    return events;
                })
                .orElseGet(() -> invalidShape(shape, NodeType.OBJECT));
    }

    private boolean isMemberPrimitive(MemberShape member) {
        return !model.getKnowledge(BoxIndex.class).isBoxed(member);
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
                        object.getMembers().forEach((keyNode, value) -> {
                            String key = keyNode.getValue();
                            if (!members.containsKey(key)) {
                                events.add(event(
                                        "Invalid union member `" + key + "` found for `" + shape.getId() + "`"));
                            } else {
                                events.addAll(withNode(key, value).memberShape(members.get(key)));
                            }
                        });
                    }
                    return events;
                })
                .orElseGet(() -> invalidShape(shape, NodeType.OBJECT));
    }

    @Override
    public List<ValidationEvent> memberShape(MemberShape shape) {
        List<ValidationEvent> events = applyPlugins(shape);
        events.addAll(model.getShape(shape.getTarget())
                              .map(member -> member.accept(this))
                              .orElse(ListUtils.of()));
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
        // Boxed shapes allow null values.
        if (allowBoxedNull && value.isNullNode() && model.getKnowledge(BoxIndex.class).isBoxed(shape)) {
            return Collections.emptyList();
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

    private ValidationEvent event(String message) {
        return ValidationEvent.builder()
                .eventId(eventId)
                .severity(Severity.ERROR)
                .sourceLocation(value.getSourceLocation())
                .shapeId(eventShapeId)
                .message(context.isEmpty() ? message : context + ": " + message)
                .build();
    }

    private List<ValidationEvent> applyPlugins(Shape shape) {
        return plugins.stream()
                .flatMap(plugin -> plugin.apply(shape, value, model).stream())
                .map(this::event)
                .collect(Collectors.toList());
    }

    /**
     * Builds a {@link NodeValidationVisitor}.
     */
    public static final class Builder implements SmithyBuilder<NodeValidationVisitor> {
        private String eventId = Validator.MODEL_ERROR;
        private String context = "";
        private ShapeId eventShapeId;
        private Node value;
        private Model model;
        private TimestampValidationStrategy timestampValidationStrategy = TimestampValidationStrategy.FORMAT;
        private boolean allowBoxedNull;

        Builder() {}

        @Deprecated
        public Builder index(ShapeIndex index) {
            this.model = Model.builder().shapeIndex(index).build();
            return this;
        }

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
         * @param context Starting event message content.
         * @return Returns the builder.
         */
        public Builder startingContext(String context) {
            this.context = Objects.requireNonNull(context);
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

        /**
         * Configure how null values are handled when they are provided for
         * boxed types.
         *
         * <p>By default, null values are not allowed for boxed types.
         *
         * @param allowBoxedNull Set to true to allow null values for boxed shapes.
         * @return Returns the builder.
         */
        public Builder allowBoxedNull(boolean allowBoxedNull) {
            this.allowBoxedNull = allowBoxedNull;
            return this;
        }

        @Override
        public NodeValidationVisitor build() {
            return new NodeValidationVisitor(this);
        }
    }
}
