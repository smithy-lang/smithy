/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.linters;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.InputTrait;
import software.amazon.smithy.model.traits.OutputTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidatorService;

/**
 * Validates that all operations define input and output structures that are marked
 * with the input and output traits.
 */
public final class InputOutputStructureReuseValidator extends AbstractValidator {

    public static final class Provider extends ValidatorService.Provider {
        public Provider() {
            super(InputOutputStructureReuseValidator.class, InputOutputStructureReuseValidator::new);
        }
    }

    private static final String INPUT = "Input";
    private static final String OUTPUT = "Output";

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        OperationIndex index = OperationIndex.of(model);
        for (OperationShape operation : model.getOperationShapes()) {
            StructureShape input = index.expectInputShape(operation);
            StructureShape output = index.expectOutputShape(operation);
            validateInputOutputSet(operation, input, output, events);
        }
        return events;
    }

    private void validateInputOutputSet(
            OperationShape operation,
            StructureShape input,
            StructureShape output,
            List<ValidationEvent> events
    ) {
        if (!input.hasTrait(InputTrait.class)) {
            events.add(warning(input,
                    String.format(
                            "This structure is the input of `%s`, but it is not marked with the "
                                    + "@input trait. The @input trait gives operations more flexibility to "
                                    + "evolve their top-level input members in ways that would otherwise "
                                    + "be backward incompatible.",
                            operation.getId()),
                    INPUT,
                    operation.getId().getName()));
        }

        if (!output.hasTrait(OutputTrait.class)) {
            events.add(warning(output,
                    String.format(
                            "This structure is the output of `%s`, but it is not marked with "
                                    + "the @output trait.",
                            operation.getId()),
                    OUTPUT,
                    operation.getId().getName()));
        }
    }
}
