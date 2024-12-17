/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidatedResultException;

public class PropertyBindingIndexTest {

    @Test
    public void testIndex() {
        ValidatedResult<Model> vrmodel = Model.assembler()
                .addImport(OperationIndexTest.class.getResource("member-property-index.smithy"))
                .assemble();
        Model model = vrmodel.getResult().get();

        PropertyBindingIndex index = PropertyBindingIndex.of(model);
        assertFalse(index.doesMemberShapeRequireProperty(model.expectShape(
                ShapeId.from("com.example#ResourceStructure_1$token"),
                MemberShape.class)));
        assertFalse(index.doesMemberShapeRequireProperty(model.expectShape(
                ShapeId.from("com.example#ResourceStructure_1$id"),
                MemberShape.class)));
        assertFalse(index.doesMemberShapeRequireProperty(model.expectShape(
                ShapeId.from("com.example#ResourceStructure_1$spurious"),
                MemberShape.class)));
        assertFalse(index.doesMemberShapeRequireProperty(model.expectShape(
                ShapeId.from("com.example#ResourceStructure_2$nested"),
                MemberShape.class)));
        assertTrue(index.doesMemberShapeRequireProperty(model.expectShape(
                ShapeId.from("com.example#ResourceStructure_1$property"),
                MemberShape.class)));

        assertTrue(index.isMemberShapeProperty(model.expectShape(
                ShapeId.from("com.example#ResourceStructure_1$property"),
                MemberShape.class)));
        assertFalse(index.isMemberShapeProperty(model.expectShape(
                ShapeId.from("com.example#ResourceStructure_1$spurious"),
                MemberShape.class)));

        assertTrue(index.getPropertyName(ShapeId.from("com.example#ResourceStructure_1$property")).isPresent());
        assertEquals("property", index.getPropertyName(ShapeId.from("com.example#ResourceStructure_1$property")).get());
        assertFalse(index.getPropertyName(ShapeId.from("com.example#ResourceStructure_1$spurious")).isPresent());
        assertFalse(index.getPropertyName(ShapeId.from("com.example#Foo$notAnIdForAnything")).isPresent());

        OperationShape get = model.expectShape(ShapeId.from("com.example#GetResource"), OperationShape.class);
        OperationShape update = model.expectShape(ShapeId.from("com.example#UpdateResource"), OperationShape.class);

        assertEquals(ShapeId.from("com.example#ResourceStructure_1"), index.getInputPropertiesShape(update).getId());
        assertEquals(ShapeId.from("com.example#ResourceDescription"), index.getOutputPropertiesShape(get).getId());
        assertEquals(ShapeId.from("com.example#ResourceDescription"), index.getOutputPropertiesShape(update).getId());

        assertTrue(index.doesMemberShapeRequireProperty(model.expectShape(
                ShapeId.from("com.example#ChangeResourceOutput$id"),
                MemberShape.class)));

        Assertions.assertThrows(ValidatedResultException.class, () -> vrmodel.unwrap());
    }
}
