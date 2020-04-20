/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.selector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import software.amazon.smithy.model.neighbor.RelationshipType;
import software.amazon.smithy.model.shapes.CollectionShape;
import software.amazon.smithy.model.shapes.NumberShape;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.SimpleShape;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SetUtils;

/**
 * Parses a selector expression.
 */
final class Parser {

    private static final Logger LOGGER = Logger.getLogger(Parser.class.getName());
    private static final Set<Character> BREAK_TOKENS = SetUtils.of(',', ']', ')');
    private static final Set<String> REL_TYPES = new HashSet<>();

    static {
        // Adds selector relationship labels for warnings when unknown relationship names are used.
        for (RelationshipType rel : RelationshipType.values()) {
            rel.getSelectorLabel().ifPresent(REL_TYPES::add);
        }
    }

    private final String expression;
    private int position = 0;

    private Parser(String selector) {
        this.expression = selector;
    }

    static Selector parse(String selector) {
        return new WrappedSelector(selector, AndSelector.of(new Parser(selector).expression()));
    }

    private List<Selector> expression() {
        return recursiveParse();
    }

    private List<Selector> recursiveParse() {
        List<Selector> selectors = new ArrayList<>();

        // createSelector() will strip leading ws.
        selectors.add(createSelector());

        // Need to always strip after calling createSelector in case we are at EOF.
        ws();

        // Parse until a break token: ",", "]", and ")".
        while (position != expression.length() && !BREAK_TOKENS.contains(expression.charAt(position))) {
            selectors.add(createSelector());
            // Always skip ws after calling createSelector.
            ws();
        }

        return selectors;
    }

    private Selector createSelector() {
        ws();

        // Require at least one selector.
        switch (charPeek()) {
            case ':': // function
                position++;
                return parseFunction();
            case '[': // attribute
                position++;
                return parseAttribute();
            case '>': // undirected neighbor
                position++;
                return new NeighborSelector(ListUtils.of());
            case '~': // ~>
                position++;
                expect('>');
                return new RecursiveNeighborSelector();
            case '-': // directed neighbor
                position++;
                expect('[');
                return parseMultiEdgeDirectedNeighbor();
            case '*': // Any shape
                position++;
                return Selector.IDENTITY;
            default:
                if (validIdentifierStart(charPeek())) {
                    String identifier = parseIdentifier();
                    switch (identifier) {
                        case "number":
                            return new ShapeTypeCategorySelector(NumberShape.class);
                        case "simpleType":
                            return new ShapeTypeCategorySelector(SimpleShape.class);
                        case "collection":
                            return new ShapeTypeCategorySelector(CollectionShape.class);
                        default:
                            ShapeType shape = ShapeType.fromString(identifier)
                                    .orElseThrow(() -> syntax("Unknown shape type: " + identifier));
                            return new ShapeTypeSelector(shape);
                    }
                } else {
                    char c = charPeek();
                    if (c == Character.MIN_VALUE) {
                        throw syntax("Unexpected selector EOF");
                    } else {
                        throw syntax("Unexpected selector character: " + charPeek());
                    }
                }
        }
    }

    private void ws() {
        while (position < expression.length() && isWhitespace(expression.charAt(position))) {
            position++;
        }
    }

