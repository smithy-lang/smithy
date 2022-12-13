/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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
            if (trait.getProperty()
                    .filter(property -> !TaggingShapeUtils.isTagDesiredName(property))
                    .isPresent()) {
                events.add(warning(resource, String.format("Suggested tag property name is '%s'.",
                    TaggingShapeUtils.getDesiredTagsPropertyName())));
            }
        }

        return events;
    }
}
