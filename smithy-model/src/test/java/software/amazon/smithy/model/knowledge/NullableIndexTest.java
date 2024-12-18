/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.knowledge;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.IntegerShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.shapes.SetShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.BoxTrait;
import software.amazon.smithy.model.traits.ClientOptionalTrait;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.InputTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.SparseTrait;

public class NullableIndexTest {

    @ParameterizedTest
    @MethodSource("data")
    public void checksIfBoxed(Model model, String shapeId, boolean isBoxed) {
        NullableIndex index = NullableIndex.of(model);
        ShapeId targetId = ShapeId.from(shapeId);
        Shape shape = model.expectShape(targetId);

        boolean actual;

        if (shape.isMemberShape()) {
            // Test member shapes using 2.0 semantics.
            MemberShape member = shape.asMemberShape().get();
            actual = index.isMemberNullable(member);
        } else {
            // Test root shapes using 1.0 semantics.
            actual = index.isNullable(targetId);
        }

        if (isBoxed != actual) {
            if (isBoxed) {
                Assertions.fail("Expected shape to be nullable but it was not: " + targetId);
            } else {
                Assertions.fail("Did not expect shape to be nullable but it was: " + targetId);
            }
        }
    }

    public static Collection<Object[]> data() {
        ListShape denseList = ListShape.builder()
                .id("smithy.test#DenseList")
                .member(ShapeId.from("smithy.api#Boolean"))
                .build();
        ListShape sparseList = ListShape.builder()
                .id("smithy.test#SparseList")
                .member(ShapeId.from("smithy.api#Boolean"))
                .addTrait(new SparseTrait())
                .build();

        MapShape denseMap = MapShape.builder()
                .id("smithy.test#DenseMap")
                .key(ShapeId.from("smithy.api#String"))
                .value(ShapeId.from("smithy.api#String"))
                .build();
        MapShape sparseMap = MapShape.builder()
                .id("smithy.test#SparseMap")
                .key(ShapeId.from("smithy.api#String"))
                .value(ShapeId.from("smithy.api#String"))
                .addTrait(new SparseTrait())
                .build();

        SetShape denseSet = SetShape.builder()
                .id("smithy.test#DenseSet")
                .member(ShapeId.from("smithy.api#String"))
                .build();

        UnionShape union = UnionShape.builder()
                .id("smithy.test#Union")
                .addMember("a", ShapeId.from("smithy.api#String"))
                .build();

        StructureShape structure = StructureShape.builder()
                .id("smithy.test#Struct")
                // Nullable
                .addMember("a", ShapeId.from("smithy.api#String"))
                // Nullable because the target is boxed
                .addMember("b", ShapeId.from("smithy.api#Boolean"))
                // Nullable because the member is boxed
                .addMember("c",
                        ShapeId.from("smithy.api#PrimitiveBoolean"),
                        b -> b.addTrait(new DefaultTrait(Node.nullNode())))
                // Non-nullable due to the default trait with a zero value.
                .addMember("d",
                        ShapeId.from("smithy.api#PrimitiveBoolean"),
                        b -> b.addTrait(new DefaultTrait(Node.from(false))))
                .addMember("e", ShapeId.from("smithy.api#Document"))
                .build();

        Model model = Model.assembler()
                .addShapes(denseList, sparseList)
                .addShapes(denseMap, sparseMap)
                .addShapes(denseSet, union, structure)
                .assemble()
                .unwrap();

        return Arrays.asList(new Object[][] {
                {model, "smithy.api#String", true},
                {model, "smithy.api#Blob", true},
                {model, "smithy.api#Boolean", true},
                {model, "smithy.api#Timestamp", true},
                {model, "smithy.api#Byte", true},
                {model, "smithy.api#Short", true},
                {model, "smithy.api#Integer", true},
                {model, "smithy.api#Long", true},
                {model, "smithy.api#Float", true},
                {model, "smithy.api#Double", true},
                {model, "smithy.api#BigInteger", true},
                {model, "smithy.api#BigDecimal", true},

                // Dense lists are nullable but their members are not.
                {model, denseList.getId().toString(), true},
                {model, denseList.getMember().getId().toString(), false},

                // Sparse lists are nullable and so are their members.
                {model, sparseList.getId().toString(), true},
                {model, sparseList.getMember().getId().toString(), true},

                // Dense maps are nullable but their value members are not.
                {model, denseMap.getId().toString(), true},
                {model, denseMap.getValue().getId().toString(), false},
                {model, denseMap.getKey().getId().toString(), false},

                // Sparse maps are nullable and so are their value members.
                {model, sparseMap.getId().toString(), true},
                {model, sparseMap.getValue().getId().toString(), true},
                {model, sparseMap.getKey().getId().toString(), false},

                // Unions are nullable, but their members never are.
                {model, union.getId().toString(), true},
                {model, union.getMember("a").get().getId().toString(), false},

                // Structures are nullable, but their members are conditionally nullable.
                {model, structure.getId().toString(), true},
                {model, structure.getMember("a").get().getId().toString(), true},
                {model, structure.getMember("b").get().getId().toString(), true},
                {model, structure.getMember("c").get().getId().toString(), true},
                {model, structure.getMember("d").get().getId().toString(), false},
                // documents are nullable as structure members
                {model, structure.getMember("e").get().getId().toString(), true},
        });
    }

