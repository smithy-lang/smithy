/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.traits.HttpApiKeyAuthTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates that if an HttpApiKeyAuth trait's scheme field is present then
 * the 'in' field must specify "header". Scheme should only be used with the
 * "Authorization" http header.
 */
public final class HttpApiKeyAuthTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        Set<ServiceShape> serviceShapesWithTrait = model.getServiceShapesWithTrait(HttpApiKeyAuthTrait.class);
        List<ValidationEvent> events = new ArrayList<>();

        for (ServiceShape serviceShape : serviceShapesWithTrait) {
            HttpApiKeyAuthTrait trait = serviceShape.expectTrait(HttpApiKeyAuthTrait.class);
            trait.getScheme().ifPresent(scheme -> {
                if (trait.getIn() != HttpApiKeyAuthTrait.Location.HEADER) {
                    events.add(error(serviceShape,
                            trait,
                            String.format("The httpApiKeyAuth trait must have an `in` value of `header` when a `scheme`"
                                    + " is provided, found: %s", trait.getIn())));
                }
            });
        }

        return events;
    }
}
