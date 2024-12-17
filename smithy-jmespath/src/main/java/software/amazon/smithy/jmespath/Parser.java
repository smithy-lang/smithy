/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.jmespath.ast.AndExpression;
import software.amazon.smithy.jmespath.ast.ComparatorExpression;
import software.amazon.smithy.jmespath.ast.ComparatorType;
import software.amazon.smithy.jmespath.ast.CurrentExpression;
import software.amazon.smithy.jmespath.ast.ExpressionTypeExpression;
import software.amazon.smithy.jmespath.ast.FieldExpression;
import software.amazon.smithy.jmespath.ast.FilterProjectionExpression;
import software.amazon.smithy.jmespath.ast.FlattenExpression;
import software.amazon.smithy.jmespath.ast.FunctionExpression;
import software.amazon.smithy.jmespath.ast.IndexExpression;
import software.amazon.smithy.jmespath.ast.LiteralExpression;
import software.amazon.smithy.jmespath.ast.MultiSelectHashExpression;
import software.amazon.smithy.jmespath.ast.MultiSelectListExpression;
import software.amazon.smithy.jmespath.ast.NotExpression;
import software.amazon.smithy.jmespath.ast.ObjectProjectionExpression;
import software.amazon.smithy.jmespath.ast.OrExpression;
import software.amazon.smithy.jmespath.ast.ProjectionExpression;
import software.amazon.smithy.jmespath.ast.SliceExpression;
import software.amazon.smithy.jmespath.ast.Subexpression;

/**
 * A top-down operator precedence parser (aka Pratt parser) for JMESPath.
 */
final class Parser {

    /** The maximum binding power for a token that can stop a projection. */
    private static final int PROJECTION_STOP = 10;

    /** Tokens that can start an expression. */
    private static final TokenType[] NUD_TOKENS = {
            TokenType.CURRENT,
            TokenType.IDENTIFIER,
            TokenType.LITERAL,
            TokenType.STAR,
            TokenType.LBRACE,
            TokenType.LBRACKET,
            TokenType.FLATTEN,
            TokenType.EXPREF,
            TokenType.NOT,
            TokenType.FILTER,
            TokenType.LPAREN
    };

    /** Tokens that can follow led tokens. */
    private static final TokenType[] LED_TOKENS = {
            TokenType.DOT,
            TokenType.LBRACKET,
            TokenType.OR,
            TokenType.AND,
            TokenType.PIPE,
            TokenType.FLATTEN,
            TokenType.FILTER,
            TokenType.EQUAL,
            TokenType.NOT_EQUAL,
            TokenType.GREATER_THAN,
            TokenType.GREATER_THAN_EQUAL,
            TokenType.LESS_THAN,
            TokenType.LESS_THAN_EQUAL,
            // While not found in the led() method, a led LPAREN is handled
            // when parsing a nud identifier because it creates a function.
            TokenType.LPAREN
    };

    private final String expression;
    private final TokenIterator iterator;

    private Parser(String expression) {
        this.expression = expression;
        iterator = Lexer.tokenize(expression);
    }

    static JmespathExpression parse(String expression) {
        Parser parser = new Parser(expression);
        JmespathExpression result = parser.expression(0);
        parser.iterator.expect(TokenType.EOF);
        return result;
    }

    private JmespathExpression expression(int rbp) {
        JmespathExpression left = nud();
        while (iterator.hasNext() && rbp < iterator.peek().type.lbp) {
            left = led(left);
        }
        return left;
    }

