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
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceException;

public class ServiceShapeTest {
    @Test
    public void returnsAppropriateType() {
        ServiceShape shape = ServiceShape.builder().id("ns.foo#Bar").version("2017-01-17").build();

        assertEquals(shape.getType(), ShapeType.SERVICE);
        assertThat(shape, is(shape.expectServiceShape()));
    }

    @Test
    public void mustNotContainMembersInShapeId() {
        Assertions.assertThrows(SourceException.class, () -> {
            ServiceShape.builder().id("ns.foo#Bar$baz").build();
        });
    }

    @Test
    public void convertsToBuilder() {
        ServiceShape service = ServiceShape.builder()
                .id("ns.foo#Bar")
                .version("2017-01-17")
                .build();
        assertEquals(service, service.toBuilder().build());
    }
}
