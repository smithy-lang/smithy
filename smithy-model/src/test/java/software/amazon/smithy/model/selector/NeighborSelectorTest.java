/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

public class NeighborSelectorTest {

    private static final Model MODEL = Model.assembler()
            .addImport(NeighborSelectorTest.class.getResource("neighbor-test.smithy"))
            .assemble()
            .unwrap();

    @Test
    public void specialCasesDeprecatedBoundSelectorFromResource() {
        Selector selector = Selector.parse("resource -[bound]->");

        assertThat(selector.select(MODEL), contains(MODEL.expectShape(ShapeId.from("smithy.example#MyService2"))));
    }

    @Test
    public void specialCasesDeprecatedBoundSelectorWithBugCompatibility() {
        Selector selector = Selector.parse("operation -[bound]->");

        assertThat(selector.select(MODEL), empty());
    }

    @Test
    public void specialCasesDeprecatedInstanceOperation() {
        Selector selector = Selector.parse("-[instanceOperation]->");

        assertThat(selector.select(MODEL),
                containsInAnyOrder(
                        MODEL.expectShape(ShapeId.from("smithy.example#DeleteMyResource")),
                        MODEL.expectShape(ShapeId.from("smithy.example#GetMyResource"))));
    }

    @Test
    public void canUseDeprecatedRelsWithRealRels() {
        Selector selector = Selector.parse("-[bound, instanceOperation, resource]->");

        assertThat(selector.select(MODEL),
                containsInAnyOrder(
                        MODEL.expectShape(ShapeId.from("smithy.example#MyService2")),
                        MODEL.expectShape(ShapeId.from("smithy.example#MyResource")),
                        MODEL.expectShape(ShapeId.from("smithy.example#DeleteMyResource")),
                        MODEL.expectShape(ShapeId.from("smithy.example#GetMyResource"))));
    }
}
