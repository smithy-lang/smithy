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

package software.amazon.smithy.model.transform;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;

public class MapShapesTest {

    @Test
    public void replacesMappedShapes() {
        ShapeId shapeId = ShapeId.from("ns.foo#id1");
        StringShape shape = StringShape.builder()
                .id(shapeId)
                .addTrait(new SensitiveTrait())
                .build();
        Model model = Model.builder().addShape(shape).build();
        ModelTransformer transformer = ModelTransformer.create();
        Model result = transformer.mapShapes(model, s -> Shape.shapeToBuilder(s).removeTrait("sensitive").build());

        assertThat(result.expectShape(shapeId).getId(), Matchers.equalTo(shapeId));
        assertThat(result.expectShape(shapeId).getTrait(SensitiveTrait.class), Matchers.is(Optional.empty()));
    }

    @Test
    public void throwsWhenMapperChangesShapeId() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            ShapeId shapeId = ShapeId.from("ns.foo#id1");
            StringShape shape = StringShape.builder().id(shapeId).build();
            Model model = Model.builder().addShape(shape).build();
            ModelTransformer transformer = ModelTransformer.create();
            transformer.mapShapes(model, (s -> Shape.shapeToBuilder(s).id("ns.foo#change").build()));
        });
    }

    @Test
    public void mapsUsingAllMappers() {
        ShapeId shapeId = ShapeId.from("ns.foo#id1");
        StringShape shape = StringShape.builder()
                .id(shapeId)
                .addTrait(new SensitiveTrait())
                .addTrait(new DocumentationTrait("docs", SourceLocation.NONE))
                .build();
        Model model = Model.builder().addShape(shape).build();
        ModelTransformer transformer = ModelTransformer.create();
        Model result = transformer.mapShapes(model, Arrays.asList(
                s -> Shape.shapeToBuilder(s).removeTrait("sensitive").build(),
                s -> Shape.shapeToBuilder(s).removeTrait("documentation").build()));

        assertThat(result.expectShape(shapeId).getTrait(SensitiveTrait.class), Matchers.is(Optional.empty()));
        assertThat(result.expectShape(shapeId).getTrait(DocumentationTrait.class), Matchers.is(Optional.empty()));
    }
}
