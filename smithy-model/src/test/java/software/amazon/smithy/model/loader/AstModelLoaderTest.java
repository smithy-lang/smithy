/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;

public class AstModelLoaderTest {
    @Test
    public void failsToLoadPropertiesFromV1() {
        ValidatedResult<Model> model = Model.assembler()
                .addImport(getClass().getResource("invalid/version/properties-v2-only.json"))
                .assemble();
        assertEquals(1, model.getValidationEvents(Severity.ERROR).size());
        assertTrue(model.getValidationEvents(Severity.ERROR)
                .get(0)
                .getMessage()
                .contains("Resource properties can only be used with Smithy version 2 or later."));
    }

    @Test
    public void doesNotFailOnEmptyApply() {
        // Empty apply statements are pointless but shouldn't break the loader.
        Model.assembler()
                .addImport(getClass().getResource("ast-empty-apply-1.json"))
                .addImport(getClass().getResource("ast-empty-apply-2.json"))
                .assemble()
                .unwrap();
    }

    @Test
    public void allowsMixinsOnOperationsWithoutWarningOrError() {
        ValidatedResult<Model> model = Model.assembler()
                .addImport(getClass().getResource("mixins/operation-mixins.json"))
                .assemble();
        assertEquals(0, model.getValidationEvents(Severity.WARNING).size());
        assertEquals(0, model.getValidationEvents(Severity.ERROR).size());
    }

    @Test
    public void allowsMixinsOnResourcesWithoutWarningOrError() {
        ValidatedResult<Model> model = Model.assembler()
                .addImport(getClass().getResource("mixins/resource-mixins.json"))
                .assemble();
        assertEquals(0, model.getValidationEvents(Severity.WARNING).size());
        assertEquals(0, model.getValidationEvents(Severity.ERROR).size());
    }

    @Test
    public void allowsMixinsOnServiceWithoutWarningOrError() {
        ValidatedResult<Model> model = Model.assembler()
                .addImport(getClass().getResource("mixins/service-mixins.json"))
                .assemble();
        assertEquals(0, model.getValidationEvents(Severity.WARNING).size());
        assertEquals(0, model.getValidationEvents(Severity.ERROR).size());
    }

    @Test
    public void allowsMixinsOnStructuresWithoutWarningOrError() {
        ValidatedResult<Model> model = Model.assembler()
                .addImport(getClass().getResource("mixins/structure-mixins.json"))
                .assemble();
        assertEquals(0, model.getValidationEvents(Severity.WARNING).size());
        assertEquals(0, model.getValidationEvents(Severity.ERROR).size());
    }

    @Test
    public void allowsMixinsOnUnionsWithoutWarningOrError() {
        ValidatedResult<Model> model = Model.assembler()
                .addImport(getClass().getResource("mixins/union-mixins.json"))
                .assemble();
        assertEquals(0, model.getValidationEvents(Severity.WARNING).size());
        assertEquals(0, model.getValidationEvents(Severity.ERROR).size());
    }

    @Test
    public void allowsMixinsOnSimpleShapesWithoutWarningOrError() {
        ValidatedResult<Model> model = Model.assembler()
                .addImport(getClass().getResource("mixins/simple-shape-mixins.json"))
                .assemble();
        assertEquals(0, model.getValidationEvents(Severity.WARNING).size());
        assertEquals(0, model.getValidationEvents(Severity.ERROR).size());
    }

    @Test
    public void allowsMixinsOnCollectionShapesWithoutWarningOrError() {
        ValidatedResult<Model> model = Model.assembler()
                .addImport(getClass().getResource("mixins/collection-mixins.json"))
                .assemble();
        assertEquals(0, model.getValidationEvents(Severity.WARNING).size());
        assertEquals(0, model.getValidationEvents(Severity.ERROR).size());
    }

