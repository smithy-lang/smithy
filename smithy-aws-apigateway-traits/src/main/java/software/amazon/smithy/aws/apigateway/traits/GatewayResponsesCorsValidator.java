/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.traits;

import java.util.List;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.traits.CorsTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Emits a DANGER validation event when a service has both
 * {@code @gatewayResponses} and {@code @cors} applied, since gateway
 * responses take precedence over CORS-generated headers and may produce
 * unexpected results.
 */
@SmithyInternalApi
public final class GatewayResponsesCorsValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        return model.shapes(ServiceShape.class)
                .filter(service -> service.hasTrait(GatewayResponsesTrait.class))
                .filter(service -> service.hasTrait(CorsTrait.class))
                .map(service -> ValidationEvent.builder()
                        .id(getName())
                        .shape(service)
                        .severity(Severity.DANGER)
                        .message("Service has both @gatewayResponses and @cors applied. "
                                + "Gateway response parameters take precedence over "
                                + "CORS-generated headers. Verify that response parameters "
                                + "in @gatewayResponses do not unintentionally override "
                                + "CORS headers.")
                        .build())
                .collect(Collectors.toList());
    }
}
