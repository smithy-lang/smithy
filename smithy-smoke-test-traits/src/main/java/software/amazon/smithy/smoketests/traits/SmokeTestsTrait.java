/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.smoketests.traits;

import java.util.List;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.ListUtils;

/**
 * Defines a set of test cases to send to a live service to ensure that
 * a client can connect to the service and get the right kind of response.
 */
public final class SmokeTestsTrait extends AbstractTrait {
    public static final ShapeId ID = ShapeId.from("smithy.test#smokeTests");

    private final List<SmokeTestCase> testCases;

    public SmokeTestsTrait(List<SmokeTestCase> testCases) {
        this(SourceLocation.NONE, testCases);
    }

    public SmokeTestsTrait(SourceLocation sourceLocation, List<SmokeTestCase> testCases) {
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
            List<SmokeTestCase> testCases = values.getElementsAs(SmokeTestCase::fromNode);
            SmokeTestsTrait result = new SmokeTestsTrait(value.getSourceLocation(), testCases);
            result.setNodeCache(value);
            return result;
        }
    }

    /**
     * @return The smoke test cases to perform on the operation.
     */
    public List<SmokeTestCase> getTestCases() {
        return this.testCases;
    }

    @Override
    protected Node createNode() {
        return this.getTestCases().stream().collect(ArrayNode.collect(this.getSourceLocation()));
    }
}
