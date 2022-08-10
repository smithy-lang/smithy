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

package software.amazon.smithy.linters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.model.validation.ValidatorService;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SetUtils;

/**
 * <p>Validates that shapes and members that possibly contain sensitive data are marked with the sensitive trait.
 */
public final class MissingSensitiveTraitValidator extends AbstractValidator {
    public static final String SENSITIVE = "sensitive";
    static final Set<String> DEFAULT_SENSITIVE_WORDS = SetUtils.of(
            "authorization",
            "bank",
            "billing",
            "birth",
            "credential",
            "email",
            "ethnicity",
            "insurance",
            "license",
            "passphrase",
            "passport",
            "password",
            "phone",
            "private",
            "secret",
            "sensitive",
            "telephone",
            "token",
            "username",
            "zip"
    );
    // "Phrases" will be searched for without regard to word boundaries. (This is so that user configuration )
    static final Set<String> DEFAULT_SENSITIVE_PHRASES = SetUtils.of(
            "accesskey",
            "creditcard",
            "firstname",
            "lastname",
            "plaintext",
            "taxpayer"
    );

    public static final class Provider extends ValidatorService.Provider {
        public Provider() {
            super(MissingSensitiveTraitValidator.class, node -> {
                NodeMapper mapper = new NodeMapper();
                return new MissingSensitiveTraitValidator(
                        mapper.deserialize(node, MissingSensitiveTraitValidator.Config.class));
            });
        }
    }

    /**
     * MissingSensitiveTrait configuration.
     */
    public static final class Config {
        private List<String> phrases = ListUtils.of();
        private boolean excludeDefaults;

        public List<String> getPhrases() {
            return phrases;
        }

        public void setPhrases(List<String> phrases) {
            this.phrases = phrases;
        }

        public boolean getExcludeDefaults() {
            return excludeDefaults;
        }

        public void setExcludeDefaults(boolean excludeDefaults) {
            this.excludeDefaults = excludeDefaults;
        }
    }

    private final Set<String> sensitiveWords;
    private final Set<String> sensitivePhrases;

    private MissingSensitiveTraitValidator(Config config) {
        if (!config.getExcludeDefaults()) {
            Set<String> phrasesInit = new HashSet<>(DEFAULT_SENSITIVE_PHRASES);
            phrasesInit.addAll(config.getPhrases()
                    .stream()
                    .map(phrase -> phrase.toLowerCase(Locale.US))
                    .collect(Collectors.toSet()));
            sensitivePhrases = Collections.unmodifiableSet(phrasesInit);
            sensitiveWords = Collections.unmodifiableSet(DEFAULT_SENSITIVE_WORDS);
        } else {
            if (config.getPhrases().isEmpty()) {
                //This configuration combination makes the validator a no-op.
                throw new IllegalArgumentException("Cannot set 'excludeDefaults' to true and leave "
                                                 + "'phrases' empty or unspecified.");
            }
            sensitivePhrases = Collections.unmodifiableSet(new HashSet<>(config.getPhrases()));
            sensitiveWords = Collections.EMPTY_SET;
        }
    }

    /**
     * Finds shapes without the sensitive trait that possibly contain sensitive data,
     * based on the shape/member name and the list of key words and phrases.
     * @param model Model to validate.
     * @return list of violation events
     */
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> validationEvents = new ArrayList<>();
        validationEvents.addAll(scanShapeNames(model));
        validationEvents.addAll(scanMemberNames(model));
        return validationEvents;
    }

    private List<ValidationEvent> scanShapeNames(Model model) {
        List<ValidationEvent> validationEvents = new ArrayList<>();
        model.shapes()
                .filter(shape -> !shape.isMemberShape()
                        && !shape.isOperationShape()
                        && !shape.isServiceShape()
                        && !shape.isResourceShape())
                .filter(shape -> !shape.hasTrait(SENSITIVE))
                .filter(shape -> containsSensitiveWord(shape.toShapeId().getName())
                        || containsSensitivePhrase(shape.toShapeId().getName()))
                .forEach(shape -> validationEvents.add(emit(shape)));

        return validationEvents;
    }

    private List<ValidationEvent> scanMemberNames(Model model) {
        return model.shapes()
                // filter out members with an already sensitive enclosing shape
                .filter(shape -> !shape.hasTrait(SENSITIVE))
                .flatMap(shape -> shape.members().stream())
                // filter out members that target a sensitive shape
                .filter(memberShape ->
                        model.getShape(memberShape.getTarget())
                        .map(shape -> !shape.hasTrait(SENSITIVE))
                        .orElse(false))
                .filter(memberShape -> containsSensitiveWord(memberShape.getMemberName())
                        || containsSensitivePhrase(memberShape.getMemberName()))
                .map(this::emit)
                .collect(Collectors.toList());
    }

    private boolean containsSensitiveWord(String name) {
        return ValidationUtils.splitCamelCaseWord(name)
                .stream()
                .map(word -> word.toLowerCase(Locale.US))
                .anyMatch(sensitiveWords::contains);
    }

    private boolean containsSensitivePhrase(String name) {
        String lowerCasedName = name.toLowerCase(Locale.US);
        return sensitivePhrases.stream().anyMatch(lowerCasedName::contains);
    }

    private ValidationEvent emit(Shape shape) {
        return ValidationEvent.builder()
                .severity(Severity.WARNING)
                .id(ValidatorService.determineValidatorName(MissingSensitiveTraitValidator.class))
                .shape(shape)
                .message("Detected that this shape possibly contains sensitive data "
                        + "but is not marked with the 'sensitive' trait")
                .build();
    }
}
