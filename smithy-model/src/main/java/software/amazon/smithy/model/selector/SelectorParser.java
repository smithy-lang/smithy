/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import software.amazon.smithy.model.loader.ParserUtils;
import software.amazon.smithy.model.neighbor.RelationshipType;
import software.amazon.smithy.model.shapes.CollectionShape;
import software.amazon.smithy.model.shapes.NumberShape;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.SimpleShape;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SimpleParser;

/**
 * Parses a selector expression.
 */
final class SelectorParser extends SimpleParser {

    private static final Logger LOGGER = Logger.getLogger(SelectorParser.class.getName());
    private static final Set<Character> BREAK_TOKENS = SetUtils.of(',', ']', ')');
    private static final Set<String> REL_TYPES = new HashSet<>();
    private final List<InternalSelector> roots = new ArrayList<>();

    static {
        // Adds selector relationship labels for warnings when unknown relationship names are used.
        for (RelationshipType rel : RelationshipType.values()) {
            rel.getSelectorLabel().ifPresent(REL_TYPES::add);
        }
    }

    private SelectorParser(String selector) {
        super(selector);
    }

    static Selector parse(String selector) {
        SelectorParser parser = new SelectorParser(selector);
        List<InternalSelector> result = parser.parse();
        return new WrappedSelector(selector, result, parser.roots);
    }

    List<InternalSelector> parse() {
        return recursiveParse();
    }

    private List<InternalSelector> recursiveParse() {
        List<InternalSelector> selectors = new IgnoreIdentitySelectorArray();

        // createSelector() will strip leading ws.
        selectors.add(createSelector());

        // Need to always strip after calling createSelector in case we are at EOF.
        ws();

        // Parse until a break token: ",", "]", and ")".
        while (!eof() && !BREAK_TOKENS.contains(peek())) {
            selectors.add(createSelector());
            // Always skip ws after calling createSelector.
            ws();
        }

        return selectors;
    }

    /**
     * Filter out unnecessary identity selectors when creating the finalized AST to evaluate selectors.
     */
    private static final class IgnoreIdentitySelectorArray extends ArrayList<InternalSelector> {
        @Override
        public boolean add(InternalSelector o) {
            return o != InternalSelector.IDENTITY && super.add(o);
        }
    }

    private InternalSelector createSelector() {
        ws();

        // Require at least one selector.
        switch (peek()) {
            case ':': // function
                skip();
                return parseSelectorFunction();
            case '[': // attribute
                skip();
                if (peek() == '@') {
                    skip();
                    return parseScopedAttribute();
                } else {
                    return parseAttribute();
                }
            case '>': // forward undirected neighbor
                skip();
                return NeighborSelector.FORWARD;
            case '<': // reverse [un]directed neighbor
                skip();
                if (peek() == '-') { // reverse directed neighbor (<-[X, Y, Z]-)
                    skip();
                    expect('[');
                    return parseSelectorDirectedReverseNeighbor();
                } else { // reverse undirected neighbor (<)
                    return NeighborSelector.REVERSE;
                }
            case '~': // ~>
                skip();
                expect('>');
                return new RecursiveNeighborSelector();
            case '-': // forward directed neighbor
                skip();
                expect('[');
                return parseSelectorForwardDirectedNeighbor();
            case '*': // Any shape
                skip();
                return InternalSelector.IDENTITY;
            case '$': // variable
                skip();
                return parseVariable();
            default:
                if (ParserUtils.isIdentifierStart(peek())) {
                    String identifier = ParserUtils.parseIdentifier(this);
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
                } else if (peek() == Character.MIN_VALUE) {
                    throw syntax("Unexpected selector EOF");
                } else {
                    throw syntax("Unexpected selector character: " + peek());
                }
        }
    }

    @Override
    public SelectorSyntaxException syntax(String message) {
        return new SelectorSyntaxException(message, input().toString(), position(), line(), column());
    }

    private InternalSelector parseVariable() {
        ws();

        if (peek() == '{') {
            skip();
            ws();
            String variableName = ParserUtils.parseIdentifier(this);
            ws();
            expect('}');
            return new VariableGetSelector(variableName);
        }

        String name = ParserUtils.parseIdentifier(this);
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
        return NeighborSelector.forward(relationships);
    }

