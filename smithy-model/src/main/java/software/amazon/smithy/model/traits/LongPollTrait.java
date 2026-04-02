/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Indicates that the service may not respond immediately to requests for the
 * targeted operation, allowing services more time to prepare responses or hold
 * the request open while waiting for information to become available.
 */
public final class LongPollTrait extends AbstractTrait implements ToSmithyBuilder<LongPollTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.api#longPoll");

    private final Integer timeoutMillis;

    private LongPollTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        this.timeoutMillis = builder.timeoutMillis;
    }

    /**
     * Gets the timeout in milliseconds that a client should wait for a response.
     *
     * @return Returns the optional timeout in milliseconds.
     */
    public Optional<Integer> getTimeoutMillis() {
        return Optional.ofNullable(timeoutMillis);
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .sourceLocation(getSourceLocation())
                .timeoutMillis(timeoutMillis);
    }

    @Override
    protected Node createNode() {
        return Node.objectNodeBuilder()
                .sourceLocation(getSourceLocation())
                .withOptionalMember("timeoutMillis", getTimeoutMillis().map(Node::from))
                .build();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof LongPollTrait)) {
            return false;
        } else if (other == this) {
            return true;
        } else {
            return Objects.equals(timeoutMillis, ((LongPollTrait) other).timeoutMillis);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(toShapeId(), timeoutMillis);
    }

    public static final class Provider implements TraitService {
        @Override
        public ShapeId getShapeId() {
            return ID;
        }

        @Override
        public LongPollTrait createTrait(ShapeId target, Node value) {
            Builder builder = builder().sourceLocation(value.getSourceLocation());
            value.expectObjectNode().getNumberMember("timeoutMillis", n -> builder.timeoutMillis(n.intValue()));
            LongPollTrait result = builder.build();
            result.setNodeCache(value);
            return result;
        }
    }

    /**
     * Creates a builder for a {@link LongPollTrait}.
     *
     * @return Returns the created builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds a {@link LongPollTrait} trait.
     */
    public static final class Builder extends AbstractTraitBuilder<LongPollTrait, Builder> {
        private Integer timeoutMillis;

        private Builder() {}

        @Override
        public LongPollTrait build() {
            return new LongPollTrait(this);
        }

        /**
         * Sets the timeout in milliseconds that a client should wait for a response.
         *
         * @param timeoutMillis The timeout in milliseconds.
         * @return Returns the builder.
         */
        public Builder timeoutMillis(Integer timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
            return this;
        }
    }
}
