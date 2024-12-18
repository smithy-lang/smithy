/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.tagging;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates expected tagging property name used for a taggable resource to encourage consistency.
 */
public final class TagResourcePropertyNameValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();

        for (ResourceShape resource : model.getResourceShapesWithTrait(TaggableTrait.class)) {
            TaggableTrait trait = resource.expectTrait(TaggableTrait.class);
            if (trait.getProperty().isPresent() && !TaggingShapeUtils.isTagDesiredName(trait.getProperty().get())) {
                events.add(warning(resource,
                        String.format("Suggested tag property name is '%s'.",
                                TaggingShapeUtils.getDesiredTagsPropertyName())));
            }
        }

        return events;
    }
}
