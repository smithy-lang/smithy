/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.waiters;

import java.util.Map;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitService;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Indicates that an operation has various named "waiters" that can be used
 * to poll a resource until it enters a desired state.
 */
public final class WaitableTrait extends AbstractTrait implements ToSmithyBuilder<WaitableTrait> {

    public static final ShapeId ID = ShapeId.from("smithy.waiters#waitable");

    private final Map<String, Waiter> waiters;

    private WaitableTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        this.waiters = builder.waiters.copy();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return new Builder().sourceLocation(getSourceLocation()).replace(waiters);
    }

    /**
     * Gets the waiters defined on the trait.
     *
     * @return Returns the defined waiters.
     */
    public Map<String, Waiter> getWaiters() {
        return waiters;
    }

    @Override
    protected Node createNode() {
        ObjectNode.Builder builder = ObjectNode.objectNodeBuilder();
        builder.sourceLocation(getSourceLocation());
        for (Map.Entry<String, Waiter> entry : waiters.entrySet()) {
            builder.withMember(entry.getKey(), entry.getValue().toNode());
        }
        return builder.build();
    }

    public static final class Builder extends AbstractTraitBuilder<WaitableTrait, Builder> {

        private final BuilderRef<Map<String, Waiter>> waiters = BuilderRef.forOrderedMap();

        private Builder() {}

        @Override
        public WaitableTrait build() {
            return new WaitableTrait(this);
        }

        public Builder put(String name, Waiter value) {
            waiters.get().put(name, value);
            return this;
        }

        public Builder clear() {
            this.waiters.clear();
            return this;
        }

        public Builder replace(Map<String, Waiter> waiters) {
            clear();
            this.waiters.get().putAll(waiters);
            return this;
        }
    }

    public static final class Provider implements TraitService {
        @Override
        public ShapeId getShapeId() {
            return ID;
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            ObjectNode node = value.expectObjectNode();
            Builder builder = builder().sourceLocation(value);
            for (Map.Entry<String, Node> entry : node.getStringMap().entrySet()) {
                builder.put(entry.getKey(), Waiter.fromNode(entry.getValue()));
            }
            WaitableTrait result = builder.build();
            result.setNodeCache(value);
            return result;
        }
    }
}