    @ParameterizedTest
    @MethodSource("nullableTraitTests")
    public void takesNullableIntoAccount(
            NullableIndex.CheckMode mode,
            boolean foo,
            boolean bar,
            boolean baz,
            boolean bam,
            boolean boo
    ) {
        StringShape str = StringShape.builder().id("smithy.example#Str").build();
        StructureShape struct = StructureShape.builder()
                .id("smithy.example#Struct")
                // This member is technically invalid, but clientOptional takes precedent here
                // over the default trait.
                .addMember("foo",
                        str.getId(),
                        b -> b.addTrait(new ClientOptionalTrait())
                                .addTrait(new DefaultTrait(Node.from("a")))
                                .build())
                .addMember("bar",
                        str.getId(),
                        b -> b.addTrait(new ClientOptionalTrait())
                                .addTrait(new RequiredTrait())
                                .build())
                .addMember("baz", str.getId(), b -> b.addTrait(new ClientOptionalTrait()).build())
                .addMember("bam", str.getId(), b -> b.addTrait(new RequiredTrait()).build())
                .addMember("boo", str.getId(), b -> b.addTrait(new DefaultTrait(Node.from("boo"))).build())
                .build();

        Model model = Model.builder().addShapes(str, struct).build();
        NullableIndex nullableIndex = NullableIndex.of(model);

        assertThat(nullableIndex.isMemberNullable(struct.getMember("foo").get(), mode), is(foo));
        assertThat(nullableIndex.isMemberNullable(struct.getMember("bar").get(), mode), is(bar));
        assertThat(nullableIndex.isMemberNullable(struct.getMember("baz").get(), mode), is(baz));
        assertThat(nullableIndex.isMemberNullable(struct.getMember("bam").get(), mode), is(bam));
        assertThat(nullableIndex.isMemberNullable(struct.getMember("boo").get(), mode), is(boo));
    }

    public static Stream<Arguments> nullableTraitTests() {
        return Stream.of(
                Arguments.of(NullableIndex.CheckMode.CLIENT, true, true, true, false, false),
                Arguments.of(NullableIndex.CheckMode.SERVER, false, false, true, false, false));
    }

    @ParameterizedTest
    @MethodSource("inputTraitTests")
    public void takesInputTraitIntoAccount(NullableIndex.CheckMode mode, boolean foo, boolean bar, boolean baz) {
        StringShape str = StringShape.builder().id("smithy.example#Str").build();
        StructureShape struct = StructureShape.builder()
                .id("smithy.example#Struct")
                .addTrait(new InputTrait())
                .addMember("foo", str.getId(), b -> b.addTrait(new DefaultTrait(Node.from("foo"))).build())
                .addMember("bar", str.getId(), b -> b.addTrait(new RequiredTrait()).build())
                .addMember("baz", str.getId())
                .build();

        Model model = Model.builder().addShapes(str, struct).build();
        NullableIndex nullableIndex = NullableIndex.of(model);

        assertThat(nullableIndex.isMemberNullable(struct.getMember("foo").get(), mode), is(foo));
        assertThat(nullableIndex.isMemberNullable(struct.getMember("bar").get(), mode), is(bar));
        assertThat(nullableIndex.isMemberNullable(struct.getMember("baz").get(), mode), is(baz));
    }

