/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Indicates that an operation is idempotent.
 *
 * <p>Optionally describes the errors returned when encountering unexpected
 * resource states (already exists for creates, not found for deletes).
 */
public final class IdempotentTrait extends AbstractTrait implements ToSmithyBuilder<IdempotentTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.api#idempotent");

    private static final String EXISTS = "exists";
    private static final String NOT_FOUND = "notFound";

    private final List<ShapeId> exists;
    private final List<ShapeId> notFound;

    private IdempotentTrait(Builder builder) {
        super(ID, builder.sourceLocation);
        this.exists = builder.exists;
        this.notFound = builder.notFound;
    }

    public IdempotentTrait(ObjectNode node) {
        this(builder().sourceLocation(node.getSourceLocation()).fromNode(node));
    }

    public IdempotentTrait() {
        this(builder());
    }

    /**
     * Gets the errors returned when the resource already exists.
     *
     * <p>When present, an empty list indicates the operation returns a
     * successful response. When absent, the behavior is unspecified.
     *
     * @return The exists errors list, if present.
     */
    public Optional<List<ShapeId>> getExists() {
        return Optional.ofNullable(exists);
    }

    /**
     * Gets the errors returned when the resource does not exist.
     *
     * <p>When present, an empty list indicates the operation returns a
     * successful response. When absent, the behavior is unspecified.
     *
     * @return The notFound errors list, if present.
     */
    public Optional<List<ShapeId>> getNotFound() {
        return Optional.ofNullable(notFound);
    }

    @Override
    protected Node createNode() {
        ObjectNode.Builder nodeBuilder = Node.objectNodeBuilder();
        if (exists != null) {
            nodeBuilder.withMember(EXISTS, shapeIdListToNode(exists));
        }
        if (notFound != null) {
            nodeBuilder.withMember(NOT_FOUND, shapeIdListToNode(notFound));
        }
        return nodeBuilder.build();
    }

    private static ArrayNode shapeIdListToNode(List<ShapeId> ids) {
        return ids.stream()
                .map(id -> Node.from(id.toString()))
                .collect(ArrayNode.collect());
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .sourceLocation(getSourceLocation())
                .exists(exists)
                .notFound(notFound);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            IdempotentTrait result = new IdempotentTrait(value.expectObjectNode());
            result.setNodeCache(value);
            return result;
        }
    }

    public static final class Builder extends AbstractTraitBuilder<IdempotentTrait, Builder> {
        private List<ShapeId> exists;
        private List<ShapeId> notFound;

        private Builder() {}

        @Override
        public IdempotentTrait build() {
            return new IdempotentTrait(this);
        }

        public Builder exists(List<ShapeId> exists) {
            this.exists = exists;
            return this;
        }

        public Builder notFound(List<ShapeId> notFound) {
            this.notFound = notFound;
            return this;
        }

        Builder fromNode(ObjectNode node) {
            node.getArrayMember(EXISTS).ifPresent(arr -> {
                exists = Collections.unmodifiableList(
                        arr.getElementsAs(n -> ShapeId.from(n.expectStringNode().getValue())));
            });
            node.getArrayMember(NOT_FOUND).ifPresent(arr -> {
                notFound = Collections.unmodifiableList(
                        arr.getElementsAs(n -> ShapeId.from(n.expectStringNode().getValue())));
            });
            return this;
        }
    }
}
