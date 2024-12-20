/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.smithy.model.shapes.ShapeId.from;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SetUtils;

public class TopologicalShapeSortTest {

    @Test
    public void topologicallySorts() {
        List<Pair<ShapeId, Set<ShapeId>>> input = new ArrayList<>();
        input.add(Pair.of(from("test#A"), SetUtils.of(from("test#B"), from("test#C"))));
        input.add(Pair.of(from("test#B"), SetUtils.of(from("test#C"), from("test#D"))));
        input.add(Pair.of(from("test#C"), SetUtils.of(from("test#E"))));
        input.add(Pair.of(from("test#D"), SetUtils.of()));
        input.add(Pair.of(from("test#E"), SetUtils.of()));

        for (int i = 0; i < 100; i++) {
            Collections.shuffle(input);
            TopologicalShapeSort sort = new TopologicalShapeSort();
            input.forEach(pair -> sort.enqueue(pair.getLeft(), pair.getRight()));
            List<ShapeId> result = sort.dequeueSortedShapes();

            assertTrue("test#D".equals(result.get(0).toString()) || "test#E".equals(result.get(0).toString()));
            assertTrue("test#D".equals(result.get(1).toString()) || "test#E".equals(result.get(1).toString()));
            assertThat("test#C", equalTo(result.get(2).toString()));
            assertThat("test#B", equalTo(result.get(3).toString()));
            assertThat("test#A", equalTo(result.get(4).toString()));
        }
    }

    @Test
    public void detectsCycles() {
        List<Pair<ShapeId, Set<ShapeId>>> input = new ArrayList<>();
        input.add(Pair.of(from("test#A"), SetUtils.of(from("test#B"), from("test#C"))));
        input.add(Pair.of(from("test#B"), SetUtils.of(from("test#C"), from("test#D"))));
        input.add(Pair.of(from("test#C"), SetUtils.of(from("test#E"))));
        input.add(Pair.of(from("test#D"), SetUtils.of()));
        input.add(Pair.of(from("test#E"), SetUtils.of(from("test#A"))));

        for (int i = 0; i < 100; i++) {
            Collections.shuffle(input);
            TopologicalShapeSort sort = new TopologicalShapeSort();
            input.forEach(pair -> sort.enqueue(pair.getLeft(), pair.getRight()));
            try {
                sort.dequeueSortedShapes();
                throw new IllegalArgumentException("should have detected a cycle");
            } catch (TopologicalShapeSort.CycleException e) {
                assertThat(e.getUnresolved(),
                        containsInAnyOrder(from("test#A"), from("test#B"), from("test#C"), from("test#E")));
            }
        }
    }
}