    private boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n';
    }

    private char charPeek() {
        return position == expression.length() ? Character.MIN_VALUE : expression.charAt(position);
    }

    private char expect(char... tokens) {
        for (char token : tokens) {
            if (charPeek() == token) {
                position++;
                return token;
            }
        }

        StringBuilder message = new StringBuilder("Expected one of the following tokens:");
        for (char c : tokens) {
            message.append(' ').append('\'').append(c).append('\'');
        }

        throw syntax(message.toString());
    }

    private SelectorSyntaxException syntax(String message) {
        return new SelectorSyntaxException(message, expression, position);
    }

    private Selector parseMultiEdgeDirectedNeighbor() {
        // Parses a multi edge neighbor selector: "-[" relationship-type *("," relationship-type) "]"
        List<String> relationships = new ArrayList<>();
        String next;
        char peek;

        do {
            // Requires at least one relationship type.
            ws();
            next = parseIdentifier();
            relationships.add(next);

            // Tolerate unknown relationships, but log a warning.
            if (!REL_TYPES.contains(next)) {
                LOGGER.warning(String.format(
                        "Unknown relationship type '%s' found near %s. Expected one of: %s",
                        next, position - next.length(), REL_TYPES));
            }

            ws();
            peek = expect(']', ',');
        } while (peek != ']');

        // Get the remainder of the "]->" token.
        expect('-');
        expect('>');

        return new NeighborSelector(relationships);
    }

    private Selector parseFunction() {
        String name = parseIdentifier();
        List<Selector> selectors = parseVariadic();
        switch (name) {
            case "not":
                return new NotSelector(selectors);
            case "test":
                return new TestSelector(selectors);
            case "is":
                return IsSelector.of(selectors);
            case "each":
                LOGGER.warning("The `:each` selector function has been renamed to `:is`: " + expression);
                return IsSelector.of(selectors);
            case "of":
                return new OfSelector(selectors);
            default:
                LOGGER.warning(String.format("Unknown function name `%s` found in selector: %s", name, expression));
                return (model, neighborProvider, shapes) -> Collections.emptySet();
        }
    }

    private List<Selector> parseVariadic() {
        ws();
        List<Selector> selectors = new ArrayList<>();
        expect('(');
        char next;

        do {
            selectors.add(AndSelector.of(recursiveParse()));
            ws();
            next = expect(')', ',');
        } while (next != ')');

        return selectors;
    }

    private Selector parseAttribute() {
        ws();
        AttributeSelector.KeyGetter attributeKey = parseAttributeKey();
        ws();
        char next = expect(']', '=', '!', '^', '$', '*', '>', '<');
        AttributeSelector.Comparator comparator;

        switch (next) {
            case ']':
                return new AttributeSelector(attributeKey);
            case '=':
                comparator = AttributeSelector.EQUALS;
                break;
            case '!':
                expect('=');
                comparator = AttributeSelector.NOT_EQUALS;
                break;
            case '^':
                expect('=');
                comparator = AttributeSelector.STARTS_WITH;
                break;
            case '$':
                expect('=');
                comparator = AttributeSelector.ENDS_WITH;
                break;
            case '*':
                expect('=');
                comparator = AttributeSelector.CONTAINS;
                break;
            case '>':
                if (charPeek() == '=') {
                    position++;
                    comparator = AttributeSelector.GTE;
                } else {
                    comparator = AttributeSelector.GT;
                }
                break;
            case '<':
                if (charPeek() == '=') {
                    position++;
                    comparator = AttributeSelector.LTE;
                } else {
                    comparator = AttributeSelector.LT;
                }
                break;
            default:
                // Unreachable
                throw syntax("Unknown attribute comparator token '" + next + "'");
        }

        List<String> values = parseAttributeValues();
        ws();

        boolean insensitive = charPeek() == 'i';
        if (insensitive) {
            position++;
            ws();
        }

        expect(']');
        return new AttributeSelector(attributeKey, comparator, values, insensitive);
    }

    private AttributeSelector.KeyGetter parseAttributeKey() {
        // Parse the top-level namespace key.
        String namespace = parseIdentifier();

        // It is optionally followed by "|" delimited path keys.
        List<String> path = parsePipeDelimitedTraitAttributes();

        switch (namespace) {
            case "id":
                if (path.isEmpty()) {
                    return AttributeSelector.KEY_ID;
                } else if (path.size() == 1) {
                    switch (path.get(0)) {
                        case "namespace":
                            return AttributeSelector.KEY_ID_NAMESPACE;
                        case "name":
                            return AttributeSelector.KEY_ID_NAME;
                        case "member":
                            return AttributeSelector.KEY_ID_MEMBER;
                        default:
                            // Unknown attributes always return no result.
                            LOGGER.warning("Unknown  selector attribute `id` path " + path.get(0) + ": " + expression);
                            return s -> Collections.emptyList();
                    }
                } else {
                    // Unknown attributes always return no result.
                    LOGGER.warning("Too many selector attribute `id` paths " + path + ": " + expression);
                    return s -> Collections.emptyList();
                }
            case "service":
                if (path.size() != 1) {
                    throw syntax("service attributes require exactly one path item");
                } else if (path.get(0).equals("version")) {
                    return AttributeSelector.KEY_SERVICE_VERSION;
                } else {
                    // Unknown attributes always return no result.
                    LOGGER.warning("Unknown selector service attribute path " + path + ": " + expression);
                    return s -> Collections.emptyList();
                }
            case "trait":
                if (path.isEmpty()) {
                    throw syntax("Trait attributes require a trait shape ID");
                } else if (path.size() == 1) {
                    return new TraitAttributeKey(path.get(0), Collections.emptyList());
                } else {
                    return new TraitAttributeKey(path.get(0), path.subList(1, path.size()));
                }
            default:
                // Unknown attributes always return no result.
                LOGGER.warning("Unknown selector attribute `" + namespace + "` " + expression);
                return s -> Collections.emptyList();
        }
    }

    // Can be a shape_id, quoted string, number, or pseudo_key.
    private List<String> parsePipeDelimitedTraitAttributes() {
        ws();

        if (charPeek() != '|') {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        do {
            position++; // skip '|'
            ws();
            // Handle pseudo-keys enclosed in "(" identifier ")".
            if (charPeek() == '(') {
                position++;
                String propertyName = parseIdentifier();
                expect(')');
                result.add("(" + propertyName + ")");
            } else {
                result.add(parseAttributeValue());
            }
        } while (charPeek() == '|');

        return result;
    }

    private List<String> parseAttributeValues() {
        List<String> result = new ArrayList<>();
        result.add(parseAttributeValue());
        ws();

        while (charPeek() == ',') {
            position++;
            result.add(parseAttributeValue());
            ws();
        }

        return result;
    }

    private String parseAttributeValue() {
        ws();

        switch (charPeek()) {
            case '\'':
                return consumeInside('\'');
            case '"':
                return consumeInside('"');
            case '-':
                position++;
                return parseNumber(true);
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                return parseNumber(false);
            default:
                return parseShapeId();
        }
    }

    private String consumeInside(char c) {
        int i = ++position;
        while (i < expression.length()) {
            if (expression.charAt(i) == c) {
                String result = expression.substring(position, i);
                position = i + 1;
                ws();
                return result;
            }
            i++;
        }
        throw syntax("Expected " + c + " to close " + expression.substring(position));
    }

    private String parseIdentifier() {
        StringBuilder builder = new StringBuilder();
        char current = charPeek();

        // needs at least one character
        if (!validIdentifierStart(current)) {
            throw syntax("Invalid attribute start character `" + current + "`");
        }

        builder.append(current);
        position++;

        current = charPeek();
        while (validIdentifierInner(current)) {
            builder.append(current);
            position++;
            current = charPeek();
        }

        return builder.toString();
    }

    private boolean validIdentifierStart(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private boolean validIdentifierInner(char c) {
        return validIdentifierStart(c) || (c >= '0' && c <= '9');
    }

    private String parseShapeId() {
        StringBuilder builder = new StringBuilder();
        builder.append(parseIdentifier());

        if (charPeek() == '.') {
            do {
                position++;
                builder.append('.').append(parseIdentifier());
            } while (charPeek() == '.');
            // "." is only allowed in the namespace part, so it must be followed by a "#".
            expect('#');
            builder.append('#').append(parseIdentifier());
        } else if (charPeek() == '#') { // a shape id with no namespace dots, but with a namespace.
            position++;
            builder.append('#').append(parseIdentifier());
        }

        // Note that members are not supported in this production!
        return builder.toString();
    }

    private String parseNumber(boolean negative) {
        StringBuilder result = new StringBuilder();

        if (negative) {
            result.append('-');
        }

        addSimpleNumberToBuilder(result);

        // Consume the fraction part.
        if (charPeek() == '.') {
            result.append('.');
            position++;
            addSimpleNumberToBuilder(result);
        }

        // Consume the exponent, if present.
        if (charPeek() == 'e') {
            result.append('e');
            position++;
            if (charPeek() == '-' || charPeek() == '+') {
                result.append(charPeek());
                position++;
            }
            addSimpleNumberToBuilder(result);
        }

        return result.toString();
    }

    private void addSimpleNumberToBuilder(StringBuilder result) {
        // Require at least one numeric value.
        result.append(expect('0', '1', '2', '3', '4', '5', '6', '7', '8', '9'));

        // Consume all numbers after the first number.
        while (Character.isDigit(charPeek())) {
            result.append(charPeek());
            position++;
        }
    }
}
