/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.HttpErrorTrait;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.OptionalUtils;

/**
 * Ensures that HTTP response codes are appropriate for operations and errors.
 */
public final class HttpResponseCodeSemanticsValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        events.addAll(validateOperations(model));
        events.addAll(validateErrors(model));
        return events;
    }

    private List<ValidationEvent> validateOperations(Model model) {
        return model.shapes(OperationShape.class)
                .flatMap(shape -> Trait.flatMapStream(shape, HttpTrait.class))
                .filter(pair -> pair.getRight().getCode() < 200 || pair.getRight().getCode() >= 300)
                .map(pair -> invalidOperation(pair.getLeft(), pair.getRight()))
                .collect(Collectors.toList());
    }

    private ValidationEvent invalidOperation(Shape shape, HttpTrait trait) {
        return danger(shape, trait, "Expected an `http` code in the 2xx range, but found " + trait.getCode());
    }

    private List<ValidationEvent> validateErrors(Model model) {
        return model.shapes(StructureShape.class)
                .flatMap(shape -> Trait.flatMapStream(shape, ErrorTrait.class))
                .flatMap(pair -> OptionalUtils.stream(validateError(pair.getLeft(), pair.getRight())))
                .collect(Collectors.toList());
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
        return danger(shape, trait, String.format(
                "Expected an `httpError` code in the %s range for a `%s` error, but found %s",
                range, errorValue, code));
    }
}
