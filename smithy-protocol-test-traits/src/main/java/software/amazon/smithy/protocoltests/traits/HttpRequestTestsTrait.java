/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.protocoltests.traits;

import java.util.List;
import java.util.stream.Collectors;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.ListUtils;

/**
 * Defines HTTP request protocol tests.
 */
public final class HttpRequestTestsTrait extends AbstractTrait {
    public static final ShapeId ID = ShapeId.from("smithy.test#httpRequestTests");

    private final List<HttpRequestTestCase> testCases;

    public HttpRequestTestsTrait(List<HttpRequestTestCase> testCases) {
        this(SourceLocation.NONE, testCases);
    }

    public HttpRequestTestsTrait(SourceLocation sourceLocation, List<HttpRequestTestCase> testCases) {
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
            List<HttpRequestTestCase> testCases = values.getElementsAs(HttpRequestTestCase::fromNode);
            HttpRequestTestsTrait result = new HttpRequestTestsTrait(value.getSourceLocation(), testCases);
            result.setNodeCache(value);
            return result;
        }
    }

    public List<HttpRequestTestCase> getTestCases() {
        return testCases;
    }

    /**
     * Gets all test cases that apply to a client or server.
     *
     * <p>Test cases that define an {@code appliesTo} member are tests that
     * should only be implemented by clients or servers. Is is assumed that
     * test cases that do not define an {@code appliesTo} member are
     * implemented by both client and server implementations.
     *
     * @param appliesTo The type of test case to retrieve.
     * @return Returns the matching test cases.
     */
    public List<HttpRequestTestCase> getTestCasesFor(AppliesTo appliesTo) {
        return testCases.stream()
                .filter(test -> !test.getAppliesTo().filter(value -> value != appliesTo).isPresent())
                .collect(Collectors.toList());
    }

    @Override
    protected Node createNode() {
        return getTestCases().stream().collect(ArrayNode.collect(getSourceLocation()));
    }
}
