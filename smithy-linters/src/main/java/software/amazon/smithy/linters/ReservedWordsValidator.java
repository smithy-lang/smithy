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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidatorService;
import software.amazon.smithy.utils.OptionalUtils;

/**
 * Emits validation events for a configuration of reserved words.
 *
 * <p>This validator accepts the following optional configuration options:
 *
 * <ul>
 *     <li>reserved: ([object]) A list of reserved word configuration
 *     objects as follows:
 *         <ul>
 *             <li>words: ([string]) A list of words that are
 *             case-insensitively reserved. Leading and trailing wildcards
 *             ("*") are supported.
 *             <li>selector: (string) Specifies a selector for this
 *             configuration. Defaults to validating all shapes, including
 *             member names.
 *             <li>reason: (string) A reason to display for why this set of
 *             words is reserved.
 *         </ul>
 *     </li>
 * </ul>
 */
public final class ReservedWordsValidator extends AbstractValidator {

    /**
     * ReservedWords validator configuration.
     */
    public static final class Config {
        private List<ReservedWords> reserved = Collections.emptyList();

        public List<ReservedWords> getReserved() {
            return reserved;
        }

        /**
         * Sets the reserved words to validate.
         *
         * @param reserved Reserved words to set.
         */
        public void setReserved(List<ReservedWords> reserved) {
            this.reserved = reserved;
        }
    }

    /**
     * A single reserved words configuration.
     */
    public static final class ReservedWords {
        private List<String> words = Collections.emptyList();
        private Selector selector = Selector.IDENTITY;
        private String reason = "";

        /**
         * Sets the list of reserved word definitions.
         *
         * <p>Each word must be a valid word. The word cannot equal "*", and if present,
         * "*", must appear at the start or end of the word.
         *
         * @param words Words to set.
         */
        public void setWords(List<String> words) {
            this.words = new ArrayList<>(words.size());
            for (String word : words) {
                if (word.equals("*")) {
                    throw new IllegalArgumentException("Reservations cannot be made against '*'");
                }
                if (CONTAINS_INNER_WILDCARD.matcher(word).find()) {
                    throw new IllegalArgumentException("Only preceding and trailing wildcards ('*') are supported.");
                }
                this.words.add(word.toLowerCase(Locale.ENGLISH));
            }
        }

        /**
         * Sets the selector to use for determining which shapes to validate.
         *
         * @param selector Selector to set.
         */
        public void setSelector(Selector selector) {
            this.selector = selector;
        }

        /**
         * Sets the reason for why the words are reserved.
         *
         * @param reason Reason to set.
         */
        public void setReason(String reason) {
            this.reason = reason;
        }

        private Stream<ValidationEvent> validate(Model model) {
            return selector.select(model).stream().flatMap(shape -> OptionalUtils.stream(validateShape(shape)));
        }

        private Optional<ValidationEvent> validateShape(Shape shape) {
            String name = shape.asMemberShape()
                    .map(MemberShape::getMemberName)
                    .orElseGet(() -> shape.getId().getName());

            return isReservedWord(name) ? Optional.of(emit(shape, name, reason)) : Optional.empty();
        }

        /**
         * Checks a passed word against the list of reserved words in this
         * configuration. Validates these in a case-insensitive manner, and
         * supports starting and ending wildcards '*'.
         *
         * @param word A value that may be reserved.
         * @return Returns true if the word is reserved by this configuration
         */
        private boolean isReservedWord(String word) {
            String compare = word.toLowerCase(Locale.US);
            return words.stream().anyMatch(reservation -> {
                // Comparisons against '*' have been rejected at configuration load.
                if (reservation.startsWith("*")) {
                    if (reservation.endsWith("*")) {
                        return compare.contains(reservation.substring(1, reservation.lastIndexOf("*")));
                    }
                    return compare.endsWith(reservation.substring(1));
                }
                if (reservation.endsWith("*")) {
                    return compare.startsWith(reservation.substring(0, reservation.lastIndexOf("*")));
                }
                return compare.equals(reservation);
            });
        }

        private ValidationEvent emit(Shape shape, String word, String reason) {
            return ValidationEvent.builder()
                    .severity(Severity.DANGER)
                    .id(ValidatorService.determineValidatorName(ReservedWordsValidator.class))
                    .shape(shape)
                    .message(format("The word `%s` is reserved. %s", word, reason))
                    .build();
        }
    }

    public static final class Provider extends ValidatorService.Provider {
        public Provider() {
            super(ReservedWordsValidator.class, node -> {
                NodeMapper mapper = new NodeMapper();
                return new ReservedWordsValidator(mapper.deserialize(node, Config.class));
            });
        }
    }

    private static final Pattern CONTAINS_INNER_WILDCARD = Pattern.compile("^.+\\*.+$");

    private final Config config;

    private ReservedWordsValidator(Config config) {
        this.config = config;

        if (config.getReserved().isEmpty()) {
            throw new IllegalArgumentException("Missing `reserved` words");
        }
    }

    @Override
    public List<ValidationEvent> validate(Model model) {
        return config.getReserved().stream().flatMap(reservation -> reservation.validate(model))
                .collect(Collectors.toList());
    }
}