    private JmespathExpression nud() {
        Token token = iterator.expect(NUD_TOKENS);
        switch (token.type) {
            case CURRENT: // Example: @
                return new CurrentExpression(token.line, token.column);
            case IDENTIFIER: // Example: foo
                // For example, "foo(" starts a function expression.
                if (iterator.peek().type == TokenType.LPAREN) {
                    iterator.expect(TokenType.LPAREN);
                    List<JmespathExpression> arguments = parseList(TokenType.RPAREN);
                    return new FunctionExpression(token.value.expectStringValue(), arguments, token.line, token.column);
                } else {
                    return new FieldExpression(token.value.expectStringValue(), token.line, token.column);
                }
            case STAR: // Example: *
                return parseWildcardObject(new CurrentExpression(token.line, token.column));
            case LITERAL: // Example: `true`
                return new LiteralExpression(token.value.getValue(), token.line, token.column);
            case LBRACKET: // Example: [1]
                return parseNudLbracket();
            case LBRACE: // Example: {foo: bar}
                return parseNudLbrace();
            case FLATTEN: // Example: [].bar
                return parseFlatten(new CurrentExpression(token.line, token.column));
            case EXPREF: // Example: sort_by(@, &foo)
                JmespathExpression expressionRef = expression(token.type.lbp);
                return new ExpressionTypeExpression(expressionRef, token.line, token.column);
            case NOT: // Example: !foo
                JmespathExpression notNode = expression(token.type.lbp);
                return new NotExpression(notNode, token.line, token.column);
            case FILTER: // Example: [?foo == bar]
                return parseFilter(new CurrentExpression(token.line, token.column));
            case LPAREN: // Example (foo)
                JmespathExpression insideParens = expression(0);
                iterator.expect(TokenType.RPAREN);
                return insideParens;
            default:
                throw iterator.syntax("Invalid nud token: " + token);
        }
    }

    private JmespathExpression led(JmespathExpression left) {
        Token token = iterator.expect(LED_TOKENS);

        switch (token.type) {
            case DOT:
                // For example, "foo.bar"
                if (iterator.peek().type == TokenType.STAR) {
                    // "Example: foo.*". This is mostly an optimization of the
                    // generated AST to not need a subexpression to contain the
                    // projection.
                    iterator.expect(TokenType.STAR); // skip the "*".
                    return parseWildcardObject(left);
                } else {
                    // "foo.*", "foo.bar", "foo.[bar]", "foo.length(@)", etc.
                    JmespathExpression dotRhs = parseDotRhs(TokenType.DOT.lbp);
                    return new Subexpression(left, dotRhs, token.line, token.column);
                }
            case FLATTEN: // Example: a[].b
                return parseFlatten(left);
            case OR: // Example: a || b
                return new OrExpression(left, expression(token.type.lbp), token.line, token.column);
            case AND: // Example: a && b
                return new AndExpression(left, expression(token.type.lbp), token.line, token.column);
            case PIPE: // Example: a | b
                return new Subexpression(left, expression(token.type.lbp), token.line, token.column, true);
            case FILTER: // Example: a[?foo == bar]
                return parseFilter(left);
            case LBRACKET:
                Token bracketToken = iterator.expectPeek(TokenType.NUMBER, TokenType.COLON, TokenType.STAR);
                if (bracketToken.type == TokenType.STAR) {
                    // For example, "foo[*]"
                    return parseWildcardIndex(left);
                } else {
                    // For example, "foo[::1]", "foo[1]"
                    return new Subexpression(left, parseIndex(), token.line, token.column);
                }
            case EQUAL: // Example: a == b
                return parseComparator(ComparatorType.EQUAL, left);
            case NOT_EQUAL: // Example: a != b
                return parseComparator(ComparatorType.NOT_EQUAL, left);
            case GREATER_THAN: // Example: a > b
                return parseComparator(ComparatorType.GREATER_THAN, left);
            case GREATER_THAN_EQUAL: // Example: a >= b
                return parseComparator(ComparatorType.GREATER_THAN_EQUAL, left);
            case LESS_THAN: // Example: a < b
                return parseComparator(ComparatorType.LESS_THAN, left);
            case LESS_THAN_EQUAL: // Example: a <= b
                return parseComparator(ComparatorType.LESS_THAN_EQUAL, left);
            default:
                throw iterator.syntax("Invalid led token: " + token);
        }
    }

