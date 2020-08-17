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

package software.amazon.smithy.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

/**
 * TODO: Rewrite the formatter parser to use a custom {@link SimpleParser}.
 */
final class CodeFormatter {
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-z]+[a-zA-Z0-9_.#$]*$");
    private static final Set<Character> VALID_FORMATTER_CHARS = SetUtils.of(
            '!', '#', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/', ':', ';', '<', '=', '>', '?', '@',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S',
            'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '[', ']', '^', '_', '`', '{', '|', '}', '~');

    private final Map<Character, BiFunction<Object, String, String>> formatters = new HashMap<>();

    void putFormatter(Character identifier, BiFunction<Object, String, String> formatter) {
        if (!VALID_FORMATTER_CHARS.contains(identifier)) {
            throw new IllegalArgumentException("Invalid formatter identifier: " + identifier);
        }

        formatters.put(identifier, formatter);
    }

    String format(char expressionStart, Object content, String indent, CodeWriter writer, Object... args) {
        String expression = String.valueOf(content);

        // Simple case of no arguments and no expressions.
        if (args.length == 0 && expression.indexOf(expressionStart) == -1) {
            return expression;
        }

        return parse(expressionStart, new State(content, indent, writer, args));
    }

    private String parse(char expressionStart, State state) {
        while (!state.eof()) {
            char c = state.c();
            state.next();
            if (c == expressionStart) {
                parseArgumentWrapper(expressionStart, state);
            } else {
                state.result.append(c);
            }
        }

        if (state.relativeIndex == -1) {
            ensureAllPositionalArgumentsWereUsed(state.expression, state.positionals);
        } else if (state.relativeIndex < state.args.length) {
            throw new IllegalArgumentException(String.format(
                    "Found %d unused relative format arguments: %s",
                    state.args.length - state.relativeIndex, state.expression));
        }

        return state.result.toString();
    }

    private void parseArgumentWrapper(char expressionStart, State state) {
        if (state.eof()) {
            throw new IllegalArgumentException("Invalid format string: " + state);
        }

        char c = state.c();
        if (c == expressionStart) {
            // $$ -> $
            state.result.append(expressionStart);
            state.next();
        } else if (c == '{') {
            parseBracedArgument(state);
        } else {
            parseArgument(state, false);
        }
    }

    private void parseBracedArgument(State state) {
        state.next(); // Skip "{"
        parseArgument(state, true);

        if (state.c() != '}') {
            throw new IllegalArgumentException("Unclosed expression argument: " + state);
        }

        state.next(); // Skip "}"
    }

    private void parseArgument(State state, boolean insideBrace) {
        if (state.eof()) {
            throw new IllegalArgumentException("Invalid format string: " + state);
        }

        char c = state.c();
        if (Character.isLowerCase(c)) {
            parseNamedArgument(state, insideBrace);
        } else if (Character.isDigit(c)) {
            parsePositionalArgument(state, insideBrace);
        } else {
            parseRelativeArgument(state, insideBrace);
        }
    }

    private void parseNamedArgument(State state, boolean insideBrace) {
        // Expand a named context value: "$" key ":" identifier
        String name = parseNameUntil(state, ':');
        state.next();

        // Consume the character after the colon.
        if (state.eof()) {
            throw new IllegalArgumentException("Expected an identifier after the ':' in a named argument: " + state);
        }

        char identifier = consumeFormatterIdentifier(state);
        state.result.append(applyFormatter(state, identifier, state.writer.getContext(name), insideBrace));
    }

    private char consumeFormatterIdentifier(State state) {
        char identifier = state.c();
        state.next();
        return identifier;
    }

    private void parsePositionalArgument(State state, boolean insideBrace) {
        // Expand a positional argument: "$" 1*digit identifier
        expectConsistentRelativePositionals(state, state.relativeIndex <= 0);
        state.relativeIndex = -1;
        int startPosition = state.position;
        while (state.next() && Character.isDigit(state.c())) {}
        int index = Integer.parseInt(state.expression.substring(startPosition, state.position)) - 1;

        if (index < 0 || index >= state.args.length) {
            throw new IllegalArgumentException(String.format(
                    "Positional argument index %d out of range of provided %d arguments in "
                    + "format string: %s", index, state.args.length, state));
        }

        Object arg = getPositionalArgument(state.expression, index, state.args);
        state.positionals[index] = true;
        char identifier = consumeFormatterIdentifier(state);
        state.result.append(applyFormatter(state, identifier, arg, insideBrace));
    }

    private void parseRelativeArgument(State state, boolean insideBrace) {
        // Expand to a relative argument.
        expectConsistentRelativePositionals(state, state.relativeIndex > -1);
        state.relativeIndex++;
        Object argument = getPositionalArgument(state.expression, state.relativeIndex - 1, state.args);
        char identifier = consumeFormatterIdentifier(state);
        state.result.append(applyFormatter(state, identifier, argument, insideBrace));
    }

    private String parseNameUntil(State state, char endToken) {
        int endIndex = state.expression.indexOf(endToken, state.position);

        if (endIndex == -1) {
            throw new IllegalArgumentException("Invalid named format argument: " + state);
        }

        String name = state.expression.substring(state.position, endIndex);
        ensureNameIsValid(state, name);
        state.position = endIndex;
        return name;
    }

    private static void expectConsistentRelativePositionals(State state, boolean expectation) {
        if (!expectation) {
            throw new IllegalArgumentException("Cannot mix positional and relative arguments: " + state);
        }
    }

    private static void ensureAllPositionalArgumentsWereUsed(String expression, boolean[] positionals) {
        int unused = 0;

        for (boolean b : positionals) {
            if (!b) {
                unused++;
            }
        }

        if (unused > 0) {
            throw new IllegalArgumentException(String.format(
                    "Found %d unused positional format arguments: %s", unused, expression));
        }
    }

    private Object getPositionalArgument(String content, int index, Object[] args) {
        if (index >= args.length) {
            throw new IllegalArgumentException(String.format(
                    "Given %d arguments but attempted to format index %d: %s", args.length, index, content));
        }

        return args[index];
    }

    private String applyFormatter(State state, char formatter, Object argument, boolean inBrace) {
        if (!formatters.containsKey(formatter)) {
            throw new IllegalArgumentException(String.format(
                    "Unknown formatter `%s` found in format string: %s", formatter, state));
        }

        String result = formatters.get(formatter).apply(argument, state.indent);

        if (!state.eof() && state.c() == '@') {
            if (!inBrace) {
                throw new IllegalArgumentException("Inline blocks can only be created inside braces: " + state);
            }
            result = expandInlineSection(state, result);
        }

        return result;
    }

    private String expandInlineSection(State state, String argument) {
        state.next(); // Skip "@"
        String sectionName = parseNameUntil(state, '}');
        ensureNameIsValid(state, sectionName);
        return state.writer.expandSection(sectionName, argument, s -> state.writer.write(s));
    }

    private static void ensureNameIsValid(State state, String name) {
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException(String.format(
                    "Invalid format expression name `%s` at position %d of: %s",
                    name, state.position + 1, state));
        }
    }

    private static final class State {
        StringBuilder result = new StringBuilder();
        int position = 0;
        int relativeIndex = 0;
        CodeWriter writer;
        String expression;
        String indent;
        Object[] args;
        boolean[] positionals;

        State(Object expression, String indent, CodeWriter writer, Object[] args) {
            this.expression = String.valueOf(expression);
            this.indent = indent;
            this.writer = writer;
            this.args = args;
            this.positionals = new boolean[args.length];
        }

        char c() {
            return expression.charAt(position);
        }

        boolean eof() {
            return position >= expression.length();
        }

        boolean next() {
            return ++position < expression.length() - 1;
        }

        @Override
        public String toString() {
            return expression;
        }
    }
}
