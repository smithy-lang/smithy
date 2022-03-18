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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SmithyInternalApi
final class CodeFormatter {

    private CodeFormatter() {}

    static void run(Appendable sink, AbstractCodeWriter<?> writer, String template, Object[] args) {
        try {
            TrackingAppendable wrappedSink = new TrackingAppendable(sink);
            Operation block = new Parser(writer, template, args).parse();
            block.apply(wrappedSink, writer);
        } catch (IOException e) {
            throw new RuntimeException("Error appending to CodeWriter template: " + e, e);
        }
    }

    private static class TrackingAppendable implements Appendable {
        int column = 0;
        private final Appendable delegate;

        TrackingAppendable(Appendable delegate) {
            this.delegate = delegate;
        }

        @Override
        public final String toString() {
            return delegate.toString();
        }

        @Override
        public final TrackingAppendable append(CharSequence csq) throws IOException {
            return append(csq, 0, csq.length());
        }

        @Override
        public final TrackingAppendable append(CharSequence csq, int start, int end) throws IOException {
            for (int i = start; i < end; i++) {
                append(csq.charAt(i));
            }
            return this;
        }

        @Override
        public TrackingAppendable append(char c) throws IOException {
            if (c == '\r' || c == '\n') {
                column = 0;
            } else {
                column++;
            }
            delegate.append(c);
            return this;
        }

        public int column() {
            return column;
        }
    }

    @FunctionalInterface
    private interface Operation {
        void apply(TrackingAppendable sink, AbstractCodeWriter<?> writer) throws IOException;

        // Writes literal segments of the input string.
        static Operation stringSlice(String source, int start, int end) {
            return (sink, writer) -> sink.append(source, start, end);
        }

        // Evaluates a formatter using the provided writer. This is done lazily because formatters
        // should only be evaluated inside conditions that evaluate to true. This ensures that formatters
        // with side effects don't have their side effects enacted when a condition is not evaluated.
        static Operation formatted(
                Function<AbstractCodeWriter<?>, Object> valueGetter,
                char formatter,
                Supplier<String> errorMessage
        ) {
            return (sink, writer) -> {
                Object value = valueGetter.apply(writer);
                String result = writer.applyFormatter(formatter, value);
                if (result == null) {
                    throw new RuntimeException(errorMessage.get());
                }
                sink.append(result);
            };
        }

        // Expands inline sections.
        static Operation inlineSection(String sectionName, Operation delegate) {
            return (sink, writer) -> {
                // First capture the given default value.
                TrackingAppendable buffer = new TrackingAppendable(new StringBuilder());
                delegate.apply(buffer, writer);
                String defaultValue = buffer.toString();
                // Create an interceptable code section for the inline section.
                CodeSection section = CodeSection.forName(sectionName);
                // Expand the section, passing in the default value as a start.
                String expanded = writer.expandSection(section, defaultValue, writer::writeInlineWithNoFormatting);
                // Inline sections are called within the context of an existing call to write or writeInline.
                // These _outer_ methods control whether a trailing newline appended to an inline section. This
                // behavior exactly matches how $C is treated.
                expanded = writer.removeTrailingNewline(expanded);
                // Write the final expanded section to the target sink.
                sink.append(expanded);
            };
        }

        // Used for "|". Wraps another operation and ensures newlines are properly indented.
        static Operation block(Operation delegate, String staticWhitespace) {
            return (sink, writer) -> delegate.apply(new BlockAppender(sink, staticWhitespace), writer);
        }
    }

    private static final class BlockAppender extends TrackingAppendable {
        private final TrackingAppendable delegate;
        private final int spaces;
        private final String staticWhitespace;
        private boolean previousIsCarriageReturn;
        private boolean previousIsNewline;

        BlockAppender(TrackingAppendable delegate, String staticWhitespace) {
            super(delegate);
            this.delegate = delegate;
            this.spaces = delegate.column();
            this.staticWhitespace = staticWhitespace;
        }

