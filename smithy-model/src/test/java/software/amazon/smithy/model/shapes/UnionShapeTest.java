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
