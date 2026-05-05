/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.traits;

import static org.hamcrest.MatcherAssert.assertThat;
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

public class GatewayResponsesCorsValidatorTest {
    @Test
    public void emitsDangerWhenBothTraitsPresent() {
        ValidatedResult<Model> result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("gateway-responses-cors-conflict.smithy"))
                .assemble();

        List<ValidationEvent> events = result.getValidationEvents()
                .stream()
                .filter(e -> e.getId().equals("GatewayResponsesCors"))
                .collect(Collectors.toList());

        assertThat(events, hasSize(1));
        assertThat(events.get(0).getSeverity(), is(Severity.DANGER));
    }

    @Test
    public void noEventWhenOnlyGatewayResponses() {
        ValidatedResult<Model> result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("gateway-responses-no-cors.smithy"))
                .assemble();

        List<ValidationEvent> events = result.getValidationEvents()
                .stream()
                .filter(e -> e.getId().equals("GatewayResponsesCors"))
                .collect(Collectors.toList());

        assertThat(events, is(empty()));
    }
}
