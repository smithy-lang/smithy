/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.traits;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.validation.AbstractValidator;
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
        List<ValidationEvent> events = new ArrayList<>();
        for (ServiceShape service : model.getServiceShapesWithTrait(EndpointConfigurationTrait.class)) {
            EndpointConfigurationTrait trait = service.expectTrait(EndpointConfigurationTrait.class);
            Optional<String> ipAddressType = trait.getIpAddressType();
            if (ipAddressType.isPresent()
                    && trait.getTypes().contains(PRIVATE)
                    && !DUALSTACK.equals(ipAddressType.get())) {
                events.add(error(service,
                        String.format("The `PRIVATE` endpoint type requires `ipAddressType` to be "
                                + "`dualstack`, but found `%s`.",
                                ipAddressType.get())));
            }
        }
        return events;
    }
}
