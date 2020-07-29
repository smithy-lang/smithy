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

package software.amazon.smithy.model.loader;

import static java.lang.String.format;

import java.util.Objects;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Validator;
import software.amazon.smithy.model.validation.ValidatorFactory;
import software.amazon.smithy.utils.ListUtils;

final class ValidatorFromDefinitionFactory {

    private final ValidatorFactory delegate;

    ValidatorFromDefinitionFactory(ValidatorFactory delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate validator must not be null");
    }

    ValidatedResult<Validator> loadValidator(ValidatorDefinition definition) {
        try {
            return delegate.createValidator(definition.name, definition.configuration)
                    .map(validator -> ValidatedResult.fromValue(mapValidator(definition, validator)))
                    .orElseGet(ValidatedResult::empty);
        } catch (SourceException e) {
            return ValidatedResult.fromErrors(ListUtils.of(ValidationEvent.fromSourceException(
                    e, format("Error creating `%s` validator: ", definition.name))));
        } catch (RuntimeException e) {
            return ValidatedResult.fromErrors(ListUtils.of(
                    ValidationEvent.builder()
                            .id(Validator.MODEL_ERROR)
                            .sourceLocation(definition.sourceLocation)
                            .severity(Severity.ERROR)
                            .message(format("Error creating `%s` validator: %s", definition.name, e.getMessage()))
                            .build()));
        }
    }

    private Validator mapValidator(ValidatorDefinition definition, Validator upstream) {
        return model -> definition.map(model, upstream.validate(model));
    }
}
