/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
 *
 */

package software.amazon.smithy.aws.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;

public class HttpChecksumTraitValidatorTest {

    @Test
    public void validateLocations() {
        ValidatedResult<Model> result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("http-checksum-trait.json"))
                .assemble();

        List<ValidationEvent> warningEvents = result.getValidationEvents(Severity.WARNING);
        assertThat(warningEvents, not(empty()));
        assertThat(warningEvents.get(0).getMessage(),
                containsString("This shape applies a trait that is unstable: smithy.api#httpChecksum"));
        assertThat(warningEvents.get(1).getMessage(),
                containsString("This shape applies a trait that is unstable: smithy.api#httpChecksum"));
        assertThat(warningEvents.get(2).getMessage(),
                containsString("This shape applies a trait that is unstable: smithy.api#httpChecksum"));

        List<ValidationEvent> errorEvents = result.getValidationEvents(Severity.ERROR);
        assertThat(errorEvents, not(empty()));
        assertThat(errorEvents.get(0).getMessage(), containsString(
                "For aws protocols, the `response` property of the `httpChecksum` trait only supports `header` "
                        + "as `location`, found \"[trailer, header]\"."));
        assertThat(errorEvents.get(1).getMessage(), containsString(
                "For operation using sigv4 auth scheme, the `request` property of the "
                        + "`httpChecksum` trait must support `header` checksum location."));
        assertThat(errorEvents.get(2).getMessage(), containsString(
                "For aws protocols, the `response` property of the `httpChecksum` trait only supports `header` "
                        + "as `location`, found \"[trailer]\"."));
    }

}
