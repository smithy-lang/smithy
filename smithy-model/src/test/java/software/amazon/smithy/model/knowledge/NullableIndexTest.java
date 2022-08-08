/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
                .addMember("c", ShapeId.from("smithy.api#PrimitiveBoolean"), b -> b.addTrait(new BoxTrait()))
                // Nullable because we don't know if this is a 1.0 or 2.0 model.
                .addMember("d", ShapeId.from("smithy.api#PrimitiveBoolean"))
                .addMember("e", ShapeId.from("smithy.api#Document"))
                .build();

        Model model = Model.assembler()
                .addShapes(denseList, sparseList)
                .addShapes(denseMap, sparseMap)
                .addShapes(denseSet, union, structure)
                .assemble()
                .unwrap();

        return Arrays.asList(new Object[][]{
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
                {model, structure.getMember("d").get().getId().toString(), true},
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
                .addMember("foo", str.getId(), b -> b.addTrait(new ClientOptionalTrait())
                        .addTrait(new DefaultTrait(Node.from("a")))
                        .build())
                .addMember("bar", str.getId(), b -> b.addTrait(new ClientOptionalTrait())
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
            Arguments.of(NullableIndex.CheckMode.SERVER, false, false, true, false, false)
        );
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
            Arguments.of(NullableIndex.CheckMode.SERVER, false, false, true)
        );
    }

    @Test
    public void worksWithV1NullabilityRulesForInteger() {
        // In Smithy v1, integer was non-nullable by default.
        IntegerShape integer = IntegerShape.builder()
                .id("smithy.example#Integer")
                .build();
        StructureShape struct = StructureShape.builder()
                .id("smithy.example#Struct")
                .addMember("foo", integer.getId())
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

        NullableIndex index = NullableIndex.of(model);

        for (MemberShape shape : model.getMemberShapes()) {
            if (shape.getId().getNamespace().equals("smithy.example")) {
                if (shape.getMemberName().startsWith("nullable")) {
                    assertThat(index.isMemberNullable(shape), is(true));
                    assertThat(index.isNullable(shape), is(true));
                } else if (shape.getMemberName().startsWith("nonNullable")) {
                    assertThat(index.isMemberNullable(shape), is(false));
                    assertThat(index.isNullable(shape), is(false));
                }
            }
        }
    }
}
