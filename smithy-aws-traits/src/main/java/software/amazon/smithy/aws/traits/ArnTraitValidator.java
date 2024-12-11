/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Validates that the `resourceDelimiter` field of the `aws.api#arn` trait
 * is only used in conjunction with absolute ARNs.
 */
@SmithyInternalApi
public final class ArnTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (ResourceShape resource : model.getResourceShapesWithTrait(ArnTrait.class)) {
            ArnTrait trait = resource.expectTrait(ArnTrait.class);
            if (!trait.isAbsolute() && trait.getResourceDelimiter().isPresent()) {
                events.add(error(resource, trait, "A `resourceDelimiter` can only be set for an `absolute` ARN."));
            }
        }
        return events;
    }
}
