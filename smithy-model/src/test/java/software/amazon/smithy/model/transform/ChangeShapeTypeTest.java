/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.IntEnumShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.SetShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.EnumDefinition;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.EnumValueTrait;
import software.amazon.smithy.model.traits.InternalTrait;
import software.amazon.smithy.model.traits.TagsTrait;
import software.amazon.smithy.model.traits.UnitTypeTrait;
import software.amazon.smithy.model.traits.synthetic.SyntheticEnumTrait;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;

public class ChangeShapeTypeTest {
    @ParameterizedTest
    @MethodSource("simpleTypeTransforms")
    public void changesSimpleShapeTypes(ShapeType start, ShapeType dest, boolean success) {
        DocumentationTrait docTrait = new DocumentationTrait("Hi");
        ShapeId id = ShapeId.from("smithy.example#Test");
        SourceLocation source = new SourceLocation("/foo", 1, 1);
        Shape startShape;
        if (start == ShapeType.ENUM) {
            startShape = EnumShape.builder()
                    .id(id)
                    .addMember("FOO", "foo")
                    .addTrait(docTrait)
                    .source(source)
                    .build();
        } else if (start == ShapeType.INT_ENUM) {
            startShape = IntEnumShape.builder()
                    .id(id)
                    .addMember("FOO", 1)
                    .addTrait(docTrait)
                    .source(source)
                    .build();
        } else {
            startShape = start.createBuilderForType()
                    .addTrait(docTrait)
                    .id(id)
                    .source(source)
                    .build();
        }
        Model model = Model.builder().addShape(startShape).build();

        try {
            Model result = ModelTransformer.create().changeShapeType(model, MapUtils.of(id, dest));
            if (!success) {
                Assertions.fail("Expected test to fail");
            }
            assertThat(result.expectShape(id).getType(), Matchers.is(dest));
            assertThat(result.expectShape(id).expectTrait(DocumentationTrait.class), Matchers.equalTo(docTrait));
            assertThat(result.expectShape(id).getSourceLocation(), Matchers.equalTo(source));
        } catch (Exception e) {
            if (success) {
                throw e;
            }
        }
    }

    private static List<Arguments> simpleTypeTransforms() {
        Set<ShapeType> simpleTypes = new TreeSet<>();
        for (ShapeType type : ShapeType.values()) {
            if (type.getCategory() == ShapeType.Category.SIMPLE) {
                simpleTypes.add(type);
            }
        }

        List<Arguments> result = new ArrayList<>();
        for (ShapeType start : simpleTypes) {
            for (ShapeType dest : ShapeType.values()) {
                if (start != dest) {
                    result.add(Arguments.of(start, dest, expectedResult(dest)));
                }
            }
        }

        return result;
    }

    private static boolean expectedResult(ShapeType dest) {
        if (dest == ShapeType.ENUM || dest == ShapeType.INT_ENUM) {
            return false;
        }
        return dest.getCategory() == ShapeType.Category.SIMPLE;
    }

    @Test
    public void changesListToSet() {
        DocumentationTrait docTrait = new DocumentationTrait("Hi");
        ShapeId id = ShapeId.from("smithy.example#Test");
        SourceLocation source = new SourceLocation("/foo", 1, 1);
        Shape startShape = ListShape.builder()
                .addTrait(docTrait)
                .id(id)
                .source(source)
                .member(ShapeId.from("smithy.api#String"))
                .build();
        Model model = Model.assembler().addShape(startShape).assemble().unwrap();
        Model result = ModelTransformer.create().changeShapeType(model, MapUtils.of(id, ShapeType.SET));

        assertThat(result.expectShape(id).getType(), Matchers.is(ShapeType.SET));
        assertThat(result.expectShape(id).expectTrait(DocumentationTrait.class), Matchers.equalTo(docTrait));
        assertThat(result.expectShape(id).getSourceLocation(), Matchers.equalTo(source));
    }

