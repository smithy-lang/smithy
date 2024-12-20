/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.Optional;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Constrains a shape to minimum and maximum number of elements or size.
 */
public final class LengthTrait extends AbstractTrait implements ToSmithyBuilder<LengthTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.api#length");

    private final Long min;
    private final Long max;

    private LengthTrait(Builder builder) {
        super(ID, builder.sourceLocation);
        this.min = builder.min;
        this.max = builder.max;
        if (max == null && min == null) {
            throw new SourceException("One of 'min' or 'max' must be provided.", getSourceLocation());
        }
    }

    /**
     * Gets the min value.
     *
     * @return returns the optional min value.
     */
    public Optional<Long> getMin() {
        return Optional.ofNullable(min);
    }

    /**
     * Gets the max value.
     *
     * @return returns the optional max value.
     */
    public Optional<Long> getMax() {
        return Optional.ofNullable(max);
    }

    @Override
    protected Node createNode() {
        return new ObjectNode(MapUtils.of(), getSourceLocation())
                .withOptionalMember("min", getMin().map(Node::from))
                .withOptionalMember("max", getMax().map(Node::from));
    }

    @Override
    public Builder toBuilder() {
        return builder().min(min).max(max).sourceLocation(getSourceLocation());
    }

    /**
     * @return Returns a builder used to create a length trait.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder used to create a LongTrait.
     */
    public static final class Builder extends AbstractTraitBuilder<LengthTrait, Builder> {
        private Long min;
        private Long max;

        public Builder min(Long min) {
            this.min = min;
            return this;
        }

        public Builder max(Long max) {
            this.max = max;
            return this;
        }

        @Override
        public LengthTrait build() {
            return new LengthTrait(this);
        }
    }

    public static final class Provider implements TraitService {
        @Override
        public ShapeId getShapeId() {
            return ID;
        }

        @Override
        public LengthTrait createTrait(ShapeId target, Node value) {
            LengthTrait.Builder builder = builder().sourceLocation(value.getSourceLocation());
            value.expectObjectNode()
                    .getNumberMember("min", n -> builder.min(n.longValue()))
                    .getNumberMember("max", n -> builder.max(n.longValue()));
            LengthTrait result = builder.build();
            result.setNodeCache(value);
            return result;
        }
    }
}