    public static Stream<Arguments> inputTraitTests() {
        return Stream.of(
                Arguments.of(NullableIndex.CheckMode.CLIENT, true, true, true),
                Arguments.of(NullableIndex.CheckMode.SERVER, false, false, true));
    }

    @Test
    public void worksWithV2NullabilityRulesForInteger() {
        // 2.0 nullability rules are assumed. Using a model assembler with a 1.0 model will ensure that 1.0
        // semantics are used in the NullableIndex.
        IntegerShape integer = IntegerShape.builder()
                .id("smithy.example#Integer")
                .build();
        StructureShape struct = StructureShape.builder()
                .id("smithy.example#Struct")
                .addMember("foo", integer.getId())
                .build();
        Model model = Model.builder().addShapes(integer, struct).build();
        NullableIndex index = NullableIndex.of(model);

        assertThat(index.isNullable(struct.getMember("foo").get()), is(true));
    }

    @Test
    public void settingDefaultIsNoticedByNullableIndexToo() {
        IntegerShape integer = IntegerShape.builder()
                .id("smithy.example#Integer")
                .build();
        StructureShape struct = StructureShape.builder()
                .id("smithy.example#Struct")
                .addMember("foo", integer.getId(), b -> b.addTrait(new DefaultTrait(Node.from(0))))
                .build();
        Model model = Model.builder().addShapes(integer, struct).build();
        NullableIndex index = NullableIndex.of(model);

        assertThat(index.isNullable(struct.getMember("foo").get()), is(false));
    }

