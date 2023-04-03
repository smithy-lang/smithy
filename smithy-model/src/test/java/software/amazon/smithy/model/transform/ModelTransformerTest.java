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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collections;
import java.util.Optional;
import java.util.function.Predicate;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.MixinTrait;
import software.amazon.smithy.model.traits.ReadonlyTrait;
import software.amazon.smithy.model.traits.UnitTypeTrait;

public class ModelTransformerTest {

    private Model createTestModel() {
        return Model.assembler()
                .addImport(ModelTransformerTest.class.getResource("test-model.json"))
                .assemble()
                .unwrap();
    }

    @Test
    public void discoversOnRemoveClassesWithSpi() {
        ModelTransformer transformer = ModelTransformer.create();
        Model model = createTestModel();
        Model result = transformer.removeShapesIf(model, Shape::isStructureShape);
        ShapeId operation = ShapeId.from("ns.foo#MyOperation");

        assertThat(result.expectShape(operation), Matchers.not(Optional.empty()));
        assertThat(result.expectShape(operation).asOperationShape().map(OperationShape::getInputShape),
                   Matchers.equalTo(Optional.of(UnitTypeTrait.UNIT)));
        assertThat(result.expectShape(operation).asOperationShape().map(OperationShape::getOutputShape),
                   Matchers.equalTo(Optional.of(UnitTypeTrait.UNIT)));
        assertThat(result.expectShape(operation).asOperationShape().map(OperationShape::getErrors),
                          Matchers.equalTo(Optional.of(Collections.emptyList())));
    }

    @Test
    public void removesTraitShapesButNotTraitUsage() {
        ModelTransformer transformer = ModelTransformer.create();
        Model model = createTestModel();
        Model nonTraitShapes = transformer.getModelWithoutTraitShapes(model);
        ShapeId operation = ShapeId.from("ns.foo#MyOperation");

        assertThat(nonTraitShapes.getShape(operation), Matchers.not(Optional.empty()));
        assertThat(nonTraitShapes.getShape(operation).get().getTrait(ReadonlyTrait.class), Matchers.not(Optional.empty()));
        assertTrue(nonTraitShapes.getShape(operation).get().hasTrait("ns.foo#MyTrait"));
        assertTrue(nonTraitShapes.getShape(operation).get().hasTrait("ns.foo#MyOtherTrait"));
        assertFalse(nonTraitShapes.getShape(ShapeId.from("ns.foo#MyTrait")).isPresent());
        assertFalse(nonTraitShapes.getShape(ShapeId.from("ns.foo#MyOtherTrait")).isPresent());
        assertThat(nonTraitShapes.getShape(EnumTrait.ID), Matchers.equalTo(Optional.empty()));
    }

    @Test
    public void removesTraitShapesExcludingFilteredButNotTraitUsage() {
        ModelTransformer transformer = ModelTransformer.create();
        Model model = createTestModel();
        Predicate<Shape> keepFilter = trait -> !trait.getId().equals(ShapeId.from("ns.foo#MyTrait"));
        Model nonTraitShapes = transformer.getModelWithoutTraitShapes(model, keepFilter);
        ShapeId operation = ShapeId.from("ns.foo#MyOperation");

        assertThat(nonTraitShapes.getShape(operation), Matchers.not(Optional.empty()));
        assertThat(nonTraitShapes.getShape(operation).get().getTrait(ReadonlyTrait.class), Matchers.not(Optional.empty()));
        assertTrue(nonTraitShapes.getShape(operation).get().hasTrait("ns.foo#MyTrait"));
        assertTrue(nonTraitShapes.getShape(operation).get().hasTrait("ns.foo#MyOtherTrait"));
        assertTrue(nonTraitShapes.getShape(ShapeId.from("ns.foo#MyTrait")).isPresent());
        assertFalse(nonTraitShapes.getShape(ShapeId.from("ns.foo#MyOtherTrait")).isPresent());
        assertThat(nonTraitShapes.getShape(EnumTrait.ID), Matchers.equalTo(Optional.empty()));
    }

    @Test
    public void canFilterAndRemoveMixinsWhenNoMixinsArePresent() {
        ModelTransformer transformer = ModelTransformer.create();
        Model model = createTestModel();

        assertThat(transformer.flattenAndRemoveMixins(model), Matchers.equalTo(model));
    }

    @Test
    public void canFilterAndRemoveMixinsWhenMixinsArePresent() {
        ModelTransformer transformer = ModelTransformer.create();
        Model.Builder builder = Model.builder();
        StringShape string = StringShape.builder().id("smithy.example#String").build();
        StructureShape mixin1 = StructureShape.builder()
                .id("smithy.example#Mixin1")
                .addTrait(MixinTrait.builder().build())
                .addMember("a", string.getId())
                .build();
        StructureShape mixin2 = StructureShape.builder()
                .id("smithy.example#Mixin2")
                .addMember("b", string.getId())
                .addTrait(MixinTrait.builder().build())
                .build();
        StructureShape mixin3 = StructureShape.builder()
                .id("smithy.example#Mixin3")
                .addMember("c", string.getId())
                .addTrait(MixinTrait.builder().build())
                .addMixin(mixin2)
                .build();
        StructureShape concrete = StructureShape.builder()
                .id("smithy.example#Concrete")
                .addMember("d", string.getId())
                .addMixin(mixin1)
                .addMixin(mixin3)
                .build();
        builder.addShapes(mixin1, mixin2, mixin3, concrete);
        Model model = builder.build();
        Model result = transformer.flattenAndRemoveMixins(model);

        assertThat(result.toSet(), Matchers.not(Matchers.hasItem(mixin1)));
        assertThat(result.toSet(), Matchers.not(Matchers.hasItem(mixin2)));
        assertThat(result.toSet(), Matchers.not(Matchers.hasItem(mixin3)));
        assertThat(result.getShape(concrete.getId()).get(),
                   Matchers.equalTo(concrete.toBuilder().flattenMixins().build()));
    }

    @ParameterizedTest
    @MethodSource("flattenShapesData")
    public void flattenShapes(String name) {
        Model original = Model.assembler()
                .addImport(Model.class.getResource("loader/valid/mixins/" + name + ".smithy"))
                .assemble()
                .unwrap();
        Model expectedModel = Model.assembler()
                .addImport(Model.class.getResource("loader/valid/mixins/" + name + ".flattened.smithy"))
                .assemble()
                .unwrap();
        Model flattened = ModelTransformer.create().flattenAndRemoveMixins(original);

        Node result = ModelSerializer.builder().build().serialize(flattened);
        Node expected = ModelSerializer.builder().build().serialize(expectedModel);

        try {
            Node.assertEquals(result, expected);
        } catch (ExpectationNotMetException e) {
            fail(name + ": " + e.getMessage());
        }
    }

    public static String[] flattenShapesData() {
        return new String[] {
            "loads-mixins",
            "mixins-with-members",
            "mixins-with-members-and-traits",
            "mixins-with-member-override-1",
            "mixins-with-member-override-2",
            "mixins-with-member-override-3",
            "mixins-with-member-override-4",
            "mixins-with-mixin-local-traits",
            "operations",
            "resources",
            "services",
            "idl-mixins-redefine-member",
            "enum-mixins"
        };
    }
}
