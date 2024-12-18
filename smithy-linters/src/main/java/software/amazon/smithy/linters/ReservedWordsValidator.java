/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.linters;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidatorService;

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
 *             <li>terms: ([string]) A list of word boundary terms to test.</li>
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
        private Selector selector = Selector.IDENTITY;
        private String reason = "";
        private final WildcardMatcher wildcardMatcher = new WildcardMatcher();
        private final WordBoundaryMatcher wordMatcher = new WordBoundaryMatcher();

        /**
         * Sets the list of reserved word definitions.
         *
         * <p>Each word must be a valid word. The word cannot equal "*", and if present,
         * "*", must appear at the start or end of the word.
         *
         * @param words Words to set.
         */
        public void setWords(List<String> words) {
            words.forEach(wildcardMatcher::addSearch);
        }

        /**
         * Sets the list of reserved word terms to match based on word boundaries.
         *
         * @param terms Terms to set.
         */
        public void setTerms(List<String> terms) {
            terms.forEach(wordMatcher::addSearch);
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

        private void validate(Model model, List<ValidationEvent> events) {
            for (Shape shape : selector.select(model)) {
                validateShape(shape).ifPresent(events::add);
            }
        }

        private Optional<ValidationEvent> validateShape(Shape shape) {
            String name = shape.asMemberShape()
                    .map(MemberShape::getMemberName)
                    .orElseGet(() -> shape.getId().getName());

            return isReservedWord(name) ? Optional.of(emit(shape, name, reason)) : Optional.empty();
        }

        /**
         * Checks a passed word against the reserved words in this configuration.
         *
         * @param word A value that may be reserved.
         * @return Returns true if the word is reserved by this configuration
         */
        private boolean isReservedWord(String word) {
            return wildcardMatcher.test(word) || wordMatcher.test(word);
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

    private final Config config;

    private ReservedWordsValidator(Config config) {
        this.config = config;
    }

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (ReservedWords reserved : config.getReserved()) {
            reserved.validate(model, events);
        }
        return events;
    }
}
