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

    @Test
    public void testListBoundOperationIndex() {
        Model model = Model.assembler()
                .addImport(OperationIndexTest.class.getResource("list-bound-property-index.smithy"))
                .assemble()
                .unwrap();

        PropertyBindingIndex index = PropertyBindingIndex.of(model);
        OperationShape listTasks = model.expectShape(ShapeId.from("com.example#ListTasks"), OperationShape.class);

        // Output properties resolve through the list member to the element structure.
        assertEquals(ShapeId.from("com.example#TaskSummary"), index.getOutputPropertiesShape(listTasks).getId());
        assertEquals(ShapeId.from("com.example#ListTasksInput"), index.getInputPropertiesShape(listTasks).getId());

        // Element members matching resource properties are property bindings.
        assertTrue(index.isMemberShapeProperty(model.expectShape(
                ShapeId.from("com.example#TaskSummary$name"),
                MemberShape.class)));
        assertEquals("name", index.getPropertyName(ShapeId.from("com.example#TaskSummary$name")).get());
        assertTrue(index.isMemberShapeProperty(model.expectShape(
                ShapeId.from("com.example#TaskSummary$status"),
                MemberShape.class)));

        // Element members matching resource identifiers are not properties.
        assertFalse(index.isMemberShapeProperty(model.expectShape(
                ShapeId.from("com.example#TaskSummary$taskName"),
                MemberShape.class)));
        assertFalse(index.doesMemberShapeRequireProperty(model.expectShape(
                ShapeId.from("com.example#TaskSummary$taskName"),
                MemberShape.class)));

        // Element members matching nothing are ignored rather than required.
        assertFalse(index.isMemberShapeProperty(model.expectShape(
                ShapeId.from("com.example#TaskSummary$lastUpdatedAt"),
                MemberShape.class)));
        assertFalse(index.doesMemberShapeRequireProperty(model.expectShape(
                ShapeId.from("com.example#TaskSummary$lastUpdatedAt"),
                MemberShape.class)));

        // Top-level output members and all input members never bind properties.
        assertFalse(index.isMemberShapeProperty(model.expectShape(
                ShapeId.from("com.example#ListTasksOutput$tasks"),
                MemberShape.class)));
        assertFalse(index.doesMemberShapeRequireProperty(model.expectShape(
                ShapeId.from("com.example#ListTasksOutput$tasks"),
                MemberShape.class)));
        assertFalse(index.doesMemberShapeRequireProperty(model.expectShape(
                ShapeId.from("com.example#ListTasksInput$maxResults"),
                MemberShape.class)));
    }

    @Test
    public void testPaginatedItemsDisambiguatesBetweenMultipleListMembers() {
        Model model = Model.assembler()
                .addImport(OperationIndexTest.class.getResource("list-bound-paginated-disambiguation.smithy"))
                .assemble()
                .unwrap();

        PropertyBindingIndex index = PropertyBindingIndex.of(model);
        OperationShape listTasks = model.expectShape(ShapeId.from("com.example#ListTasks"), OperationShape.class);

        // The @paginated(items) member wins over the other list-of-structures member.
        assertEquals(ShapeId.from("com.example#TaskSummary"), index.getOutputPropertiesShape(listTasks).getId());

        // Only the selected element structure binds properties.
        assertTrue(index.isMemberShapeProperty(model.expectShape(
                ShapeId.from("com.example#TaskSummary$name"),
                MemberShape.class)));
        assertFalse(index.isMemberShapeProperty(model.expectShape(
                ShapeId.from("com.example#RelatedResourceSummary$arn"),
                MemberShape.class)));
    }
}
