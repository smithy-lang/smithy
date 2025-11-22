/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.jmespath.traits;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.Map.Entry;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyGenerated;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * These expressions must produce 'true'
 */
@SmithyGenerated
public final class ConstraintsTrait extends AbstractTrait implements ToSmithyBuilder<ConstraintsTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.jmespath#constraints");

    private final Map<String, Constraint> values;

    private ConstraintsTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        this.values = builder.values.copy();
    }

    @Override
    protected Node createNode() {
        return values.entrySet().stream()
            .map(entry -> new SimpleImmutableEntry<>(
                Node.from(entry.getKey()), entry.getValue().toNode()))
            .collect(ObjectNode.collect(Entry::getKey, Entry::getValue))
            .toBuilder().sourceLocation(getSourceLocation()).build();
    }

    /**
     * Creates a {@link ConstraintsTrait} from a {@link Node}.
     *
     * @param node Node to create the ConstraintsTrait from.
     * @return Returns the created ConstraintsTrait.
     * @throws ExpectationNotMetException if the given Node is invalid.
     */
    public static ConstraintsTrait fromNode(Node node) {
        Builder builder = builder();
        node.expectObjectNode().getMembers().forEach((k, v) -> {
            builder.putValues(k.expectStringNode().getValue(), Constraint.fromNode(v));
        });
        return builder.build();
    }

    /**
     * These expressions must produce 'true'
     */
    public Map<String, Constraint> getValues() {
        return values;
    }

    /**
     * Creates a builder used to build a {@link ConstraintsTrait}.
     */
    public SmithyBuilder<ConstraintsTrait> toBuilder() {
        return builder().sourceLocation(getSourceLocation())
            .values(values);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ConstraintsTrait}.
     */
    public static final class Builder extends AbstractTraitBuilder<ConstraintsTrait, Builder> {
        private final BuilderRef<Map<String, Constraint>> values = BuilderRef.forOrderedMap();

        private Builder() {}

        public Builder values(Map<String, Constraint> values) {
            clearValues();
            this.values.get().putAll(values);
            return this;
        }

        public Builder clearValues() {
            this.values.get().clear();
            return this;
        }

        public Builder putValues(String key, Constraint value) {
            this.values.get().put(key, value);
            return this;
        }

        public Builder removeValues(String values) {
            this.values.get().remove(values);
            return this;
        }

        @Override
        public ConstraintsTrait build() {
            return new ConstraintsTrait(this);
        }
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            ConstraintsTrait result = ConstraintsTrait.fromNode(value);
            result.setNodeCache(value);
            return result;
        }
    }
}
