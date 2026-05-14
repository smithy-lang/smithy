/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.List;
import java.util.Optional;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Defines values which are specifically allowed and/or disallowed for a shape.
 */
public final class ShapeExamplesTrait extends AbstractTrait implements ToSmithyBuilder<ShapeExamplesTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.api#shapeExamples");

    private final List<Node> allowed;
    private final List<Node> disallowed;

    private ShapeExamplesTrait(ShapeExamplesTrait.Builder builder) {
        super(ID, builder.sourceLocation);
        this.allowed = builder.allowed;
        this.disallowed = builder.disallowed;
        if (allowed == null && disallowed == null) {
            throw new SourceException("One of 'allowed' or 'disallowed' must be provided.", getSourceLocation());
        }
        if (allowed != null && allowed.isEmpty()) {
            throw new SourceException("'allowed' must be non-empty when provided.", getSourceLocation());
        }
        if (disallowed != null && disallowed.isEmpty()) {
            throw new SourceException("'disallowed' must be non-empty when provided.", getSourceLocation());
        }
    }

    /**
     * Gets the allowed values.
     *
     * @return returns the optional allowed values.
     */
    public Optional<List<Node>> getAllowed() {
        return Optional.ofNullable(allowed);
    }

    /**
     * Gets the disallowed values.
     *
     * @return returns the optional disallowed values.
     */
    public Optional<List<Node>> getDisallowed() {
        return Optional.ofNullable(disallowed);
    }

    @Override
    protected Node createNode() {
        return new ObjectNode(MapUtils.of(), getSourceLocation())
                .withOptionalMember("allowed", getAllowed().map(ArrayNode::fromNodes))
                .withOptionalMember("disallowed", getDisallowed().map(ArrayNode::fromNodes));
    }

    @Override
    public ShapeExamplesTrait.Builder toBuilder() {
        return builder().allowed(allowed).disallowed(disallowed).sourceLocation(getSourceLocation());
    }

    /**
     * @return Returns a new ShapeExamplesTrait builder.
     */
    public static ShapeExamplesTrait.Builder builder() {
        return new ShapeExamplesTrait.Builder();
    }

    /**
     * Builder used to create a ShapeExamplesTrait.
     */
    public static final class Builder extends AbstractTraitBuilder<ShapeExamplesTrait, ShapeExamplesTrait.Builder> {
        private List<Node> allowed;
        private List<Node> disallowed;

        public ShapeExamplesTrait.Builder allowed(List<Node> allowed) {
            this.allowed = allowed;
            return this;
        }

        public ShapeExamplesTrait.Builder disallowed(List<Node> disallowed) {
            this.disallowed = disallowed;
            return this;
        }

        @Override
        public ShapeExamplesTrait build() {
            return new ShapeExamplesTrait(this);
        }
    }

    public static final class Provider implements TraitService {
        @Override
        public ShapeId getShapeId() {
            return ID;
        }

        @Override
        public ShapeExamplesTrait createTrait(ShapeId target, Node value) {
            ShapeExamplesTrait.Builder builder = builder().sourceLocation(value.getSourceLocation());
            value.expectObjectNode()
                    .getMember("allowed", ShapeExamplesTrait.Provider::convertToShapeExampleList, builder::allowed)
                    .getMember("disallowed",
                            ShapeExamplesTrait.Provider::convertToShapeExampleList,
                            builder::disallowed);
            ShapeExamplesTrait result = builder.build();
            result.setNodeCache(value);
            return result;
        }

        private static List<Node> convertToShapeExampleList(Node node) {
            return node.expectArrayNode().getElements();
        }
    }
}
