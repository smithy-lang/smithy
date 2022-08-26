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

import java.util.List;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Describes test cases for validating an endpoint rule-set.
 */
@SmithyUnstableApi
public final class EndpointTestsTrait extends AbstractTrait implements ToSmithyBuilder<EndpointTestsTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.rules#endpointTests");
    private final String version;
    private final List<EndpointTestCase> testCases;

    private EndpointTestsTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        this.version = SmithyBuilder.requiredState("version", builder.version);
        this.testCases = builder.testCases.copy();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static EndpointTestsTrait fromNode(Node node) {
        NodeMapper mapper = new NodeMapper();
        mapper.disableFromNodeForClass(EndpointTestsTrait.class);
        EndpointTestsTrait trait = mapper.deserialize(node, EndpointTestsTrait.class);
        trait.setNodeCache(node);
        return trait;
    }

    public String getVersion() {
        return version;
    }

    public List<EndpointTestCase> getTestCases() {
        return testCases;
    }

    @Override
    protected Node createNode() {
        NodeMapper mapper = new NodeMapper();
        mapper.setOmitEmptyValues(true);
        mapper.disableToNodeForClass(EndpointTestsTrait.class);
        return mapper.serialize(this);
    }

    @Override
    public SmithyBuilder<EndpointTestsTrait> toBuilder() {
        return builder()
                .sourceLocation(getSourceLocation())
                .version(version)
                .testCases(testCases);
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            return EndpointTestsTrait.fromNode(value);
        }
    }

    public static final class Builder extends AbstractTraitBuilder<EndpointTestsTrait, Builder> {
        private final BuilderRef<List<EndpointTestCase>> testCases = BuilderRef.forList();
        private String version;

        private Builder() {
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder testCases(List<EndpointTestCase> testCases) {
            this.testCases.clear();
            this.testCases.get().addAll(testCases);
            return this;
        }

        public Builder addTestCase(EndpointTestCase testCase) {
            this.testCases.get().add(testCase);
            return this;
        }

        public Builder removeTestCase(EndpointTestCase testCase) {
            this.testCases.get().remove(testCase);
            return this;
        }

        @Override
        public EndpointTestsTrait build() {
            return new EndpointTestsTrait(this);
        }
    }
}
