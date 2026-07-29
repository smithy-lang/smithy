/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.protocol.traits;

import java.math.BigDecimal;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;

/**
 * Defines the stable wire index of a structure or union member.
 */
public final class IdxTrait extends AbstractTrait {

    public static final ShapeId ID = ShapeId.from("smithy.protocols#idx");

    private final int value;

    /**
     * @param value Index value.
     * @param sourceLocation Where the trait was defined.
     */
    public IdxTrait(int value, FromSourceLocation sourceLocation) {
        super(ID, sourceLocation);
        this.value = value;
    }

    /**
     * @param value Index value.
     */
    public IdxTrait(int value) {
        this(value, SourceLocation.NONE);
    }

    /**
     * Gets the index value.
     *
     * @return Returns the index value.
     */
    public int getValue() {
        return value;
    }

    @Override
    protected Node createNode() {
        return new NumberNode(value, getSourceLocation());
    }

    /**
     * Implements the {@link AbstractTrait.Provider}.
     */
    public static final class Provider extends AbstractTrait.Provider {

        public Provider() {
            super(ID);
        }

        @Override
        public IdxTrait createTrait(ShapeId target, Node value) {
            NumberNode numberNode = value.expectNumberNode();
            BigDecimal number = numberNode.asBigDecimal()
                    .orElseThrow(() -> new ExpectationNotMetException(
                            "Expected idx to be a finite integer",
                            numberNode));
            final int exactValue;
            try {
                exactValue = number.intValueExact();
            } catch (ArithmeticException e) {
                throw new ExpectationNotMetException(
                        "Expected idx to be an integer between -2147483648 and 2147483647",
                        numberNode);
            }

            IdxTrait result = new IdxTrait(exactValue, value);
            result.setNodeCache(value);
            return result;
        }
    }
}
