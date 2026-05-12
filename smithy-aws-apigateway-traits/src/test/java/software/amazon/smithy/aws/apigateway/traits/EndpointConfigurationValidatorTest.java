/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;

public class EndpointConfigurationValidatorTest {
    private static final String VALIDATOR_ID = "EndpointConfiguration";

    @Test
    public void emitsErrorWhenPrivateEndpointUsesIpv4() {
        List<ValidationEvent> events = validate("endpoint-configuration-private-ipv4.smithy");

        assertThat(events, hasSize(1));
        assertThat(events.get(0).getSeverity(), is(Severity.ERROR));
        assertThat(events.get(0).getMessage(), containsString("PRIVATE"));
        assertThat(events.get(0).getMessage(), containsString("dualstack"));
    }

    @Test
    public void noEventWhenPrivateEndpointUsesDualstack() {
        List<ValidationEvent> events = validate("endpoint-configuration-private-dualstack.smithy");
        assertThat(events, is(empty()));
    }

    @Test
    public void noEventWhenRegionalEndpointUsesIpv4() {
        List<ValidationEvent> events = validate("endpoint-configuration-regional.smithy");
        assertThat(events, is(empty()));
    }

    private List<ValidationEvent> validate(String model) {
        ValidatedResult<Model> result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource(model))
                .assemble();

        return result.getValidationEvents()
                .stream()
                .filter(e -> e.getId().equals(VALIDATOR_ID))
                .collect(Collectors.toList());
    }
}
