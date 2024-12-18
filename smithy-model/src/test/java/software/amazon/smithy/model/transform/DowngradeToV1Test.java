/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.IntEnumShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.AddedDefaultTrait;
import software.amazon.smithy.model.traits.BoxTrait;
import software.amazon.smithy.model.traits.ClientOptionalTrait;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.MixinTrait;
import software.amazon.smithy.model.traits.NotPropertyTrait;
import software.amazon.smithy.model.traits.PropertyTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;

public class DowngradeToV1Test {
    @Test
    public void convertsIntEnums() {
        IntEnumShape intEnumShape = IntEnumShape.builder()
                .id("smithy.example#IntEnum")
                .addMember("foo", 1)
                .build();
        Model model = Model.assembler()
                .addShape(intEnumShape)
                .assemble()
                .unwrap();

        Model downgraded = ModelTransformer.create().downgradeToV1(model);

        assertThat(downgraded.expectShape(intEnumShape.getId()).isIntEnumShape(), Matchers.is(false));
        assertThat(downgraded.expectShape(intEnumShape.getId()).isIntegerShape(), Matchers.is(true));
        assertThat(downgraded.expectShape(intEnumShape.getId()).getAllTraits().entrySet(), Matchers.empty());
    }

    @Test
    public void convertsEnumShapes() {
        EnumShape enumShape = EnumShape.builder()
                .id("smithy.example#Enum")
                .addMember("foo", "hello")
                .build();
        Model model = Model.assembler()
                .addShape(enumShape)
                .assemble()
                .unwrap();

        Model downgraded = ModelTransformer.create().downgradeToV1(model);

        assertThat(downgraded.expectShape(enumShape.getId()).isEnumShape(), Matchers.is(false));
        assertThat(downgraded.expectShape(enumShape.getId()).isStringShape(), Matchers.is(true));
        assertThat(downgraded.expectShape(enumShape.getId()).getAllTraits(), Matchers.hasKey(EnumTrait.ID));
    }

    @Test
    public void flattensMixins() {
        StringShape mixin = StringShape.builder()
                .id("smithy.example#Mixin")
                .addTrait(MixinTrait.builder().build())
                .addTrait(new SensitiveTrait())
                .build();
        StringShape concrete = StringShape.builder()
                .id("smithy.example#Concrete")
                .addMixin(mixin)
                .build();
        Model model = Model.assembler()
                .addShapes(mixin, concrete)
                .assemble()
                .unwrap();

        Model downgraded = ModelTransformer.create().downgradeToV1(model);

        assertThat(downgraded.expectShape(concrete.getId()).getMixins(), Matchers.empty());
        assertThat(downgraded.expectShape(concrete.getId()).getAllTraits(), Matchers.hasKey(SensitiveTrait.ID));
        assertThat(downgraded.getShape(mixin.getId()).isPresent(), Matchers.is(false));
    }

    @Test
    public void removesResourceProperties() {
        StructureShape input = StructureShape.builder()
                .id("smithy.example#Input")
                .addMember("foo", ShapeId.from("smithy.api#String"), b -> b.addTrait(PropertyTrait.builder().build()))
                .addMember("baz", ShapeId.from("smithy.api#String"), b -> b.addTrait(new NotPropertyTrait()))
                .build();
        OperationShape operation = OperationShape.builder()
                .id(ShapeId.from("smithy.example#Op"))
                .input(input)
                .build();
        ResourceShape resource = ResourceShape.builder()
                .id(ShapeId.from("smithy.example#Foo"))
                .addProperty("foo", "smithy.api#String")
                .addOperation(operation)
                .build();

        Model model = Model.assembler()
                .addShapes(input, operation, resource)
                .assemble()
                .unwrap();

        Model downgraded = ModelTransformer.create().downgradeToV1(model);

        assertThat(downgraded.expectShape(resource.getId(), ResourceShape.class).getProperties(),
                Matchers.anEmptyMap());

        assertThat(downgraded.expectShape(input.getMember("foo").get().getId()).hasTrait(PropertyTrait.class),
                Matchers.is(false));
        assertThat(downgraded.expectShape(input.getMember("baz").get().getId()).hasTrait(NotPropertyTrait.class),
                Matchers.is(false));
    }

