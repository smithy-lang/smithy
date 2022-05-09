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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ShapeVisitorDefaultTest {

    @Test
    public void sendsVisitorToDefaultValue() {
        ShapeVisitor<Integer> visitor = new ShapeVisitor.Default<Integer>() {
            @Override
            protected Integer getDefault(Shape shape) {
                return 1;
            }
        };

        testCases(visitor);
    }

    private void testCases(ShapeVisitor<Integer> visitor) {
        Integer value = 1;
        assertEquals(value, BlobShape.builder().id("ns.foo#Bar").build().accept(visitor));
        assertEquals(value, BooleanShape.builder().id("ns.foo#Bar").build().accept(visitor));
        assertEquals(value, ListShape.builder()
                .id("ns.foo#Bar")
                .member(MemberShape.builder().id("ns.foo#Bar$member").target("ns.foo#Baz").build())
                .build()
                .accept(visitor));
        assertEquals(value, SetShape.builder()
                .id("ns.foo#Bar")
                .member(MemberShape.builder().id("ns.foo#Bar$member").target("ns.foo#Baz").build())
                .build()
                .accept(visitor));
        assertEquals(value, MapShape.builder()
                .id("ns.foo#Bar")
                .key(MemberShape.builder().id("ns.foo#Bar$key").target("ns.foo#Baz").build())
                .value(MemberShape.builder().id("ns.foo#Bar$value").target("ns.foo#Baz").build())
                .build()
                .accept(visitor));
        assertEquals(value, OperationShape.builder()
                .id("ns.foo#Bar")
                .build()
                .accept(visitor));
        assertEquals(value, ResourceShape.builder().id("ns.foo#Bar").build().accept(visitor));
        assertEquals(value, ServiceShape.builder()
                .id("ns.foo#Bar")
                .version("2017-01-17")
                .build()
                .accept(visitor));
        assertEquals(value, StringShape.builder().id("ns.foo#Bar").build().accept(visitor));
        assertEquals(value, StructureShape.builder().id("ns.foo#Bar").build().accept(visitor));
        assertEquals(value, UnionShape.builder()
                .id("ns.foo#Bar")
                .addMember(MemberShape.builder().id("ns.foo#Bar$baz").target("ns.foo#Abc").build())
                .build()
                .accept(visitor));
        assertEquals(value, MemberShape.builder()
                .id("ns.foo#Bar$member")
                .target("ns.foo#Bam")
                .build()
                .accept(visitor));
        assertEquals(value, TimestampShape.builder().id("ns.foo#Bar").build().accept(visitor));
        assertEquals(value, ByteShape.builder().id("ns.foo#Bar").build().accept(visitor));
        assertEquals(value, ShortShape.builder().id("ns.foo#Bar").build().accept(visitor));
        assertEquals(value, IntegerShape.builder().id("ns.foo#Bar").build().accept(visitor));
        assertEquals(value, LongShape.builder().id("ns.foo#Bar").build().accept(visitor));
        assertEquals(value, FloatShape.builder().id("ns.foo#Bar").build().accept(visitor));
        assertEquals(value, DoubleShape.builder().id("ns.foo#Bar").build().accept(visitor));
        assertEquals(value, BigDecimalShape.builder().id("ns.foo#Bar").build().accept(visitor));
        assertEquals(value, BigIntegerShape.builder().id("ns.foo#Bar").build().accept(visitor));
    }
}
