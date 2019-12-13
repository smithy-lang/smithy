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

package software.amazon.smithy.aws.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;

public class ArnTemplateValidatorTest {
    private static ModelAssembler baseModel;

    @BeforeAll
    public static void beforeClass() {
        baseModel = Model.assembler().discoverModels(ArnTemplateValidatorTest.class.getClassLoader());
    }

    private ValidatedResult<Model> getModel(String name) {
        return baseModel.copy().addImport(getClass().getResource(name)).assemble();
    }

    @Test
    public void findsMissingIdentifiers() {
        ValidatedResult<Model> result = getModel("not-enough-identifiers.json");

        assertThat(result.getValidationEvents(Severity.ERROR), hasSize(2));
        List<ValidationEvent> events = result.getValidationEvents(Severity.ERROR);
        events.sort(Comparator.comparing(event -> event.getShapeId().get().toString()));

        String message1 = events.get(0).getMessage();
        assertThat(message1, containsString("a"));
        assertThat(message1, containsString("`aid`"));

        String message2 = events.get(1).getMessage();
        assertThat(message2, containsString("a/b"));
        assertThat(message2, containsString("`aid`, `bid`"));
    }

    @Test
    public void findsExtraneousIdentifiers() {
        ValidatedResult<Model> result = getModel("too-many-identifiers.json");

        assertThat(result.getValidationEvents(Severity.ERROR), hasSize(2));
        List<ValidationEvent> events = result.getValidationEvents(Severity.ERROR);
        events.sort(Comparator.comparing(event -> event.getShapeId().get().toString()));

        assertThat(events.get(0).getMessage(), equalTo(
                "Invalid aws.api#arn trait resource, `a/{aid}/{InvalidId}/{InvalidId2}`. Found template "
                + "labels in the trait that are not the names of the identifiers of the resource: [aid]. "
                + "Extraneous identifiers: [`InvalidId`, `InvalidId2`]"));
        assertThat(events.get(1).getMessage(), containsString(
                "Invalid aws.api#arn trait resource, `a/{aid}/{InvalidId}/{InvalidId2}/b/{bid}/{AnotherInvalid}`. "
                + "Found template labels in the trait that are not the names of the identifiers of the resource"));
    }

    @Test
    public void validatesTemplatePlaceHolders() {
        ValidatedResult<Model> result = getModel("invalid-arn-template-string.json");

        assertThat(result.getValidationEvents(Severity.ERROR), hasSize(1));
        List<ValidationEvent> events = result.getValidationEvents(Severity.ERROR);
        assertThat(events.get(0).getMessage(),
                   containsString("aws.api#arn trait contains invalid template labels"));
    }
}
