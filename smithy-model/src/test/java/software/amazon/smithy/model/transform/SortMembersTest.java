/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Comparator;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;

public class SortMembersTest {
    @Test
    public void sortsModelMembers() {
        ModelAssembler assembler = Model.assembler();
        UnionShape u = UnionShape.builder()
                .id("com.foo#U")
                .addMember("zoo", ShapeId.from("smithy.api#String"))
                .addMember("abc", ShapeId.from("smithy.api#String"))
                .build();
        StructureShape s = StructureShape.builder()
                .id("com.foo#S")
                .addMember("zoo", ShapeId.from("smithy.api#String"))
                .addMember("abc", ShapeId.from("smithy.api#String"))
                .build();
        assembler.addShapes(u, s);
        Model model = assembler.assemble().unwrap();
        ModelTransformer transformer = ModelTransformer.create();
        Model sortedModel = transformer.sortMembers(model, Comparator.comparing(MemberShape::getMemberName));

        // Members use given order by default.
        assertThat(u.getMemberNames(), Matchers.contains("zoo", "abc"));
        assertThat(s.getMemberNames(), Matchers.contains("zoo", "abc"));

        // Members in the new model use the sorted order.
        assertThat(sortedModel.expectShape(u.getId(), UnionShape.class).getMemberNames(),
                Matchers.contains("abc", "zoo"));
        assertThat(sortedModel.expectShape(s.getId(), StructureShape.class).getMemberNames(),
                Matchers.contains("abc", "zoo"));
    }
}
