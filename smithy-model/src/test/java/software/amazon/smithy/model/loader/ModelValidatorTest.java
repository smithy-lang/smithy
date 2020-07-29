/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * This test exercises ModelValidator but done so using the ModelAssembler.
 */
public class ModelValidatorTest {

    @Test
    public void addsExplicitValidators() {
        ValidationEvent event = ValidationEvent.builder()
                .severity(Severity.ERROR).id("Foo").message("bar").build();
        String document = "{\"smithy\": \"1.0\"}";
        ValidatedResult<Model> result = new ModelAssembler()
                .addUnparsedModel("[N/A]", document)
                .addValidator(index -> Collections.singletonList(event))
                .assemble();

        assertThat(result.getValidationEvents(), contains(event));
    }

    @Test
    public void registersTraitValidators() {
        ValidatedResult<Model> result = new ModelAssembler()
                .addImport(getClass().getResource("errors.json"))
                .assemble();

        assertThat(result.getValidationEvents(), hasSize(2));
        assertThat(result.getValidationEvents().get(0).getMessage(), containsString("client"));
        assertThat(result.getValidationEvents().get(1).getMessage(), containsString("server"));
    }
}
