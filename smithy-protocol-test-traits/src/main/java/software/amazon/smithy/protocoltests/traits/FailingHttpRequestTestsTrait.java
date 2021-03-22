/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.protocoltests.traits;

import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.ListUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Defines HTTP request protocol tests.
 */
public final class FailingHttpRequestTestsTrait extends AbstractTrait {
    public static final ShapeId ID = ShapeId.from("smithy.test#failingHttpRequestTests");

    private final List<FailingHttpRequestTest> tests;

    public FailingHttpRequestTestsTrait(List<FailingHttpRequestTest> testCases) {
        this(SourceLocation.NONE, testCases);
    }

    public FailingHttpRequestTestsTrait(SourceLocation sourceLocation, List<FailingHttpRequestTest> testCases) {
        super(ID, sourceLocation);
        this.tests = ListUtils.copyOf(testCases);
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            ArrayNode values = value.expectArrayNode();
            List<FailingHttpRequestTest> testCases = values.getElementsAs(FailingHttpRequestTest::fromNode);
            return new FailingHttpRequestTestsTrait(value.getSourceLocation(), testCases);
        }
    }

    public List<FailingHttpRequestTest> getTests() {
        return tests;
    }

    public List<FailingHttpRequestTest.Client> getClientTests() {
        return tests.stream().filter((test) -> test instanceof FailingHttpRequestTest.Client).map(test -> (FailingHttpRequestTest.Client) test).collect(Collectors.toList());
    }

    public List<FailingHttpRequestTest.Server> getServerTests() {
        return tests.stream().filter((test) -> test instanceof FailingHttpRequestTest.Server).map(test -> (FailingHttpRequestTest.Server) test).collect(Collectors.toList());
    }

    @Override
    protected Node createNode() {
        return getTests().stream().collect(ArrayNode.collect());
    }
}
