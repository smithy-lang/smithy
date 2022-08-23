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

package software.amazon.smithy.rulesengine.language;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public final class EndpointTestSuite implements ToNode, FromSourceLocation {
    public static final String TEST_CASES = "testCases";

    private final List<EndpointTest> testCases;

    private final SourceLocation sourceLocation;

    private EndpointTestSuite(Builder b) {
        this.sourceLocation = b.sourceLocation;
        this.testCases = b.testCases.copy();
    }

    public static EndpointTestSuite fromNode(Node node) {
        Objects.requireNonNull(node);
        Builder builder = new Builder(node.getSourceLocation());
        ObjectNode on = node.expectObjectNode("endpoint test suite must be an object node");
        on
                .expectArrayMember(TEST_CASES)
                .getElements()
                .stream()
                .map(EndpointTest::fromNode)
                .forEach(builder::addTestCase);
        return builder.build();
    }

    public static EndpointTestSuite fromFiles(String service, String... files) {
        EndpointTestSuite.Builder builder = EndpointTestSuite.builder();
        Stream.of(files)
                .map(path ->
                        Objects.requireNonNull(
                                Thread.currentThread().getContextClassLoader().getResourceAsStream(path)))
                .map(stream -> EndpointTestSuite.fromNode(Node.parse(stream)))
                .flatMap(suite -> suite.getTestCases().stream())
                .forEach(builder::addTestCase);
        return builder.build();
    }

    public static Builder builder() {
        return new Builder(SourceAwareBuilder.javaLocation());
    }

    public void execute(EndpointRuleset ruleset) {
        for (EndpointTest test : this.getTestCases()) {
            test.execute(ruleset);
        }
    }

    public List<EndpointTest> getTestCases() {
        return testCases;
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder builder = ObjectNode.builder();

        builder.withMember(TEST_CASES, testCases.stream().map(ToNode::toNode).collect(ArrayNode.collect()));

        return builder.build();
    }

    @Override
    public int hashCode() {
        return testCases.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EndpointTestSuite that = (EndpointTestSuite) o;

        return testCases.equals(that.testCases);
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    public static class Builder extends SourceAwareBuilder<Builder, EndpointTestSuite> {
        private final BuilderRef<List<EndpointTest>> testCases = BuilderRef.forList();

        public Builder(FromSourceLocation sourceLocation) {
            super(sourceLocation);
        }

        public Builder addTestCase(EndpointTest testCase) {
            this.testCases.get().add(testCase);
            return this;
        }

        public EndpointTestSuite build() {
            return new EndpointTestSuite(this);
        }
    }

}
