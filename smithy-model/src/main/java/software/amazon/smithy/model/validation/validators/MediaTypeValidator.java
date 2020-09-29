/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.MediaTypeTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.MediaType;

public final class MediaTypeValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (Shape shape : model.getShapesWithTrait(MediaTypeTrait.class)) {
            validateMediaType(shape, shape.expectTrait(MediaTypeTrait.class)).ifPresent(events::add);
        }

        return events;
    }

    private Optional<ValidationEvent> validateMediaType(Shape shape, MediaTypeTrait trait) {
        try {
            MediaType.from(trait.getValue());
            return Optional.empty();
        } catch (RuntimeException e) {
            return Optional.of(error(shape, trait, String.format(
                    "Invalid mediaType value, \"%s\": %s", trait.getValue(), e.getMessage())));
        }
    }
}
