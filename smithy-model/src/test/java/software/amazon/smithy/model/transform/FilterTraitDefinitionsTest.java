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
import java.util.Set;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.DynamicTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.traits.TraitDefinition;

public class FilterTraitDefinitionsTest {

    @Test
    public void removesTraitsWhenDefinitionIsRemoved() {
        TraitDefinition definition1 = TraitDefinition.builder().name("ns.foo#baz").build();
        TraitDefinition definition2 = TraitDefinition.builder().name("ns.foo#bar").build();
        ShapeId shapeId1 = ShapeId.from("ns.foo#id1");
        StringShape shape1 = StringShape.builder()
                .id(shapeId1)
                .addTrait(new DynamicTrait("foo", Node.from(true)))
                .addTrait(new SensitiveTrait(SourceLocation.NONE))
                .build();
        ShapeId shapeId2 = ShapeId.from("ns.foo#id2");
        StringShape shape2 = StringShape.builder()
                .id(shapeId2)
                .addTrait(new DynamicTrait("ns.foo#baz", Node.from(true)))
                .addTrait(new DynamicTrait("ns.foo#bar", Node.from(true)))
                .addTrait(new SensitiveTrait(SourceLocation.NONE))
                .build();
        Model model = Model.builder()
                .addTraitDefinition(definition1)
                .addTraitDefinition(definition2)
                .shapeIndex(ShapeIndex.builder().addShapes(shape1, shape2).build())
                .build();
        ModelTransformer transformer = ModelTransformer.create();
        Model result = transformer.filterTraitDefinitions(model, def -> !def.getFullyQualifiedName().equals("ns.foo#baz"));
        Set<TraitDefinition> definitions = result.getTraitDefinitions();
        ShapeIndex index = result.getShapeIndex();

        assertThat(definitions, Matchers.hasSize(1));
        assertThat(definitions, Matchers.contains(definition2));
        assertThat(index.getShape(shapeId1).get().getTrait(SensitiveTrait.class), Matchers.not(Optional.empty()));
        assertThat(index.getShape(shapeId1).get().findTrait("ns.foo#baz"), Matchers.is(Optional.empty()));
        assertThat(index.getShape(shapeId2).get().getTrait(SensitiveTrait.class), Matchers.not(Optional.empty()));
        assertThat(index.getShape(shapeId2).get().findTrait("ns.foo#baz"), Matchers.is(Optional.empty()));
        assertThat(index.getShape(shapeId2).get().findTrait("ns.foo#bar"), Matchers.not(Optional.empty()));
    }

    @Test
    public void keepsModelAsIsWhenNothingIsChanged() {
        Model model = Model.builder().shapeIndex(ShapeIndex.builder().build()).build();
        ModelTransformer transformer = ModelTransformer.create();
        Model result = transformer.filterTraitDefinitions(model, def -> false);

        assertThat(model, Matchers.is(result));
    }
}
