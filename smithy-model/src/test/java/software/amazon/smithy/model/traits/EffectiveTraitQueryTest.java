/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StringShape;

public class EffectiveTraitQueryTest {
    @Test
    public void detectsTraitOnShape() {
        Shape stringShape = StringShape.builder()
                .id("foo.bar#Baz")
                .addTrait(new SensitiveTrait())
                .build();
        Model model = Model.builder()
                .addShapes(stringShape)
                .build();
        EffectiveTraitQuery query = EffectiveTraitQuery.builder()
                .model(model)
                .traitClass(SensitiveTrait.class)
                .build();

        assertTrue(query.isTraitApplied(stringShape));
    }

    @Test
    public void detectsTraitOnMemberTarget() {
        Shape stringShape = StringShape.builder()
                .id("foo.bar#Baz")
                .addTrait(new SensitiveTrait())
                .build();
        ListShape list = ListShape.builder().id("foo.bar#List").member(stringShape.getId()).build();
        Model model = Model.builder()
                .addShapes(stringShape, list)
                .build();
        EffectiveTraitQuery query = EffectiveTraitQuery.builder()
                .model(model)
                .traitClass(SensitiveTrait.class)
                .build();

        assertTrue(query.isTraitApplied(list.getMember()));
    }

    @Test
    public void ignoresTraitOnMemberContainerByDefault() {
        Shape stringShape = StringShape.builder().id("foo.bar#Baz").build();
        MemberShape member = MemberShape.builder()
                .id("foo.baz#Container$member")
                .target(stringShape)
                .build();
        ListShape list = ListShape.builder()
                .id("foo.baz#Container")
                .member(member)
                .addTrait(new SensitiveTrait())
                .build();
        Model model = Model.builder()
                .addShapes(stringShape, member, list)
                .build();
        EffectiveTraitQuery query = EffectiveTraitQuery.builder()
                .model(model)
                .traitClass(SensitiveTrait.class)
                .build();

        assertFalse(query.isTraitApplied(member));
    }

    @Test
    public void detectsTraitOnMemberContainer() {
        Shape stringShape = StringShape.builder().id("foo.bar#Baz").build();
        MemberShape member = MemberShape.builder()
                .id("foo.baz#Container$member")
                .target(stringShape)
                .build();
        ListShape list = ListShape.builder()
                .id("foo.baz#Container")
                .member(member)
                .addTrait(new SensitiveTrait())
                .build();
        Model model = Model.builder()
                .addShapes(stringShape, member, list)
                .build();
        EffectiveTraitQuery query = EffectiveTraitQuery.builder()
                .model(model)
                .traitClass(SensitiveTrait.class)
                .inheritFromContainer(true)
                .build();

        assertTrue(query.isTraitApplied(member));

        // Converts to a builder...
        assertTrue(query.toBuilder().build().isTraitApplied(member));
    }
}
