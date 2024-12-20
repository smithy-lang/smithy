/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.linters;

import static java.lang.String.format;
import static software.amazon.smithy.model.validation.ValidationUtils.splitCamelCaseWord;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidatorService;
import software.amazon.smithy.utils.ListUtils;

/**
 * Emits a validation event if shapes or member names do not use strict
 * camelCasing (e.g., XmlRequest is preferred over XMLRequest).
 */
public final class AbbreviationNameValidator extends AbstractValidator {

    /**
     * AbbreviationName configuration settings.
     */
    public static final class Config {
        private List<String> allowedAbbreviations = Collections.emptyList();

        /**
         * A list of abbreviation strings to permit.
         *
         * @return Returns the allowed abbreviations.
         */
        public List<String> getAllowedAbbreviations() {
            return allowedAbbreviations;
        }

        public void setAllowedAbbreviations(List<String> allowedAbbreviations) {
            this.allowedAbbreviations = ListUtils.copyOf(allowedAbbreviations);
        }
    }

    public static final class Provider extends ValidatorService.Provider {
        public Provider() {
            super(AbbreviationNameValidator.class, configuration -> {
                NodeMapper mapper = new NodeMapper();
                return new AbbreviationNameValidator(mapper.deserialize(configuration, Config.class));
            });
        }
    }

    private Config config;

    private AbbreviationNameValidator(Config config) {
        this.config = config;
    }

    @Override
    public List<ValidationEvent> validate(Model model) {
        return model.shapes()
                .flatMap(shape -> validateShapeName(model, shape))
                .collect(Collectors.toList());
    }

    private Stream<ValidationEvent> validateShapeName(Model model, Shape shape) {
        // Exclude members of enums from AbbreviationName validation,
        // as they're intended to be CAPS_SNAKE.
        if (shape.isMemberShape()) {
            Shape container = model.expectShape(shape.asMemberShape().get().getContainer());
            if (container.isEnumShape() || container.isIntEnumShape()) {
                return Stream.empty();
            }
        }

        String descriptor = shape.isMemberShape() ? "member" : "shape";
        String name = shape.asMemberShape()
                .map(MemberShape::getMemberName)
                .orElseGet(() -> shape.getId().getName());

        String recommendedName = createRecommendedName(name);
        if (recommendedName.equals(name)) {
            return Stream.empty();
        }

        return Stream.of(danger(shape,
                format(
                        "%s name, `%s`, contains invalid abbreviations. Change this %s name to `%s`",
                        descriptor,
                        name,
                        descriptor,
                        recommendedName)));
    }

    private String createRecommendedName(String name) {
        // Build up the recommended name which will be compared against the actual name.
        StringBuilder recommendedWordBuilder = new StringBuilder();
        for (String word : splitCamelCaseWord(name)) {
            if (config.getAllowedAbbreviations().contains(word)) {
                // Leave allowed abbreviations as-is in the recommended word.
                recommendedWordBuilder.append(word);
            } else if (!isInvalidWord(word)) {
                recommendedWordBuilder.append(word);
            } else {
                recommendedWordBuilder.append(word.substring(0, 1).toUpperCase(Locale.US));
                recommendedWordBuilder.append(word.substring(1).toLowerCase(Locale.US));
            }
        }

        return recommendedWordBuilder.toString();
    }

    private boolean isInvalidWord(String word) {
        return word.chars().filter(c -> c >= 'A' && c <= 'Z').count() > 1;
    }
}