    @Test
    public void worksWithV1NullabilityRulesForString() {
        StringShape string = StringShape.builder()
                .id("smithy.example#String")
                .build();
        StructureShape struct = StructureShape.builder()
                .id("smithy.example#Struct")
                .addMember("foo", string.getId())
                .build();
        Model model = Model.builder().addShapes(string, struct).build();
        NullableIndex index = NullableIndex.of(model);

        assertThat(index.isNullable(struct.getMember("foo").get()), is(true));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void worksWithV1NullabilityRulesForBoxedMember() {
        IntegerShape integer = IntegerShape.builder()
                .id("smithy.example#Integer")
                .build();
        StructureShape struct = StructureShape.builder()
                .id("smithy.example#Struct")
                .addMember("foo", integer.getId(), b -> b.addTrait(new BoxTrait()))
                .build();
        Model model = Model.builder().addShapes(integer, struct).build();
        NullableIndex index = NullableIndex.of(model);

        assertThat(index.isNullable(struct.getMember("foo").get()), is(true));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void worksWithV1NullabilityRulesForBoxedTarget() {
        IntegerShape integer = IntegerShape.builder()
                .id("smithy.example#Integer")
                .addTrait(new BoxTrait())
                .build();
        StructureShape struct = StructureShape.builder()
                .id("smithy.example#Struct")
                .addMember("foo", integer.getId())
                .build();
        Model model = Model.builder().addShapes(integer, struct).build();
        NullableIndex index = NullableIndex.of(model);

        assertThat(index.isNullable(struct.getMember("foo").get()), is(true));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void worksWithV1NullabilityRulesIgnoringRequired() {
        IntegerShape integer = IntegerShape.builder()
                .id("smithy.example#Integer")
                .addTrait(new BoxTrait())
                .build();
        StructureShape struct = StructureShape.builder()
                .id("smithy.example#Struct")
                // The required trait isn't used in v1 to determine nullability.
                .addMember("foo", integer.getId(), b -> b.addTrait(new RequiredTrait()))
                .build();
        Model model = Model.builder().addShapes(integer, struct).build();
        NullableIndex index = NullableIndex.of(model);

        assertThat(index.isNullable(struct.getMember("foo").get()), is(true));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void worksWithV1NullabilityRulesForDenseMaps() {
        MapShape map = MapShape.builder()
                .id("smithy.example#Map")
                .key(ShapeId.from("smithy.api#String"))
                .value(ShapeId.from("smithy.api#String"))
                .build();
        Model model = Model.assembler().addShapes(map).assemble().unwrap();
        NullableIndex index = NullableIndex.of(model);

        assertThat(index.isNullable(map), is(true));
        assertThat(index.isNullable(map.getKey()), is(false));
        assertThat(index.isNullable(map.getValue()), is(false));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void worksWithV1NullabilityRulesForSparseMaps() {
        MapShape map = MapShape.builder()
                .id("smithy.example#Map")
                .key(ShapeId.from("smithy.api#String"))
                .value(ShapeId.from("smithy.api#String"))
                .addTrait(new SparseTrait())
                .build();
        Model model = Model.assembler().addShapes(map).assemble().unwrap();
        NullableIndex index = NullableIndex.of(model);

        assertThat(index.isNullable(map), is(true));
        assertThat(index.isNullable(map.getKey()), is(false));
        assertThat(index.isNullable(map.getValue()), is(true));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void worksWithV1NullabilityRulesForDenseLists() {
        ListShape list = ListShape.builder()
                .id("smithy.example#Map")
                .member(ShapeId.from("smithy.api#String"))
                .build();
        Model model = Model.assembler().addShapes(list).assemble().unwrap();
        NullableIndex index = NullableIndex.of(model);

        assertThat(index.isNullable(list), is(true));
        assertThat(index.isNullable(list.getMember()), is(false));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void worksWithV1NullabilityRulesForSparseLists() {
        ListShape list = ListShape.builder()
                .id("smithy.example#Map")
                .member(ShapeId.from("smithy.api#String"))
                .addTrait(new SparseTrait())
                .build();
        Model model = Model.assembler().addShapes(list).assemble().unwrap();
        NullableIndex index = NullableIndex.of(model);

        assertThat(index.isNullable(list), is(true));
        assertThat(index.isNullable(list.getMember()), is(true));
    }

    @Test
    public void carefulModeTreatsStructureAndUnionAsOptional() {
        StructureShape struct = StructureShape.builder()
                .id("smithy.example#Struct")
                .build();
        UnionShape union = UnionShape.builder()
                .id("smithy.example#Union")
                .addMember("a", ShapeId.from("smithy.api#String"))
                .build();
        StructureShape outer = StructureShape.builder()
                .id("smithy.example#Outer")
                .addMember("a", struct.getId(), m -> m.addTrait(new RequiredTrait()))
                .addMember("b", union.getId(), m -> m.addTrait(new RequiredTrait()))
                .build();
        Model model = Model.assembler().addShapes(struct, union, outer).assemble().unwrap();
        NullableIndex index = NullableIndex.of(model);

        assertThat(index.isMemberNullable(outer.getMember("a").get(), NullableIndex.CheckMode.CLIENT_CAREFUL),
                is(true));
        assertThat(index.isMemberNullable(outer.getMember("b").get(), NullableIndex.CheckMode.CLIENT_CAREFUL),
                is(true));
    }

    @Test
    public void correctlyDeterminesNullabilityOfUpgradedV1Models() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("nullable-index-v1.smithy"))
                .assemble()
                .unwrap();

        // Re-serialize to test with a 2.0 model.
        Model model2 = Model.assembler()
                .addDocumentNode(ModelSerializer.builder().build().serialize(model))
                .assemble()
                .unwrap();

        correctlyDeterminesNullabilityOfUpgradedV1ModelsAssertions(model);
        correctlyDeterminesNullabilityOfUpgradedV1ModelsAssertions(model2);
    }

    private void correctlyDeterminesNullabilityOfUpgradedV1ModelsAssertions(Model model) {
        NullableIndex index = NullableIndex.of(model);

        for (MemberShape shape : model.getMemberShapes()) {
            if (shape.getId().getNamespace().equals("smithy.example") && shape.getId().getName().equals("Foo")) {
                if (shape.getMemberName().startsWith("nullable")) {
                    assertThat(shape + " is member nullable", index.isMemberNullable(shape), is(true));
                    assertThat(shape + " is nullable", index.isNullable(shape), is(true));
                } else if (shape.getMemberName().startsWith("nonNullable")) {
                    assertThat(shape + " member not nullable", index.isMemberNullable(shape), is(false));
                    assertThat(shape + " not nullable", index.isNullable(shape), is(false));
                }
            }
        }
    }

    @Test
    public void requiresIsNotInherentlyNonNullIn1_0() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("nullable-index-v1.smithy"))
                .assemble()
                .unwrap();

        // Re-serialize to test with a 2.0 model.
        Model model2 = Model.assembler()
                .addDocumentNode(ModelSerializer.builder().build().serialize(model))
                .assemble()
                .unwrap();

        requiresIsNotInherentlyNonNullIn1_0_Assertions(model);
        requiresIsNotInherentlyNonNullIn1_0_Assertions(model2);
    }

    private void requiresIsNotInherentlyNonNullIn1_0_Assertions(Model model) {
        NullableIndex index = NullableIndex.of(model);

        // V1 and V2 rules diverge for this member.
        assertThat(index.isNullable(ShapeId.from("smithy.example#Baz$bar")), is(true));
        assertThat(index.isNullable(ShapeId.from("smithy.example#Baz$bam")), is(false));

        assertThat(index.isMemberNullable(model.expectShape(ShapeId.from("smithy.example#Baz$bar"), MemberShape.class)),
                is(false));
        assertThat(index.isMemberNullable(model.expectShape(ShapeId.from("smithy.example#Baz$bam"), MemberShape.class)),
                is(false));
    }

    @Test
    public void addedDefaultMakesMemberNullableInV1NotV2() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("nullable-index-added-default.smithy"))
                .assemble()
                .unwrap();

        // Re-serialize to test with a 2.0 model.
        Model model2 = Model.assembler()
                .addDocumentNode(ModelSerializer.builder().build().serialize(model))
                .assemble()
                .unwrap();

        addedDefaultMakesMemberNullableInV1NotV2Assertions(model);
        addedDefaultMakesMemberNullableInV1NotV2Assertions(model2);
    }

    private void addedDefaultMakesMemberNullableInV1NotV2Assertions(Model model) {
        NullableIndex index = NullableIndex.of(model);

        // In 1.0 based semantics, the required trait is ignored. Because of this, if the
        // default trait is added to a member that wasn't required before, then v1 tools needs to
        // know because it needs to not treat the shape as non-null as that would transistion the
        // shape from nullable to non-nullable.
        assertThat(index.isNullable(ShapeId.from("smithy.example#Foo$baz")), is(true));

        // In v2 based semantics and tools, the addedDefault trait is ignored.
        assertThat(index.isMemberNullable(model.expectShape(ShapeId.from("smithy.example#Foo$baz"), MemberShape.class)),
                is(false));
    }

    // The required trait makes this non-nullable. The default(null) trait just means that there's no default value,
    // it doesn't make the member nullable. A value is still required to be present. Therefore, it's non-nullable
    // in code generated types. "default(null)" just means the framework does not provide a default value.
    @Test
    public void requiredMembersAreNonNullableEvenIfDefaultNullTraitIsPresent() {
        String modelText =
                "$version: \"2.0\"\n"
                        + "namespace smithy.example\n"
                        + "structure Foo {\n"
                        + "    @required\n"
                        + "    baz: Integer = null\n"
                        + "}\n";
        Model model = Model.assembler()
                .addUnparsedModel("foo.smithy", modelText)
                .assemble()
                .unwrap();

        NullableIndex index = NullableIndex.of(model);

        assertThat(
                index.isMemberNullable(model.expectShape(ShapeId.from("smithy.example#Foo$baz"), MemberShape.class)),
                is(false));
    }
}
