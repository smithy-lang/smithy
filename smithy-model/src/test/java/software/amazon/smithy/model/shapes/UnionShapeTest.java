/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceException;

public class UnionShapeTest {
    @Test
    public void returnsAppropriateType() {
        UnionShape shape = UnionShape.builder()
                .id("ns.foo#bar")
                .addMember(MemberShape.builder()
                        .id("ns.foo#bar$baz")
                        .target("ns.foo#bam")
                        .build())
                .build();

        assertEquals(shape.getType(), ShapeType.UNION);
    }

    @Test
    public void mustNotContainMembersInShapeId() {
        Assertions.assertThrows(SourceException.class, () -> {
            UnionShape.builder().id("ns.foo#bar$baz").build();
        });
    }

    @Test
    public void builderUpdatesMemberIds() {
        UnionShape original = UnionShape.builder()
                .id("ns.foo#bar")
                .addMember("foo", ShapeId.from("ns.foo#bam"))
                .addMember("baz", ShapeId.from("ns.foo#bam"))
                .build();

        UnionShape actual = original.toBuilder().id(ShapeId.from("ns.bar#bar")).build();

        UnionShape expected = UnionShape.builder()
                .id("ns.bar#bar")
                .addMember("foo", ShapeId.from("ns.foo#bam"))
                .addMember("baz", ShapeId.from("ns.foo#bam"))
                .build();

        assertThat(actual, equalTo(expected));
    }
}
