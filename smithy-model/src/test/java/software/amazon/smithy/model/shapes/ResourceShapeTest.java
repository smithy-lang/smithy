/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceException;

public class ResourceShapeTest {
    @Test
    public void returnsAppropriateType() {
        ResourceShape shape = ResourceShape.builder().id("ns.foo#Bar").build();

        assertEquals(shape.getType(), ShapeType.RESOURCE);
    }

    @Test
    public void mustNotContainMembersInShapeId() {
        Assertions.assertThrows(SourceException.class, () -> {
            ResourceShape.builder().id("ns.foo#Bar$baz").build();
        });
    }

    @Test
    public void maintainsIdentifiers() {
        Map<String, ShapeId> identifiers = new TreeMap<>();
        identifiers.put("arn", ShapeId.from("ns.foo#ARN"));
        ResourceShape shape = ResourceShape.builder().id("ns.foo#Bar")
            .identifiers(identifiers)
            .build();
        assertEquals(shape.getIdentifiers(), identifiers);
    }

    @Test
    public void maintainsProperties() {
        Map<String, ShapeId> properties = new TreeMap<>();
        properties.put("fooProperty", ShapeId.from("ns.foo#Shape"));
        ResourceShape.Builder builder = ResourceShape.builder();
        ResourceShape shape = builder.id("ns.foo#Bar")
            .properties(properties)
            .build();
        assertEquals(shape.getProperties(), properties);
        // Verify that toBuilder() works as well.
        assertEquals(properties, shape.toBuilder().build().getProperties());
    }
}
