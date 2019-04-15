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

package software.amazon.smithy.utils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Helper class for generating code.
 *
 * <p>A CodeWriter should be used for more advanced code generation than
 * what is possible inside of templates. A CodeWriter can be used to write
 * basically any kind of code, including whitespace sensitive and brace-based.
 * However, note that inserting and closing braces is not handled
 * automatically by this class.
 *
 * <p>The CodeWriter can maintain a stack of transformation states, including
 * the character used for newlines, the text used to indent, a prefix to add
 * before each line, the number of times to indent, whether or not whitespace
 * is trimmed from the end of newlines, whether or not N number of newlines
 * are combined into a single newline, and how messages are formatted. State
 * can be pushed onto the stack using {@link #pushState} which copies the
 * current state. Mutations can then be made to the top-most state of the
 * CodeWriter and do not affect previous states. The previous transformation
 * state of the CodeWriter can later be restored using {@link #popState}.
 *
 * <p>The following example writes out some Python code:
 *
 * <pre>
 * {@code
 * CodeWriter writer = CodeWriter.createDefault();
 * writer.write("def Foo(str):")
 *       .indent()
 *       .write("print str")
 * String code = writer.toString();
 * }
 * </pre>
 *
 * The CodeWriter is stateful, and a prefix can be added before each line.
 * This is useful for doing things like create Javadoc strings:
 *
 * <pre>
 * {@code
 * CodeWriter writer = CodeWriter.createDefault();
 * writer.write("/**")
 *       .setNewlinePrefix(" * ")
 *       .write("This is some docs.")
 *       .write("And more docs.\n\n\n")
 *       .write("Foo.")
 *       .setNewlinePrefix("")
 *       .write(" *\/");
 * }
 * </pre>
 *
 * The above example outputs:
 *
 * <pre>
 * {@code
 * /**
 *  * This is some docs.
 *  * And more docs.
 *  *
 *  * Foo.
 *  *\/
 *
 *   ^ Minus this escape character
 * }
 * </pre>
 */
public final class CodeWriter {
    private final StringBuilder builder = new StringBuilder();
    private final Deque<State> states = new ArrayDeque<>();
    private State currentState;
    private boolean trailingNewline;
    private boolean pendingNewline;
    private boolean onBlankLine = true;
    private int blankLineCount;

    /**
     * Creates a new CodeWriter that uses "\n" for a newline, four spaces
     * for indentation, does not strip trailing whitespace, does not flatten
     * multiple successive blank lines into a single blank line, and adds no
     * trailing new line.
     */
    public CodeWriter() {
        states.push(new State());
        currentState = states.getFirst();
    }

    /**
     * Creates a default instance of a CodeWriter that uses "\n" for newlines,
     * flattens multiple successive blank lines into a single blank line,
     * and adds a trailing new line if needed when converting the CodeWriter
     * to a string.
     *
     * @return Returns the created and configured CodeWriter.
     */
    public static CodeWriter createDefault() {
        return new CodeWriter()
                .setNewline("\n")
                .setIndentText("    ")
                .trimTrailingSpaces()
                .trimBlankLines()
                .insertTrailingNewline();
    }

    /**
     * Handles the formatting of text written by a {@code CodeWriter}.
     */
    @FunctionalInterface
    public interface Formatter {
        /**
         * Formats the given string by detecting and replacing special tokens.
         *
         * @param text Text to parse and format.
         * @param args Variadic arguments to inject into the text.
         * @return Returns the formatted text.
         */
        String format(String text, Object... args);
    }

    /**
     * Gets the contents of the generated code.
     *
     * <p>The result will have an appended newline if the CodeWriter is
     * configured to always append a newline. A newline is only appended
     * in these cases if the result does not already end with a newline.
     *
     * @return Returns the generated code.
     */
    @Override
    public String toString() {
        String result = builder.toString();
        // Insert a new line if one is pending or if trailing new lines are to be
        // added and the content doesn't already end with a newline.
        return (pendingNewline || (trailingNewline && !result.endsWith(currentState.newline)))
               ? result + currentState.newline
               : result;
    }

    /**
     * Copies and pushes the current state to the state stack.
     *
     * <p>This method is used to prepare for a corresponding {@link #popState}
     * operation later. It stores the current state of the CodeWriter into a
     * stack and keeps it active. After pushing, mutations can be made to the
     * state of the CodeWriter without affecting the previous state on the
     * stack. Changes to the state of the CodeWriter can be undone by using
     * {@link #popState()}, which returns the CodeWriter state to the state
     * it was in before calling {@code pushState}.
     *
     * @return Returns the code writer.
     */
    public CodeWriter pushState() {
        State copiedState = new State(currentState);
        states.push(copiedState);
        currentState = copiedState;
        return this;
    }

    /**
     * Pops the current CodeWriter state from the state stack.
     *
     * <p>This method is used to reverse a previous {@link #pushState}
     * operation. It configures the current CodeWriter state to what it was
     * before the last preceding {@code pushState} call.
     *
     * @return Returns the CodeWriter.
     * @throws IllegalStateException if there a no states to pop.
     */
    public CodeWriter popState() {
        if (states.size() == 1) {
            throw new IllegalStateException("Cannot pop CodeWriter state because at the root state");
        }

        states.pop();
        currentState = states.getFirst();
        return this;
    }

    /**
     * Sets the character that represents newlines ("\n" is the default).
     *
     * @param newline Newline character to use.
     * @return Returns the CodeWriter.
     */
    public CodeWriter setNewline(String newline) {
        currentState.newline = newline;
        currentState.newlineRegexQuoted = Pattern.quote(newline);
        return this;
    }

    /**
     * Sets a prefix to prepend to every line after a new line is added
     * (except for an inserted trailing newline).
     *
     * @param newlinePrefix Newline prefix to use.
     * @return Returns the CodeWriter.
     */
    public CodeWriter setNewlinePrefix(String newlinePrefix) {
        currentState.newlinePrefix = newlinePrefix;
        return this;
    }

    /**
     * Sets the text used for indentation (defaults to four spaces).
     *
     * @param indentText Indentation text.
     * @return Returns the CodeWriter.
     */
    public CodeWriter setIndentText(String indentText) {
        currentState.indentText = indentText;
        return this;
    }

    /**
     * Sets the message formatter to use for formatting CodeWriter strings.
     *
     * <p>Every call to {@link #write} and {@link #writeInline} are passed to
     * the {@code Formatter} configured for the {@code CodeWriter}. The
     * {@code Formatter} is responsible for parsing the given string and
     * injecting the provided variadic arguments into the string if necessary.
     * For example, a simple formatter might be {@link String#format} to
     * inject values into the string when {@code %s} is found.
     *
     * <p>Changes to the the formatter <strong>are</strong> affected by
     * {@link #pushState()} and {@link #popState()}.
     *
     * <pre>
     * {@code
     * CodeWriter writer = CodeWriter.createDefault();
     * writer.setFormatter(String::format);
     *       .write("print '%s';", "Hello!");
     * }
     * </pre>
     *
     * @param formatter Formatter to use.s
     * @return Returns the CodeWriter.
     */
    public CodeWriter setFormatter(Formatter formatter) {
        currentState.formatter = Objects.requireNonNull(formatter);
        return this;
    }

    /**
     * Enables the trimming of trailing spaces on a line.
     *
     * @return Returns the CodeWriter.
     */
    public CodeWriter trimTrailingSpaces() {
        return trimTrailingSpaces(true);
    }

    /**
     * Configures if trailing spaces on a line are removed.
     *
     * @param trimTrailingSpaces Set to true to trim trailing spaces.
     * @return Returns the CodeWriter.
     */
    public CodeWriter trimTrailingSpaces(boolean trimTrailingSpaces) {
        currentState.trimTrailingSpaces = trimTrailingSpaces;
        return this;
    }

    /**
     * Ensures that no more than one blank line occurs in succession.
     *
     * @return Returns the CodeWriter.
     */
    public CodeWriter trimBlankLines() {
        return trimBlankLines(1);
    }

    /**
     * Ensures that no more than the given number of newlines can occur
     * in succession, removing consecutive newlines that exceed the given
     * threshold.
     *
     * @param trimBlankLines Number of allowed consecutive newlines. Set to
     *  -1 to perform no trimming. Set to 0 to allow no blank lines. Set to
     *  1 or more to allow for no more than N consecutive blank lines.
     * @return Returns the CodeWriter.
     */
    public CodeWriter trimBlankLines(int trimBlankLines) {
        currentState.trimBlankLines = trimBlankLines;
        return this;
    }

    /**
     * Configures the CodeWriter to always append a newline at the end of
     * the text if one is not already present.
     *
     * <p>This setting is not captured as part of push/popState.
     *
     * @return Returns the CodeWriter.
     */
    public CodeWriter insertTrailingNewline() {
        return insertTrailingNewline(true);
    }

    /**
     * Configures the CodeWriter to always append a newline at the end of
     * the text if one is not already present.
     *
     * <p>This setting is not captured as part of push/popState.
     *
     * @param trailingNewline Set to true to append a trailing new line.
     *
     * @return Returns the CodeWriter.
     */
    public CodeWriter insertTrailingNewline(boolean trailingNewline) {
        this.trailingNewline = trailingNewline;
        return this;
    }

    /**
     * Indents all text one level.
     *
     * @return Returns the CodeWriter.
     */
    public CodeWriter indent() {
        return indent(1);
    }

    /**
     * Indents all text a specific number of levels.
     *
     * @param levels Number of levels to indent.
     * @return Returns the CodeWriter.
     */
    public CodeWriter indent(int levels) {
        currentState.indentation += levels;
        return this;
    }

    /**
     * Removes one level of indentation from all lines.
     *
     * @return Returns the CodeWriter.
     */
    public CodeWriter dedent() {
        return dedent(1);
    }

    /**
     * Removes a specific number of indentations from all lines.
     *
     * <p>Set to -1 to dedent back to 0 (root).
     *
     * @param levels Number of levels to remove.
     * @return Returns the CodeWriter.
     * @throws IllegalStateException when trying to dedent too far.
     */
    public CodeWriter dedent(int levels) {
        if (levels == -1) {
            currentState.indentation = 0;
        } else if (levels < 1 || currentState.indentation - levels < 0) {
            throw new IllegalStateException(String.format("Cannot dedent CodeWriter %d levels", levels));
        } else {
            currentState.indentation -= levels;
        }

        return this;
    }

    /**
     * Writes text to the CodeWriter and appends a newline.
     *
     * <p>The provided text is automatically formatted using a
     * {@link Formatter} and variadic arguments.
     *
     * @param content Content to write.
     * @param args String {@link Formatter} arguments to use for formatting.
     * @return Returns the CodeWriter.
     */
    public CodeWriter write(Object content, Object... args) {
        writeInline(content, args);
        pendingNewline = true;
        return this;
    }

    /**
     * Writes text to the CodeWriter and does not append a newline.
     *
     * <p>The provided text is automatically formatted using a
     * {@link Formatter} and variadic arguments.
     *
     * @param content Content to write.
     * @param args String {@link Formatter} arguments to use for formatting.
     * @return Returns the CodeWriter.
     */
    public CodeWriter writeInline(Object content, Object... args) {
        String formatted = currentState.formatter.format(String.valueOf(content), args);
        String[] lines = formatted.split(currentState.newlineRegexQuoted, -1);

        // Indent the given text.
        for (int lineNumber = 0; lineNumber < lines.length; lineNumber++) {
            String line = lines[lineNumber];

            // Trim newlines if blank line control is enforced.
            if (pendingNewline
                    && (currentState.trimBlankLines == -1 || blankLineCount <= currentState.trimBlankLines)) {
                writeRaw(currentState.newline);
                pendingNewline = false;
                onBlankLine = true;
            }

            // Don't register a pending new line on the last line when writing
            // inline. This is handled by the write() method.
            if (lineNumber < lines.length - 1) {
                pendingNewline = true;
            }

            //line = newlinePrefix + line;
            if (currentState.trimTrailingSpaces) {
                line = stripTrailingSpaces(line);
            }

            if (!line.isEmpty()) {
                blankLineCount = 0;

                // Only write the newline prefix on a new line.
                if (onBlankLine) {
                    writeIndent();
                    writeRaw(currentState.newlinePrefix);
                }

                writeRaw(line);
                onBlankLine = false;

            } else {
                // Track how many blank lines have been seen so that they can
                // be omitted when necessary.
                blankLineCount++;

                // If the line was blank, a newline was written, and the newline
                // prefix is not empty, then write the newline prefix.
                if (onBlankLine && !currentState.newlinePrefix.isEmpty()) {
                    writeIndent();
                    writeRaw(currentState.newlinePrefix);
                    onBlankLine = false;
                }
            }
        }

        return this;
    }

    private void writeIndent() {
        for (int i = 0; i < currentState.indentation; i++) {
            writeRaw(currentState.indentText);
        }
    }

    private static String stripTrailingSpaces(String text) {
        return text.replaceAll("\\s+$", "");
    }

    /**
     * Writes raw text that is not processed in any way.
     *
     * @param content Content to write.
     */
    private void writeRaw(String content) {
        builder.append(content);
    }

    private static final class State {
        private String newline = "\n";
        private String newlineRegexQuoted = Pattern.quote("\n");
        private String indentText = "    ";
        private String newlinePrefix = "";
        private int indentation;
        private boolean trimTrailingSpaces;
        private int trimBlankLines = -1;
        private Formatter formatter = String::format;

        State() {}

        State(State copy) {
            this.newline = copy.newline;
            this.newlineRegexQuoted = copy.newlineRegexQuoted;
            this.indentText = copy.indentText;
            this.newlinePrefix = copy.newlinePrefix;
            this.indentation = copy.indentation;
            this.trimTrailingSpaces = copy.trimTrailingSpaces;
            this.trimBlankLines = copy.trimBlankLines;
            this.formatter = copy.formatter;
        }
    }
}
