/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

public class SetShapeTest {

    @Test
    public void builderUpdatesMemberId() {
        SetShape shape = SetShape.builder()
                .id("ns.foo#bar")
                .member(ShapeId.from("ns.foo#bam"))
                .id("ns.bar#bar")
                .build();
        assertThat(shape.getMember().getId(), equalTo(ShapeId.from("ns.bar#bar$member")));
        assertThat(shape.getMember().getTarget(), equalTo(ShapeId.from("ns.foo#bam")));
    }
}
