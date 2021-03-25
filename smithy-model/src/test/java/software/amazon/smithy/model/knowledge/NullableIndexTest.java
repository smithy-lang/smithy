/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.Arrays;
import java.util.Collection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.SetShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.BoxTrait;
import software.amazon.smithy.model.traits.SparseTrait;

public class NullableIndexTest {

    @ParameterizedTest
    @MethodSource("data")
    public void checksIfBoxed(Model model, String shapeId, boolean isBoxed) {
        NullableIndex index = NullableIndex.of(model);
        ShapeId targetId = ShapeId.from(shapeId);
        boolean actual = index.isNullable(targetId);

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
                // Not nullable.
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

                {model, "smithy.api#PrimitiveByte", false},
                {model, "smithy.api#PrimitiveShort", false},
                {model, "smithy.api#PrimitiveInteger", false},
                {model, "smithy.api#PrimitiveLong", false},
                {model, "smithy.api#PrimitiveFloat", false},
                {model, "smithy.api#PrimitiveDouble", false},
                {model, "smithy.api#PrimitiveBoolean", false},

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
}
