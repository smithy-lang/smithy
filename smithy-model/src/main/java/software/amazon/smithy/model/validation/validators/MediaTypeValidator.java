/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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
            return Optional.of(error(shape,
                    trait,
                    String.format(
                            "Invalid mediaType value, \"%s\": %s",
                            trait.getValue(),
                            e.getMessage())));
        }
    }
}