    @Test
    public void cannotConvertListToAnythingButSet() {
        Shape startShape = ListShape.builder()
                .id(ShapeId.from("smithy.example#Test"))
                .member(ShapeId.from("smithy.api#String"))
                .build();
        Model model = Model.assembler().addShape(startShape).assemble().unwrap();

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ModelTransformer.create().changeShapeType(model, MapUtils.of(startShape.getId(), ShapeType.STRING));
        });
    }

    @Test
    public void changesSetToList() {
        DocumentationTrait docTrait = new DocumentationTrait("Hi");
        ShapeId id = ShapeId.from("smithy.example#Test");
        SourceLocation source = new SourceLocation("/foo", 1, 1);
        Shape startShape = SetShape.builder()
                .addTrait(docTrait)
                .id(id)
                .source(source)
                .member(ShapeId.from("smithy.api#String"))
                .build();
        Model model = Model.assembler().addShape(startShape).assemble().unwrap();
        Model result = ModelTransformer.create().changeShapeType(model, MapUtils.of(id, ShapeType.LIST));

        assertThat(result.expectShape(id).getType(), Matchers.is(ShapeType.LIST));
        assertThat(result.expectShape(id).expectTrait(DocumentationTrait.class), Matchers.equalTo(docTrait));
        assertThat(result.expectShape(id).getSourceLocation(), Matchers.equalTo(source));
    }

    @Test
    public void cannotConvertSetToAnythingButList() {
        Shape startShape = SetShape.builder()
                .id(ShapeId.from("smithy.example#Test"))
                .member(ShapeId.from("smithy.api#String"))
                .build();
        Model model = Model.assembler().addShape(startShape).assemble().unwrap();

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ModelTransformer.create().changeShapeType(model, MapUtils.of(startShape.getId(), ShapeType.STRING));
        });
    }

    @Test
    public void changesStructureToUnion() {
        DocumentationTrait docTrait = new DocumentationTrait("Hi");
        ShapeId id = ShapeId.from("smithy.example#Test");
        SourceLocation source = new SourceLocation("/foo", 1, 1);
        Shape startShape = StructureShape.builder()
                .addTrait(docTrait)
                .id(id)
                .source(source)
                .addMember("foo", ShapeId.from("smithy.api#String"))
                .build();
        Model model = Model.assembler().addShape(startShape).assemble().unwrap();
        Model result = ModelTransformer.create().changeShapeType(model, MapUtils.of(id, ShapeType.UNION));

        assertThat(result.expectShape(id).getType(), Matchers.is(ShapeType.UNION));
        assertThat(result.expectShape(id).expectTrait(DocumentationTrait.class), Matchers.equalTo(docTrait));
        assertThat(result.expectShape(id).getSourceLocation(), Matchers.equalTo(source));
        assertThat(result.expectShape(id).members(), Matchers.hasSize(1));
        assertThat(result.expectShape(id).members().iterator().next(),
                Matchers.equalTo(startShape.members().iterator().next()));
    }

    @Test
    public void changesUnionToStructure() {
        DocumentationTrait docTrait = new DocumentationTrait("Hi");
        ShapeId id = ShapeId.from("smithy.example#Test");
        SourceLocation source = new SourceLocation("/foo", 1, 1);
        Shape startShape = UnionShape.builder()
                .addTrait(docTrait)
                .id(id)
                .source(source)
                .addMember("foo", ShapeId.from("smithy.api#String"))
                .build();
        Model model = Model.assembler().addShape(startShape).assemble().unwrap();
        Model result = ModelTransformer.create().changeShapeType(model, MapUtils.of(id, ShapeType.STRUCTURE));

        assertThat(result.expectShape(id).getType(), Matchers.is(ShapeType.STRUCTURE));
        assertThat(result.expectShape(id).expectTrait(DocumentationTrait.class), Matchers.equalTo(docTrait));
        assertThat(result.expectShape(id).getSourceLocation(), Matchers.equalTo(source));
        assertThat(result.expectShape(id).members(), Matchers.hasSize(1));
        assertThat(result.expectShape(id).members().iterator().next(),
                Matchers.equalTo(startShape.members().iterator().next()));
    }

    @Test
    public void cannotConvertStructureToAnythingButUnion() {
        Shape startShape = StructureShape.builder().id(ShapeId.from("smithy.example#Test")).build();
        Model model = Model.assembler().addShape(startShape).assemble().unwrap();

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ModelTransformer.create().changeShapeType(model, MapUtils.of(startShape.getId(), ShapeType.STRING));
        });
    }

    @Test
    public void ignoresConversionToSameType() {
        Shape startShape = StructureShape.builder().id(ShapeId.from("smithy.example#Test")).build();
        Model model = Model.assembler().addShape(startShape).assemble().unwrap();

        ModelTransformer.create().changeShapeType(model, MapUtils.of(startShape.getId(), ShapeType.STRUCTURE));
    }

    @Test
    public void cannotConvertUnionToAnythingButStructure() {
        Shape startShape = UnionShape.builder()
                .id(ShapeId.from("smithy.example#Test"))
                .addMember("foo", ShapeId.from("smithy.api#String"))
                .build();
        Model model = Model.assembler().addShape(startShape).assemble().unwrap();

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ModelTransformer.create().changeShapeType(model, MapUtils.of(startShape.getId(), ShapeType.STRING));
        });
    }

    @Test
    public void canConvertStringToEnum() {
        EnumTrait trait = EnumTrait.builder()
                .addEnum(EnumDefinition.builder()
                        .name("foo")
                        .value("bar")
                        .addTag("internal")
                        .build())
                .build();
        SourceLocation source = new SourceLocation("/foo", 1, 1);
        ShapeId id = ShapeId.fromParts("ns.foo", "bar");
        StringShape startShape = StringShape.builder()
                .id(id)
                .addTrait(trait)
                .source(source)
                .build();
        Model model = Model.assembler().addShape(startShape).assemble().unwrap();
        Model result = ModelTransformer.create().changeShapeType(model, MapUtils.of(id, ShapeType.ENUM));
        assertThat(result.expectShape(id).getType(), Matchers.is(ShapeType.ENUM));
        assertThat(result.expectShape(id).getSourceLocation(), Matchers.equalTo(source));
        assertThat(result.expectShape(id).members(), Matchers.hasSize(1));
        assertThat(result.expectShape(id).members().iterator().next(),
                Matchers.equalTo(MemberShape.builder()
                        .id(id.withMember("foo"))
                        .target(UnitTypeTrait.UNIT)
                        .addTrait(EnumValueTrait.builder().stringValue("bar").build())
                        .addTrait(new InternalTrait())
                        .addTrait(TagsTrait.builder().addValue("internal").build())
                        .build()));
    }

    @Test
    public void cantConvertBaseStringWithoutEnumTrait() {
        SourceLocation source = new SourceLocation("/foo", 1, 1);
        ShapeId id = ShapeId.fromParts("ns.foo", "bar");
        StringShape startShape = StringShape.builder()
                .id(id)
                .source(source)
                .build();
        Model model = Model.assembler().addShape(startShape).assemble().unwrap();

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ModelTransformer.create().changeShapeType(model, MapUtils.of(startShape.getId(), ShapeType.ENUM));
        });
    }

    @Test
    public void cantConvertBaseStringWithNamelessEnumTrait() {
        EnumTrait trait = EnumTrait.builder()
                .addEnum(EnumDefinition.builder()
                        .value("bar")
                        .build())
                .build();
        SourceLocation source = new SourceLocation("/foo", 1, 1);
        ShapeId id = ShapeId.fromParts("ns.foo", "bar");
        StringShape startShape = StringShape.builder()
                .id(id)
                .addTrait(trait)
                .source(source)
                .build();
        Model model = Model.assembler().addShape(startShape).assemble().unwrap();

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ModelTransformer.create().changeShapeType(model, MapUtils.of(startShape.getId(), ShapeType.ENUM));
        });
    }

    @Test
    public void canConvertEnumToString() {
        SourceLocation source = new SourceLocation("/foo", 1, 1);
        ShapeId id = ShapeId.fromParts("ns.foo", "bar");
        Shape startShape = EnumShape.builder()
                .id(id)
                .addMember("FOO", "foo")
                .source(source)
                .build();

        Model model = Model.assembler()
                .addShape(startShape)
                .assemble()
                .unwrap();
        Model result = ModelTransformer.create().changeShapeType(model, MapUtils.of(id, ShapeType.STRING));

        assertThat(result.expectShape(id).getType(), Matchers.is(ShapeType.STRING));
        assertThat(result.expectShape(id).getSourceLocation(), Matchers.equalTo(source));
        assertTrue(result.expectShape(id).hasTrait(EnumTrait.ID));

        EnumTrait trait = result.expectShape(id).expectTrait(EnumTrait.class);
        assertFalse(trait instanceof SyntheticEnumTrait);

        assertThat(trait.getValues(),
                Matchers.equalTo(ListUtils.of(
                        EnumDefinition.builder()
                                .name("FOO")
                                .value("foo")
                                .build())));
    }

    @Test
    public void canFindEnumsToConvert() {
        EnumTrait compatibleTrait = EnumTrait.builder()
                .addEnum(EnumDefinition.builder()
                        .name("foo")
                        .value("bar")
                        .build())
                .build();
        ShapeId compatibleStringId = ShapeId.fromParts("ns.foo", "CompatibleString");
        StringShape compatibleString = StringShape.builder()
                .id(compatibleStringId)
                .addTrait(compatibleTrait)
                .build();

        EnumTrait incompatibleTrait = EnumTrait.builder()
                .addEnum(EnumDefinition.builder()
                        .value("bar")
                        .build())
                .build();
        ShapeId incompatibleStringId = ShapeId.fromParts("ns.foo", "IncompatibleString");
        StringShape incompatibleString = StringShape.builder()
                .id(incompatibleStringId)
                .addTrait(incompatibleTrait)
                .build();

        Model model = Model.assembler()
                .addShape(compatibleString)
                .addShape(incompatibleString)
                .assemble()
                .unwrap();
        Model result = ModelTransformer.create().changeStringEnumsToEnumShapes(model);

        assertThat(result.expectShape(compatibleStringId).getType(), Matchers.is(ShapeType.ENUM));
        assertThat(result.expectShape(compatibleStringId).members(), Matchers.hasSize(1));
        assertThat(result.expectShape(compatibleStringId).members().iterator().next(),
                Matchers.equalTo(MemberShape.builder()
                        .id(compatibleStringId.withMember("foo"))
                        .target(UnitTypeTrait.UNIT)
                        .addTrait(EnumValueTrait.builder().stringValue("bar").build())
                        .build()));

        assertThat(result.expectShape(incompatibleStringId).getType(), Matchers.is(ShapeType.STRING));
        assertThat(result.expectShape(incompatibleStringId).members(), Matchers.hasSize(0));
    }

    @Test
    public void canSynthesizeEnumNames() {
        EnumTrait trait = EnumTrait.builder()
                .addEnum(EnumDefinition.builder()
                        .value("foo:bar")
                        .build())
                .build();
        ShapeId shapeId = ShapeId.fromParts("ns.foo", "ConvertableShape");
        StringShape initialShape = StringShape.builder()
                .id(shapeId)
                .addTrait(trait)
                .build();

        Model model = Model.assembler()
                .addShape(initialShape)
                .assemble()
                .unwrap();
        Model result = ModelTransformer.create().changeStringEnumsToEnumShapes(model, true);

        assertThat(result.expectShape(shapeId).getType(), Matchers.is(ShapeType.ENUM));
        assertThat(result.expectShape(shapeId).members(), Matchers.hasSize(1));
        assertThat(result.expectShape(shapeId).members().iterator().next(),
                Matchers.equalTo(MemberShape.builder()
                        .id(shapeId.withMember("foo_bar"))
                        .target(UnitTypeTrait.UNIT)
                        .addTrait(EnumValueTrait.builder().stringValue("foo:bar").build())
                        .build()));
    }

    @Test
    public void canDowngradeEnums() {
        EnumShape.Builder stringEnumBuilder = (EnumShape.Builder) EnumShape.builder().id("ns.foo#Enum");
        EnumShape stringEnum = stringEnumBuilder.addMember("FOO", "foo").build();

        IntEnumShape.Builder intEnumBuilder = (IntEnumShape.Builder) IntEnumShape.builder().id("ns.foo#IntEnum");
        IntEnumShape intEnum = intEnumBuilder.addMember("FOO", 1).build();

        Model model = Model.assembler()
                .addShapes(stringEnum, intEnum)
                .assemble()
                .unwrap();
        Model result = ModelTransformer.create().downgradeEnums(model);

        assertThat(result.expectShape(stringEnum.getId()).getType(), Matchers.is(ShapeType.STRING));
        assertThat(result.expectShape(intEnum.getId()).getType(), Matchers.is(ShapeType.INTEGER));

        EnumTrait trait = result.expectShape(stringEnum.getId()).expectTrait(EnumTrait.class);
        assertFalse(trait instanceof SyntheticEnumTrait);
        assertThat(trait.getValues(),
                Matchers.equalTo(ListUtils.of(
                        EnumDefinition.builder()
                                .name("FOO")
                                .value("foo")
                                .build())));
    }
}
