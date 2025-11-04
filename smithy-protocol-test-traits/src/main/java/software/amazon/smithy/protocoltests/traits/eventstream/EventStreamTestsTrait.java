/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.protocoltests.traits.eventstream;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.protocoltests.traits.AppliesTo;

/**
 * Defines a list of protocol tests that enforce how an event stream
 * is serialized / deserialized for a specific protocol.
 */
public final class EventStreamTestsTrait extends AbstractTrait {
    public static final ShapeId ID = ShapeId.from("smithy.test#eventStreamTests");

    private final List<EventStreamTestCase> testCases;

    public EventStreamTestsTrait(List<EventStreamTestCase> testCases) {
        this(SourceLocation.NONE, testCases);
    }

    public EventStreamTestsTrait(SourceLocation sourceLocation, List<EventStreamTestCase> testCases) {
        super(ID, sourceLocation);
        this.testCases = testCases;
    }

    /**
     * @return Returns all test cases.
     */
    public List<EventStreamTestCase> getTestCases() {
        return testCases;
    }

    /**
     * Gets all test cases that apply to a client or server.
     *
     * <p>Test cases that define an {@code appliesTo} member are tests that
     * should only be implemented by clients or servers. It is assumed that
     * test cases that do not define an {@code appliesTo} member are
     * implemented by both client and server implementations.
     *
     * @param appliesTo The type of test case to retrieve.
     * @return Returns the matching test cases.
     */
    public List<EventStreamTestCase> getTestCasesFor(AppliesTo appliesTo) {
        return testCases.stream()
                .filter(test -> !test.getAppliesTo().filter(value -> value != appliesTo).isPresent())
                .collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EventStreamTestsTrait)) {
            return false;
        }
        EventStreamTestsTrait that = (EventStreamTestsTrait) o;
        return Objects.equals(testCases, that.testCases);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), testCases);
    }

    @Override
    protected Node createNode() {
        NodeMapper mapper = new NodeMapper();
        mapper.disableToNodeForClass(EventStreamTestsTrait.class);
        mapper.setOmitEmptyValues(true);
        return mapper.serialize(this);
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            ArrayNode values = value.expectArrayNode();
            NodeMapper mapper = new NodeMapper();
            List<EventStreamTestCase> cases = new ArrayList<>(values.size());
            for (Node testCase : values) {
                cases.add(mapper.deserialize(testCase, EventStreamTestCase.class));
            }
            EventStreamTestsTrait result = new EventStreamTestsTrait(cases);
            result.setNodeCache(value);
            return result;
        }
    }
}
