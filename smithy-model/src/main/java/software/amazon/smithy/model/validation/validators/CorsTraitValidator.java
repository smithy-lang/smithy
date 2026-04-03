/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.traits.CorsTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates that the cors trait does not set both `origin` and `origins`.
 */
public final class CorsTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (ServiceShape service : model.getServiceShapesWithTrait(CorsTrait.class)) {
            CorsTrait trait = service.expectTrait(CorsTrait.class);
            if (trait.getSpecifiedOrigin().isPresent() && !trait.getOrigins().isEmpty()) {
                events.add(error(service,
                        trait,
                        "The `origin` and `origins` members of the `cors` trait are mutually exclusive."));
            }
        }
        return events;
    }
}
