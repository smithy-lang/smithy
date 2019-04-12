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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.ValidatedResult;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.Suppression;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Validator;
import software.amazon.smithy.model.validation.ValidatorFactory;
import software.amazon.smithy.utils.ListUtils;

/**
 * Validates a model, including validators and suppressions loaded from
 * metadata.
 *
 * <p>ModelValidator is used to apply validation to a loaded Model. This
 * class is used in tandem with {@link ModelAssembler}.
 *
 * <p>Validators and suppressions found in the metadata of the validated
 * model are automatically created and applied to the model. Explicitly
 * provided suppressions and validators are merged together with the
 * validators and suppressions loaded from metadata.
 */
final class ModelValidator {
    private final List<Validator> validators;
    private final List<Suppression> suppressions;
    private final ArrayList<ValidationEvent> events = new ArrayList<>();
    private final ValidatorFactory validatorFactory;
    private final Model model;

    private ModelValidator(
            Model model,
            ValidatorFactory validatorFactory,
            List<Validator> validators,
            List<Suppression> suppressions
    ) {
        this.model = model;
        this.validatorFactory = validatorFactory;
        this.validators = new ArrayList<>(validators);
        this.suppressions = new ArrayList<>(suppressions);
    }

    /**
     * Validates the given Model using validators configured explicitly and
     * detected through metadata.
     *
     * @param model Model to validate.
     * @param validatorFactory Factory used to find ValidatorService providers.
     * @param validators Additional validators to use.
     * @param suppressions Additional suppressions to use.
     * @return Returns the encountered validation events.
     */
    static List<ValidationEvent> validate(
            Model model,
            ValidatorFactory validatorFactory,
            List<Validator> validators,
            List<Suppression> suppressions
    ) {
        return new ModelValidator(model, validatorFactory, validators, suppressions).doValidate();
    }

    /**
     * Validates the given Model using validators configured explicitly and
     * detected through metadata.
     *
     * @param model Model to validate.
     * @param validatorFactory Factory used to find ValidatorService providers.
     * @return Returns the encountered validation events.
     */
    static List<ValidationEvent> validate(Model model, ValidatorFactory validatorFactory) {
        return validate(model, validatorFactory, ListUtils.of(), ListUtils.of());
    }

    private List<ValidationEvent> doValidate() {
        List<ValidatorDefinition> assembledValidatorDefinitions = assembleValidatorDefinitions();
        assembleValidators(assembledValidatorDefinitions);
        assembleSuppressions();
        events.addAll(validators
                .parallelStream()
                .flatMap(validator -> validator.validate(model).stream())
                .map(event -> Suppression.suppressEvent(event, suppressions))
                .filter(ModelValidator::filterPrelude)
                .collect(Collectors.toList()));
        return events;
    }

    private static boolean filterPrelude(ValidationEvent event) {
        // Don't emit any non-error events for prelude shapes and traits.
        // This prevents custom validators from unnecessarily needing to
        // worry about prelude shapes and trait definitions, but still
        // allows for validation events when the prelude is broken.
        return event.getSeverity() == Severity.ERROR || !event.getShapeId()
                .filter(Prelude::isPreludeShape)
                .isPresent();
    }

    /**
     * Load validator definitions, aggregating any errors along the way.
     *
     * @return Returns the loaded validator definitions.
     */
    private List<ValidatorDefinition> assembleValidatorDefinitions() {
        ValidatedResult<List<ValidatorDefinition>> result = ValidationLoader.loadValidators(model.getMetadata());
        events.addAll(result.getValidationEvents());
        return result.getResult().orElseGet(Collections::emptyList);
    }

    /**
     * Loads suppressions based on the model metadata, aggregating any
     * errors along the way.
     */
    private void assembleSuppressions() {
        ValidatedResult<List<Suppression>> result = ValidationLoader.loadSuppressions(model.getMetadata());
        events.addAll(result.getValidationEvents());
        result.getResult().ifPresent(suppressions::addAll);
    }

    /**
     * Loads validators from model metadata, combines with explicit
     * validators, and aggregates errors.
     *
     * @param definitions List of validator definitions to resolve
     *  using the validator factory.
     */
    private void assembleValidators(List<ValidatorDefinition> definitions) {
        ValidatorFromDefinitionFactory factory = new ValidatorFromDefinitionFactory(validatorFactory);

        // Attempt to create the Validator instances and collect errors along the way.
        for (ValidatorDefinition val : definitions) {
            ValidatedResult<Validator> result = factory.loadValidator(val);
            result.getResult().ifPresent(validators::add);
            events.addAll(result.getValidationEvents());
            if (result.getValidationEvents().isEmpty() && !result.getResult().isPresent()) {
                events.add(unknownValidatorError(val.name, val.sourceLocation));
            }
        }
    }

    private ValidationEvent unknownValidatorError(String name, SourceLocation location) {
        return ValidationEvent.builder()
                // Per the spec, the eventID is "UnknownValidator.<validatorName>".
                .eventId("UnknownValidator." + name)
                .severity(Severity.WARNING)
                .sourceLocation(location)
                .message("Unable to locate a validator named `" + name + "`")
                .build();
    }
}
