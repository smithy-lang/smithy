/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.utils.ListUtils;

public class ReverseNeighborSelectorTest {

    private static Model model;

    @BeforeAll
    public static void before() {
        model = Model.assembler()
                .addImport(SelectorTest.class.getResource("reverse-neighbor.smithy"))
                .assemble()
                .unwrap();
    }

    @Test
    public void parsesValidReverseNeighbors() {
        List<String> exprs = ListUtils.of(
                "string < member < list",
                "structure <-[input]-",
                "structure <-[input]- operation",
                "structure <-[input, output]- operation",
                "structure <-[input, output, error]- operation",
                "structure <-[  input  ,    output,error  ]-operation");

        for (String expr : exprs) {
            Selector.parse(expr);
        }
    }

    @Test
    public void detectsInvalidReverseNeighbors() {
        List<String> exprs = ListUtils.of(
                "structure <-",
                "structure <-[",
                "structure <-[input",
                "structure <-[input,",
                "structure <-[input, output",
                "structure <-[input, output]",
                "structure <-[input, output]--",
                "structure < - [input, output]-", // no spaces between '-' and '['
                "structure <-[input, output] -", // no spaces between ']' and '-'
                "structure <-[]-");

        for (String expr : exprs) {
            Assertions.assertThrows(SelectorSyntaxException.class, () -> Selector.parse(expr));
        }
    }

    @Test
    public void traversesUndirectedReverseNeighbors() {
        Set<String> result = SelectorTest.exampleIds(model, "string < member < list");

        assertThat(result, containsInAnyOrder("smithy.example#StringList"));
    }

    @Test
    public void traversesUndirectedReverseNeighborsInList() {
        Set<String> result = SelectorTest.exampleIds(model, "string :test(< member < list)");

        assertThat(result, containsInAnyOrder("smithy.example#MyString1"));
    }

    @Test
    public void traversesUndirectedReverseNeighborsInMap() {
        Set<String> result = SelectorTest.exampleIds(model, "list :test(< member < map)");

        assertThat(result, containsInAnyOrder("smithy.example#StringList"));
    }

    @Test
    public void traversesWithNot() {
        Set<String> result = SelectorTest.exampleIds(model, "string :not(< member < list)");

        assertThat(result, containsInAnyOrder("smithy.example#MyString2"));
    }

    @Test
    public void traversesDirectedReverseOperationInputOutputAndErrors() {
        Set<String> result = SelectorTest.exampleIds(model, ":test(structure <-[input]- operation)");

        assertThat(result, containsInAnyOrder("smithy.example#OperationInput"));
    }

    @Test
    public void traversesDirectedReverseOperationDeepInputOutput() {
        Set<String> result = SelectorTest.exampleIds(
                model,
                "string :test(< member < list < member < structure <-[output]- operation)");

        assertThat(result, containsInAnyOrder("smithy.example#MyString1"));
    }

    @Test
    public void traversesDirectedReverseNeighborsInMap() {
        Set<String> result = SelectorTest.exampleIds(model, ":test(list < member < map)");

        assertThat(result, containsInAnyOrder("smithy.example#StringList"));
    }

    @Test
    public void traversesUndirectedReverseOperationInputOutputAndErrors() {
        Set<String> result = SelectorTest.exampleIds(model, ":test(structure <-[input, output, error]- operation)");

        assertThat(result,
                containsInAnyOrder(
                        "smithy.example#OperationInput",
                        "smithy.example#OperationOutput",
                        "smithy.example#Error"));
    }

    @Test
    public void traversesUndirectedReverseOperationDeepInputOutput() {
        Set<String> result = SelectorTest.exampleIds(
                model,
                ":test(string < member < list < member < structure <-[input, output]- operation)");

        assertThat(result, containsInAnyOrder("smithy.example#MyString1"));
    }

    @Test
    public void findsShapesNotConnectedToOtherShapes() {
        Set<String> result = SelectorTest.exampleIds(model, ":not([trait|trait]) :not(< *)");

        assertThat(result,
                containsInAnyOrder(
                        "smithy.example#Operation",
                        "smithy.example#MyString2"));
    }

    @Test
    public void findsTraitsNotConnectedToOtherShapes() {
        Set<String> result = SelectorTest.exampleIds(model, "[trait|trait] :not(<-[trait]-)");

        assertThat(result, containsInAnyOrder("smithy.example#notConnected"));
    }
}