    @Test
    public void reportsInvalidMemberTargetAsValidationEventNotCrash() {
        // An invalid shape id in a member target must surface as a validation event, not escape as a
        // raw ShapeIdSyntaxException. (Found by fuzzing.)
        String json = "{\n"
                + "    \"smithy\": \"2.0\",\n"
                + "    \"shapes\": {\n"
                + "        \"smithy.example#Struct\": {\n"
                + "            \"type\": \"structure\",\n"
                + "            \"members\": {\"foo\": {\"target\": \"not a valid id\"}}\n"
                + "        }\n"
                + "    }\n"
                + "}";
        ValidatedResult<Model> result = Model.assembler().addUnparsedModel("test.json", json).assemble();

        assertFalse(result.getValidationEvents(Severity.ERROR).isEmpty());
    }

    @Test
    public void reportsInvalidMemberNameAsValidationEventNotCrash() {
        // An invalid member name must surface as a validation event, not a raw ShapeIdSyntaxException
        // from ShapeId#withMember. (Found by fuzzing.)
        String json = "{\n"
                + "    \"smithy\": \"2.0\",\n"
                + "    \"shapes\": {\n"
                + "        \"smithy.example#Struct\": {\n"
                + "            \"type\": \"structure\",\n"
                + "            \"members\": {\"+bad\": {\"target\": \"smithy.api#String\"}}\n"
                + "        }\n"
                + "    }\n"
                + "}";
        ValidatedResult<Model> result = Model.assembler().addUnparsedModel("test.json", json).assemble();

        assertFalse(result.getValidationEvents(Severity.ERROR).isEmpty());
    }

    @Test
    public void appliesVersionEvenWhenShapesPrecedeSmithyVersion() {
        // The version drives version-specific trait validation, so it must apply to shapes regardless
        // of key order. @box is an error in 2.0 but ignored under UNKNOWN; placing `shapes` before the
        // `smithy` key must still produce the 2.0 error (the loader defers the shapes until the version
        // is known, without materializing the whole model).
        String json = "{\n"
                + "    \"shapes\": {\n"
                + "        \"smithy.example#Boxed\": {\n"
                + "            \"type\": \"integer\",\n"
                + "            \"traits\": {\"smithy.api#box\": {}}\n"
                + "        }\n"
                + "    },\n"
                + "    \"smithy\": \"2.0\"\n"
                + "}";
        ValidatedResult<Model> result = Model.assembler().addUnparsedModel("test.json", json).assemble();

        assertTrue(result.getValidationEvents(Severity.ERROR)
                .stream()
                .anyMatch(e -> e.getMessage().contains("@box is not supported in Smithy IDL 2.0")),
                () -> "Expected the 2.0 @box error, proving the version applied to the deferred shapes. "
                        + "Got: " + result.getValidationEvents());
    }

    @Test
    public void loadsShapeWhenTypeIsNotTheFirstMember() {
        // The AST always serializes `type` first, but key order is not significant in JSON; a shape
        // definition with members/traits before `type` must still load (the streaming loader falls
        // back to materializing the definition to find `type`).
        String json = "{\n"
                + "    \"smithy\": \"2.0\",\n"
                + "    \"shapes\": {\n"
                + "        \"smithy.example#Struct\": {\n"
                + "            \"traits\": {\"smithy.api#documentation\": \"hi\"},\n"
                + "            \"members\": {\"foo\": {\"target\": \"smithy.api#String\"}},\n"
                + "            \"type\": \"structure\"\n"
                + "        }\n"
                + "    }\n"
                + "}";
        ValidatedResult<Model> result = Model.assembler().addUnparsedModel("test.json", json).assemble();

        assertFalse(result.isBroken(), () -> result.getValidationEvents().toString());
        Model model = result.unwrap();
        StructureShape struct = model.expectShape(ShapeId.from("smithy.example#Struct"), StructureShape.class);
        assertTrue(struct.getMember("foo").isPresent());
        assertEquals("hi", struct.expectTrait(DocumentationTrait.class).getValue());
    }
}
