/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.traits.SuppressTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationEventDecorator;
import software.amazon.smithy.model.validation.Validator;
import software.amazon.smithy.model.validation.ValidatorFactory;
import software.amazon.smithy.model.validation.suppressions.Suppression;
import software.amazon.smithy.model.validation.validators.ResourceCycleValidator;
import software.amazon.smithy.model.validation.validators.TargetValidator;
import software.amazon.smithy.utils.SetUtils;

/**
 * Validates a model, including validators and suppressions loaded from
 * traits and metadata.
 *
 * <p>Validators found in metadata and suppressions found in traits are
 * automatically created and applied to the model. Explicitly provided
 * validators are merged together with the validators and suppressions
 * loaded from metadata.
 */
final class ModelValidator {
    private static final String SUPPRESSIONS = "suppressions";

    // Lazy initialization holder class idiom to hold a default validator factory.
    private static final class LazyValidatorFactoryHolder {
        static final ValidatorFactory INSTANCE = ValidatorFactory.createServiceFactory(
                ModelAssembler.class.getClassLoader());
    }

    /** If these validators fail, then many others will too. Validate these first. */
    private static final Set<Class<? extends Validator>> CORE_VALIDATORS = SetUtils.of(
            TargetValidator.class,
            ResourceCycleValidator.class
    );

    private final List<Validator> validators = new ArrayList<>();
    private final List<Suppression> suppressions = new ArrayList<>();
    private final List<ValidationEvent> includeEvents = new ArrayList<>();
    private ValidatorFactory validatorFactory;
    private Consumer<ValidationEvent> eventListener;

    /**
     * Sets the custom {@link Validator}s to use when running the ModelValidator.
     *
     * @param validators Validators to set.
     * @return Returns the ModelValidator.
     */
    public ModelValidator validators(Collection<? extends Validator> validators) {
        this.validators.clear();
        validators.forEach(this::addValidator);
        return this;
    }

    /**
     * Adds a custom {@link Validator} to the ModelValidator.
     *
     * @param validator Validator to add.
     * @return Returns the ModelValidator.
     */
    public ModelValidator addValidator(Validator validator) {
        validators.add(Objects.requireNonNull(validator));
        return this;
    }

    /**
     * Sets the {@link Suppression}s to use with the validator.
     *
     * @param suppressions Suppressions to set.
     * @return Returns the ModelValidator.
     */
    public ModelValidator suppressions(Collection<? extends Suppression> suppressions) {
        this.suppressions.clear();
        suppressions.forEach(this::addSuppression);
        return this;
    }

    /**
     * Adds a custom {@link Suppression} to the validator.
     *
     * @param suppression Suppression to add.
     * @return Returns the ModelValidator.
     */
    public ModelValidator addSuppression(Suppression suppression) {
        suppressions.add(Objects.requireNonNull(suppression));
        return this;
    }

    /**
     * Sets the factory used to find built-in {@link Validator}s and to load
     * validators found in model metadata.
     *
     * @param validatorFactory Factory to use to load {@code Validator}s.
     *
     * @return Returns the ModelValidator.
     */
    public ModelValidator validatorFactory(ValidatorFactory validatorFactory) {
        this.validatorFactory = validatorFactory;
        return this;
    }

    /**
     * Sets a custom event listener that receives each {@link ValidationEvent}
     * as it is emitted.
     *
     * @param eventListener Event listener that consumes each event.
     * @return Returns the ModelValidator.
     */
    public ModelValidator eventListener(Consumer<ValidationEvent> eventListener) {
        this.eventListener = eventListener;
        return this;
    }

    /**
     * Includes a set of events that were already encountered in the result.
     *
     * <p>The included events may be suppressed if they match any registered
     * suppressions or suppressions loaded from the model.
     *
     * @param events Events to include.
     * @return Returns the ModelValidator.
     */
    public ModelValidator includeEvents(List<ValidationEvent> events) {
        this.includeEvents.clear();
        this.includeEvents.addAll(events);
        return this;
    }

    /**
     * Creates a reusable Model Validator that uses every registered validator,
     * suppression, and extracts validators and suppressions from each
     * provided model.
     *
     * @return Returns the created {@link Validator}.
     */
    public Validator createValidator() {
        if (validatorFactory == null) {
            validatorFactory = LazyValidatorFactoryHolder.INSTANCE;
        }

        List<Validator> staticValidators = resolveStaticValidators();
        List<ValidationEventDecorator> staticDecorators = validatorFactory.loadDecorators();

        return model -> {
            List<ValidationEvent> coreEvents = new ArrayList<>();

            // Add suppressions found in the model via metadata.
            List<Suppression> modelSuppressions = new ArrayList<>(suppressions);
            loadModelSuppressions(modelSuppressions, model, coreEvents);

            // Add validators defined in the model through metadata.
            List<Validator> modelValidators = new ArrayList<>(staticValidators);
            loadModelValidators(validatorFactory, modelValidators, model, coreEvents, modelSuppressions);

            // Perform critical validation before other more granular semantic validators.
            // If these validators fail, then many other validators will fail as well,
            // which will only obscure the root cause.
            coreEvents.addAll(suppressEvents(model, new TargetValidator().validate(model), modelSuppressions));
            coreEvents.addAll(suppressEvents(model, new ResourceCycleValidator().validate(model), modelSuppressions));
            // Decorate all the events
            coreEvents = decorateEvents(staticDecorators, coreEvents);
            // Emit any events that have already occurred.
            coreEvents.forEach(eventListener);

            if (LoaderUtils.containsErrorEvents(coreEvents)) {
                return coreEvents;
            }

            List<ValidationEvent> result = modelValidators.parallelStream()
                    .map(validator -> validator.validate(model))
                    .flatMap(Collection::stream)
                    .filter(ModelValidator::filterPrelude)
                    .map(event -> suppressEvent(model, event, modelSuppressions))
                    .map(event -> decorateEvent(staticDecorators, event))
                    // Emit events as they occur during validation.
                    .peek(eventListener)
                    .collect(Collectors.toList());

            for (ValidationEvent event : includeEvents) {
                if (ModelValidator.filterPrelude(event)) {
                    result.add(decorateEvent(staticDecorators, suppressEvent(model, event, modelSuppressions)));
                }
            }

            // Add in events encountered while building up validators and suppressions.
            result.addAll(coreEvents);

            return result;
        };
    }

