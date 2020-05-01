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
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import software.amazon.smithy.model.neighbor.RelationshipType;
import software.amazon.smithy.model.shapes.CollectionShape;
import software.amazon.smithy.model.shapes.NumberShape;
import software.amazon.smithy.model.shapes.Shape;
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
    private final int length;
    private int position = 0;

    private Parser(String selector) {
        expression = selector;
        length = expression.length();
    }

    static Selector parse(String selector) {
        return new WrappedSelector(selector, new Parser(selector).expression());
    }

    private List<InternalSelector> expression() {
        return recursiveParse();
    }

    private List<InternalSelector> recursiveParse() {
        List<InternalSelector> selectors = new ArrayList<>();

        // createSelector() will strip leading ws.
        selectors.add(createSelector());

        // Need to always strip after calling createSelector in case we are at EOF.
        ws();

        // Parse until a break token: ",", "]", and ")".
        while (position != length && !BREAK_TOKENS.contains(expression.charAt(position))) {
            selectors.add(createSelector());
            // Always skip ws after calling createSelector.
            ws();
        }

        return selectors;
    }

    private InternalSelector createSelector() {
        ws();

        // Require at least one selector.
        switch (charPeek()) {
            case ':': // function
                position++;
                return parseSelectorFunction();
            case '[': // attribute
                position++;
                if (charPeek() == '@') {
                    position++;
                    return parseScopedAttribute();
                } else {
                    return parseAttribute();
                }
            case '>': // forward undirected neighbor
                position++;
                return new ForwardNeighborSelector(ListUtils.of());
            case '<': // reverse [un]directed neighbor
                position++;
                if (charPeek() == '-') { // reverse directed neighbor (<-[X, Y, Z]-)
                    position++;
                    expect('[');
                    return parseSelectorDirectedReverseNeighbor();
                } else { // reverse undirected neighbor (<)
                    return new ReverseNeighborSelector(ListUtils.of());
                }
            case '~': // ~>
                position++;
                expect('>');
                return new RecursiveNeighborSelector();
            case '-': // forward directed neighbor
                position++;
                expect('[');
                return parseSelectorForwardDirectedNeighbor();
            case '*': // Any shape
                position++;
                return InternalSelector.IDENTITY;
            case '$': // variable
                position++;
                return parseVariable();
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
        for (; position < length; position++) {
            char c = expression.charAt(position);
            if (c != ' ' && c != '\t' && c != '\r' && c != '\n') {
                break;
            }
        }
    }

    private char charPeek() {
        return position == length ? Character.MIN_VALUE : expression.charAt(position);
    }

    private char expect(char token) {
        if (charPeek() == token) {
            position++;
            return token;
        }

        throw syntax("Expected: '" + token + "'");
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

    private InternalSelector parseVariable() {
        ws();

        if (charPeek() == '{') {
            position++;
            ws();
            String variableName = parseIdentifier();
            ws();
            expect('}');
            return new VariableGetSelector(variableName);
        }

        String name = parseIdentifier();
        ws();
        expect('(');
        ws();
        InternalSelector selector = AndSelector.of(recursiveParse());
        ws();
        expect(')');

        return new VariableStoreSelector(name, selector);
    }

    // Parses a multi edge neighbor selector: "-[" relationship-type *("," relationship-type) "]"
    private InternalSelector parseSelectorForwardDirectedNeighbor() {
        List<String> relationships = parseSelectorDirectedRelationships();
        // Get the remainder of the "]->" token.
        expect('-');
        expect('>');
        return new ForwardNeighborSelector(relationships);
    }

    private InternalSelector parseSelectorDirectedReverseNeighbor() {
        List<String> relationships = parseSelectorDirectedRelationships();
        expect('-');
        return new ReverseNeighborSelector(relationships);
    }

    private List<String> parseSelectorDirectedRelationships() {
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

        return relationships;
    }

    private InternalSelector parseSelectorFunction() {
        int functionPosition = position;
        String name = parseIdentifier();
        List<InternalSelector> selectors = parseSelectorFunctionArgs();
        switch (name) {
            case "not":
                if (selectors.size() != 1) {
                    throw new SelectorSyntaxException(
                            "The :not function requires a single selector argument", expression, functionPosition);
                }
                return new NotSelector(selectors.get(0));
            case "test":
                return new TestSelector(selectors);
            case "is":
                return IsSelector.of(selectors);
            case "each":
                LOGGER.warning("The `:each` selector function has been renamed to `:is`: " + expression);
                return IsSelector.of(selectors);
            default:
                LOGGER.warning(String.format("Unknown function name `%s` found in selector: %s", name, expression));
                return (context, shape, next) -> true;
        }
    }

    private List<InternalSelector> parseSelectorFunctionArgs() {
        ws();
        List<InternalSelector> selectors = new ArrayList<>();
        expect('(');
        char next;

        do {
            selectors.add(AndSelector.of(recursiveParse()));
            ws();
            next = expect(')', ',');
        } while (next != ')');

        return selectors;
    }

    private InternalSelector parseAttribute() {
        ws();
        BiFunction<Shape, Map<String, Set<Shape>>, AttributeValue> keyFactory = parseAttributePath();
        ws();
        char next = expect(']', '=', '!', '^', '$', '*', '?', '>', '<');

        if (next == ']') {
            return AttributeSelector.existence(keyFactory);
        }

        AttributeComparator comparator = parseComparator(next);
        List<String> values = parseAttributeValues();
        boolean insensitive = parseCaseInsensitiveToken();
        expect(']');
        return new AttributeSelector(keyFactory, values, comparator, insensitive);
    }

    private boolean parseCaseInsensitiveToken() {
        ws();
        boolean insensitive = charPeek() == 'i';
        if (insensitive) {
            position++;
            ws();
        }
        return insensitive;
    }

    private AttributeComparator parseComparator(char next) {
        AttributeComparator comparator;
        switch (next) {
            case '=': // =
                comparator = AttributeComparator.EQUALS;
                break;
            case '!':
                expect('='); // !=
                comparator = AttributeComparator.NOT_EQUALS;
                break;
            case '^':
                expect('='); // ^=
                comparator = AttributeComparator.STARTS_WITH;
                break;
            case '$':
                expect('='); // $=
                comparator = AttributeComparator.ENDS_WITH;
                break;
            case '*':
                expect('='); // *=
                comparator = AttributeComparator.CONTAINS;
                break;
            case '?':
                expect('='); // ?=
                comparator = AttributeComparator.EXISTS;
                break;
            case '>':
                if (charPeek() == '=') { // >=
                    position++;
                    comparator = AttributeComparator.GTE;
                } else { // >
                    comparator = AttributeComparator.GT;
                }
                break;
            case '<':
                if (charPeek() == '=') { // <=
                    position++;
                    comparator = AttributeComparator.LTE;
                } else { // <
                    comparator = AttributeComparator.LT;
                }
                break;
            case '{': // projection comparators
                char nextSet = expect('<', '=', '!');
                if (nextSet == '<') {
                    if (charPeek() == '<') {
                        expect('<'); // {<<}
                        comparator = AttributeComparator.PROPER_SUBSET;
                    } else { // {<}
                        comparator = AttributeComparator.SUBSET;
                    }
                } else if (nextSet == '=') { // {=}
                    comparator = AttributeComparator.PROJECTION_EQUALS;
                } else { // {!=}
                    expect('=');
                    comparator = AttributeComparator.PROJECTION_NOT_EQUALS;
                }
                expect('}');
                break;
            default:
                // Unreachable
                throw syntax("Unknown attribute comparator token '" + next + "'");
        }

        ws();
        return comparator;
    }

    // "[@" selector_key ":" selector_scoped_comparisons "]"
    private InternalSelector parseScopedAttribute() {
        ws();
        BiFunction<Shape, Map<String, Set<Shape>>, AttributeValue> keyScope = parseAttributePath();
        ws();
        expect(':');
        ws();
        return new ScopedAttributeSelector(keyScope, parseScopedAssertions());
    }

    // selector_scoped_comparison *("&&" selector_scoped_comparison)
    private List<ScopedAttributeSelector.Assertion> parseScopedAssertions() {
        List<ScopedAttributeSelector.Assertion> assertions = new ArrayList<>();
        assertions.add(parseScopedAssertion());
        ws();

        while (charPeek() == '&') {
            expect('&');
            expect('&');
            ws();
            assertions.add(parseScopedAssertion());
        }

        expect(']');
        return assertions;
    }

    private ScopedAttributeSelector.Assertion parseScopedAssertion() {
        ScopedAttributeSelector.ScopedFactory lhs = parseScopedValue();
        char next = charPeek();
        position++;
        AttributeComparator comparator = parseComparator(next);

        List<ScopedAttributeSelector.ScopedFactory> rhs = new ArrayList<>();
        rhs.add(parseScopedValue());

        while (charPeek() == ',') {
            position++;
            rhs.add(parseScopedValue());
        }

        boolean insensitive = parseCaseInsensitiveToken();
        return new ScopedAttributeSelector.Assertion(lhs, comparator, rhs, insensitive);
    }

    private ScopedAttributeSelector.ScopedFactory parseScopedValue() {
        ws();
        if (charPeek() == '@') {
            position++;
            expect('{');
            // parse at least one path segment, followed by any number of
            // comma separated segments.
            List<String> path = new ArrayList<>();
            path.add(parseSelectorPathSegment());
            path.addAll(parseSelectorPath());
            expect('}');
            ws();
            return value -> value.getPath(path);
        } else {
            String parsedValue = parseAttributeValue();
            ws();
            return value -> AttributeValue.literal(parsedValue);
        }
    }

    private BiFunction<Shape, Map<String, Set<Shape>>, AttributeValue> parseAttributePath() {
        ws();

        // '[@:' binds the current shape as the context.
        if (charPeek() == ':') {
            return AttributeValue::shape;
        }

        List<String> path = new ArrayList<>();
        // Parse the top-level namespace key.
        path.add(parseIdentifier());

        // It is optionally followed by "|" delimited path keys.
        path.addAll(parseSelectorPath());

        return (shape, variables) -> AttributeValue.shape(shape, variables).getPath(path);
    }

    // Can be a shape_id, quoted string, number, or function key.
    private List<String> parseSelectorPath() {
        ws();

        if (charPeek() != '|') {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        do {
            position++; // skip '|'
            result.add(parseSelectorPathSegment());
        } while (charPeek() == '|');

        return result;
    }

    private String parseSelectorPathSegment() {
        ws();
        // Handle function properties enclosed in "(" identifier ")".
        if (charPeek() == '(') {
            position++;
            String propertyName = parseIdentifier();
            expect(')');
            return "(" + propertyName + ")";
        } else {
            return parseAttributeValue();
        }
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
        while (i < length) {
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
