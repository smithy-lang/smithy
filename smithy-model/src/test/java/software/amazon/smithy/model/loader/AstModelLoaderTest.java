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
                .addImport(getClass().getResource("invalid/properties-v2-only.json"))
                .assemble();
        assertEquals(1, model.getValidationEvents(Severity.ERROR).size());
        assertTrue(model.getValidationEvents(Severity.ERROR).get(0).getMessage()
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
}
