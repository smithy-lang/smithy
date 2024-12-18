/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.HttpBindingIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.HttpErrorTrait;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Ensures that HTTP response codes are appropriate for operations and errors.
 */
public final class HttpResponseCodeSemanticsValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();

        for (OperationShape operation : model.getOperationShapesWithTrait(HttpTrait.class)) {
            validateOperationsWithHttpTrait(model, operation).ifPresent(events::add);
        }

        for (StructureShape structure : model.getStructureShapesWithTrait(ErrorTrait.class)) {
            validateError(structure, structure.expectTrait(ErrorTrait.class)).ifPresent(events::add);
        }

        return events;
    }

    private Optional<ValidationEvent> validateOperationsWithHttpTrait(Model model, OperationShape operation) {
        HttpTrait trait = operation.expectTrait(HttpTrait.class);
        if (trait.getCode() < 200 || trait.getCode() >= 300) {
            return Optional.of(invalidOperation(operation, trait));
        }

        if (trait.getCode() == 204 || trait.getCode() == 205) {
            if (HttpBindingIndex.of(model).hasResponseBody(operation)) {
                return Optional.of(warning(operation,
                        String.format(
                                "The HTTP %d status code does not allow a response body. To use this status code, all output "
                                        + "members need to be bound to @httpHeader, @httpPrefixHeaders, @httpResponseCode, etc.",
                                trait.getCode())));
            }
        }

        return Optional.empty();
    }

    private ValidationEvent invalidOperation(Shape shape, HttpTrait trait) {
        return danger(shape, trait, "Expected an `http` code in the 2xx range, but found " + trait.getCode());
    }

    private Optional<ValidationEvent> validateError(StructureShape shape, ErrorTrait error) {
        return shape.getTrait(HttpErrorTrait.class).flatMap(httpErrorTrait -> {
            // Make sure that client errors are 4xx, and server errors are 5xx.
            int code = httpErrorTrait.getCode();
            if (error.isClientError() && (code < 400 || code >= 500)) {
                return Optional.of(invalidError(shape, httpErrorTrait, code, "4xx", error.getValue()));
            } else if (error.isServerError() && (code < 500 || code >= 600)) {
                return Optional.of(invalidError(shape, httpErrorTrait, code, "5xx", error.getValue()));
            } else {
                return Optional.empty();
            }
        });
    }

    private ValidationEvent invalidError(Shape shape, Trait trait, int code, String range, String errorValue) {
        return danger(shape,
                trait,
                String.format(
                        "Expected an `httpError` code in the %s range for a `%s` error, but found %s",
                        range,
                        errorValue,
                        code));
    }
}
