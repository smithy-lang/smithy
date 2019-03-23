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

package software.smithy.linters;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.Pair;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.IdempotentTrait;
import software.amazon.smithy.model.traits.ReadonlyTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidatorService;

/**
 * Validates that `http` traits applied to operation shapes use the most
 * semantically appropriate HTTP method according to RFC 7231.
 */
public class HttpMethodSemanticsValidator extends AbstractValidator {
    private static final Map<String, Pair<Boolean, Boolean>> EXPECTED = Map.of(
            "GET", new Pair<>(true, false),
            "HEAD", new Pair<>(true, false),
            "OPTIONS", new Pair<>(true, false),
            "TRACE", new Pair<>(true, false),
            "POST", new Pair<>(false, false),
            "DELETE", new Pair<>(false, true),
            "PUT", new Pair<>(false, true),
            "PATCH", new Pair<>(false, false));

    public static ValidatorService provider() {
        return ValidatorService.createSimpleProvider(
                HttpMethodSemanticsValidator.class, HttpMethodSemanticsValidator::new);
    }

    @Override
    public List<ValidationEvent> validate(Model model) {
        ShapeIndex index = model.getShapeIndex();
        return index.shapes(OperationShape.class)
                .flatMap(shape -> Trait.flatMapStream(shape, HttpTrait.class))
                .flatMap(pair -> validateOperation(pair.getLeft(), pair.getRight()).stream())
                .collect(Collectors.toList());
    }

    private Optional<ValidationEvent> validateOperation(OperationShape shape, HttpTrait trait) {
        String method = trait.getMethod().toUpperCase(Locale.US);

        if (!EXPECTED.containsKey(method)) {
            return Optional.empty();
        }

        Pair<Boolean, Boolean> pair = EXPECTED.get(method);
        boolean isReadonly = shape.getTrait(ReadonlyTrait.class).isPresent();
        if (isReadonly != pair.getLeft()) {
            return Optional.of(danger(shape, trait, String.format(
                    "This operation uses the `%s` method in the `http` trait, but %s marked with the readonly trait",
                    method, isReadonly ? "is" : "is not")));
        }

        boolean isIdempotent = shape.getTrait(IdempotentTrait.class).isPresent();
        if (pair.getRight() != isIdempotent) {
            return Optional.of(danger(shape, trait, String.format(
                    "This operation uses the `%s` method in the `http` trait, but %s marked with the idempotent trait",
                    method, isIdempotent ? "is" : "is not")));
        }

        return Optional.empty();
    }
}
