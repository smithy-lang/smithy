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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.TagsTrait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.transform.ModelTransformer;

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
        ShapeIndex index = ShapeIndex.builder().addShapes(shape1, shape2, shape3).build();
        Model model = Model.builder().shapeIndex(index).build();
        Model result = new IncludeTags()
                .createTransformer(Collections.singletonList("foo"))
                .apply(ModelTransformer.create(), model);

        assertThat(result.getShapeIndex().getShape(shape1.getId()).get().getTags(), contains("foo"));
        assertThat(result.getShapeIndex().getShape(shape2.getId()).get(), equalTo(shape2));
        assertThat(result.getShapeIndex().getShape(shape3.getId()).get(), equalTo(shape3));
    }

    @Test
    public void removesTagsFromTraitDefsNotInList() {
        TraitDefinition foo = TraitDefinition.builder()
                .name("ns#foo")
                .addTag("foo")
                .addTag("baz")
                .build();
        TraitDefinition baz = TraitDefinition.builder().name("ns#baz").addTag("foo").build();
        TraitDefinition yuck = TraitDefinition.builder().name("ns#yuck").build();
        Model model = Model.builder()
                .shapeIndex(ShapeIndex.builder().build())
                .addTraitDefinition(foo)
                .addTraitDefinition(baz)
                .addTraitDefinition(yuck)
                .build();
        Model result = new IncludeTags()
                .createTransformer(Collections.singletonList("foo"))
                .apply(ModelTransformer.create(), model);

        assertThat(result.getTraitDefinition(foo.getFullyQualifiedName()).get().getTags(), contains("foo"));
        assertThat(result.getTraitDefinition(baz.getFullyQualifiedName()).get(), equalTo(baz));
        assertThat(result.getTraitDefinition(yuck.getFullyQualifiedName()).get(), equalTo(yuck));
    }
}
