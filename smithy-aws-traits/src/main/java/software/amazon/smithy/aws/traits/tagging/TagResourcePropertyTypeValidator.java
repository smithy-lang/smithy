/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.tagging;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates tagging property used for a taggable resource to encourage consistency.
 */
public final class TagResourcePropertyTypeValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();

        for (ResourceShape resource : model.getResourceShapesWithTrait(TaggableTrait.class)) {
            TaggableTrait trait = resource.expectTrait(TaggableTrait.class);
            Map<String, ShapeId> properties = resource.getProperties();
            if (trait.getProperty().isPresent() && properties.containsKey(trait.getProperty().get())) {
                Shape propertyShape = model.expectShape(properties.get(trait.getProperty().get()));
                if (!TaggingShapeUtils.verifyTagsShape(model, propertyShape)) {
                    events.add(error(resource,
                            "Tag property must be a list shape targeting a member"
                                    + " containing a pair of strings, or a Map shape targeting a string member."));
                }
            }
        }

        return events;
    }
}
