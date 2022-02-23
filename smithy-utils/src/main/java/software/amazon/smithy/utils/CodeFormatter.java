/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@SmithyInternalApi
final class CodeFormatter {

    private CodeFormatter() {}

    static void run(Appendable sink, CodeWriter writer, String template, Object[] args) {
        ColumnTrackingAppendable wrappedSink = new ColumnTrackingAppendable(sink);
        List<Operation> program = new Parser(writer, template, args).parse();
        try {
            for (Operation op : program) {
                op.apply(wrappedSink, writer, wrappedSink.column);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error appending to CodeWriter template: " + e, e);
        }
    }

    private abstract static class DecoratedAppendable implements Appendable {
        @Override
        public Appendable append(CharSequence csq) throws IOException {
            return append(csq, 0, csq.length());
        }

        @Override
        public Appendable append(CharSequence csq, int start, int end) throws IOException {
            for (int i = start; i < end; i++) {
                append(csq.charAt(i));
            }
            return this;
        }
    }

    private static final class ColumnTrackingAppendable extends DecoratedAppendable {
        int column = 0;
        private final Appendable delegate;

        ColumnTrackingAppendable(Appendable delegate) {
            this.delegate = delegate;
        }

        @Override
        public Appendable append(char c) throws IOException {
            if (c == '\r' || c == '\n') {
                column = 0;
            } else {
                column++;
            }
            delegate.append(c);
            return this;
        }
    }

    @FunctionalInterface
    private interface Operation {
        void apply(Appendable sink, CodeWriter writer, int column) throws IOException;

        // Writes literal segments of the input string.
        static Operation stringSlice(String source, int start, int end) {
            return (sink, writer, column) -> sink.append(source, start, end);
        }

        // An operation that writes a resolved variable to the writer (e.g., positional arguments).
        static Operation staticValue(String value) {
            return (sink, writer, column) -> sink.append(value);
        }

        // Expands inline sections.
        static Operation inlineSection(String sectionName, Operation delegate) {
            return (sink, writer, column) -> {
                StringBuilder buffer = new StringBuilder();
                delegate.apply(buffer, writer, column);
                sink.append(writer.expandSection(sectionName, buffer.toString(), writer::writeWithNoFormatting));
            };
        }

        // Used for "|". Wraps another operation and ensures newlines are properly indented.
        static Operation block(Operation delegate, String staticWhitespace) {
            return (sink, writer, column) -> delegate.apply(
                    new BlockAppender(sink, column, staticWhitespace), writer, column);
        }
    }

    private static final class BlockAppender extends DecoratedAppendable {
        private final Appendable delegate;
        private final int spaces;
        private final String staticWhitespace;
        private boolean previousIsCarriageReturn;

        BlockAppender(Appendable delegate, int spaces, String staticWhitespace) {
            this.delegate = delegate;
            this.spaces = spaces;
            this.staticWhitespace = staticWhitespace;
        }

        @Override
        public Appendable append(char c) throws IOException {
            if (c == '\n') {
                delegate.append('\n');
                writeSpaces();
                previousIsCarriageReturn = false;
            } else {
                if (previousIsCarriageReturn) {
                    writeSpaces();
                }
                previousIsCarriageReturn = c == '\r';
                delegate.append(c);
            }

            return this;
        }

        private void writeSpaces() throws IOException {
            if (staticWhitespace != null) {
                delegate.append(staticWhitespace);
            } else {
                for (int i = 0; i < spaces; i++) {
                    delegate.append(' ');
                }
            }
        }
    }

    private static final class Parser {
        private static final Pattern NAME_PATTERN = Pattern.compile("^[a-z]+[a-zA-Z0-9_.#$]*$");

        private final String template;
        private final SimpleParser parser;
        private final char expressionStart;
        private final CodeWriter writer;
        private final Object[] arguments;
        private final boolean[] positionals;
        private final List<Operation> operations = new ArrayList<>();
        private int relativeIndex = 0;

        Parser(CodeWriter writer, String template, Object[] arguments) {
            this.template = template;
            this.writer = writer;
            this.expressionStart = writer.getExpressionStart();
            this.parser = new SimpleParser(template);
            this.arguments = arguments;
            this.positionals = new boolean[arguments.length];
        }

        private void pushOperation(Operation op) {
            operations.add(op);
        }

        private RuntimeException error(String message) {
            return parser.syntax(message + " (template: " + template + ") " + writer.getDebugInfo());
        }

        private List<Operation> parse() {
            boolean parsingLiteral = false;
            int literalStartCharacter = 0;

            while (!parser.eof()) {
                char c = parser.peek();
                parser.skip();

                if (c != expressionStart) {
                    parsingLiteral = true;
                } else if (parser.peek() == expressionStart) {
                    // Don't write escaped expression starts.
                    pushOperation(Operation.stringSlice(template, literalStartCharacter, parser.position()));
                    parser.expect(expressionStart);
                    parsingLiteral = true;
                    literalStartCharacter = parser.position();
                } else {
                    if (parsingLiteral) {
                        // If previously parsing literal text, then add that to the operation (not including '$').
                        pushOperation(Operation.stringSlice(template, literalStartCharacter, parser.position() - 1));
                        parsingLiteral = false;
                    }
                    pushOperation(parseArgument());
                    literalStartCharacter = parser.position();
                }
            }

            if (parsingLiteral && literalStartCharacter < parser.position() && parser.position() > 0) {
                pushOperation(Operation.stringSlice(template, literalStartCharacter, parser.position()));
            }

            if (relativeIndex == -1) {
                ensureAllPositionalArgumentsWereUsed();
            } else if (relativeIndex < arguments.length) {
                int unusedCount = arguments.length - relativeIndex;
                throw error(String.format("Found %d unused relative format arguments", unusedCount));
            }

            return operations;
        }

        private void ensureAllPositionalArgumentsWereUsed() {
            int unused = 0;
            for (boolean b : positionals) {
                if (!b) {
                    unused++;
                }
            }
            if (unused > 0) {
                throw error(String.format("Found %d unused positional format arguments", unused));
            }
        }

        private Operation parseArgument() {
            return parser.peek() == '{' ? parseBracedArgument() : parseNormalArgument();
        }

        private Operation parseBracedArgument() {
            // Track the starting position of the interpolation (here minus the opening '$').
            int startPosition = parser.position() - 1;
            int startColumn = parser.column() - 2;

            parser.expect('{');
            Operation operation = parseNormalArgument();

            if (parser.peek() == '@') {
                parser.skip();
                int start = parser.position();
                parser.consumeUntilNoLongerMatches(c -> c != '}' && c != '|');
                String sectionName = parser.sliceFrom(start);
                ensureNameIsValid(sectionName);
                operation = Operation.inlineSection(sectionName, operation);
            }

            if (parser.peek() == '|') {
                String staticWhitespace = isAllLeadingWhitespaceOnLine(startPosition, startColumn)
                        ? template.substring(startPosition - startColumn, startPosition)
                        : null;
                parser.expect('|');
                operation = Operation.block(operation, staticWhitespace);
            }

            parser.expect('}');

            return operation;
        }

        private boolean isAllLeadingWhitespaceOnLine(int startPosition, int startColumn) {
            for (int i = startPosition - startColumn; i < startPosition; i++) {
                char ch = template.charAt(i);
                if (ch != ' ' && ch != '\t') {
                    return false;
                }
            }
            return true;
        }

        private Operation parseNormalArgument() {
            char c = parser.peek();

            Object value;
            if (Character.isLowerCase(c)) {
                value = parseNamedArgument();
            } else if (Character.isDigit(c)) {
                value = parsePositionalArgument();
            } else {
                value = parseRelativeArgument();
            }

            // Parse the formatter and apply it.
            String formatted = consumeAndApplyFormatterIdentifier(value);
            return Operation.staticValue(formatted);
        }

        private Object parseNamedArgument() {
            // Expand a named context value: "$" key ":" identifier
            int start = parser.position();
            parser.consumeUntilNoLongerMatches(c -> c != ':');
            String name = parser.sliceFrom(start);
            ensureNameIsValid(name);

            parser.expect(':');

            // Consume the character after the colon.
            if (parser.eof()) {
                throw error("Expected an identifier after the ':' in a named argument");
            }

            return writer.getContext(name);
        }

        private String consumeAndApplyFormatterIdentifier(Object value) {
            char identifier = parser.expect(CodeWriterFormatterContainer.VALID_FORMATTER_CHARS);
            String result = writer.applyFormatter(identifier, value);
            if (result == null) {
                throw error(String.format("Unknown formatter `%c` found in format string", identifier));
            }
            return result;
        }

        private Object parseRelativeArgument() {
            if (relativeIndex == -1) {
                throw error("Cannot mix positional and relative arguments");
            }

            relativeIndex++;
            return getPositionalArgument(relativeIndex - 1);
        }

        private Object getPositionalArgument(int index) {
            if (index >= arguments.length) {
                throw error(String.format("Given %d arguments but attempted to format index %d",
                                          arguments.length, index));
            } else {
                // Track the usage of the positional argument.
                positionals[index] = true;
                return arguments[index];
            }
        }

        private Object parsePositionalArgument() {
            // Expand a positional argument: "$" 1*digit identifier
            if (relativeIndex > 0) {
                throw error("Cannot mix positional and relative arguments");
            }

            relativeIndex = -1;
            int startPosition = parser.position();
            parser.consumeUntilNoLongerMatches(Character::isDigit);
            int index = Integer.parseInt(parser.sliceFrom(startPosition)) - 1;

            if (index < 0 || index >= arguments.length) {
                throw error(String.format(
                        "Positional argument index %d out of range of provided %d arguments in format string",
                        index, arguments.length));
            }

            return getPositionalArgument(index);
        }

        private void ensureNameIsValid(String name) {
            if (!NAME_PATTERN.matcher(name).matches()) {
                throw error(String.format("Invalid format expression name `%s`", name));
            }
        }
    }
}