    static List<ValidationEvent> decorateEvents(
        List<ValidationEventDecorator> decorators,
        List<ValidationEvent> events
    ) {
        if (!decorators.isEmpty()) {
            for (int idx = 0; idx < events.size(); idx++) {
                events.set(idx, decorateEvent(decorators, events.get(idx)));
            }
        }
        return events;
    }

    static ValidationEvent decorateEvent(List<ValidationEventDecorator> decorators, ValidationEvent event) {
        ValidationEvent decoratedEvent = event;
        for (ValidationEventDecorator decorator : decorators) {
            if (decorator.canDecorate(event)) {
                decoratedEvent = decorator.decorate(decoratedEvent);
            }
        }
        return decoratedEvent;
    }

    static ValidatorFactory defaultValidationFactory() {
        return LazyValidatorFactoryHolder.INSTANCE;
    }

    private List<Validator> resolveStaticValidators() {
        List<Validator> resolvedValidators = new ArrayList<>(validatorFactory.loadBuiltinValidators());
        resolvedValidators.addAll(validators);
        // These core validators are applied first, so don't run them again.
        resolvedValidators.removeIf(v -> CORE_VALIDATORS.contains(v.getClass()));
        return resolvedValidators;
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

    private static void loadModelValidators(
            ValidatorFactory validatorFactory,
            List<Validator> validators,
            Model model,
            List<ValidationEvent> events,
            List<Suppression> suppressions
    ) {
        // Load validators defined in metadata.
        ValidatedResult<List<ValidatorDefinition>> loaded = ValidationLoader
                .loadValidators(model.getMetadata());
        events.addAll(loaded.getValidationEvents());
        List<ValidatorDefinition> definitions = loaded.getResult().orElseGet(Collections::emptyList);
        ValidatorFromDefinitionFactory factory = new ValidatorFromDefinitionFactory(validatorFactory);

        // Attempt to create the Validator instances and collect errors along the way.
        for (ValidatorDefinition val : definitions) {
            ValidatedResult<Validator> result = factory.loadValidator(val);
            result.getResult().ifPresent(validators::add);
            events.addAll(result.getValidationEvents());
            if (result.getValidationEvents().isEmpty() && !result.getResult().isPresent()) {
                ValidationEvent event = unknownValidatorError(val.name, val.sourceLocation);
                events.add(suppressEvent(model, event, suppressions));
            }
        }
    }

    // Unknown validators don't fail the build!
    private static ValidationEvent unknownValidatorError(String name, SourceLocation location) {
        return ValidationEvent.builder()
                // Per the spec, the eventID is "UnknownValidator_<validatorName>".
                .id("UnknownValidator_" + name)
                .severity(Severity.WARNING)
                .sourceLocation(location)
                .message("Unable to locate a validator named `" + name + "`")
                .build();
    }

    private static void loadModelSuppressions(
            List<Suppression> suppressions,
            Model model,
            List<ValidationEvent> events
    ) {
        model.getMetadataProperty(SUPPRESSIONS).ifPresent(value -> {
            List<ObjectNode> values = value.expectArrayNode().getElementsAs(ObjectNode.class);
            for (ObjectNode rule : values) {
                try {
                    suppressions.add(Suppression.fromMetadata(rule));
                } catch (SourceException e) {
                    events.add(ValidationEvent.fromSourceException(e));
                }
            }
        });
    }

    private static List<ValidationEvent> suppressEvents(
            Model model,
            List<ValidationEvent> events,
            List<Suppression> suppressions) {
        return events.stream().map(event -> suppressEvent(model, event, suppressions)).collect(Collectors.toList());
    }

    private static ValidationEvent suppressEvent(Model model, ValidationEvent event, List<Suppression> suppressions) {
        // ERROR and SUPPRESSED events cannot be suppressed.
        if (!event.getSeverity().canSuppress()) {
            return event;
        }

        Suppression matchedSuppression = findMatchingSuppression(model, event, suppressions);

        if (matchedSuppression == null) {
            return event;
        }

        // The event was suppressed so change the severity and reason.
        ValidationEvent.Builder builder = event.toBuilder();
        builder.severity(Severity.SUPPRESSED);
        matchedSuppression.getReason().ifPresent(builder::suppressionReason);

        return builder.build();
    }

    private static Suppression findMatchingSuppression(
            Model model,
            ValidationEvent event,
            List<Suppression> suppressions
    ) {
        return event.getShapeId()
                .flatMap(model::getShape)
                // First check for trait based suppressions.
                .flatMap(shape -> shape.hasTrait(SuppressTrait.class)
                                  ? Optional.of(Suppression.fromSuppressTrait(shape))
                                  : Optional.empty())
                // Try to suppress it.
                .flatMap(suppression -> suppression.test(event) ? Optional.of(suppression) : Optional.empty())
                // If it wasn't suppressed, then try the rules loaded from metadata.
                .orElseGet(() -> {
                    for (Suppression suppression : suppressions) {
                        if (suppression.test(event)) {
                            return suppression;
                        }
                    }
                    return null;
                });
    }
}
