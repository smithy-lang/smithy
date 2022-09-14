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
                + "@default(10)\n"
                + "integer MyInteger\n"
                + "structure Struct {\n"
                + "  @default(10)\n"
                + "  foo: MyInteger\n"
                + "  baz: PrimitiveInteger = null\n"
                + "  @default(\"hi\")\n"
                + "  @addedDefault\n"
                + "  bar: String\n"
                + "  @required\n"
                + "  @clientOptional\n"
                + "  bam: String\n"
                + "}";

        Model model = Model.assembler()
                .addUnparsedModel("example.smithy", stringModel)
                .assemble()
                .unwrap();

        Model downgraded = ModelTransformer.create().downgradeToV1(model);

        ShapeId integerShape = ShapeId.from("smithy.example#MyInteger");
        assertThat(downgraded.expectShape(integerShape).hasTrait(DefaultTrait.class), Matchers.is(false));
        assertThat(downgraded.expectShape(integerShape).hasTrait(BoxTrait.class), Matchers.is(true));

        StructureShape dStruct = downgraded.expectShape(ShapeId.from("smithy.example#Struct"), StructureShape.class);
        assertThat(dStruct.getAllMembers().get("foo").hasTrait(DefaultTrait.class), Matchers.is(false));

        assertThat(dStruct.getAllMembers().get("baz").hasTrait(DefaultTrait.class), Matchers.is(true));
        assertThat(dStruct.getAllMembers().get("baz").hasTrait(BoxTrait.class), Matchers.is(true));

        assertThat(dStruct.getAllMembers().get("bar").hasTrait(DefaultTrait.class), Matchers.is(false));
        assertThat(dStruct.getAllMembers().get("bar").hasTrait(AddedDefaultTrait.class), Matchers.is(false));

        assertThat(dStruct.getAllMembers().get("bam").hasTrait(RequiredTrait.class), Matchers.is(true));
        assertThat(dStruct.getAllMembers().get("bam").hasTrait(ClientOptionalTrait.class), Matchers.is(false));
    }
}