    @Test
    public void removesUnnecessaryDefaults() {
        String stringModel = "$version: \"2.0\"\n"
                + "namespace smithy.example\n"
                // A Root level shape with a 1.0 non-zero value drops the default value and is boxed
                + "@default(10)\n"
                + "integer MyInteger\n"
                // A Root level shape with a 1.0 zero value keeps the default value
                + "@default(0)\n"
                + "integer ZeroInteger\n"
                // A Root level shape with no default value is boxed
                + "integer BoxedInteger\n"
                // Omitted Test Case: [ERROR] The @default trait can be set to null only on members
                + "// @default(null)\n"
                + "// integer ExplicitlyBoxedInteger\n"
                // StructureShape still exists
                + "structure Struct {\n"
                // A Member that targets a shape with a 1.0 non-zero value drops the default value and is not boxed
                + "  @default(10)\n"
                + "  foo: MyInteger\n"
                // Omitted Test Case: [ERROR] Member defines a default value that differs from the default value of the target shape
                + "  // @default(5)\n"
                + "  // fooFive: MyInteger\n"
                // A Member that targets a shape with a matching default 1.0 zero value keeps the default value
                + "  zeroTargetZeroMember: ZeroInteger = 0\n"
                // Omitted Test Case: [ERROR] Member defines a default value that differs from the default value of the target shape
                + "  // zeroTargetNonzeroMember: ZeroInteger = 1\n"
                // A Member that has a default value of null keeps the default value of null and is boxed
                + "  zeroTargetBoxedMember: ZeroInteger = null\n"
                // Omitted Test Case: [ERROR] Member defines a default value that differs from the default value of the target shape
                + "  // zeroTargetImplicitBoxedMember: ZeroInteger\n"
                // A Member that has a target shape with no default value drops the default value
                + "  boxedTargetZeroMember: BoxedInteger = 0\n"
                // A Member that has a target shape with no default value drops the default value
                + "  boxedTargetNonzeroMember: BoxedInteger = 1\n"
                // A Member that has a default value of null keeps the default value of null and is boxed
                + "  boxedTargetBoxedMember: BoxedInteger = null\n"
                // A Member that has no default value has no default trait and the member is not boxed
                + "  boxedTargetImplicitBoxedMember: BoxedInteger\n"
                // A Member that has a default value of null keeps the default value of null and is boxed
                + "  baz: PrimitiveInteger = null\n"
                // A Member with the addedDefault trait drops both the default and addedDefault trait
                + "  @default(\"hi\")\n"
                + "  @addedDefault\n"
                + "  bar: String\n"
                // A Member keeps the required trait and drops the clientOptional trait
                + "  @required\n"
                + "  @clientOptional\n"
                + "  bam: String\n"
                + "}";

        Model model = Model.assembler()
                .addUnparsedModel("example.smithy", stringModel)
                .assemble()
                .unwrap();

        Model downgraded = ModelTransformer.create().downgradeToV1(model);

        Model.assembler()
                .addModel(downgraded)
                .assemble()
                .unwrap();

        // A Root level shape with a 1.0 non-zero value drops the default value and is boxed
        ShapeId integerShape = ShapeId.from("smithy.example#MyInteger");
        assertThat(downgraded.expectShape(integerShape).hasTrait(DefaultTrait.class), Matchers.is(false));
        assertThat(downgraded.expectShape(integerShape).hasTrait(AddedDefaultTrait.class), Matchers.is(false));
        assertThat(downgraded.expectShape(integerShape).hasTrait(BoxTrait.class), Matchers.is(true));

        // A Root level shape with a 1.0 zero value keeps the default value
        ShapeId zeroIntegerShape = ShapeId.from("smithy.example#ZeroInteger");
        assertThat(downgraded.expectShape(zeroIntegerShape).hasTrait(DefaultTrait.class), Matchers.is(true));
        assertThat(downgraded.expectShape(zeroIntegerShape)
                .expectTrait(DefaultTrait.class)
                .toNode()
                .expectNumberNode()
                .getValue(),
                Matchers.is(0L));
        assertThat(downgraded.expectShape(zeroIntegerShape).hasTrait(AddedDefaultTrait.class), Matchers.is(false));
        assertThat(downgraded.expectShape(zeroIntegerShape).hasTrait(BoxTrait.class), Matchers.is(false));

        // A Root level shape with no default value is boxed
        ShapeId boxedIntegerShape = ShapeId.from("smithy.example#BoxedInteger");
        assertThat(downgraded.expectShape(boxedIntegerShape).hasTrait(DefaultTrait.class), Matchers.is(false));
        assertThat(downgraded.expectShape(boxedIntegerShape).hasTrait(AddedDefaultTrait.class), Matchers.is(false));
        assertThat(downgraded.expectShape(boxedIntegerShape).hasTrait(BoxTrait.class), Matchers.is(true));

        // StructureShape still exists
        StructureShape dStruct = downgraded.expectShape(ShapeId.from("smithy.example#Struct"), StructureShape.class);

        // A Member that targets a shape with a 1.0 non-zero value drops the default value and is not boxed
        assertThat(dStruct.getAllMembers().get("foo").hasTrait(DefaultTrait.class), Matchers.is(false));
        assertThat(dStruct.getAllMembers().get("foo").hasTrait(AddedDefaultTrait.class), Matchers.is(false));
        assertThat(dStruct.getAllMembers().get("foo").hasTrait(BoxTrait.class), Matchers.is(false));

        // A Member that targets a shape with a matching default 1.0 zero value keeps the default value
        assertThat(dStruct.getAllMembers().get("zeroTargetZeroMember").hasTrait(DefaultTrait.class), Matchers.is(true));
        assertThat(dStruct.getAllMembers()
                .get("zeroTargetZeroMember")
                .expectTrait(DefaultTrait.class)
                .toNode()
                .expectNumberNode()
                .getValue(),
                Matchers.is(0L));
        assertThat(dStruct.getAllMembers().get("zeroTargetZeroMember").hasTrait(AddedDefaultTrait.class),
                Matchers.is(false));
        assertThat(dStruct.getAllMembers().get("zeroTargetZeroMember").hasTrait(BoxTrait.class), Matchers.is(false));

        // A Member that has a default value of null keeps the default value of null and is boxed
        assertThat(dStruct.getAllMembers().get("zeroTargetBoxedMember").hasTrait(DefaultTrait.class),
                Matchers.is(true));
        assertThat(
                dStruct.getAllMembers()
                        .get("zeroTargetBoxedMember")
                        .expectTrait(DefaultTrait.class)
                        .toNode()
                        .isNullNode(),
                Matchers.is(true));
        assertThat(dStruct.getAllMembers().get("zeroTargetBoxedMember").hasTrait(AddedDefaultTrait.class),
                Matchers.is(false));
        assertThat(dStruct.getAllMembers().get("zeroTargetBoxedMember").hasTrait(BoxTrait.class), Matchers.is(true));

        // A Member that has a target shape with no default value drops the default value
        assertThat(dStruct.getAllMembers().get("boxedTargetZeroMember").hasTrait(DefaultTrait.class),
                Matchers.is(false));
        assertThat(dStruct.getAllMembers().get("boxedTargetZeroMember").hasTrait(AddedDefaultTrait.class),
                Matchers.is(false));
        assertThat(dStruct.getAllMembers().get("boxedTargetZeroMember").hasTrait(BoxTrait.class), Matchers.is(false));

        // A Member that has a target shape with no default value drops the default value
        assertThat(dStruct.getAllMembers().get("boxedTargetNonzeroMember").hasTrait(DefaultTrait.class),
                Matchers.is(false));
        assertThat(dStruct.getAllMembers().get("boxedTargetNonzeroMember").hasTrait(AddedDefaultTrait.class),
                Matchers.is(false));
        assertThat(dStruct.getAllMembers().get("boxedTargetNonzeroMember").hasTrait(BoxTrait.class),
                Matchers.is(false));

        // A Member that has a default value of null keeps the default value of null and is boxed
        assertThat(dStruct.getAllMembers().get("boxedTargetBoxedMember").hasTrait(DefaultTrait.class),
                Matchers.is(true));
        assertThat(
                dStruct.getAllMembers()
                        .get("boxedTargetBoxedMember")
                        .expectTrait(DefaultTrait.class)
                        .toNode()
                        .isNullNode(),
                Matchers.is(true));
        assertThat(dStruct.getAllMembers().get("boxedTargetBoxedMember").hasTrait(AddedDefaultTrait.class),
                Matchers.is(false));
        assertThat(dStruct.getAllMembers().get("boxedTargetBoxedMember").hasTrait(BoxTrait.class), Matchers.is(true));

        // A Member that has no default value has no default trait and the member is not boxed
        assertThat(dStruct.getAllMembers().get("boxedTargetImplicitBoxedMember").hasTrait(DefaultTrait.class),
                Matchers.is(false));
        assertThat(dStruct.getAllMembers().get("boxedTargetImplicitBoxedMember").hasTrait(AddedDefaultTrait.class),
                Matchers.is(false));
        assertThat(dStruct.getAllMembers().get("boxedTargetImplicitBoxedMember").hasTrait(BoxTrait.class),
                Matchers.is(false));

        // A Member that has a default value of null keeps the default value of null and is boxed
        assertThat(dStruct.getAllMembers().get("baz").hasTrait(DefaultTrait.class), Matchers.is(true));
        assertThat(dStruct.getAllMembers().get("baz").expectTrait(DefaultTrait.class).toNode().isNullNode(),
                Matchers.is(true));
        assertThat(dStruct.getAllMembers().get("baz").hasTrait(AddedDefaultTrait.class), Matchers.is(false));
        assertThat(dStruct.getAllMembers().get("baz").hasTrait(BoxTrait.class), Matchers.is(true));

        // A Member with the addedDefault trait drops both the default and addedDefault trait
        assertThat(dStruct.getAllMembers().get("bar").hasTrait(DefaultTrait.class), Matchers.is(false));
        assertThat(dStruct.getAllMembers().get("bar").hasTrait(AddedDefaultTrait.class), Matchers.is(false));

        // A Member keeps the required trait and drops the clientOptional trait
        assertThat(dStruct.getAllMembers().get("bam").hasTrait(DefaultTrait.class), Matchers.is(false));
        assertThat(dStruct.getAllMembers().get("bam").hasTrait(AddedDefaultTrait.class), Matchers.is(false));
        assertThat(dStruct.getAllMembers().get("bam").hasTrait(RequiredTrait.class), Matchers.is(true));
        assertThat(dStruct.getAllMembers().get("bam").hasTrait(ClientOptionalTrait.class), Matchers.is(false));
    }
}
