/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.rulesengine.logic.bdd.Bdd;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/***
 * Defines an endpoint rule-set using a binary decision diagram (BDD) used to resolve the client's transport endpoint.
 */
@SmithyUnstableApi
public final class BddTrait extends AbstractTrait implements ToSmithyBuilder<BddTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.rules#bdd");

    private final Bdd bdd;

    private BddTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        bdd = SmithyBuilder.requiredState("bdd", builder.bdd);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Bdd getBdd() {
        return bdd;
    }

    @Override
    protected Node createNode() {
        return bdd.toNode();
    }

    @Override
    public Builder toBuilder() {
        return builder().sourceLocation(getSourceLocation()).bdd(bdd);
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            BddTrait trait = builder().sourceLocation(value).bdd(Bdd.fromNode(value)).build();
            trait.setNodeCache(value);
            return trait;
        }
    }

    public static final class Builder extends AbstractTraitBuilder<BddTrait, Builder> {
        private Bdd bdd;

        private Builder() {}

        public Builder bdd(Bdd bdd) {
            this.bdd = bdd;
            return this;
        }

        @Override
        public BddTrait build() {
            return new BddTrait(this);
        }
    }
}
