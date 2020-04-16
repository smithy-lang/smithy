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

package software.amazon.smithy.model.selector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import software.amazon.smithy.model.neighbor.RelationshipType;
import software.amazon.smithy.model.shapes.CollectionShape;
import software.amazon.smithy.model.shapes.NumberShape;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.SimpleShape;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SetUtils;

/**
 * Parses a selector expression.
 */
final class Parser {

    private static final Logger LOGGER = Logger.getLogger(Parser.class.getName());
    private static final Set<Character> BREAK_TOKENS = SetUtils.of(',', ']', ')');
    private static final Set<String> REL_TYPES = new HashSet<>();
    private static final List<String> FUNCTIONS = ListUtils.of("test", "each", "of", "not");
    private static final List<String> ATTRIBUTES = ListUtils.of(
            "trait|", "id|namespace", "id|name", "id|member", "id", "service|version");
    private static final List<String> AFTER_ATTRIBUTE = ListUtils.of("=", "^=", "$=", "*=", "]");
    private static final List<String> AFTER_ATTRIBUTE_RHS = ListUtils.of("i]", "]");
    private static final List<String> START_FUNCTION = ListUtils.of("(");
    private static final List<String> FUNCTION_ARG_NEXT_TOKEN = ListUtils.of(")", ",");
    private static final List<String> MULTI_EDGE_NEXT_ARG_TOKEN = ListUtils.of(",", "]->");
    private static final List<String> EXPRESSION_TOKENS = new ArrayList<>(Arrays.asList(
            ":", "[", ">", "-[", "*", "number", "simpleType", "collection"));

    static {
        // Adds selector relationship labels.
        for (RelationshipType rel : RelationshipType.values()) {
            rel.getSelectorLabel().ifPresent(REL_TYPES::add);
        }

        // Adds all shape types as possible tokens.
        for (ShapeType type : ShapeType.values()) {
            EXPRESSION_TOKENS.add(type.toString());
        }
    }

    private final String expression;
    private int position = 0;

    private Parser(String selector) {
        this.expression = selector;
        ws(); // Skip leading whitespace.
    }

    static Selector parse(String selector) {
        return new WrappedSelector(selector, AndSelector.of(new Parser(selector).expression()));
    }

    private List<Selector> expression() {
        List<Selector> selectors = recursiveParse();
        ws(); // Skip trailing whitespace.
        if (position != expression.length()) {
            throw syntax("Invalid expression");
        }
        return selectors;
    }

    private List<Selector> recursiveParse() {
        List<Selector> selectors = new ArrayList<>();
        // Require at least one selector.
        selectors.add(createSelector(expect(EXPRESSION_TOKENS)));
        // Parse until a break token: ",", "]", and ")".
        while (position != expression.length() && !BREAK_TOKENS.contains(expression.charAt(position))) {
            selectors.add(createSelector(expect(EXPRESSION_TOKENS)));
        }
        return selectors;
    }

    private void ws() {
        while (position < expression.length() && Character.isWhitespace(expression.charAt(position))) {
            position++;
        }
    }

    private char charPeek() {
        return position == expression.length() ? Character.MIN_VALUE : expression.charAt(position);
    }

    private String expect(Collection<String> tokens) {
        for (String token : tokens) {
            if (compareExpressionSlice(token)) {
                position += token.length();
                ws(); // Skip whitespace after taking token.
                return token;
            }
        }

        throw syntax("Expected one of the following tokens: " + ValidationUtils.tickedList(tokens));
    }

    private boolean compareExpressionSlice(String token) {
        if (token.length() > expression.length() - position) {
            return false;
        }

        for (int i = 0; i < token.length(); i++) {
            if (token.charAt(i) != expression.charAt(position + i)) {
                return false;
            }
        }

        return true;
    }

    private SelectorSyntaxException syntax(String message) {
        return new SelectorSyntaxException(message, expression, position);
    }

    private Selector createSelector(String token) {
        switch (token) {
            case ">": return new NeighborSelector(ListUtils.of());
            case "-[": return parseMultiEdgeDirectedNeighbor();
            case "[": return parseAttribute();
            case ":": return parseFunction();
            case "*": return Selector.IDENTITY;
            case "number": return new ShapeTypeCategorySelector(NumberShape.class);
            case "simpleType": return new ShapeTypeCategorySelector(SimpleShape.class);
            case "collection": return new ShapeTypeCategorySelector(CollectionShape.class);
            // Anything else matches shapes by name (e.g., "structure").
            default:
                ShapeType shape = ShapeType.fromString(token).orElseThrow(() -> syntax("Unreachable token " + token));
                return new ShapeTypeSelector(shape);
        }
    }