        @Override
        public int column() {
            // Note: this is never called because this appended is never used as a delegate.
            return super.column() + spaces;
        }

        @Override
        public TrackingAppendable append(char c) throws IOException {
            if (previousIsNewline) {
                writeSpaces();
                previousIsNewline = false;
            }

            if (c == '\n') {
                delegate.append('\n');
                previousIsNewline = true;
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

    /**
     * ParseBlocks are used to track the state of a template. When a condition or loop is parsed,
     * it pushes a new ParseBlock. Any operations that need to be performed in that pushed block
     * are tracked within the block and only executed if the block is valid, or executed N times
     * for the value of a loop.
     */
    private abstract static class ParseBlock implements Operation {
        private final String variable;

        ParseBlock(String variable) {
            this.variable = variable;
        }

        final String variable() {
            return variable;
        }

        abstract void push(Operation operation);

        /**
         * A ParseBlock that always runs.
         */
        static class Unconditional extends ParseBlock {
            private final int line;
            private final int column;
            private final List<Operation> operations = new ArrayList<>();

            Unconditional(String variable, int line, int column) {
                super(variable);
                this.line = line;
                this.column = column;
            }

            @Override
            public void apply(TrackingAppendable sink, AbstractCodeWriter<?> writer) throws IOException {
                for (Operation operation : operations) {
                    operation.apply(sink, writer);
                }
            }

            @Override
            public void push(Operation operation) {
                operations.add(operation);
            }
        }

        /**
         * A ParseBlock that only runs if the referenced variable matches the condition.
         */
        static final class Conditional extends Unconditional {
            private final boolean negate;

            Conditional(String variable, boolean negate, int line, int column) {
                super(variable, line, column);
                this.negate = negate;
            }

            @Override
            public void apply(TrackingAppendable sink, AbstractCodeWriter<?> writer) throws IOException {
                Object value = writer.getContext(variable());
                if (!isConditionTruthy(value) == negate) {
                    super.apply(sink, writer);
                }
            }
        }

        /**
         * A ParseBlock that repeats a template for each value referenced by the block. Each iteration pushes
         * a state to the writer that includes a "currentKey" and "currentValue" context property representing
         * the current index or key and the current value.
         */
        static final class Loop extends Unconditional {
            Loop(String variableName, int line, int column) {
                super(variableName, line, column);
            }

            @Override
            public void apply(TrackingAppendable sink, AbstractCodeWriter<?> writer) throws IOException {
                Object value = writer.getContext(variable());
                Iterator<? extends Map.Entry<?, ?>> iterator = getValueIterator(value);
                boolean isFirst = true;
                while (iterator.hasNext()) {
                    Map.Entry<?, ?> current = iterator.next();
                    writer.pushState();
                    writer.putContext("currentKey", current.getKey());
                    writer.putContext("currentValue", current.getValue());
                    writer.putContext("currentIsFirst", isFirst);
                    writer.putContext("currentIsLast", !iterator.hasNext());
                    isFirst = false;
                    super.apply(sink, writer);
                    writer.popState();
                }
            }
        }
    }

    private static boolean isConditionTruthy(Object value) {
        if (value == null) {
            return false;
        } else if (value instanceof Optional) {
            return ((Optional<?>) value).isPresent();
        } else if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof Iterable) {
            return ((Iterable<?>) value).iterator().hasNext();
        } else if (value instanceof Map) {
            return !((Map<?, ?>) value).isEmpty();
        } else if (value instanceof String) {
            return !((String) value).isEmpty();
        } else {
            return true;
        }
    }

    private static Iterator<? extends Map.Entry<?, ?>> getValueIterator(Object value) {
        if (value instanceof Map) {
            return ((Map<?, ?>) value).entrySet().iterator();
        } else if (value instanceof Iterable) {
            Iterator<?> iter = ((Iterable<?>) value).iterator();
            return new Iterator<Map.Entry<?, ?>>() {
                int position = 0;

                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                @Override
                public Map.Entry<?, ?> next() {
                    return Pair.of(position++, iter.next());
                }
            };
        } else {
            return Collections.emptyIterator();
        }
    }

    private static final class Parser {
        private static final Pattern NAME_PATTERN = Pattern.compile("^[a-z]+[a-zA-Z0-9_.#$]*$");

        private final String template;
        private final SimpleParser parser;
        private final char expressionStart;
        private final AbstractCodeWriter<?> writer;
        private final Object[] arguments;
        private final boolean[] positionals;
        private int relativeIndex = 0;
        private final Deque<ParseBlock> blocks = new ArrayDeque<>();

        Parser(AbstractCodeWriter<?> writer, String template, Object[] arguments) {
            this.template = template;
            this.writer = writer;
            this.expressionStart = writer.getExpressionStart();
            this.parser = new SimpleParser(template);
            this.arguments = arguments;
            this.positionals = new boolean[arguments.length];
            blocks.add(new ParseBlock.Unconditional("", 1, 1));
        }

        private void pushOperation(Operation op) {
            blocks.getFirst().push(op);
        }

        private RuntimeException error(String message) {
            return parser.syntax(createErrorMessage(message));
        }

        private String createErrorMessage(String message) {
            return message + " (template: " + template + ") " + writer.getDebugInfo();
        }

        private Operation parse() {
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
                    int pendingTextStart = parsingLiteral ? literalStartCharacter : -1;
                    parsingLiteral = false;
                    parseArgument(pendingTextStart);
                    literalStartCharacter = parser.position();
                }
            }

            if (parsingLiteral) {
                pushOperation(Operation.stringSlice(template, literalStartCharacter, parser.position()));
            }

            if (relativeIndex == -1) {
                ensureAllPositionalArgumentsWereUsed();
            } else if (relativeIndex < arguments.length) {
                int unusedCount = arguments.length - relativeIndex;
                throw error(String.format("Found %d unused relative format arguments", unusedCount));
            }

            if (blocks.size() == 1) {
                return blocks.getFirst();
            }

            throw new IllegalArgumentException(
                    "Unclosed parse conditional blocks: ["
                    + blocks.stream()
                            // Don't include the root block.
                            .filter(b -> !b.variable().isEmpty())
                            .map(ParseBlock::variable)
                            .collect(Collectors.joining(", "))
                    + "]");
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

        private void parseArgument(int pendingTextStart) {
            if (parser.peek() == '{') {
                parseBracedArgument(pendingTextStart);
            } else {
                if (pendingTextStart > -1) {
                    pushOperation(Operation.stringSlice(parser.expression(), pendingTextStart, parser.position() - 1));
                }
                pushOperation(parseNormalArgument());
            }
        }

        private void parseBracedArgument(int pendingTextStart) {
            // Track the starting position of the interpolation (here minus the opening '$').
            int startPosition = parser.position() - 1;
            int startLine = parser.line();
            int startColumn = parser.column() - 2;

            parser.expect('{');

            if (parser.peek() == '?' || parser.peek() == '^' || parser.peek() == '#') {
                pushBlock(pendingTextStart, startPosition, startLine, startColumn, parser.peek());
                return;
            }

            if (parser.peek() == '/') {
                popBlock(pendingTextStart, startPosition, startColumn);
                return;
            }

            if (pendingTextStart > -1) {
                pushOperation(Operation.stringSlice(parser.expression(), pendingTextStart, startPosition));
            }

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

            pushOperation(operation);
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

        private void pushBlock(int pendingTextStart, int startPosition, int startLine, int startColumn, char c) {
            parser.expect(c);
            String name = parseArgumentName();
            parser.expect('}');
            handleConditionalOnLine(pendingTextStart, startPosition, startColumn);
            blocks.push(createBlockForChar(c, name, startLine, startColumn));
        }

        private ParseBlock createBlockForChar(char c, String name, int line, int column) {
            switch (c) {
                case '#':
                    return new ParseBlock.Loop(name, line, column);
                case '^':
                    return new ParseBlock.Conditional(name, true, line, column);
                case '?':
                default:
                    return new ParseBlock.Conditional(name, false, line, column);
            }
        }

        private void handleConditionalOnLine(int pendingTextStart, int startPosition, int startColumn) {
            // If the expression is not followed directly by a newline, then all leading text.
            if (parser.peek() != '\r' && parser.peek() != '\n' && parser.peek() != Character.MIN_VALUE) {
                if (pendingTextStart > -1) {
                    pushOperation(Operation.stringSlice(parser.expression(), pendingTextStart, startPosition));
                }
            } else if (isAllLeadingWhitespaceOnLine(startPosition, startColumn)) {
                if (pendingTextStart > -1) {
                    pushOperation(Operation.stringSlice(parser.expression(), pendingTextStart,
                                                        startPosition - startColumn));
                }
                parser.skip();
            } else if (pendingTextStart > -1) {
                pushOperation(Operation.stringSlice(parser.expression(), pendingTextStart, startPosition));
            }
        }

        private void popBlock(int pendingTextStart, int startPosition, int startColumn) {
            parser.expect('/');
            String name = parseArgumentName();
            parser.expect('}');

            if (blocks.size() == 1) {
                throw new IllegalArgumentException("Attempted to close unopened tag: '" + name + "'");
            }

            handleConditionalOnLine(pendingTextStart, startPosition, startColumn);

            ParseBlock block = blocks.pop();
            if (!block.variable().equals(name)) {
                throw new IllegalArgumentException("Invalid closing tag: '" + name + "'. Expected: '"
                                                   + block.variable() + "'");
            }

            blocks.getFirst().push(block);
        }

        private Operation parseNormalArgument() {
            char c = parser.peek();

            // Create the appropriate function for retrieving the value. Positional and relative arguments
            // are known statically, but getting context properties is deferring until it's time to write.
            // This allows things like loops to populate loop control variables.
            Function<AbstractCodeWriter<?>, Object> getter;
            if (Character.isLowerCase(c)) {
                String name = parseNamedArgumentName();
                getter = w -> w.getContext(name);
            } else if (Character.isDigit(c)) {
                getter = parsePositionalArgumentGetter();
            } else {
                getter = parseRelativeArgumentGetter();
            }

            // Parse the formatter and apply it.
            int line = parser.line();
            int column = parser.column();
            char identifier = parser.expect(CodeWriterFormatterContainer.VALID_FORMATTER_CHARS);

            // The error message needs to be created here and given to the operation in way that it can
            // throw with an appropriate message.
            return Operation.formatted(getter, identifier, () -> createErrorMessage(String.format(
                    "Syntax error at line %d column %d: Unknown formatter `%c` found in format string",
                    line, column, identifier)));
        }

        private String parseArgumentName() {
            int start = parser.position();
            parser.consumeUntilNoLongerMatches(c -> c != ':' && c != '}');
            String name = parser.sliceFrom(start);
            ensureNameIsValid(name);
            return name;
        }

        private String parseNamedArgumentName() {
            // Expand a named context value: "$" key ":" identifier
            String name = parseArgumentName();
            parser.expect(':');

            // Consume the character after the colon.
            if (parser.eof()) {
                throw error("Expected an identifier after the ':' in a named argument");
            }

            return name;
        }

        private Function<AbstractCodeWriter<?>, Object> parseRelativeArgumentGetter() {
            if (relativeIndex == -1) {
                throw error("Cannot mix positional and relative arguments");
            }

            relativeIndex++;
            Object result = getPositionalArgument(relativeIndex - 1);
            return w -> result;
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

        private Function<AbstractCodeWriter<?>, Object> parsePositionalArgumentGetter() {
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

            Object value = getPositionalArgument(index);
            return w -> value;
        }

        private void ensureNameIsValid(String name) {
            if (!NAME_PATTERN.matcher(name).matches()) {
                throw error(String.format("Invalid format expression name `%s`", name));
            }
        }
    }
}
