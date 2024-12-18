/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.TagsTrait;

public class IncludeTagsTest {

    @Test
    public void removesTagsFromShapesNotInList() {
        Shape shape1 = StringShape.builder()
                .id("ns.foo#shape1")
                .addTrait(TagsTrait.builder().addValue("foo").addValue("baz").build())
                .build();
        Shape shape2 = StringShape.builder()
                .id("ns.foo#shape2")
                .addTrait(TagsTrait.builder().addValue("foo").build())
                .build();
        Shape shape3 = StringShape.builder().id("ns.foo#shape3").build();
        Model model = Model.builder().addShapes(shape1, shape2, shape3).build();
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode().withMember("tags", Node.fromStrings("foo")))
                .build();
        Model result = new IncludeTags().transform(context);

        assertThat(result.expectShape(shape1.getId()).getTags(), contains("foo"));
        assertThat(result.expectShape(shape2.getId()), equalTo(shape2));
        assertThat(result.expectShape(shape3.getId()), equalTo(shape3));
    }
}
