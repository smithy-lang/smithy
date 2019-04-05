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

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.selector.SelectorSyntaxException;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeIndex;
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
    private static final Pattern CONTAINS_INNER_WILDCARD = Pattern.compile("^.+\\*.+$");

    private final List<ReservedWords> reservations;

    private ReservedWordsValidator(List<ReservedWords> reservations) {
        this.reservations = reservations;
    }

    public static final class Provider extends ValidatorService.Provider {
        public Provider() {
            super(ReservedWordsValidator.class, node -> new ReservedWordsValidator(
                    node.expectMember("reserved")
                        .expectArrayNode().getElements().stream()
                        .map(Node::expectObjectNode)
                        .map(ReservedWordsValidator::createConfiguration)
                        .collect(Collectors.toList())));
        }
    }

    @Override
    public List<ValidationEvent> validate(Model model) {
        ShapeIndex shapeIndex = model.getShapeIndex();
        return reservations.stream().flatMap(reservation -> reservation.validate(shapeIndex))
                .collect(Collectors.toList());
    }

    private static ReservedWords createConfiguration(ObjectNode node) {
        return new ReservedWords(parseReservedWords(node),
                node.getStringMember("selector").orElse(Node.from("*")),
                node.getStringMember("reason").map(StringNode::getValue).orElse(""));
    }

    private static List<String> parseReservedWords(ObjectNode node) {
        // Load a list of reserved words, but throw an exception if a full
        // wildcard or inner wildcard is specified.
        return node.getArrayMember("words")
                .map(reservations -> reservations.getElements().stream()
                        .map(Node::expectStringNode)
                        .map(ReservedWordsValidator::getValidReservationOrThrow)
                        .collect(Collectors.toList()))
                .orElseThrow(() -> new SourceException("A reservation must supply an array of strings "
                                                       + "under `words`.", node));
    }

    private static String getValidReservationOrThrow(StringNode string) {
        String value = string.getValue();
        if (value.equals("*")) {
            throw new SourceException("Reservations cannot be made against '*'", string);
        }
        if (CONTAINS_INNER_WILDCARD.matcher(value).find()) {
            throw new SourceException("Only preceding and trailing wildcards ('*') are supported.", string);
        }
        return value.toLowerCase(Locale.US);
    }

    private static final class ReservedWords {
        private List<String> reservations;
        private Selector selector;
        private String reason;

        private ReservedWords(List<String> reservations, StringNode selector, String reason) {
            this.reservations = reservations;
            this.selector = parse(selector);
            this.reason = reason;
        }

        private Selector parse(StringNode expression) {
            try {
                return Selector.parse(expression.getValue().trim());
            } catch (SelectorSyntaxException e) {
                throw new SourceException("Invalid selector expression: " + e.getMessage(), expression, e);
            }
        }

        private Stream<ValidationEvent> validate(ShapeIndex shapeIndex) {
            return selector.select(shapeIndex).stream().flatMap(shape -> validateShape(shape).stream());
        }

        private Optional<ValidationEvent> validateShape(Shape shape) {
            var name = shape.asMemberShape()
                    .map(MemberShape::getMemberName)
                    .orElseGet(() -> shape.getId().getName());
            if (isReservedWord(name)) {
                return Optional.of(emit(shape, name, reason));
            }
            return Optional.empty();
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
            return reservations.stream().anyMatch(reservation -> {
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
                    .eventId(ValidatorService.determineValidatorName(ReservedWordsValidator.class))
                    .shape(shape)
                    .message(format("The word `%s` is reserved. %s", word, reason))
                    .build();
        }
    }
}
