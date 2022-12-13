/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.rulesengine.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;
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

    private EndpointRuleSetTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        this.ruleSet = SmithyBuilder.requiredState("ruleSet", builder.ruleSet);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Node getRuleSet() {
        return ruleSet;
    }

    @Override
    protected Node createNode() {
        return ruleSet;
    }

    @Override
    public SmithyBuilder<EndpointRuleSetTrait> toBuilder() {
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
            EndpointRuleSetTrait trait = builder()
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
