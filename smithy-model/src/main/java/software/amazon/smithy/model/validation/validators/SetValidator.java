/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.SetShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SmithyInternalApi;

@SmithyInternalApi
public final class SetValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (SetShape set : model.getSetShapes()) {
            ValidationEvent event = ValidationEvent.builder()
                    .id(AbstractValidator.MODEL_DEPRECATION)
                    .severity(Severity.WARNING)
                    .shape(set)
                    .message("Set shapes are deprecated and have been removed in Smithy IDL v2. "
                            + "Use a list shape with the @uniqueItems trait instead.")
                    .build();
            events.add(event);
        }
        return events;
    }
}
