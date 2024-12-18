/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceException;

public class TimestampShapeTest {
    @Test
    public void returnsAppropriateType() {
        TimestampShape shape = TimestampShape.builder().id("ns.foo#bar").build();

        assertEquals(shape.getType(), ShapeType.TIMESTAMP);
    }

    @Test
    public void mustNotContainMembersInShapeId() {
        Assertions.assertThrows(SourceException.class, () -> {
            TimestampShape.builder().id("ns.foo#bar$baz").build();
        });
    }

    @Test
    public void convertsToBuilder() {
        TimestampShape a = TimestampShape.builder().id("ns.foo#Baz").build();

        assertEquals(a, a.toBuilder().build());
    }
}
