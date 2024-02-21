/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.model.traits;

import java.util.Map;
import java.util.Objects;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Implementation of smithy.api#constrainShapes.
 */
public final class ConstrainShapesTrait extends AbstractTrait implements ToSmithyBuilder<ConstrainShapesTrait> {

    public static final ShapeId ID = ShapeId.from("smithy.api#constrainShapes");

    private final Map<String, Definition> definitions;

    private ConstrainShapesTrait(Builder builder) {
        super(ID, builder.sourceLocation);
        this.definitions = builder.definitions.copy();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected Node createNode() {
        ObjectNode.Builder builder = ObjectNode.builder();
        builder.sourceLocation(getSourceLocation());
        definitions.forEach((k, v) -> builder.withMember(k, v.toNode()));
        return builder.build();
    }

    @Override
    public Builder toBuilder() {
        Builder builder = builder().sourceLocation(getSourceLocation());
        definitions.forEach(builder::putDefinition);
        return builder;
    }

    /**
     * Get a map of ID suffixes to constraint definitions.
     *
     * @return Returns an unmodifiable map.
     */
    public Map<String, Definition> getDefinitions() {
        return definitions;
    }

    public static final class Definition implements ToNode {

        private final Selector selector;
        private final String message;
        private final Severity severity;

        public Definition(Selector selector, String message) {
            this(selector, message, Severity.ERROR);
        }

        public Definition(Selector selector, String message, Severity severity) {
            this.selector = selector;
            this.message = message;
            this.severity = severity;
        }

        public static Definition fromNode(Node node) {
            ObjectNode obj = node.expectObjectNode();
            Selector selector = Selector.fromNode(obj.expectStringMember("selector"));
            String message = obj.expectStringMember("message").getValue();
            Severity severity = obj.getStringMember("severity").map(Severity::fromNode).orElse(Severity.ERROR);
            return new Definition(selector, message, severity);
        }

        @Override
        public Node toNode() {
            ObjectNode.Builder builder = ObjectNode.builder();
            builder.withMember("selector", selector.toString());
            builder.withMember("message", message);
            if (severity != Severity.ERROR) {
                builder.withMember("severity", severity.toString());
            }
            return builder.build();
        }

        public Selector getSelector() {
            return selector;
        }

        public String getMessage() {
            return message;
        }

        public Severity getSeverity() {
            return severity;
        }

        @Override
        public int hashCode() {
            return Objects.hash(selector, message, severity);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Definition that = (Definition) o;
            return selector.equals(that.selector) && message.equals(that.message) && severity == that.severity;
        }
    }

    public static final class Builder extends AbstractTraitBuilder<ConstrainShapesTrait, Builder> {

        private final BuilderRef<Map<String, Definition>> definitions = BuilderRef.forOrderedMap();

        private Builder() {}

        @Override
        public ConstrainShapesTrait build() {
            return new ConstrainShapesTrait(this);
        }

        public Builder putDefinition(String idSuffix, Definition definition) {
            definitions.get().put(idSuffix, definition);
            return this;
        }

        public Builder removeDefinition(String idSuffix) {
            definitions.get().remove(idSuffix);
            return this;
        }
    }

    public static final class Provider implements TraitService {
        @Override
        public ShapeId getShapeId() {
            return ID;
        }

        @Override
        public ConstrainShapesTrait createTrait(ShapeId target, Node value) {
            Builder builder = builder().sourceLocation(value);
            value.expectObjectNode().getMembers().forEach((k, v) -> {
                String key = k.expectStringNode().getValue();
                ObjectNode valueNode = v.expectObjectNode();
                builder.putDefinition(key, Definition.fromNode(valueNode));
            });
            ConstrainShapesTrait result = builder.build();
            result.setNodeCache(value);
            return result;
        }
    }
}