    private JmespathExpression parseNudLbracket() {
        switch (iterator.expectNotEof().type) {
            case NUMBER:
            case COLON:
                // An index is parsed when things like '[1' or '[1:' are encountered.
                return parseIndex();
            case STAR:
                if (iterator.peek(1).type == TokenType.RBRACKET) {
                    // A led '[*]' sets the left-hand side of the projection to the left node,
                    // but a nud '[*]' uses the current node as the left node.
                    return parseWildcardIndex(new CurrentExpression(iterator.line(), iterator.column()));
                } // fall-through
            default:
                // Everything else is a multi-select list that creates an array of values.
                return parseMultiList();
        }
    }

    // Parses [0], [::-1], [0:-1], [0:1], etc.
    private JmespathExpression parseIndex() {
        int line = iterator.line();
        int column = iterator.column();
        Integer[] parts = new Integer[] {null, null, 1}; // start, stop, step (defaults to 1)
        int pos = 0;

        loop: while (true) {
            Token next = iterator.expectPeek(TokenType.NUMBER, TokenType.RBRACKET, TokenType.COLON);
            switch (next.type) {
                case NUMBER:
                    iterator.expect(TokenType.NUMBER);
                    parts[pos] = next.value.expectNumberValue().intValue();
                    iterator.expectPeek(TokenType.COLON, TokenType.RBRACKET);
                    break;
                case RBRACKET:
                    break loop;
                default: // COLON
                    iterator.expect(TokenType.COLON);
                    if (++pos == 3) {
                        throw iterator.syntax("Too many colons in slice expression");
                    }
                    break;
            }
        }

        iterator.expect(TokenType.RBRACKET);

        if (pos == 0) {
            // No colons were found, so this is a simple index extraction.
            return new IndexExpression(parts[0], line, column);
        }

        // Sliced array from start (e.g., [2:]). A projection is created here
        // because a projection has very similar semantics to what's actually
        // happening here (i.e., turn the LHS into an array, take specific
        // items from it, then pass the result to RHS). The only difference
        // between foo[*] and foo[1:] is the size of the array. Anything that
        // selects more than one element is a generally a projection.
        JmespathExpression slice = new SliceExpression(parts[0], parts[1], parts[2], line, column);
        JmespathExpression rhs = parseProjectionRhs(TokenType.STAR.lbp);
        return new ProjectionExpression(slice, rhs, line, column);
    }

    private JmespathExpression parseMultiList() {
        int line = iterator.line();
        int column = iterator.column();
        List<JmespathExpression> nodes = parseList(TokenType.RBRACKET);
        return new MultiSelectListExpression(nodes, line, column);
    }

    // Parse a comma separated list of expressions until a closing token.
    //
    // This function is used for functions and multi-list parsing. Note
    // that this function allows empty lists. This is fine when parsing
    // multi-list expressions because "[]" is tokenized as Token::Flatten.
    //
    // Examples: [foo, bar], foo(bar), foo(), foo(baz, bar).
    private List<JmespathExpression> parseList(TokenType closing) {
        List<JmespathExpression> nodes = new ArrayList<>();

        while (iterator.peek().type != closing) {
            nodes.add(expression(0));
            // Skip commas.
            if (iterator.peek().type == TokenType.COMMA) {
                iterator.expect(TokenType.COMMA);
                if (iterator.peek().type == closing) {
                    throw iterator.syntax("Invalid token after ',': " + iterator.peek());
                }
            }
        }

        iterator.expect(closing);
        return nodes;
    }

