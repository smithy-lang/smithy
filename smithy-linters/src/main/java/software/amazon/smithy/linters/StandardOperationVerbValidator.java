/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.linters;

import static java.lang.String.format;
import static software.amazon.smithy.model.validation.ValidationUtils.splitCamelCaseWord;
import static software.amazon.smithy.model.validation.ValidationUtils.tickedList;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidatorService;
import software.amazon.smithy.utils.OptionalUtils;

/**
 * <p>Validates that operation shape names start with standard verbs.
 *
 * <p>Looks at each operation shape name and determines if the first
 * word in the shape name is one of the pre-defined allowed verbs.
 *
 * <p>If the verb is a key in the map of verbs that have recommended
 * alternatives, then the event that is emitted contains the list of
 * alternate verbs to use.
 */
public final class StandardOperationVerbValidator extends AbstractValidator {

    /**
     * StandardOperationVerb configuration settings.
     */
    public static final class Config {
        private List<String> verbs = Collections.emptyList();
        private List<String> prefixes = Collections.emptyList();
        private Map<String, List<String>> suggestAlternatives = Collections.emptyMap();

        public List<String> getVerbs() {
            return verbs;
        }

        /**
         * Sets the list of standard operation verbs.
         *
         * @param verbs Verbs to allow.
         */
        public void setVerbs(List<String> verbs) {
            this.verbs = verbs;
        }

        public List<String> getPrefixes() {
            return prefixes;
        }

        /**
         * Sets the list of acceptable verb prefixed (e.g., Batch).
         *
         * @param prefixes The list of verb prefixes.
         */
        public void setPrefixes(List<String> prefixes) {
            this.prefixes = prefixes;
        }

        public Map<String, List<String>> getSuggestAlternatives() {
            return suggestAlternatives;
        }

        /**
         * Sets a map of invalid verbs to suggested alternatives.
         *
         * @param suggestAlternatives Alternative verb suggestions.
         */
        public void setSuggestAlternatives(Map<String, List<String>> suggestAlternatives) {
            this.suggestAlternatives = suggestAlternatives;
        }
    }

    public static final class Provider extends ValidatorService.Provider {
        public Provider() {
            super(StandardOperationVerbValidator.class, node -> {
                Config config = new NodeMapper().deserialize(node, Config.class);
                if (config.getVerbs().isEmpty() && config.getSuggestAlternatives().isEmpty()) {
                    throw new SourceException("Either verbs or suggestAlternatives must be set when configuring "
                            + "StandardOperationVerb", node);
                }
                return new StandardOperationVerbValidator(config);
            });
        }
    }

    private final Config config;

    private StandardOperationVerbValidator(Config config) {
        this.config = config;
    }

    @Override
    public List<ValidationEvent> validate(Model model) {
        return model.shapes(OperationShape.class)
                .flatMap(shape -> OptionalUtils.stream(validateShape(shape, config)))
                .collect(Collectors.toList());
    }

    private Optional<ValidationEvent> validateShape(OperationShape operation, Config config) {
        List<String> words = splitCamelCaseWord(operation.getId().getName());
        String name;
        String foundPrefix = null;

        // Detect and remove any single prefix if needed.
        if (!config.getPrefixes().contains(words.get(0))) {
            name = words.get(0);
        } else if (words.size() == 1) {
            return Optional.of(danger(operation,
                    format(
                            "Operation name consists of only a verb prefix: %s",
                            operation.getId().getName())));
        } else {
            foundPrefix = words.get(0);
            name = words.get(1);
        }

        if (config.getSuggestAlternatives().containsKey(name)) {
            return Optional.of(danger(operation,
                    format(
                            "%s Consider using one of the following verbs instead: %s",
                            createMessagePrefix(operation, name, foundPrefix),
                            tickedList(config.getSuggestAlternatives().get(name)))));
        } else if (!config.getVerbs().contains(name)) {
            return Optional.of(danger(operation,
                    format(
                            "%s Expected one of the following verbs: %s",
                            createMessagePrefix(operation, name, foundPrefix),
                            tickedList(config.getVerbs()))));
        } else {
            return Optional.empty();
        }
    }

    private static String createMessagePrefix(Shape shape, String name, String prefix) {
        StringBuilder builder = new StringBuilder();
        builder.append(format("Operation shape `%s` uses a non-standard verb, `%s`", shape.getId().getName(), name));
        if (prefix != null) {
            builder.append(format(", with a detected prefix of `%s`", prefix));
        }
        return builder.append(".").toString();
    }
}
