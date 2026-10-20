/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.Objects;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Indicates that this union is serialized as an internally tagged union.
 *
 * <p>The value of the trait is the name of the field that identifies the active union member. The value is optional
 * when applying the trait; a bare {@code @discriminated} is equivalent to {@code @discriminated("__type")}. An explicit
 * {@code null} value, which a model loader passes to the provider in the same way as a bare trait, is also equivalent
 * to {@code @discriminated("__type")}. This is handled here rather than by extending {@link StringTrait} because a
 * string trait requires an explicit value, and a {@code @default} on the trait's definition is not applied to traits.
 */
public final class DiscriminatedTrait extends AbstractTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#discriminated");

    /**
     * The discriminator field name used when the trait is applied without a value.
     */
    public static final String DEFAULT_VALUE = "__type";

    private final String value;

    public DiscriminatedTrait(String value, FromSourceLocation sourceLocation) {
        super(ID, sourceLocation);
        this.value = Objects.requireNonNull(value, "Trait values must not be null");
    }

    public DiscriminatedTrait(String value) {
        this(value, SourceLocation.NONE);
    }

    /**
     * Creates a trait that uses the {@link #DEFAULT_VALUE default} discriminator field name.
     */
    public DiscriminatedTrait() {
        this(DEFAULT_VALUE);
    }

    /**
     * @return the name of the field that identifies the active union member.
     */
    public String getValue() {
        return value;
    }

    @Override
    protected Node createNode() {
        return new StringNode(value, getSourceLocation());
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            // A bare `@discriminated` is provided as a null node, and takes the default.
            if (value.isNullNode()) {
                return new DiscriminatedTrait(DEFAULT_VALUE, value.getSourceLocation());
            }
            DiscriminatedTrait result = new DiscriminatedTrait(
                    value.expectStringNode().getValue(),
                    value.getSourceLocation());
            result.setNodeCache(value);
            return result;
        }
    }
}
