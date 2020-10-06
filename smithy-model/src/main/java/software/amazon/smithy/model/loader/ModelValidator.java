/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.SuppressTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Validator;
import software.amazon.smithy.model.validation.ValidatorFactory;
import software.amazon.smithy.utils.ListUtils;

/**
 * Validates a model, including validators and suppressions loaded from
 * traits.
 *
 * <p>ModelValidator is used to apply validation to a loaded Model. This
 * class is used in tandem with {@link ModelAssembler}.
 *
 * <p>Validators found in metadata and suppressions found in traits are
 * automatically created and applied to the model. Explicitly provided
 * validators are merged together with the validators and suppressions
 * loaded from metadata.
 */
final class ModelValidator {

    private static final String SUPPRESSIONS = "suppressions";
    private static final String ID = "id";
    private static final String NAMESPACE = "namespace";
    private static final String REASON = "reason";
    private static final String STAR = "*";
    private static final String EMPTY_REASON = "";
    private static final Collection<String> SUPPRESSION_KEYS = ListUtils.of(ID, NAMESPACE, REASON);

    private final List<Validator> validators;
    private final ArrayList<ValidationEvent> events = new ArrayList<>();
    private final ValidatorFactory validatorFactory;
    private final Model model;
    private final Map<String, Map<String, String>> namespaceSuppressions = new HashMap<>();

    private ModelValidator(
            Model model,
            ValidatorFactory validatorFactory,
            List<Validator> validators
    ) {
        this.model = model;
        this.validatorFactory = validatorFactory;
        this.validators = new ArrayList<>(validators);
    }

    /**
     * Validates the given Model using validators configured explicitly and
     * detected through metadata.
     *
     * @param model Model to validate.
     * @param validatorFactory Factory used to find ValidatorService providers.
     * @param validators Additional validators to use.
     * @return Returns the encountered validation events.
     */
    static List<ValidationEvent> validate(
            Model model,
            ValidatorFactory validatorFactory,
            List<Validator> validators
    ) {
        return new ModelValidator(model, validatorFactory, validators).doValidate();
    }

    private List<ValidationEvent> doValidate() {
        assembleNamespaceSuppressions();
        List<ValidatorDefinition> assembledValidatorDefinitions = assembleValidatorDefinitions();
        assembleValidators(assembledValidatorDefinitions);

        List<ValidationEvent> result = validators
                .parallelStream()
                .flatMap(validator -> validator.validate(model).stream())
                .map(this::suppressEvent)
                .filter(ModelValidator::filterPrelude)
                .collect(Collectors.toList());

        // Add in events encountered while building up validators and suppressions.
        result.addAll(events);

        return result;
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
                events.add(suppressEvent(unknownValidatorError(val.name, val.sourceLocation)));
            }
        }
    }

    // Unknown validators don't fail the build!
    private ValidationEvent unknownValidatorError(String name, SourceLocation location) {
        return ValidationEvent.builder()
                // Per the spec, the eventID is "UnknownValidator_<validatorName>".
                .id("UnknownValidator_" + name)
                .severity(Severity.WARNING)
                .sourceLocation(location)
                .message("Unable to locate a validator named `" + name + "`")
                .build();
    }

    // Find all namespace suppressions.
    private void assembleNamespaceSuppressions() {
        model.getMetadataProperty(SUPPRESSIONS).ifPresent(value -> {
            List<ObjectNode> values = value.expectArrayNode().getElementsAs(ObjectNode.class);
            for (ObjectNode rule : values) {
                rule.warnIfAdditionalProperties(SUPPRESSION_KEYS);
                String id = rule.expectStringMember(ID).getValue();
                String namespace = rule.expectStringMember(NAMESPACE).getValue();
                String reason = rule.getStringMemberOrDefault(REASON, EMPTY_REASON);
                namespaceSuppressions.computeIfAbsent(id, i -> new HashMap<>()).put(namespace, reason);
            }
        });
    }

    private ValidationEvent suppressEvent(ValidationEvent event) {
        // ERROR and SUPPRESSED events cannot be suppressed.
        if (!event.getSeverity().canSuppress()) {
            return event;
        }

        String reason = resolveReason(event);

        // The event is not suppressed, return as-is.
        if (reason == null) {
            return event;
        }

        // The event was suppressed so change the severity and reason.
        ValidationEvent.Builder builder = event.toBuilder();
        builder.severity(Severity.SUPPRESSED);
        if (!reason.equals(EMPTY_REASON)) {
            builder.suppressionReason(reason);
        }

        return builder.build();
    }

    // Get the reason as a String if it is suppressed, or null otherwise.
    private String resolveReason(ValidationEvent event) {
        return event.getShapeId()
                .flatMap(model::getShape)
                .flatMap(shape -> matchSuppression(shape, event.getId()))
                // This is always evaluated if a reason hasn't been found.
                .orElseGet(() -> matchWildcardNamespaceSuppressions(event.getId()));
    }

    private Optional<String> matchSuppression(Shape shape, String eventId) {
        // Traits take precedent over service suppressions.
        if (shape.getTrait(SuppressTrait.class).isPresent()) {
            if (shape.expectTrait(SuppressTrait.class).getValues().contains(eventId)) {
                // The "" is filtered out before being passed to the
                // updated ValidationEvent.
                return Optional.of(EMPTY_REASON);
            }
        }

        // Check namespace-wide suppressions.
        if (namespaceSuppressions.containsKey(eventId)) {
            Map<String, String> namespaces = namespaceSuppressions.get(eventId);
            if (namespaces.containsKey(shape.getId().getNamespace())) {
                return Optional.of(namespaces.get(shape.getId().getNamespace()));
            }
        }

        return Optional.empty();
    }

    private String matchWildcardNamespaceSuppressions(String eventId) {
        if (namespaceSuppressions.containsKey(eventId)) {
            Map<String, String> namespaces = namespaceSuppressions.get(eventId);
            if (namespaces.containsKey(STAR)) {
                return namespaces.get(STAR);
            }
        }

        return null;
    }
}
