/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.neighbor.UnreferencedShapes;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Adds a validation note event for each shape in the model that is not
 * connected to a service shape.
 *
 * <p>This validator is deprecated and no longer applied by default.
 * Use the UnreferencedShapeValidator from smithy-linters instead.
 */
@Deprecated
public final class UnreferencedShapeValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        // Do not emit validation warnings if no services are present in the model.
        if (model.getServiceShapes().isEmpty()) {
            return Collections.emptyList();
        }

        List<ValidationEvent> events = new ArrayList<>();
        for (Shape shape : new UnreferencedShapes().compute(model)) {
            events.add(note(shape,
                    String.format(
                            "The %s shape is not connected to from any service shape.",
                            shape.getType())));
        }

        return events;
    }
}
