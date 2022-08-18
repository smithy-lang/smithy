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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.SensitiveTrait;
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
    static final Set<String> DEFAULT_SENSITIVE_WORDS = SetUtils.of(
            "authentication",
            "authorization",
            "bank",
            "billing",
            "birth",
            "credential",
            "email",
            "ethnicity",
            "insurance",
            "license",
            "passkey",
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
    static final Set<String> DEFAULT_SENSITIVE_PHRASES = SetUtils.of(
            "accesskey",
            "accesstoken",
            "auth",
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
        private List<String> words = ListUtils.of();

        private boolean excludeDefaults;

        public List<String> getPhrases() {
            return phrases;
        }

        public void setPhrases(List<String> phrases) {
            this.phrases = phrases;
        }

        public List<String> getWords() {
            return words;
        }

        public void setWords(List<String> words) {
            this.words = words;
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
            Set<String> wordsInit = new HashSet<>(DEFAULT_SENSITIVE_WORDS);
            phrasesInit.addAll(config.getPhrases()
                    .stream()
                    .map(phrase -> phrase.toLowerCase(Locale.US))
                    .collect(Collectors.toSet()));
            wordsInit.addAll(config.getWords()
                    .stream()
                    .map(word -> word.toLowerCase(Locale.US))
                    .collect(Collectors.toSet()));
            sensitivePhrases = Collections.unmodifiableSet(phrasesInit);
            sensitiveWords = Collections.unmodifiableSet(wordsInit);
        } else {
            if (config.getPhrases().isEmpty() && config.getWords().isEmpty()) {
                //This configuration combination makes the validator a no-op.
                throw new IllegalArgumentException("Cannot set 'excludeDefaults' to true and leave "
                                                 + "both 'phrases' and 'words' unspecified.");
            }
            sensitivePhrases = Collections.unmodifiableSet(new HashSet<>(
                    config.getPhrases()
                            .stream()
                            .map(phrase -> phrase.toLowerCase(Locale.US))
                            .collect(Collectors.toSet()))
            );
            sensitiveWords = Collections.unmodifiableSet(new HashSet<>(
                    config.getWords()
                            .stream()
                            .map(word -> word.toLowerCase(Locale.US))
                            .collect(Collectors.toSet())));
        }
    }

    /**
     * Finds shapes without the sensitive trait that possibly contain sensitive data,
     * based on the shape/member name and the list of key words and phrases.
     *
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
        return model.shapes()
                .filter(shape -> !shape.isMemberShape()
                        && !shape.isOperationShape()
                        && !shape.isServiceShape()
                        && !shape.isResourceShape())
                .filter(shape -> !shape.hasTrait(SensitiveTrait.ID))
                .map(shape -> detectSensitiveWord(shape.toShapeId().getName(), shape).map(Optional::of)
                        .orElse(detectSensitivePhrase(shape.toShapeId().getName(), shape)))
                .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
//                Once we finally upgrade from Java 8, above line can be simplified to
//                .flatMap(Optional::stream)
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> scanMemberNames(Model model) {
        return model.shapes()
                // filter out members with an already sensitive enclosing shape
                .filter(shape -> !shape.hasTrait(SensitiveTrait.ID))
                .flatMap(shape -> shape.members().stream())
                // filter out members that target a sensitive shape
                .filter(memberShape ->
                        model.getShape(memberShape.getTarget())
                        .map(shape -> !shape.hasTrait(SensitiveTrait.ID))
                        .orElse(false))
                .map(memberShape -> detectSensitiveWord(memberShape.getMemberName(), memberShape).map(Optional::of)
                        .orElse(detectSensitivePhrase(memberShape.getMemberName(), memberShape)))
                .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
//                Once we finally upgrade from Java 8, above line can be simplified to
//                .flatMap(Optional::stream)
                .collect(Collectors.toList());
    }

    private Optional<ValidationEvent> detectSensitiveWord(String name, Shape shape) {
        return ValidationUtils.splitCamelCaseWord(name)
                .stream()
                .map(word -> word.toLowerCase(Locale.US))
                .filter(sensitiveWords::contains)
                .findAny()
                .map(word -> emit(shape, word));
    }

    private Optional<ValidationEvent> detectSensitivePhrase(String name, Shape shape) {
        String lowerCasedName = name.toLowerCase(Locale.US);
        return sensitivePhrases.stream()
                .filter(lowerCasedName::contains)
                .findAny()
                .map(phrase -> emit(shape, phrase));
    }

    private ValidationEvent emit(Shape shape, String word) {
        return ValidationEvent.builder()
                .severity(Severity.WARNING)
                .id(ValidatorService.determineValidatorName(MissingSensitiveTraitValidator.class))
                .shape(shape)
                .message(String.format("Detected that this shape possibly contains sensitive data "
                        + "(based on presence of '%s') but is not marked with the sensitive trait", word))
                .build();
    }
}
