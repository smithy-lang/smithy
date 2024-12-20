/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.protocoltests.traits;

import java.util.List;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Defines protocol tests for malformed HTTP request handling.
 */
@SmithyUnstableApi
public final class HttpMalformedRequestTestsTrait extends AbstractTrait {
    public static final ShapeId ID = ShapeId.from("smithy.test#httpMalformedRequestTests");

    private final List<ParameterizedHttpMalformedRequestTestCase> testCases;

    public HttpMalformedRequestTestsTrait(List<ParameterizedHttpMalformedRequestTestCase> testCases) {
        this(SourceLocation.NONE, testCases);
    }

    public HttpMalformedRequestTestsTrait(
            SourceLocation sourceLocation,
            List<ParameterizedHttpMalformedRequestTestCase> testCases
    ) {
        super(ID, sourceLocation);
        this.testCases = ListUtils.copyOf(testCases);
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            ArrayNode values = value.expectArrayNode();
            List<ParameterizedHttpMalformedRequestTestCase> testCases =
                    values.getElementsAs(ParameterizedHttpMalformedRequestTestCase::fromNode);
            HttpMalformedRequestTestsTrait result = new HttpMalformedRequestTestsTrait(value.getSourceLocation(),
                    testCases);
            result.setNodeCache(value);
            return result;
        }
    }

    public List<HttpMalformedRequestTestCase> getTestCases() {
        return testCases
                .stream()
                .map(ParameterizedHttpMalformedRequestTestCase::generateTestCasesFromParameters)
                .flatMap(List::stream)
                .collect(ListUtils.toUnmodifiableList());
    }

    /**
     * This is not preferred for code generation. Use {@link #getTestCases()} instead.
     * @return the parameterized test cases, as defined by the model author
     */
    List<ParameterizedHttpMalformedRequestTestCase> getParameterizedTestCases() {
        return testCases;
    }

    @Override
    protected Node createNode() {
        return getParameterizedTestCases().stream().collect(ArrayNode.collect(getSourceLocation()));
    }
}
