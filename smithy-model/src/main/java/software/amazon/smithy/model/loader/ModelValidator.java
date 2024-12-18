/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationEventDecorator;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.model.validation.Validator;
import software.amazon.smithy.model.validation.ValidatorFactory;
import software.amazon.smithy.model.validation.suppressions.ModelBasedEventDecorator;
import software.amazon.smithy.model.validation.validators.ResourceCycleValidator;
import software.amazon.smithy.model.validation.validators.TargetValidator;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyBuilder;

/**
 * Validates a model, including validators and suppressions loaded from traits and metadata.
 *
 * <p>Validators found in metadata and suppressions found in traits are automatically created and applied to the
 * model. Explicitly provided validators are merged with the validators and suppressions loaded from metadata.
 *
 * <p>The internal implementation of this class is broken into three parts: a builder, a validator, and a loaded
 * validator.
 *
 * <pre>
 * ModelValidator.builder().build().validate(model);
 * //            ^ creates Builder
 * //                      ^ creates ModelValidator
 * //                              ^ creates a LoadedModelValidator
 * </pre>
 *
 * <p>The builder is used to build up the customized context of the validator. ModelValidator is the created
 * {@link Validator} implementation isolated from the builder. LoadedModelValidator loads metadata from a Model and
 * performs the actual validation in an isolated context from the ModelValidator.
 */
final class ModelValidator implements Validator {

    // Lazy initialization holder class idiom to hold a default validator factory.
    private static final class LazyValidatorFactoryHolder {
        static final ValidatorFactory INSTANCE = ValidatorFactory.createServiceFactory(
                ModelAssembler.class.getClassLoader());
    }

    /** If these validators fail, then many others will too. Validate these first. */
    private static final Map<Class<?>, Validator> CORRECTNESS_VALIDATORS = MapUtils.of(
            TargetValidator.class,
            new TargetValidator(),
            ResourceCycleValidator.class,
            new ResourceCycleValidator());

    private final ValidatorFactory validatorFactory;
    private final List<ValidationEvent> events;
    private final List<Validator> validators;
    private final List<Validator> criticalValidators;
    private final ValidationEventDecorator validationEventDecorator;
    private final Consumer<ValidationEvent> eventListener;
    private final boolean legacyValidationMode;

    ModelValidator(Builder builder) {
        this.validatorFactory = builder.validatorFactory;
        this.eventListener = builder.eventListener;
        this.validationEventDecorator = builder.validationEventDecorator;
        this.events = builder.includeEvents.copy();
        this.validators = builder.validators.copy();
        this.criticalValidators = builder.criticalValidators.copy();
        this.legacyValidationMode = builder.legacyValidationMode;
    }

    @Override
    public List<ValidationEvent> validate(Model model) {
        return new LoadedModelValidator(model, this).validate();
    }

    static Builder builder() {
        return new Builder();
    }

    static ValidatorFactory defaultValidationFactory() {
        return LazyValidatorFactoryHolder.INSTANCE;
    }

    static final class Builder implements SmithyBuilder<ModelValidator> {

        private final BuilderRef<List<Validator>> validators = BuilderRef.forList();
        private final BuilderRef<List<Validator>> criticalValidators = BuilderRef.forList();
        private final BuilderRef<List<ValidationEvent>> includeEvents = BuilderRef.forList();
        private ValidatorFactory validatorFactory = LazyValidatorFactoryHolder.INSTANCE;
        private Consumer<ValidationEvent> eventListener = event -> {};
        private ValidationEventDecorator validationEventDecorator;
        private boolean legacyValidationMode = false;

        private Builder() {}

        /**
         * Adds an array of {@link Validator}s to use when running the ModelValidator.
         *
         * @param validators Validators to add.
         * @return Returns the builder.
         */
        public Builder addValidators(Collection<? extends Validator> validators) {
            for (Validator validator : validators) {
                addValidator(validator);
            }
            return this;
        }

        /**
         * Adds a {@link Validator}.
         *
         * @param validator Validator to add.
         * @return Returns the builder.
         */
        public Builder addValidator(Validator validator) {
            if (!CORRECTNESS_VALIDATORS.containsKey(validator.getClass())) {
                if (ValidationUtils.isCriticalValidator(validator.getClass())) {
                    criticalValidators.get().add(validator);
                } else {
                    validators.get().add(validator);
                }
            }
            return this;
        }

        /**
         * Sets the factory used to find built-in {@link Validator}s and to load validators found in model metadata.
         *
         * @param validatorFactory Factory to use to load {@code Validator}s.
         * @param validationEventDecorator Provide a previously loaded and composed decorator.
         * @return Returns the builder.
         */
        public Builder validatorFactory(
                ValidatorFactory validatorFactory,
                ValidationEventDecorator validationEventDecorator
        ) {
            this.validatorFactory = Objects.requireNonNull(validatorFactory);
            this.validationEventDecorator = validationEventDecorator;
            return this;
        }

        /**
         * Sets a custom event listener that receives each {@link ValidationEvent} as it is emitted.
         *
         * @param eventListener Event listener that consumes each event.
         * @return Returns the builder.
         */
        public Builder eventListener(Consumer<ValidationEvent> eventListener) {
            this.eventListener = Objects.requireNonNull(eventListener);
            return this;
        }