    private Selector parseMultiEdgeDirectedNeighbor() {
        // Parses a multi edge neighbor selector: "-[" relationship-type *("," relationship-type) "]"
        List<String> relationships = new ArrayList<>();
        String next;
        do {
            // Requires at least one relationship type.
            next = parseIdentifier();
            relationships.add(next);
            // Tolerate unknown relationships, but log a warning.
            if (!REL_TYPES.contains(next)) {
                LOGGER.warning(String.format(
                        "Unknown relationship type '%s' found near %s. Expected one of: %s",
                        next, position - next.length(), REL_TYPES));
            }
            next = expect(MULTI_EDGE_NEXT_ARG_TOKEN);
        } while (!next.equals("]->"));
        return new NeighborSelector(relationships);
    }

    private Selector parseFunction() {
        String name = expect(FUNCTIONS);
        switch (name) {
            case "not": return parseVariadic(NotSelector::new);
            case "test": return parseVariadic(TestSelector::new);
            case "each": return parseVariadic(EachSelector::of);
            case "of": return parseVariadic(OfSelector::new);
            default: throw new RuntimeException("Unreachable function case " + name);
        }
    }

    private Selector parseVariadic(Function<List<Selector>, Selector> creator) {
        List<Selector> selectors = new ArrayList<>();
        expect(START_FUNCTION);
        String next;
        do {
            selectors.add(AndSelector.of(recursiveParse()));
            next = expect(FUNCTION_ARG_NEXT_TOKEN);
        } while (!next.equals(")"));
        return creator.apply(selectors);
    }

    private Selector parseAttribute() {
        AttributeSelector.KeyGetter attributeKey = parseAttributeKey();
        String comparatorLexeme = expect(AFTER_ATTRIBUTE);
        AttributeSelector.Comparator comparator;

        switch (comparatorLexeme) {
            case "]":
                return new AttributeSelector(attributeKey);
            case "=":
                comparator = AttributeSelector.EQUALS;
                break;
            case "^=":
                comparator = AttributeSelector.STARTS_WITH;
                break;
            case "$=":
                comparator = AttributeSelector.ENDS_WITH;
                break;
            case "*=":
                comparator = AttributeSelector.CONTAINS;
                break;
            default:
                throw syntax("Unreachable attribute comparator case for " + comparatorLexeme);
        }

        String value = parseAttributeValue();
        ws();
        String afterValue = expect(AFTER_ATTRIBUTE_RHS);
        boolean insensitive = afterValue.equals("i]"); // Case insensitive.
        return new AttributeSelector(attributeKey, comparator, value, insensitive);
    }

    private AttributeSelector.KeyGetter parseAttributeKey() {
        String namespace = expect(ATTRIBUTES);
        switch (namespace) {
            case "trait|": return new TraitAttributeKey(parseAttributeValue());
            case "id": return AttributeSelector.KEY_ID;
            case "id|namespace": return AttributeSelector.KEY_ID_NAMESPACE;
            case "id|name": return AttributeSelector.KEY_ID_NAME;
            case "id|member": return AttributeSelector.KEY_ID_MEMBER;
            case "service|version": return AttributeSelector.KEY_SERVICE_VERSION;
            default: throw syntax("Unreachable attribute case for " + namespace);
        }
    }

    private String parseAttributeValue() {
        switch (charPeek()) {
            case '\'': return consumeInside('\'');
            case '"': return consumeInside('"');
            default: return parseIdentifier();
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
        if (!validAttributeIdentifier(current)) {
            throw syntax("Invalid attribute start character `" + current + "`");
        }

        builder.append(current);
        position++;

        current = charPeek();
        while (validInnerAttributeIdentifier(current)) {
            builder.append(current);
            position++;
            current = charPeek();
        }

        return builder.toString();
    }

    private boolean validAttributeIdentifier(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_';
    }

    private boolean validInnerAttributeIdentifier(char c) {
        return validAttributeIdentifier(c) || c == '.' || c == '-' || c == '#';
    }
}
