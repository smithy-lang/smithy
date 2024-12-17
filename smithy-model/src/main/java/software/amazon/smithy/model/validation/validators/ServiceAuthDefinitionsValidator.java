/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.traits.AuthTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates the @authDefinition traits applied to service shapes.
 */
public class ServiceAuthDefinitionsValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        ServiceIndex index = ServiceIndex.of(model);

        List<ServiceShape> services = model.getServiceShapes()
                .stream()
                .filter(serviceShape -> !serviceShape.hasTrait(AuthTrait.ID))
                .filter(serviceShape -> index.getAuthSchemes(serviceShape).size() > 1)
                .collect(Collectors.toList());

        for (ServiceShape service : services) {
            events.add(warning(service,
                    "This service uses multiple authentication schemes but does not have "
                            + "the `@auth` trait applied. The `@auth` trait defines a priority ordering "
                            + "of auth schemes for a client to use. Without it, the ordering of auth "
                            + "schemes is alphabetical based on the absolute shape ID of the auth "
                            + "schemes."));

        }
        return events;
    }
}
