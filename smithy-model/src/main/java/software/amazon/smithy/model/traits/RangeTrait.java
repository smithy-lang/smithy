/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.math.BigDecimal;
import java.util.Optional;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Constrains a shape to minimum and maximum numeric range.
 */
public final class RangeTrait extends AbstractTrait implements ToSmithyBuilder<RangeTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.api#range");

    private final BigDecimal min;
    private final BigDecimal max;

    private RangeTrait(Builder builder) {
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
    public Optional<BigDecimal> getMin() {
        return Optional.ofNullable(min);
    }

    /**
     * Gets the max value.
     *
     * @return returns the optional max value.
     */
    public Optional<BigDecimal> getMax() {
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
     * @return Returns a new RangeTrait builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder used to create a RangeTrait.
     */
    public static final class Builder extends AbstractTraitBuilder<RangeTrait, Builder> {
        private BigDecimal min;
        private BigDecimal max;

        public Builder min(BigDecimal min) {
            this.min = min;
            return this;
        }

        public Builder max(BigDecimal max) {
            this.max = max;
            return this;
        }

        @Override
        public RangeTrait build() {
            return new RangeTrait(this);
        }
    }

    public static final class Provider implements TraitService {
        @Override
        public ShapeId getShapeId() {
            return ID;
        }

        @Override
        public RangeTrait createTrait(ShapeId target, Node value) {
            Builder builder = builder().sourceLocation(value.getSourceLocation());
            value.expectObjectNode()
                    .getMember("min", Provider::convertToBigDecimal, builder::min)
                    .getMember("max", Provider::convertToBigDecimal, builder::max);
            RangeTrait result = builder.build();
            result.setNodeCache(value);
            return result;
        }

        private static BigDecimal convertToBigDecimal(Node number) {
            Number value = number.expectNumberNode().getValue();
            return value instanceof BigDecimal ? (BigDecimal) value : new BigDecimal(value.toString());
        }
    }
}