    private InternalSelector parseSelectorDirectedReverseNeighbor() {
        List<String> relationships = parseSelectorDirectedRelationships();
        expect('-');
        return NeighborSelector.reverse(relationships);
    }

    private List<String> parseSelectorDirectedRelationships() {
        List<String> relationships = new ArrayList<>();
        String next;
        char peek;

        do {
            // Requires at least one relationship type.
            ws();
            next = ParserUtils.parseIdentifier(this);
            relationships.add(next);

            // Tolerate unknown relationships, but log a warning.
            if (!REL_TYPES.contains(next)) {
                LOGGER.warning(String.format(
                        "Unknown relationship type '%s' found near %s. Expected one of: %s",
                        next,
                        position() - next.length(),
                        REL_TYPES));
            }

            ws();
            peek = expect(']', ',');
        } while (peek != ']');

        return relationships;
    }

    private InternalSelector parseSelectorFunction() {
        int functionPosition = position();
        String name = ParserUtils.parseIdentifier(this);
        List<InternalSelector> selectors = parseSelectorFunctionArgs();
        switch (name) {
            case "not":
                if (selectors.size() != 1) {
                    throw new SelectorSyntaxException(
                            "The :not function requires a single selector argument",
                            input().toString(),
                            functionPosition,
                            line(),
                            column());
                }
                return new NotSelector(selectors.get(0));
            case "test":
                return new TestSelector(selectors);
            case "is":
                return IsSelector.of(selectors);
            case "in":
                if (selectors.size() != 1) {
                    throw new SelectorSyntaxException(
                            "The :in function requires a single selector argument",
                            input().toString(),
                            functionPosition,
                            line(),
                            column());
                }
                return new InSelector(selectors.get(0));
            case "root":
                if (selectors.size() != 1) {
                    throw new SelectorSyntaxException(
                            "The :root function requires a single selector argument",
                            input().toString(),
                            functionPosition,
                            line(),
                            column());
                }
                InternalSelector root = new RootSelector(selectors.get(0), roots.size());
                roots.add(selectors.get(0));
                return root;
            case "topdown":
                if (selectors.size() > 2) {
                    throw new SelectorSyntaxException(
                            "The :topdown function accepts 1 or 2 selectors, but found " + selectors.size(),
                            input().toString(),
                            functionPosition,
                            line(),
                            column());
                }
                return new TopDownSelector(selectors);
            case "recursive":
                if (selectors.size() != 1) {
                    throw new SelectorSyntaxException(
                            "The :recursive function requires a single selector argument",
                            input().toString(),
                            functionPosition,
                            line(),
                            column());
                }
                return new RecursiveSelector(selectors.get(0));
            case "each":
                LOGGER.warning("The `:each` selector function has been renamed to `:is`: " + input());
                return IsSelector.of(selectors);
            default:
                LOGGER.warning(String.format("Unknown function name `%s` found in selector: %s",
                        name,
                        input()));
                return (context, shape, next) -> InternalSelector.Response.CONTINUE;
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
        List<String> path = parseAttributePath();
        ws();
        char next = expect(']', '=', '!', '^', '$', '*', '?', '>', '<');

        if (next == ']') {
            return AttributeSelector.existence(path);
        }

        AttributeComparator comparator = parseComparator(next);
        List<String> values = parseAttributeValues();
        boolean insensitive = parseCaseInsensitiveToken();
        expect(']');
        return new AttributeSelector(path, values, comparator, insensitive);
    }

    private boolean parseCaseInsensitiveToken() {
        ws();
        boolean insensitive = peek() == 'i';
        if (insensitive) {
            skip();
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
                if (peek() == '=') { // >=
                    skip();
                    comparator = AttributeComparator.GTE;
                } else { // >
                    comparator = AttributeComparator.GT;
                }
                break;
            case '<':
                if (peek() == '=') { // <=
                    skip();
                    comparator = AttributeComparator.LTE;
                } else { // <
                    comparator = AttributeComparator.LT;
                }
                break;
            case '{': // projection comparators
                char nextSet = expect('<', '=', '!');
                if (nextSet == '<') {
                    if (peek() == '<') {
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
        List<String> path = parseAttributePath();
        ws();
        expect(':');
        ws();
        return new ScopedAttributeSelector(path, parseScopedAssertions());
    }

    // selector_scoped_comparison *("&&" selector_scoped_comparison)
    private List<ScopedAttributeSelector.Assertion> parseScopedAssertions() {
        List<ScopedAttributeSelector.Assertion> assertions = new ArrayList<>();
        assertions.add(parseScopedAssertion());
        ws();

        while (peek() == '&') {
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
        char next = peek();
        skip();
        AttributeComparator comparator = parseComparator(next);

        List<ScopedAttributeSelector.ScopedFactory> rhs = new ArrayList<>();
        rhs.add(parseScopedValue());

        while (peek() == ',') {
            skip();
            rhs.add(parseScopedValue());
        }

        boolean insensitive = parseCaseInsensitiveToken();
        return new ScopedAttributeSelector.Assertion(lhs, comparator, rhs, insensitive);
    }

    private ScopedAttributeSelector.ScopedFactory parseScopedValue() {
        ws();
        if (peek() == '@') {
            List<String> path = parseScopedValuePath(this);
            ws();
            return value -> value.getPath(path);
        } else {
            String parsedValue = parseAttributeValue(this);
            ws();
            return value -> AttributeValue.literal(parsedValue);
        }
    }

    private List<String> parseAttributePath() {
        ws();

        // '[@:' binds the current shape as the context.
        if (peek() == ':') {
            return Collections.emptyList();
        }

        List<String> path = new ArrayList<>();
        // Parse the top-level namespace key.
        path.add(ParserUtils.parseIdentifier(this));

        // It is optionally followed by "|" delimited path keys.
        path.addAll(parseSelectorPath(this));

        return path;
    }

    private List<String> parseAttributeValues() {
        List<String> result = new ArrayList<>();
        result.add(parseAttributeValue(this));
        ws();

        while (peek() == ',') {
            skip();
            result.add(parseAttributeValue(this));
            ws();
        }

        return result;
    }

    /*
     * The following methods are static methods that aren't coupled to the
     * SelectorParser, but rather a SimpleParser. This allows the AttributeValue#parseScopedAttribute
     * method to accept a SimpleParser and then use this method to perform the actual
     * parsing of a scoped attribute value.
     *
     * This is used to parse scoped attribute values from EmitEachSelector message
     * templates.
     */

    static List<String> parseScopedValuePath(SimpleParser parser) {
        parser.expect('@');
        parser.expect('{');
        // parse at least one path segment, followed by any number of
        // comma separated segments.
        List<String> path = new ArrayList<>();
        path.add(parseSelectorPathSegment(parser));
        path.addAll(parseSelectorPath(parser));
        parser.expect('}');
        return path;
    }

    private static String parseSelectorPathSegment(SimpleParser parser) {
        parser.ws();
        // Handle function properties enclosed in "(" identifier ")".
        if (parser.peek() == '(') {
            parser.skip();
            String propertyName = ParserUtils.parseIdentifier(parser);
            parser.expect(')');
            return "(" + propertyName + ")";
        } else {
            return parseAttributeValue(parser);
        }
    }

    private static String parseAttributeValue(SimpleParser parser) {
        parser.ws();

        switch (parser.peek()) {
            case '\'':
                return consumeInside(parser, '\'');
            case '"':
                return consumeInside(parser, '"');
            case '-':
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
                return ParserUtils.parseNumber(parser);
            default:
                return ParserUtils.parseRootShapeId(parser);
        }
    }

    private static String consumeInside(SimpleParser parser, char c) {
        parser.skip(); // skip the opening character.
        int start = parser.position();

        while (!parser.eof()) {
            if (parser.peek() == c) {
                String result = parser.sliceFrom(start);
                parser.skip();
                parser.ws();
                return result;
            }
            parser.skip();
        }

        throw parser.syntax("Expected " + c + " to close " + parser.sliceFrom(start));
    }

    // Can be a shape_id, quoted string, number, or function key.
    private static List<String> parseSelectorPath(SimpleParser parser) {
        parser.ws();

        if (parser.peek() != '|') {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        do {
            parser.skip(); // skip '|'
            result.add(parseSelectorPathSegment(parser));
        } while (parser.peek() == '|');

        return result;
    }
}
