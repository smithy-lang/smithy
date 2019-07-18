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

package software.amazon.smithy.model.traits;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Constrains string values to one of the predefined enum constants.
 */
public final class EnumTrait extends AbstractTrait implements ToSmithyBuilder<EnumTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.api#enum");

    private final Map<String, EnumConstantBody> constants;

    private EnumTrait(Builder builder) {
        super(ID, builder.sourceLocation);
        this.constants = Collections.unmodifiableMap(new LinkedHashMap<>(builder.constants));
        if (constants.isEmpty()) {
            throw new SourceException("enum must have at least one entry", getSourceLocation());
        }
    }

    /**
     * Gets the enum value to body.
     *
     * @return returns the enum constant mapping.
     */
    public Map<String, EnumConstantBody> getValues() {
        return constants;
    }

    @Override
    protected Node createNode() {
        return constants.entrySet().stream()
                .map(entry -> {
                    ObjectNode value = Node.objectNode()
                            .withOptionalMember(EnumConstantBody.NAME, entry.getValue().getName().map(Node::from))
                            .withOptionalMember(EnumConstantBody.DOCUMENTATION,
                                                entry.getValue().getDocumentation().map(Node::from));
                    if (!entry.getValue().getTags().isEmpty()) {
                        value = value.withMember(EnumConstantBody.TAGS, entry.getValue().getTags().stream()
                                .map(Node::from)
                                .collect(ArrayNode.collect()));
                    }
                    return Pair.of(entry.getKey(), value);
                })
                .collect(ObjectNode.collectStringKeys(Pair::getLeft, Pair::getRight));
    }

    @Override
    public Builder toBuilder() {
        Builder builder = builder().sourceLocation(getSourceLocation());
        constants.forEach(builder::addEnum);
        return builder;
    }

    /**
     * @return Returns an enum trait builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder used to create the enum trait.
     */
    public static final class Builder extends AbstractTraitBuilder<EnumTrait, Builder> {
        private final Map<String, EnumConstantBody> constants = new LinkedHashMap<>();

        public Builder addEnum(String name, EnumConstantBody value) {
            constants.put(Objects.requireNonNull(name), Objects.requireNonNull(value));
            return this;
        }

        public Builder removeEnum(String value) {
            constants.remove(value);
            return this;
        }

        public Builder clearEnums() {
            constants.clear();
            return this;
        }

        @Override
        public EnumTrait build() {
            return new EnumTrait(this);
        }
    }

    public static final class Provider implements TraitService {
        @Override
        public ShapeId getShapeId() {
            return ID;
        }

        @Override
        public EnumTrait createTrait(ShapeId target, Node value) {
            Builder builder = builder().sourceLocation(value);
            value.expectObjectNode().getMembers().forEach((k, v) -> {
                builder.addEnum(k.expectStringNode().getValue(), parseBody(v.expectObjectNode()));
            });
            return builder.build();
        }

        private EnumConstantBody parseBody(ObjectNode value) {
            value.warnIfAdditionalProperties(Arrays.asList(
                    EnumConstantBody.NAME, EnumConstantBody.DOCUMENTATION, EnumConstantBody.TAGS));
            EnumConstantBody.Builder builder = EnumConstantBody.builder()
                    .name(value.getStringMember(EnumConstantBody.NAME).map(StringNode::getValue).orElse(null))
                    .documentation(value.getStringMember(EnumConstantBody.DOCUMENTATION)
                                           .map(StringNode::getValue)
                                           .orElse(null));

            value.getMember(EnumConstantBody.TAGS).ifPresent(node -> {
                builder.tags(Node.loadArrayOfString(EnumConstantBody.TAGS, node));
            });

            return builder.build();
        }
    }
}
