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

import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.DeprecatedTrait;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;

public class FilterTraitsTest {

    @Test
    public void removesTraits() {
        ShapeId aId = ShapeId.from("ns.foo#A");
        StringShape a = StringShape.builder()
                .id(aId)
                .addTrait(new SensitiveTrait(SourceLocation.NONE))
                .addTrait(DeprecatedTrait.builder().build())
                .build();
        ShapeId bId = ShapeId.from("ns.foo#B");
        StringShape b = StringShape.builder()
                .id(bId)
                .addTrait(new SensitiveTrait(SourceLocation.NONE))
                .addTrait(DeprecatedTrait.builder().build())
                .build();
        Model model = Model.builder().addShapes(a, b).build();
        ModelTransformer transformer = ModelTransformer.create();
        Model result = transformer.filterTraits(
                model, (shape, trait) -> !trait.toShapeId().equals(ShapeId.from("smithy.api#sensitive")));

        assertThat(result.shapes().count(), Matchers.is(2L));
        assertThat(result.expectShape(aId).getTrait(SensitiveTrait.class), Matchers.is(Optional.empty()));
        assertThat(result.expectShape(aId).getTrait(DeprecatedTrait.class), Matchers.not(Optional.empty()));
        assertThat(result.expectShape(bId).getTrait(SensitiveTrait.class), Matchers.is(Optional.empty()));
        assertThat(result.expectShape(bId).getTrait(DeprecatedTrait.class), Matchers.not(Optional.empty()));
    }

    @Test
    public void removesTraitsWithComposedPredicate() {
        ShapeId aId = ShapeId.from("ns.foo#A");
        StringShape a = StringShape.builder()
                .id(aId)
                .addTrait(new SensitiveTrait(SourceLocation.NONE))
                .addTrait(new DocumentationTrait("docs", SourceLocation.NONE))
                .addTrait(DeprecatedTrait.builder().build())
                .build();
        Model model = Model.builder().addShape(a).build();
        ModelTransformer transformer = ModelTransformer.create();
        Model result = transformer.filterTraits(
                model, (shape, trait) -> !trait.toShapeId().equals(ShapeId.from("smithy.api#sensitive"))
                                         && !trait.toShapeId().equals(ShapeId.from("smithy.api#documentation")));

        assertThat(result.shapes().count(), Matchers.is(1L));
        assertThat(result.expectShape(aId).getTrait(SensitiveTrait.class), Matchers.is(Optional.empty()));
        assertThat(result.expectShape(aId).getTrait(DocumentationTrait.class), Matchers.is(Optional.empty()));
        assertThat(result.expectShape(aId).getTrait(DeprecatedTrait.class), Matchers.not(Optional.empty()));
    }

    @Test
    public void leavesShapesAlone() {
        ShapeId aId = ShapeId.from("ns.foo#A");
        StringShape a = StringShape.builder().id(aId).build();
        Model model = Model.builder().addShape(a).build();
        ModelTransformer transformer = ModelTransformer.create();
        Model result = transformer.filterTraits(model, (shape, trait) -> true);

        assertThat(result.shapes().count(), Matchers.is(1L));
    }
}
