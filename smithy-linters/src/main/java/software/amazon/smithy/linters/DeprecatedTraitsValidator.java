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

package software.amazon.smithy.linters;

import static java.lang.String.format;

import java.util.List;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.traits.DeprecatedTrait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidatorService;

/**
 * Emits a validation event if a model contains a trait that has been marked as deprecated.
 */
public final class DeprecatedTraitsValidator extends AbstractValidator {

    public static final class Provider extends ValidatorService.Provider {
        public Provider() {
            super(DeprecatedTraitsValidator.class, DeprecatedTraitsValidator::new);
        }
    }

    @Override
    public List<ValidationEvent> validate(Model model) {

        return model.shapes()
                .filter(shape -> shape.hasTrait(TraitDefinition.class))
                .filter(trait -> trait.getTrait(DeprecatedTrait.class).isPresent())
                .map(deprecatedTrait -> warning(deprecatedTrait, format("The %s trait is deprecated.",
                        deprecatedTrait.getId().getName())))
                .collect(Collectors.toList());
    }
}
