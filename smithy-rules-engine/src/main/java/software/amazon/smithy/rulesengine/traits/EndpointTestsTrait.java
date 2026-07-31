/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.traits;

import java.util.List;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
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

    public String getVersion() {
        return version;
    }

    public List<EndpointTestCase> getTestCases() {
        return testCases;
    }

    @Override
    protected Node createNode() {
        ArrayNode.Builder builder = ArrayNode.builder();
        for (EndpointTestCase testCase : testCases) {
            builder.withValue(testCase.toNode());
        }
        return Node.objectNodeBuilder()
                .sourceLocation(getSourceLocation())
                .withMember("testCases", builder.build())
                .withMember("version", Node.from(version))
                .build();
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
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

    public static Builder builder() {
        return new Builder();
    }

    public static EndpointTestsTrait fromNode(Node node) {
        ObjectNode obj = node.expectObjectNode();
        Builder builder = builder().sourceLocation(node);
        obj.expectStringMember("version", builder::version);
        obj.expectArrayMember("testCases", EndpointTestCase::fromNode, builder::testCases);
        EndpointTestsTrait trait = builder.build();
        trait.setNodeCache(node);
        return trait;
    }

    public static final class Builder extends AbstractTraitBuilder<EndpointTestsTrait, Builder> {
        private final BuilderRef<List<EndpointTestCase>> testCases = BuilderRef.forList();
        private String version;

        private Builder() {}

        private Builder(EndpointTestsTrait trait) {
            sourceLocation(trait.getSourceLocation());
            this.version = trait.version;
            this.testCases.setBorrowed(trait.testCases);
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