    private JmespathExpression parseNudLbrace() {
        int line = iterator.line();
        int column = iterator.column();
        Map<String, JmespathExpression> entries = new LinkedHashMap<>();

        while (iterator.hasNext()) {
            // A multi-select-hash requires at least one key value pair.
            Token key = iterator.expect(TokenType.IDENTIFIER);
            iterator.expect(TokenType.COLON);
            JmespathExpression value = expression(0);
            entries.put(key.value.expectStringValue(), value);

            if (iterator.expectPeek(TokenType.RBRACE, TokenType.COMMA).type == TokenType.COMMA) {
                iterator.expect(TokenType.COMMA);
            } else {
                break;
            }
        }

        iterator.expect(TokenType.RBRACE);
        return new MultiSelectHashExpression(entries, line, column);
    }

    // Creates a projection for "[*]".
    private JmespathExpression parseWildcardIndex(JmespathExpression left) {
        int line = iterator.line();
        int column = iterator.column();
        iterator.expect(TokenType.STAR);
        iterator.expect(TokenType.RBRACKET);
        JmespathExpression right = parseProjectionRhs(TokenType.STAR.lbp);
        return new ProjectionExpression(left, right, line, column);
    }

    // Creates a projection for "*".
    private JmespathExpression parseWildcardObject(JmespathExpression left) {
        int line = iterator.line();
        int column = iterator.column() - 1; // backtrack
        return new ObjectProjectionExpression(left, parseProjectionRhs(TokenType.STAR.lbp), line, column);
    }

    // Creates a projection for "[]" that wraps the LHS to flattens the result.
    private JmespathExpression parseFlatten(JmespathExpression left) {
        int line = iterator.line();
        int column = iterator.column();
        JmespathExpression flatten = new FlattenExpression(left, left.getLine(), left.getColumn());
        JmespathExpression right = parseProjectionRhs(TokenType.STAR.lbp);
        return new ProjectionExpression(flatten, right, line, column);
    }

    // Parses the right hand side of a projection, using the given LBP to
    // determine when to stop consuming tokens.
    private JmespathExpression parseProjectionRhs(int lbp) {
        Token next = iterator.expectNotEof();
        if (next.type == TokenType.DOT) {
            // foo.*.bar
            iterator.expect(TokenType.DOT);
            return parseDotRhs(lbp);
        } else if (next.type == TokenType.LBRACKET || next.type == TokenType.FILTER) {
            // foo[*][1], foo[*][?baz]
            return expression(lbp);
        } else if (next.type.lbp < PROJECTION_STOP) {
            // foo.* || bar
            return new CurrentExpression(next.line, next.column);
        } else {
            throw iterator.syntax("Invalid projection");
        }
    }

    private JmespathExpression parseComparator(ComparatorType comparatorType, JmespathExpression lhs) {
        int line = iterator.line();
        int column = iterator.column();
        JmespathExpression rhs = expression(TokenType.EQUAL.lbp);
        return new ComparatorExpression(comparatorType, lhs, rhs, line, column);
    }

    // Parses the right hand side of a ".".
    private JmespathExpression parseDotRhs(int lbp) {
        Token token = iterator.expectPeek(
                TokenType.LBRACKET,
                TokenType.LBRACE,
                TokenType.STAR,
                TokenType.IDENTIFIER);

        if (token.type == TokenType.LBRACKET) {
            // Skip '[', parse the list.
            iterator.next();
            return parseMultiList();
        } else {
            return expression(lbp);
        }
    }

    // Parses a filter token into a Projection that filters the right
    // side of the projection using a comparison node. If the comparison
    // returns a truthy value, then the value is yielded by the projection
    // to the right hand side.
    private JmespathExpression parseFilter(JmespathExpression left) {
        // Parse the LHS of the condition node.
        JmespathExpression condition = expression(0);
        // Eat the closing bracket.
        iterator.expect(TokenType.RBRACKET);
        JmespathExpression conditionRhs = parseProjectionRhs(TokenType.FILTER.lbp);
        return new FilterProjectionExpression(
                left,
                condition,
                conditionRhs,
                condition.getLine(),
                condition.getColumn());
    }
}
