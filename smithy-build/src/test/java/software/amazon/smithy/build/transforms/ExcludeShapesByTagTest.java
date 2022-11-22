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

package software.amazon.smithy.build.transforms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Paths;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.IntEnumShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.TagsTrait;

public class ExcludeShapesByTagTest {

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
        Model result = new ExcludeShapesByTag().transform(context);

        assertThat(result.getShape(stringA.getId()), is(Optional.empty()));
        assertThat(result.getShape(stringB.getId()), not(Optional.empty()));
    }

    @Test
    public void filtersMembers() throws Exception {
        Model model = Model.assembler()
                .addImport(Paths.get(getClass().getResource("filter-by-tags.smithy").toURI()))
                .assemble()
                .unwrap();
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode().withMember("tags", Node.fromStrings("filter")))
                .build();
        Model result = new ExcludeShapesByTag().transform(context);

        EnumShape foo = result.expectShape(ShapeId.from("smithy.example#Foo"), EnumShape.class);
        assertThat(foo.members().size(), is(1));

        IntEnumShape bar = result.expectShape(ShapeId.from("smithy.example#Bar"), IntEnumShape.class);
        assertThat(bar.members().size(), is(1));

        // Mixin members are retained, but excluded traits are removed.
        MemberShape baz = result.expectShape(ShapeId.from("smithy.example#StructForMixin$baz"), MemberShape.class);
        assertFalse(baz.findMemberTrait(result, "MyTrait").isPresent());
    }
}
