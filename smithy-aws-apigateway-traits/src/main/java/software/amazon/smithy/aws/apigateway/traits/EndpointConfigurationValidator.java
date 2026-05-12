/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.traits;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Validates the {@code aws.apigateway#endpointConfiguration} trait.
 *
 * <p>Emits an ERROR when the {@code PRIVATE} endpoint type is used with an
 * {@code ipAddressType} other than {@code dualstack}, since API Gateway
 * requires {@code dualstack} for private endpoints.
 */
@SmithyInternalApi
public final class EndpointConfigurationValidator extends AbstractValidator {

    private static final String DUALSTACK = "dualstack";
    private static final String PRIVATE = "PRIVATE";

    @Override
    public List<ValidationEvent> validate(Model model) {
        return model.shapes(ServiceShape.class)
                .filter(service -> service.hasTrait(EndpointConfigurationTrait.class))
                .flatMap(service -> {
                    EndpointConfigurationTrait trait = service.expectTrait(EndpointConfigurationTrait.class);
                    return trait.getIpAddressType()
                            .filter(ipAddressType -> trait.getTypes().contains(PRIVATE)
                                    && !DUALSTACK.equals(ipAddressType))
                            .map(ipAddressType -> ValidationEvent.builder()
                                    .id(getName())
                                    .shape(service)
                                    .severity(Severity.ERROR)
                                    .message(String.format(
                                            "The `PRIVATE` endpoint type requires `ipAddressType` to be "
                                                    + "`dualstack`, but found `%s`.",
                                            ipAddressType))
                                    .build())
                            .map(Stream::of)
                            .orElseGet(Stream::empty);
                })
                .collect(Collectors.toList());
    }
}
