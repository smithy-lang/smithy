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
import static software.amazon.smithy.model.validation.ValidationUtils.splitCamelCaseWord;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidatorService;

/**
 * Emits a validation event if shapes or member names do not use strict
 * camelCasing (e.g., XmlRequest is preferred over XMLRequest).
 *
 * <p>This validator accepts the following optional configuration options:
 *
 * <ul>
 *     <li>allowedAbbreviations: ([string]) A list of abbreviation strings
 *      to permit.
 * </ul>
 */
public final class AbbreviationNameValidator extends AbstractValidator {
    private List<String> allowedAbbreviations;

    private AbbreviationNameValidator(List<String> allowedAbbreviations) {
        this.allowedAbbreviations = allowedAbbreviations;
    }

    public static final class Provider extends ValidatorService.Provider {
        public Provider() {
            super(AbbreviationNameValidator.class, configuration -> new AbbreviationNameValidator(
                    configuration.getArrayMember("allowedAbbreviations")
                            .map(arr -> arr.getElementsAs(StringNode::getValue))
                            .orElseGet(Collections::emptyList)));
        }
    }

    @Override
    public List<ValidationEvent> validate(Model model) {
        return model.shapes()
                .flatMap(this::validateShapeName)
                .collect(Collectors.toList());
    }

    private Stream<ValidationEvent> validateShapeName(Shape shape) {
        String descriptor = shape.isMemberShape() ? "member" : "shape";
        String name = shape.asMemberShape()
                .map(MemberShape::getMemberName)
                .orElseGet(() -> shape.getId().getName());

        String recommendedName = createRecommendedName(name);
        if (recommendedName.equals(name)) {
            return Stream.empty();
        }

        return Stream.of(danger(shape, format(
                "%s name, `%s`, contains invalid abbreviations. Change this %s name to `%s`",
                descriptor, name, descriptor, recommendedName)));
    }

    private String createRecommendedName(String name) {
        // Build up the recommended name which will be compared against the actual name.
        StringBuilder recommendedWordBuilder = new StringBuilder();
        for (String word : splitCamelCaseWord(name)) {
            if (allowedAbbreviations.contains(word)) {
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
