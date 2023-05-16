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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;

public class SdkServiceIdValidatorTest {
    @Test
    public void validatesServiceShapesDuringBuild() {
        ShapeId id = ShapeId.from("com.foo#Baz");
        ServiceShape serviceShape = ServiceShape.builder()
                .id(id)
                .version("2016-04-01")
                .addTrait(ServiceTrait.builder().sdkId("AWS Foo").build(id))
                .build();
        ValidatedResult<Model> result = Model.assembler()
                .addShape(serviceShape)
                .discoverModels(getClass().getClassLoader())
                .assemble();

        assertThat(result.getValidationEvents(Severity.DANGER), not(empty()));
    }

    @Test
    public void doesNotAllowCompanyNames() {
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () ->
            SdkServiceIdValidator.validateServiceId("AWS Foo"));

        assertThat(thrown.getMessage(), containsString("company names"));
    }

    @Test
    public void doesNotAllowBadSuffix() {
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () ->
                SdkServiceIdValidator.validateServiceId("Foo Service"));

        assertThat(thrown.getMessage(), containsString("case-insensitively end with"));
    }

    @Test
    public void mustMatchRegex() {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                SdkServiceIdValidator.validateServiceId("!Nope!"));
    }

    @Test
    public void noTrailingWhitespace() {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                SdkServiceIdValidator.validateServiceId("Foo "));
    }

    @Test
    public void doesNotAllowShortIds() {
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () ->
                SdkServiceIdValidator.validateServiceId("F"));

        assertThat(thrown.getMessage(), containsString("2 and 50"));
    }

    @Test
    public void doesNotAllowLongIds() {
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () ->
                SdkServiceIdValidator.validateServiceId("Foobarbazqux Foobarbazqux Foobarbazqux Foobarbazqux"));

        assertThat(thrown.getMessage(), containsString("2 and 50"));
    }
}