        /**
         * Includes a set of events that were already encountered in the result.
         *
         * <p>Suppressions and severity overrides will be applied to the given {@code events}. However, the validator
         * assumes that the event has already been decorated and the event listener has already seen the event.
         *
         * @param events Events to include.
         * @return Returns the builder.
         */
        public Builder includeEvents(List<ValidationEvent> events) {
            this.includeEvents.get().addAll(events);
            return this;
        }

        /**
         * Enables legacy validation mode that does not fail if critical Validators emit an ERROR.
         *
         * @param legacyValidationMode Set to true to enable legacy validation mode.
         * @return Returns the builder.
         */
        public Builder legacyValidationMode(boolean legacyValidationMode) {
            this.legacyValidationMode = legacyValidationMode;
            return this;
        }

        @Override
        public ModelValidator build() {
            // Adding built-in validators is deferred to allow for a custom factory to be set on the builder.
            addValidators(validatorFactory.loadBuiltinValidators());
            return new ModelValidator(this);
        }
    }

    private static final class LoadedModelValidator {

        private final Model model;
        private final List<Validator> validators;
        private final List<Validator> criticalValidators;
        private final List<ValidationEvent> events = new ArrayList<>();
        private final ValidationEventDecorator validationEventDecorator;
        private final Consumer<ValidationEvent> eventListener;
        private final boolean legacyValidationMode;

        private LoadedModelValidator(Model model, ModelValidator validator) {
            this.model = model;
            this.eventListener = validator.eventListener;
            this.validators = new ArrayList<>(validator.validators);
            this.criticalValidators = Collections.unmodifiableList(validator.criticalValidators);
            this.legacyValidationMode = validator.legacyValidationMode;

            // Suppressing and elevating events is handled by composing a given decorator with a
            // ModelBasedEventDecorator.
            ModelBasedEventDecorator modelBasedEventDecorator = new ModelBasedEventDecorator();
            ValidatedResult<ValidationEventDecorator> result = modelBasedEventDecorator.createDecorator(model);
            this.validationEventDecorator = result.getResult()
                    .map(decorator -> ValidationEventDecorator.compose(
                            ListUtils.of(decorator, validator.validationEventDecorator)))
                    .orElse(validator.validationEventDecorator);

            // Events encountered while loading suppressions and overrides have been modified by everything the
            // modelBasedEventDecorator knows about, but has not been modified by any custom decorator (if any).
            for (ValidationEvent event : result.getValidationEvents()) {
                if (validationEventDecorator.canDecorate(event)) {
                    event = validationEventDecorator.decorate(event);
                }
                events.add(event);
            }

            // Now that the decorator is available, emit/decorate/suppress/collect explicitly provided events.
            for (ValidationEvent event : validator.events) {
                pushEvent(event);
            }

            // The decorator itself doesn't handle loading and applying validators, just modifying events.
            loadModelValidators(validator.validatorFactory);
        }

        private void loadModelValidators(ValidatorFactory validatorFactory) {
            // Load validators defined in metadata.
            ValidatedResult<List<ValidatorDefinition>> loaded = ValidationLoader
                    .loadValidators(model.getMetadata());
            pushEvents(loaded.getValidationEvents());
            List<ValidatorDefinition> definitions = loaded.getResult().orElseGet(Collections::emptyList);
            ValidatorFromDefinitionFactory factory = new ValidatorFromDefinitionFactory(validatorFactory);

            // Attempt to create the Validator instances and collect errors along the way.
            for (ValidatorDefinition val : definitions) {
                ValidatedResult<Validator> result = factory.loadValidator(val);
                result.getResult().ifPresent(validators::add);
                pushEvents(result.getValidationEvents());
                if (result.getValidationEvents().isEmpty() && !result.getResult().isPresent()) {
                    ValidationEvent event = unknownValidatorError(val.name, val.sourceLocation);
                    pushEvent(event);
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

        private void pushEvents(List<ValidationEvent> source) {
            for (ValidationEvent event : source) {
                pushEvent(event);
            }
        }

        private void pushEvent(ValidationEvent event) {
            events.add(updateAndEmitEvent(event));
        }

        private ValidationEvent updateAndEmitEvent(ValidationEvent event) {
            if (validationEventDecorator.canDecorate(event)) {
                event = validationEventDecorator.decorate(event);
            }
            eventListener.accept(event);
            return event;
        }

        private List<ValidationEvent> validate() {
            // Perform critical correctness validation before other critical validators.
            events.addAll(streamEvents(CORRECTNESS_VALIDATORS.values().stream()));
            if (LoaderUtils.containsErrorEvents(events)) {
                return events;
            }

            // Same thing, but for other critical validators.
            events.addAll(streamEvents(criticalValidators.parallelStream()));

            // Only fail early here if legacy validation mode is enabled.
            if (!legacyValidationMode && LoaderUtils.containsErrorEvents(events)) {
                return events;
            }

            events.addAll(streamEvents(validators.parallelStream()));
            return events;
        }

        private List<ValidationEvent> streamEvents(Stream<Validator> validators) {
            return validators
                    .flatMap(validator -> validator.validate(model).stream())
                    .filter(this::filterPrelude)
                    .map(this::updateAndEmitEvent)
                    .collect(Collectors.toList());
        }

        private boolean filterPrelude(ValidationEvent event) {
            // Don't emit any non-error events for prelude shapes and traits.
            // This prevents custom validators from unnecessarily needing to worry about prelude shapes and trait
            // definitions, but still allows for validation events when the prelude is broken.
            return event.getSeverity() == Severity.ERROR || !event.getShapeId()
                    .filter(Prelude::isPreludeShape)
                    .isPresent();
        }
    }
}
