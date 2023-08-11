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
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/***
 * Defines an endpoint rule-set used to resolve the client's transport endpoint.
 */
@SmithyUnstableApi
public final class EndpointRuleSetTrait extends AbstractTrait implements ToSmithyBuilder<EndpointRuleSetTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.rules#endpointRuleSet");

    private final Node ruleSet;
    private EndpointRuleSet endpointRuleSet;

    private EndpointRuleSetTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        ruleSet = SmithyBuilder.requiredState("ruleSet", builder.ruleSet);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Node getRuleSet() {
        return ruleSet;
    }

    public EndpointRuleSet getEndpointRuleSet() {
        // EndpointRuleSet creation loads an SPI of functions, builtins, and more.
        // That work is deferred until necessary, usually when a ruleset is being validated.
        if (endpointRuleSet == null) {
            endpointRuleSet = EndpointRuleSet.fromNode(ruleSet);
        }
        return endpointRuleSet;
    }

    @Override
    protected Node createNode() {
        return ruleSet;
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .sourceLocation(getSourceLocation())
                .ruleSet(ruleSet);
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            EndpointRuleSetTrait trait = builder().sourceLocation(value)
                    .ruleSet(value)
                    .build();
            trait.setNodeCache(value);
            return trait;
        }
    }

    public static final class Builder extends AbstractTraitBuilder<EndpointRuleSetTrait, Builder> {
        private Node ruleSet;

        private Builder() {
        }

        public Builder ruleSet(Node ruleSet) {
            this.ruleSet = ruleSet;
            return this;
        }

        @Override
        public EndpointRuleSetTrait build() {
            return new EndpointRuleSetTrait(this);
        }
    }
}
