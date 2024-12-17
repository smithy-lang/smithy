/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.List;
import java.util.stream.Collectors;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.synthetic.SyntheticEnumTrait;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Constrains string values to one of the predefined enum constants.
 *
 * <p>This trait is deprecated, use an {@link EnumShape} instead.
 *
 * <p>There is also the {@link SyntheticEnumTrait}, which is a synthetic variant of this
 * trait used exclusively to assist in making {@link EnumShape} as backwards compatible
 * as possible.
 */
@Deprecated
public class EnumTrait extends AbstractTrait implements ToSmithyBuilder<EnumTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.api#enum");

    private final List<EnumDefinition> definitions;

    protected EnumTrait(ShapeId id, Builder builder) {
        super(id, builder.sourceLocation);
        this.definitions = builder.definitions.copy();
        if (definitions.isEmpty()) {
            throw new SourceException("enum must have at least one entry", getSourceLocation());
        }
    }

    private EnumTrait(Builder builder) {
        this(ID, builder);
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
        return definitions.stream().map(EnumDefinition::toNode).collect(ArrayNode.collect(getSourceLocation()));
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
    public static class Builder extends AbstractTraitBuilder<EnumTrait, Builder> {
        private final BuilderRef<List<EnumDefinition>> definitions = BuilderRef.forList();

        public Builder addEnum(EnumDefinition value) {
            definitions.get().add(value);
            return this;
        }

        public Builder removeEnum(String value) {
            definitions.get().removeIf(def -> def.getValue().equals(value));
            return this;
        }

        public Builder removeEnumByName(String name) {
            definitions.get().removeIf(def -> def.getName().filter(n -> n.equals(name)).isPresent());
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
            EnumTrait result = builder.build();
            result.setNodeCache(value);
            return result;
        }
    }
}
