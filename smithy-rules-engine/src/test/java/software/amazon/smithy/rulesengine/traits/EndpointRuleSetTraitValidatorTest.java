/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.rulesengine.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.validation.ValidationEvent;

public class EndpointRuleSetTraitValidatorTest {
    @Test
    public void validEndpointRuleSetInModel() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("traits-test-model.smithy"))
                .assemble()
                .unwrap();

        EndpointRuleSetTraitValidator validator = new EndpointRuleSetTraitValidator();
        List<ValidationEvent> events = validator.validate(model);
        assertThat(events, empty());
    }

    @Test
    public void invalidEndpointRuleSet() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("invalid-test-model.smithy"))
                .assemble()
                .unwrap();
        EndpointRuleSetTraitValidator validator = new EndpointRuleSetTraitValidator();
        List<ValidationEvent> events = validator.validate(model);
        assertThat(events.get(0).getMessage(),
                containsString("Expected String but found Option<String>"));
    }
}
