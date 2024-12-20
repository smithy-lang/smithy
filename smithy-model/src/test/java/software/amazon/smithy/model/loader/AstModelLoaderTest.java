/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
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
}
