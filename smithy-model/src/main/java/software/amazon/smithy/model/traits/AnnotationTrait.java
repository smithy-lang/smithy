/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.Objects;
import java.util.function.Function;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Trait implementation for traits that are an empty object.
 */
public abstract class AnnotationTrait implements Trait {

    private final ShapeId id;
    private final ObjectNode node;

    public AnnotationTrait(ShapeId id, ObjectNode node) {
        this.id = Objects.requireNonNull(id);
        this.node = Objects.requireNonNull(node);
    }

    @Override
    public final ShapeId toShapeId() {
        return id;
    }

    @Override
    public final Node toNode() {
        return node;
    }

    @Override
    public final SourceLocation getSourceLocation() {
        return node.getSourceLocation();
    }

    @Override
    public int hashCode() {
        return toShapeId().hashCode() * 17 + node.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || other.getClass() != getClass()) {
            return false;
        }

        if (this == other) {
            return true;
        }

        Trait b = (Trait) other;
        if (!toShapeId().equals(b.toShapeId())) {
            return false;
        }

        return node.equals(b.toNode());
    }

    /**
     * Trait provider that expects a boolean value of true.
     */
    public static class Provider<T extends AnnotationTrait> extends AbstractTrait.Provider {
        private final Function<ObjectNode, T> traitFactory;

        /**
         * @param id ID of the trait being created.
         * @param traitFactory Factory function used to create the trait.
         */
        public Provider(ShapeId id, Function<ObjectNode, T> traitFactory) {
            super(id);
            this.traitFactory = traitFactory;
        }

        @Override
        public T createTrait(ShapeId id, Node value) {
            if (value.isObjectNode()) {
                return traitFactory.apply(value.expectObjectNode());
            }

            throw new ExpectationNotMetException(String.format(
                    "Annotation traits  must be an object or omitted in the IDL, but found %s",
                    value.getType()), value);
        }
    }
}
