/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceException;

public class BooleanShapeTest {
    @Test
    public void returnsAppropriateType() {
        BooleanShape shape = BooleanShape.builder().id("ns.foo#bar").build();

        assertEquals(shape.getType(), ShapeType.BOOLEAN);
    }

    @Test
    public void mustNotContainMembersInShapeId() {
        Assertions.assertThrows(SourceException.class, () -> {
            BooleanShape.builder().id("ns.foo#bar$baz").build();
        });
    }
}
