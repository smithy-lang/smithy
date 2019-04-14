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
import static software.amazon.smithy.model.validation.ValidationUtils.tickedList;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidatorService;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.OptionalUtils;
import software.amazon.smithy.utils.Pair;

/**
 * <p>Validates that operation shape names start with standard verbs.
 *
 * <p>Looks at each operation shape name name and determines if the first
 * word in the shape name is one of the pre-defined allowed verbs.
 *
 * <p>If the verb is a key in the map of verbs that have recommended
 * alternatives, then the event that is emitted contains the list of
 * alternate verbs to use.
 */
public final class StandardOperationVerbValidator extends AbstractValidator {
    private final List<String> verbs;
    private final List<String> prefixes;
    private final Map<String, List<String>> alts;

    private StandardOperationVerbValidator(List<String> verbs, List<String> prefixes, Map<String, List<String>> alts) {
        this.verbs = verbs;
        this.prefixes = prefixes;
        this.alts = alts;
    }

    public static final class Provider extends ValidatorService.Provider {
        public Provider() {
            super(StandardOperationVerbValidator.class, node -> {
                List<String> verbs = Node.loadArrayOfString(
                        "verbs", node.getMember("verbs").orElseGet(Node::arrayNode));
                List<String> prefixes = Node.loadArrayOfString(
                        "prefixes", node.getMember("prefixes").orElseGet(Node::arrayNode));
                Map<String, List<String>> suggestAlternatives = extractAlternatives(node);
                if (verbs.isEmpty() && suggestAlternatives.isEmpty()) {
                    throw new SourceException(
                            "Either verbs or suggestAlternatives must be set when configuring StandardOperationVerb",
                            node);
                }
                return new StandardOperationVerbValidator(verbs, prefixes, suggestAlternatives);
            });
        }
    }

    private static Map<String, List<String>> extractAlternatives(ObjectNode node) {
        return node.getObjectMember("suggestAlternatives")
                .map(ObjectNode::getMembers)
                .map(map ->  map.entrySet().stream()
                        .map(entry -> Pair.of(entry.getKey().getValue(), Node.loadArrayOfString(
                                entry.getKey().getValue(), entry.getValue())))
                        .collect(Collectors.toMap(Pair::getLeft, Pair::getRight)))
                .orElse(MapUtils.of());
    }

    @Override
    public List<ValidationEvent> validate(Model model) {
        return model.getShapeIndex().shapes(OperationShape.class)
                .flatMap(shape -> OptionalUtils.stream(validateShape(shape, verbs, prefixes, alts)))
                .collect(Collectors.toList());
    }

    private Optional<ValidationEvent> validateShape(
            OperationShape operation,
            List<String> verbs,
            List<String> prefixes,
            Map<String, List<String>> alts
    ) {
        List<String> words = splitCamelCaseWord(operation.getId().getName());
        String name;
        String foundPrefix = null;

        // Detect and remove any single prefix if needed.
        if (!prefixes.contains(words.get(0))) {
            name = words.get(0);
        } else if (words.size() == 1) {
            return Optional.of(danger(operation, format(
                    "Operation name consists of only a verb prefix: %s", operation.getId().getName())));
        } else {
            foundPrefix = words.get(0);
            name = words.get(1);
        }

        if (alts.containsKey(name)) {
            return Optional.of(danger(operation, format(
                    "%s Consider using one of the following verbs instead: %s",
                    createMessagePrefix(operation, name, foundPrefix), tickedList(alts.get(name)))));
        } else if (!verbs.contains(name)) {
            return Optional.of(danger(operation, format(
                    "%s Expected one of the following verbs: %s",
                    createMessagePrefix(operation, name, foundPrefix), tickedList(verbs))));
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
