/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.TagsTrait;

public class IncludeShapesByTagTest {

    @Test
    public void removesTraitsNotInList() {
        StringShape stringA = StringShape.builder()
                .id("ns.foo#baz")
                .addTrait(TagsTrait.builder().addValue("foo").addValue("baz").build())
                .build();
        StringShape stringB = StringShape.builder()
                .id("ns.foo#bar")
                .addTrait(TagsTrait.builder().addValue("qux").build())
                .build();
        Model model = Model.builder()
                .addShapes(stringA, stringB)
                .build();
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode().withMember("tags", Node.fromStrings("foo")))
                .build();
        Model result = new IncludeShapesByTag().transform(context);

        assertThat(result.getShape(stringA.getId()), not(Optional.empty()));
        assertThat(result.getShape(stringB.getId()), is(Optional.empty()));
    }
}
