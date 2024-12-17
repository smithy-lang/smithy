/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Implementation of smithy.api#traitValidators.
 */
public final class TraitValidatorsTrait extends AbstractTrait implements ToSmithyBuilder<TraitValidatorsTrait> {

    public static final ShapeId ID = ShapeId.from("smithy.api#traitValidators");

    private final Map<String, Validator> validators;

    private TraitValidatorsTrait(Builder builder) {
        super(ID, builder.sourceLocation);
        this.validators = builder.validators.copy();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected Node createNode() {
        ObjectNode.Builder builder = ObjectNode.builder();
        builder.sourceLocation(getSourceLocation());
        validators.forEach((k, v) -> builder.withMember(k, v.toNode()));
        return builder.build();
    }

    @Override
    public Builder toBuilder() {
        Builder builder = builder().sourceLocation(getSourceLocation());
        validators.forEach(builder::putValidator);
        return builder;
    }

    /**
     * Get a map of ID suffixes to validators.
     *
     * @return Returns an unmodifiable map.
     */
    public Map<String, Validator> getValidators() {
        return validators;
    }

    public static final class Validator implements ToNode {

        private final Selector selector;
        private final String message;
        private final Severity severity;

        public Validator(Selector selector, String message) {
            this(selector, message, Severity.ERROR);
        }

        public Validator(Selector selector, String message, Severity severity) {
            this.selector = selector;
            this.message = message;
            this.severity = severity;
        }

        public static Validator fromNode(Node node) {
            ObjectNode obj = node.expectObjectNode();
            Selector selector = Selector.fromNode(obj.expectStringMember("selector"));
            String message = obj.getStringMember("message").map(StringNode::getValue).orElse(null);
            Severity severity = obj.getStringMember("severity").map(Severity::fromNode).orElse(Severity.ERROR);
            return new Validator(selector, message, severity);
        }

        @Override
        public Node toNode() {
            ObjectNode.Builder builder = ObjectNode.builder();
            builder.withMember("selector", selector.toString());
            if (message != null) {
                builder.withMember("message", message);
            }
            if (severity != Severity.ERROR) {
                builder.withMember("severity", severity.toString());
            }
            return builder.build();
        }

        public Selector getSelector() {
            return selector;
        }

        public Optional<String> getMessage() {
            return Optional.ofNullable(message);
        }

        public Severity getSeverity() {
            return severity;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Validator validator = (Validator) o;
            return selector.equals(validator.selector)
                    && Objects.equals(message, validator.message)
                    && severity == validator.severity;
        }

        @Override
        public int hashCode() {
            return Objects.hash(selector, message, severity);
        }
    }

    public static final class Builder extends AbstractTraitBuilder<TraitValidatorsTrait, Builder> {

        private final BuilderRef<Map<String, Validator>> validators = BuilderRef.forOrderedMap();

        private Builder() {}

        @Override
        public TraitValidatorsTrait build() {
            return new TraitValidatorsTrait(this);
        }

        public Builder putValidator(String idSuffix, Validator validator) {
            validators.get().put(idSuffix, validator);
            return this;
        }

        public Builder removeValidator(String idSuffix) {
            validators.get().remove(idSuffix);
            return this;
        }
    }

    public static final class Provider implements TraitService {
        @Override
        public ShapeId getShapeId() {
            return ID;
        }

        @Override
        public TraitValidatorsTrait createTrait(ShapeId target, Node value) {
            Builder builder = builder().sourceLocation(value);
            value.expectObjectNode().getMembers().forEach((k, v) -> {
                String key = k.expectStringNode().getValue();
                ObjectNode valueNode = v.expectObjectNode();
                builder.putValidator(key, Validator.fromNode(valueNode));
            });
            TraitValidatorsTrait result = builder.build();
            result.setNodeCache(value);
            return result;
        }
    }
}
