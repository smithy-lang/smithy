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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Constrains string values to one of the predefined enum constants.
 */
public final class EnumTrait extends AbstractTrait implements ToSmithyBuilder<EnumTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.api#enum");

    private final List<EnumDefinition> definitions;

    private EnumTrait(Builder builder) {
        super(ID, builder.sourceLocation);
        this.definitions = ListUtils.copyOf(builder.definitions);
        if (definitions.isEmpty()) {
            throw new SourceException("enum must have at least one entry", getSourceLocation());
        }
    }

    /**
     * Gets the enum value to body.
     *
     * @return returns the enum constant definitions.
     */
    public List<EnumDefinition> getValues() {
        return definitions;
    }

    /**
     * Gets the acceptable enum literal values.
     *
     * @return returns the enum constant definitions.
     */
    public List<String> getEnumDefinitionValues() {
        return definitions.stream().map(EnumDefinition::getValue).collect(Collectors.toList());
    }

    /**
     * Checks if all of the constants of an enum define a name.
     *
     * <p>Note that either all constants must have a name or no constants can
     * have a name.
     *
     * @return Returns true if all constants define a name.
     */
    public boolean hasNames() {
        return definitions.stream().allMatch(body -> body.getName().isPresent());
    }

    @Override
    protected Node createNode() {
        return definitions.stream().map(EnumDefinition::toNode).collect(ArrayNode.collect());
    }

    @Override
    public Builder toBuilder() {
        Builder builder = builder().sourceLocation(getSourceLocation());
        definitions.forEach(builder::addEnum);
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
        private final List<EnumDefinition> definitions = new ArrayList<>();

        public Builder addEnum(EnumDefinition value) {
            definitions.add(value);
            return this;
        }

        public Builder removeEnum(String value) {
            definitions.removeIf(def -> def.getValue().equals(value));
            return this;
        }

        public Builder removeEnumByName(String name) {
            definitions.removeIf(def -> def.getName().filter(n -> n.equals(name)).isPresent());
            return this;
        }

        public Builder clearEnums() {
            definitions.clear();
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
            for (ObjectNode definition : value.expectArrayNode().getElementsAs(ObjectNode.class)) {
                builder.addEnum(EnumDefinition.fromNode(definition));
            }
            return builder.build();
        }
    }
}
