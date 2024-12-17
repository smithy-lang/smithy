/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.linters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.knowledge.TextIndex;
import software.amazon.smithy.model.knowledge.TextInstance;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.model.validation.ValidatorService;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.StringUtils;

/**
 * <p>Validates that all shape names and values do not contain non-inclusive terms.
 */
public final class NoninclusiveTermsValidator extends AbstractValidator {
    static final Map<String, List<String>> BUILT_IN_NONINCLUSIVE_TERMS = MapUtils.of(
            "master",
            ListUtils.of("primary", "parent", "main"),
            "slave",
            ListUtils.of("secondary", "replica", "clone", "child"),
            "blacklist",
            ListUtils.of("denyList"),
            "whitelist",
            ListUtils.of("allowList"));

    public static final class Provider extends ValidatorService.Provider {
        public Provider() {
            super(NoninclusiveTermsValidator.class, node -> {
                NodeMapper mapper = new NodeMapper();
                return new NoninclusiveTermsValidator(
                        mapper.deserialize(node, NoninclusiveTermsValidator.Config.class));
            });
        }
    }

    private static final String TRAIT = "Trait";
    private static final String SHAPE = "Shape";
    private static final String NAMESPACE = "Namespace";

    /**
     * NoninclusiveTermsValidator configuration.
     */
    public static final class Config {
        private Map<String, List<String>> terms = MapUtils.of();
        private boolean excludeDefaults;

        public Map<String, List<String>> getTerms() {
            return terms;
        }

        public void setTerms(Map<String, List<String>> terms) {
            this.terms = terms;
        }

        public boolean getExcludeDefaults() {
            return excludeDefaults;
        }

        public void setExcludeDefaults(boolean excludeDefaults) {
            this.excludeDefaults = excludeDefaults;
        }
    }

    private final Map<String, List<String>> termsMap;

    private NoninclusiveTermsValidator(Config config) {
        Map<String, List<String>> termsMapInit = new HashMap<>(BUILT_IN_NONINCLUSIVE_TERMS);
        if (!config.getExcludeDefaults()) {
            termsMapInit.putAll(config.getTerms());
            termsMap = Collections.unmodifiableMap(termsMapInit);
        } else {
            if (config.getTerms().isEmpty()) {
                //This configuration combination makes the validator a no-op.
                throw new IllegalArgumentException("Cannot set 'excludeDefaults' to true and leave "
                        + "'terms' empty or unspecified.");
            }
            termsMap = Collections.unmodifiableMap(config.getTerms());
        }
    }

    /**
     * Runs a full text scan on a given model and stores the resulting TextOccurrences objects.
     *
     * Namespaces are checked against a global set per model.
     *
     * @param model Model to validate.
     * @return a list of ValidationEvents found by the implementer of getValidationEvents per the
     *          TextOccurrences provided by this traversal.
     */
    @Override
    public List<ValidationEvent> validate(Model model) {
        TextIndex textIndex = TextIndex.of(model);
        List<ValidationEvent> validationEvents = new ArrayList<>();
        for (TextInstance text : textIndex.getTextInstances()) {
            validationEvents.addAll(getValidationEvents(text));
        }
        return validationEvents;
    }

    /**
     * Generates zero or more @see ValidationEvents and returns them in a collection.
     *
     * @param instance text occurrence found in the body of the model
     */
    private Collection<ValidationEvent> getValidationEvents(TextInstance instance) {
        final Collection<ValidationEvent> events = new ArrayList<>();
        for (Map.Entry<String, List<String>> termEntry : termsMap.entrySet()) {
            final String termLower = termEntry.getKey().toLowerCase();
            final int startIndex = instance.getText().toLowerCase().indexOf(termLower);
            if (startIndex != -1) {
                final String matchedText = instance.getText().substring(startIndex, startIndex + termLower.length());
                events.add(constructValidationEvent(instance, termEntry.getValue(), matchedText));
            }
        }
        return events;
    }

    private ValidationEvent constructValidationEvent(
            TextInstance instance,
            List<String> replacements,
            String matchedText
    ) {
        String replacementAddendum = getReplacementAddendum(matchedText, replacements);
        switch (instance.getLocationType()) {
            case NAMESPACE:
                //Cannot use any warning() overloads because there is no shape associated with the event.
                return ValidationEvent.builder()
                        .severity(Severity.WARNING)
                        .sourceLocation(SourceLocation.none())
                        .id(getName() + "." + NAMESPACE + "." + instance.getText()
                                + "." + matchedText.toLowerCase(Locale.US))
                        .message(String.format("%s namespace uses a non-inclusive term `%s`.%s",
                                instance.getText(),
                                matchedText,
                                replacementAddendum))
                        .build();
            case APPLIED_TRAIT:
                ValidationEvent validationEvent =
                        warning(instance.getShape(), instance.getTrait().getSourceLocation(), "");
                String idiomaticTraitName = Trait.getIdiomaticTraitName(instance.getTrait());
                if (instance.getTraitPropertyPath().isEmpty()) {
                    return validationEvent.toBuilder()
                            .message(String.format("'%s' trait has a value that contains a non-inclusive term `%s`.%s",
                                    idiomaticTraitName,
                                    matchedText,
                                    replacementAddendum))
                            .id(getName() + "." + TRAIT + "."
                                    + matchedText.toLowerCase(Locale.US) + "." + idiomaticTraitName)
                            .build();
                } else {
                    String valuePropertyPathFormatted = formatPropertyPath(instance.getTraitPropertyPath());
                    return validationEvent.toBuilder()
                            .message(String.format(
                                    "'%s' trait value at path {%s} contains a non-inclusive term `%s`.%s",
                                    idiomaticTraitName,
                                    valuePropertyPathFormatted,
                                    matchedText,
                                    replacementAddendum))
                            .id(getName() + "." + TRAIT + "." + matchedText.toLowerCase(Locale.US)
                                    + "." + idiomaticTraitName + "." + valuePropertyPathFormatted)
                            .build();
                }
            case SHAPE:
            default:
                return warning(instance.getShape(),
                        instance.getShape().getSourceLocation(),
                        String.format("%s shape uses a non-inclusive term `%s`.%s",
                                StringUtils.capitalize(instance.getShape().getType().toString()),
                                matchedText,
                                replacementAddendum),
                        SHAPE,
                        matchedText.toLowerCase(Locale.US));
        }
    }

    private static String getReplacementAddendum(String matchedText, List<String> replacements) {
        List<String> caseCorrectedEntryValue = replacements.stream()
                .map(replacement -> Character.isUpperCase(matchedText.charAt(0))
                        ? StringUtils.capitalize(replacement)
                        : StringUtils.uncapitalize(replacement))
                .collect(Collectors.toList());
        String replacementAddendum = !replacements.isEmpty()
                ? String.format(" Consider using one of the following terms instead: %s",
                        ValidationUtils.tickedList(caseCorrectedEntryValue))
                : "";
        return replacementAddendum;
    }

    private static String formatPropertyPath(List<String> traitPropertyPath) {
        return String.join("/", traitPropertyPath);
    }
}
