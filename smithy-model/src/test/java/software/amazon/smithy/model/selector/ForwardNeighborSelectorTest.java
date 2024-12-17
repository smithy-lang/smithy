/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

public class ForwardNeighborSelectorTest {
    private static Model model;

    @BeforeAll
    public static void before() {
        model = Model.assembler()
                .addImport(ForwardNeighborSelectorTest.class.getResource("neighbor-test.smithy"))
                .assemble()
                .unwrap();
    }

    @AfterAll
    public static void after() {
        model = null;
    }

    private Set<String> selectIds(String expression) {
        return Selector.parse(expression)
                .select(model)
                .stream()
                .map(Shape::getId)
                .map(ShapeId::toString)
                .collect(Collectors.toSet());
    }

    @Test
    public void undirectedEdgeTraversal() {
        Set<String> result = selectIds("operation > *");

        assertThat(result,
                containsInAnyOrder(
                        "smithy.example#Input",
                        "smithy.example#Output",
                        "smithy.example#Error"));
    }

    @Test
    public void directedMultiEdgeTraversal() {
        Set<String> result = selectIds("operation -[input, output]->");

        assertThat(result, contains("smithy.example#Input", "smithy.example#Output"));
    }

    @Test
    public void skipsUnknownRelationships() {
        Set<String> result = selectIds("operation -[input, output, foo]->");

        assertThat(result, contains("smithy.example#Input", "smithy.example#Output"));
    }

    @Test
    public void returnsEmptyForUnknownRelationships() {
        Set<String> result = selectIds("operation -[foo]->");

        assertThat(result, empty());
    }

    @Test
    public void canQueryTraitRelationships() {
        Set<String> result1 = selectIds("string -[trait]-> [trait|deprecated]");

        assertThat(result1, contains("smithy.example#myTrait"));

        Set<String> result2 = selectIds(":test(string -[trait]-> [trait|deprecated])");

        assertThat(result2, contains("smithy.example#MyString"));
    }

    @Test
    public void canQueryTraitRelationshipsForProtocolServices() {
        Set<String> result1 = selectIds("service:test(-[trait]-> [trait|protocolDefinition])");
        Set<String> result2 = selectIds("service:not(:test(-[trait]-> [trait|protocolDefinition]))");

        assertThat(result1, contains("smithy.example#MyService1"));
        assertThat(result2, contains("smithy.example#MyService2"));
    }
}
