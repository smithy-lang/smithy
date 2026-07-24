/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.List;
import java.util.Optional;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.BuilderRef;
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
        super(ID, builder.getSourceLocation());
        this.exists = builder.exists.hasValue() ? builder.exists.copy() : null;
        this.notFound = builder.notFound.hasValue() ? builder.notFound.copy() : null;
    }

    public IdempotentTrait(ObjectNode object) {
        this(fromNode(object));
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
     * Gets the errors returned when the resource already exists, or {@code null} if not defined.
     *
     * @return The exists errors list, or {@code null} if not defined.
     */
    public List<ShapeId> getExistsOrNull() {
        return exists;
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

    /**
     * Gets the errors returned when the resource does not exist, or {@code null} if not defined.
     *
     * @return The notFound errors list, or {@code null} if not defined.
     */
    public List<ShapeId> getNotFoundOrNull() {
        return notFound;
    }

    @Override
    protected Node createNode() {
        ObjectNode.Builder nodeBuilder = Node.objectNodeBuilder();
        setArrayMember(nodeBuilder, exists, EXISTS);
        setArrayMember(nodeBuilder, notFound, NOT_FOUND);
        return nodeBuilder.build();
    }

    private static void setArrayMember(ObjectNode.Builder nodeBuilder, List<ShapeId> errors, String key) {
        if (errors == null) {
            return;
        }
        if (errors.isEmpty()) {
            nodeBuilder.withMember(key, ArrayNode.arrayNode());
        } else {
            ArrayNode.Builder builder = ArrayNode.builder();
            for (ShapeId shapeId : errors) {
                builder.withValue(Node.from(shapeId.toString()));
            }
            nodeBuilder.withMember(key, builder.build());
        }
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * Creates a {@link IdempotentTrait} from a {@link Node}.
     *
     * @param node Node to create the IdempotentTrait from.
     * @return Returns the created IdempotentTrait.
     * @throws ExpectationNotMetException if the given Node is invalid.
     */
    public static IdempotentTrait fromNode(Node node) {
        return fromNode(node.expectObjectNode()).build();
    }

    private static Builder fromNode(ObjectNode node) {
        Builder builder = builder().sourceLocation(node);
        node.getArrayMember(EXISTS, ShapeId::fromNode, builder::exists)
                .getArrayMember(NOT_FOUND, ShapeId::fromNode, builder::notFound);
        return builder;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public IdempotentTrait createTrait(ShapeId target, Node value) {
            IdempotentTrait result = IdempotentTrait.fromNode(value);
            result.setNodeCache(value);
            return result;
        }
    }

    /**
     * Builder for {@link IdempotentTrait}.
     */
    public static final class Builder extends AbstractTraitBuilder<IdempotentTrait, Builder> {
        private final BuilderRef<List<ShapeId>> exists = BuilderRef.forList();
        private final BuilderRef<List<ShapeId>> notFound = BuilderRef.forList();

        private Builder() {}

        private Builder(IdempotentTrait idempotentTrait) {
            if (idempotentTrait.exists != null) {
                exists.setBorrowed(idempotentTrait.exists);
            }
            if (idempotentTrait.notFound != null) {
                notFound.setBorrowed(idempotentTrait.notFound);
            }
        }

        public Builder exists(List<ShapeId> exists) {
            clearExists();
            this.exists.get().addAll(exists);
            return this;
        }

        public Builder clearExists() {
            this.exists.get().clear();
            return this;
        }

        public Builder addExists(ShapeId value) {
            this.exists.get().add(value);
            return this;
        }

        public Builder removeExists(ShapeId value) {
            this.exists.get().remove(value);
            return this;
        }

        public Builder notFound(List<ShapeId> notFound) {
            clearNotFound();
            this.notFound.get().addAll(notFound);
            return this;
        }

        public Builder clearNotFound() {
            this.notFound.get().clear();
            return this;
        }

        public Builder addNotFound(ShapeId value) {
            this.notFound.get().add(value);
            return this;
        }

        public Builder removeNotFound(ShapeId value) {
            this.notFound.get().remove(value);
            return this;
        }

        @Override
        public IdempotentTrait build() {
            return new IdempotentTrait(this);
        }
    }
}
