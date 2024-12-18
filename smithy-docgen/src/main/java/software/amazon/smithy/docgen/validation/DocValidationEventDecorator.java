/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.validation;

import software.amazon.smithy.model.traits.UnitTypeTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationEventDecorator;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds context to validation events describing how they impact docs.
 */
@SmithyInternalApi
public class DocValidationEventDecorator implements ValidationEventDecorator {
    private static final String REUSE_DOCUMENTATION_CONTEXT = """
            Additionally, reusing your input and output structures can make your \
            documentation confusing for customers, because they'll see those \
            structures both as the inputs or outputs of your operation and as \
            standalone structures. This can be particularly confusing if not all of \
            your operation inputs and outputs do this.""";

    @Override
    public boolean canDecorate(ValidationEvent event) {
        return event.containsId("InputOutputStructureReuse");
    }

    @Override
    public ValidationEvent decorate(ValidationEvent event) {
        if (event.getShapeId().isPresent() && event.getShapeId().get().equals(UnitTypeTrait.UNIT)) {
            return event.toBuilder().severity(Severity.SUPPRESSED).build();
        }
        return event.toBuilder()
                .message(event.getMessage() + " " + REUSE_DOCUMENTATION_CONTEXT)
                .severity(Severity.DANGER)
                .build();
    }
}
