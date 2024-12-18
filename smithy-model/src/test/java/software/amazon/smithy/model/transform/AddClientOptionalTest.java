/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.ClientOptionalTrait;
import software.amazon.smithy.model.traits.InputTrait;
import software.amazon.smithy.model.traits.RequiredTrait;

public class AddClientOptionalTest {
    @Test
    public void addsClientOptionalToInputStructures() {
        StructureShape structure = StructureShape.builder()
                .id("smithy.example#Struct")
                .addTrait(new InputTrait())
                .addMember("foo", ShapeId.from("smithy.api#String"))
                .addMember("bar", ShapeId.from("smithy.api#String"), bar -> bar.addTrait(new RequiredTrait()))
                .build();
        Model model = Model.assembler().addShape(structure).assemble().unwrap();
        Model model2 = ModelTransformer.create().addClientOptional(model, false);
        StructureShape struct2 = model2.expectShape(structure.getId(), StructureShape.class);

        assertTrue(struct2.getMember("foo").get().hasTrait(ClientOptionalTrait.class));
        assertTrue(struct2.getMember("bar").get().hasTrait(ClientOptionalTrait.class));
    }

    @Test
    public void addsClientOptionalToImplicitNulls() {
        StructureShape structure = StructureShape.builder()
                .id("smithy.example#Struct")
                .addMember("foo", ShapeId.from("smithy.api#String"))
                .addMember("bar", ShapeId.from("smithy.api#String"), bar -> bar.addTrait(new RequiredTrait()))
                .build();
        Model model = Model.assembler().addShape(structure).assemble().unwrap();
        Model model2 = ModelTransformer.create().addClientOptional(model, false);
        StructureShape struct2 = model2.expectShape(structure.getId(), StructureShape.class);

        assertTrue(struct2.getMember("foo").get().hasTrait(ClientOptionalTrait.class));
        assertFalse(struct2.getMember("bar").get().hasTrait(ClientOptionalTrait.class));
    }

    @Test
    public void addsClientOptionalWhenNoZeroValue() {
        StructureShape other = StructureShape.builder().id("smithy.example#Other").build();
        StructureShape structure = StructureShape.builder()
                .id("smithy.example#Struct")
                .addMember("foo", ShapeId.from("smithy.api#String"))
                .addMember("bar", ShapeId.from("smithy.api#String"), bar -> bar.addTrait(new RequiredTrait()))
                .addMember("baz", other.getId(), bar -> bar.addTrait(new RequiredTrait()))
                .build();
        Model model = Model.assembler().addShapes(structure, other).assemble().unwrap();
        Model model2 = ModelTransformer.create().addClientOptional(model, true);
        StructureShape struct2 = model2.expectShape(structure.getId(), StructureShape.class);

        assertTrue(struct2.getMember("foo").get().hasTrait(ClientOptionalTrait.class));
        assertFalse(struct2.getMember("bar").get().hasTrait(ClientOptionalTrait.class));
        assertTrue(struct2.getMember("baz").get().hasTrait(ClientOptionalTrait.class));
    }
}
