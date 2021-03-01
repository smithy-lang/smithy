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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Helper class for generating code.
 *
 * <p>A CodeWriter can be used to write basically any kind of code, including
 * whitespace sensitive and brace-based.
 *
 * <p>The following example generates some Python code:
 *
 * <pre>{@code
 * CodeWriter writer = new CodeWriter();
 * writer.write("def Foo(str):")
 *       .indent()
 *       .write("print str");
 * String code = writer.toString();
 * }</pre>
 *
 * <h2>Code interpolation</h2>
 *
 * <p>The {@link #write}, {@link #openBlock}, and {@link #closeBlock} methods
 * take a code expression and a variadic list of arguments that are
 * interpolated into the expression. Consider the following call to
 * {@code write}:
 *
 * <pre>{@code
 * CodeWriter writer = new CodeWriter();
 * writer.write("Hello, $L", "there!");
 * String code = writer.toString();
 * }</pre>
 *
 * <p>In the above example, {@code $L} is interpolated and replaced with the
 * relative argument {@code there!}.
 *
 * <p>A CodeWriter supports three kinds of interpolations: relative,
 * positional, and named. Each of these kinds of interpolations pass a value
 * to a <em>formatter</em>.</p>
 *
 * <h3>Formatters</h3>
 *
 * <p>Formatters are named functions that accept an object as input, accepts a
 * string that contains the current indentation (it can be ignored if not useful),
 * and returns a string as output. The {@code CodeWriter} registers two built-in
 * formatters:
 *
 * <ul>
 *     <li>{@code L}: Outputs a literal value of an {@code Object} using
 *     the following implementation: (1) A null value is formatted as "".
 *     (2) An empty {@code Optional} value is formatted as "". (3) A non-empty
 *     {@code Optional} value is recursively formatted using the value inside
 *     of the {@code Optional}. (3) All other valeus are formatted using the
 *     result of calling {@link String#valueOf}.</li>
 *     <li>{@code S}: Adds double quotes around the result of formatting a
 *     value first using the default literal "L" implementation described
 *     above and then wrapping the value in an escaped string safe for use in
 *     Java according to https://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6.
 *     This formatter can be overridden if needed to support other
 *     programming languages.</li>
 * </ul>
 *
 * <p>Custom formatters can be registered using {@link #putFormatter}. The identifier
 * given to a formatter must match the following ABNF:
 *
 * <pre>
 * %x21-23    ; ( '!' - '#' )
 * / %x25-2F  ; ( '%' - '/' )
 * / %x3A-60  ; ( ':' - '`' )
 * / %x7B-7E  ; ( '{' - '~' )
 * </pre>
 *
 * <h3>Relative parameters</h3>
 *
 * <p>Placeholders in the form of "$" followed by a formatter name are treated
 * as relative parameters. The first instance of a relative parameter
 * interpolates the first positional argument, the second the second, etc.
 *
 * <pre>{@code
 * CodeWriter writer = new CodeWriter();
 * writer.write("$L $L $L", "a", "b", "c");
 * System.out.println(writer.toString());
 * // Outputs: "a b c"
 * }</pre>
 *
 * <p>All relative arguments must be used as part of an expression and
 * relative interpolation cannot be mixed with positional variables.
 *
 * <h3>Positional parameters</h3>
 *
 * <p>Placeholders in the form of "$" followed by a positive number,
 * followed by a formatter name are treated as positional parameters. The
 * number refers to the 1-based index of the argument to interpolate.
 *
 * <pre>{@code
 * CodeWriter writer = new CodeWriter();
 * writer.write("$1L $2L $3L, $3L $2L $1L", "a", "b", "c");
 * System.out.println(writer.toString());
 * // Outputs: "a b c c b a"
 * }</pre>
 *
 * <p>All positional arguments must be used as part of an expression
 * and relative interpolation cannot be mixed with positional variables.
 *
 * <h3>Named parameters</h3>
 *
 * <p>Named parameters are parameters that take a value from the context of
 * the current state. They take the following form {@code $<variable>:<formatter>},
 * where {@code <variable>} is a string that starts with a lowercase letter,
 * followed by any number of {@code [A-Za-z0-9_#$.]} characters, and
 * {@code <formatter>} is the name of a formatter.
 *
 * <pre>{@code
 * CodeWriter writer = new CodeWriter();
 * writer.putContext("foo", "a");
 * writer.putContext("baz.bar", "b");
 * writer.write("$foo:L $baz.bar:L");
 * System.out.println(writer.toString());
 * // Outputs: "a b"
 * }</pre>
 *
 * <h3>Escaping interpolation</h3>
 *
 * <p>You can escape the "$" character using two "$$".
 *
 * <pre>{@code
 * CodeWriter writer = new CodeWriter().write("$$L");
 * System.out.println(writer.toString());
 * // Outputs: "$L"
 * }</pre>
 *
 * <h3>Custom expression characters</h3>
 *
 * <p>The character used to start a code block expression can be customized
 * to make it easier to write code that makes heavy use of {@code $}. The
 * default character used to start an expression is, {@code $}, but this can
 * be changed for the current state of the CodeWriter by calling
 * {@link #setExpressionStart(char)}. A custom start character can be escaped
 * using two start characters in a row. For example, given a custom start
 * character of {@code #}, {@code #} can be escaped using {@code ##}.
 *
 * <pre>{@code
 * CodeWriter writer = new CodeWriter();
 * writer.setExpressionStart('#');
 * writer.write("#L ##L $L", "hi");
 * System.out.println(writer.toString());
 * // Outputs: "hi #L $L"
 * }</pre>
 *
 * <em>The start character cannot be set to ' ' or '\n'.</em>
 *
 * <h2>Opening and closing blocks</h2>
 *
 * <p>{@code CodeWriter} provides a short cut for opening code blocks that that
 * have an opening an closing delimiter (for example, "{" and "}") and that
 * require indentation inside of the delimiters. Calling {@link #openBlock}
 * and providing the opening statement will write and format a line followed
 * by indenting one level. Calling {@link #closeBlock} will first dedent and
 * then print a formatted statement.
 *
 * <pre>{@code
 * CodeWriter writer = new CodeWriter()
 *       .openBlock("if ($L) {", someValue)
 *       .write("System.out.println($S);", "Hello!")
 *       .closeBlock("}");
 * }</pre>
 *
 * <p>The above example outputs (assuming someValue is equal to "foo"):
 *
 * <pre>{@code
 * if (foo) {
 *     System.out.println("Hello!");
 * }
 * }</pre>
 *
 * <h2>Pushing and popping state</h2>
 *
 * <p>The CodeWriter can maintain a stack of transformation states, including
 * the text used to indent, a prefix to add before each line, newline character,
 * the number of times to indent, a map of context values, whether or not
 * whitespace is trimmed from the end of newlines, whether or not the automatic
 * insertion of newlines is disabled, and the character used to start code
 * expressions (defaults to {@code $}). State can be pushed onto the stack
 * using {@link #pushState} which copies the current state. Mutations can then
 * be made to the top-most state of the CodeWriter and do not affect previous
 * states. The previous transformation state of the CodeWriter can later be
 * restored using {@link #popState}.
 *
 * <p>The CodeWriter is stateful, and a prefix can be added before each line.
 * This is useful for doing things like create Javadoc strings:
 *
 * <pre>{@code
 * CodeWriter writer = new CodeWriter();
 * writer
 *       .pushState()
 *       .write("/**")
 *       .setNewlinePrefix(" * ")
 *       .write("This is some docs.")
 *       .write("And more docs.\n\n\n")
 *       .write("Foo.")
 *       .popState()
 *       .write(" *\/");
 * }</pre>
 *
 * <p>The above example outputs:
 *
 * <pre>{@code
 * /**
 *  * This is some docs.
 *  * And more docs.
 *  *
 *  * Foo.
 *  *\/
 *
 *   ^ Minus this escape character
 * }</pre>
 *
 * <p>The CodeWriter maintains some global state that is not affected by
 * {@link #pushState} and {@link #popState}:
 *
 * <ul>
 *     <li>The number of successive blank lines to trim.</li>
 *     <li>Code formatters registered through {@link #putFormatter}</li>
 *     <li>Whether or not a trailing newline is inserted or removed from
 *     the result of converting the {@code CodeWriter} to a string.</li>
 * </ul>
 *
 * <h2>Limiting blank lines</h2>
 *
 * <p>Many coding standards recommend limiting the number of successive blank
 * lines. This can be handled automatically by {@code CodeWriter} by calling
 * {@link #trimBlankLines}. The removal of blank lines is handled when the
 * {@code CodeWriter} is converted to a string. Lines that consist solely
 * of spaces or tabs are considered blank. If the number of blank lines
 * exceeds the allowed threshold, they are omitted from the result.
 *
 * <h2>Trimming trailing spaces</h2>
 *
 * <p>Trailing spaces can be automatically trimmed from each line by calling
 * {@link #trimTrailingSpaces}.
 *
 * <p>In the the following example:
 *
 * <pre>{@code
 * CodeWriter writer = new CodeWriter();
 * String result = writer.trimTrailingSpaces().write("hello  ").toString();
 * }</pre>
 *
 * <p>The value of {@code result} contains {@code "hello"}
 *
 * <h2>Extending CodeWriter</h2>
 *
 * <p>{@code CodeWriter} can be extended to add functionality for specific
 * programming languages. For example, Java specific code generator could
 * be implemented that makes it easier to write Javadocs.
 *
 * <pre>{@code
 * class JavaCodeWriter extends CodeWriter {
 *     public JavaCodeWriter javadoc(Runnable runnable) {
 *         pushState()
 *         write("/**")
 *         setNewlinePrefix(" * ")
 *         runnable.run();
 *         popState()
 *         write(" *\/");
 *         return this;
 *     }
 * }
 *
 * JavaCodeWriter writer = new JavaCodeWriter();
 * writer.javadoc(() -> {
 *     writer.write("This is an example.");
 * });
 * }</pre>
 *
 * <h2>Code sections</h2>
 *
 * <p>Named sections can be marked in the code writer that can be intercepted
 * and modified by <em>section interceptors</em>. This gives the
 * {@code CodeWriter} a lightweight extension system for augmenting generated
 * code.
 *
 * <p>A section of code can be captured using a block state or an inline
 * section. Section names must match the following regular expression:
 * <code>^[a-z]+[a-zA-Z0-9_.#$]*$</code>.
 *
 * <h3>Block states</h3>
 *
 * <p>A block section is created by passing a string to {@link #pushState}.
 * This string gives the state a name and captures all of the output written
 * inside of this state to an internal buffer. This buffer is then passed to
 * each registered interceptor for that name. These interceptors can choose
 * to use the default contents of the section or emit entirely different
 * content. Interceptors are expected to make calls to the {@code CodeWriter}
 * in order to emit content. Interceptors need to have a reference to the
 * {@code CodeWriter} as one is not provided to them when they are invoked.
 * Interceptors are invoked in the order in which they are added to the
 * {@code CodeBuilder}.
 *
 * <pre>{@code
 * CodeWriter writer = new CodeWriter();
 * writer.onSection("example", text -> writer.write("Intercepted: " + text));
 * writer.pushState("example");
 * writer.write("Original contents");
 * writer.popState();
 * System.out.println(writer.toString());
 * // Outputs: "Intercepted: Original contents\n"
 * }</pre>
 *
 * <h3>Inline sections</h3>
 *
 * An inline section is created using a special {@code CodeWriter} interpolation
 * format that appends "@" followed by the section name. Inline sections are
 * function just like block sections, but they can appear inline inside of
 * other content passed in calls to {@link CodeWriter#write}. An inline section
 * that makes no calls to {@link CodeWriter#write} expands to an empty string.
 *
 * <p>Inline sections are created in a format string inside of braced arguments
 * after the formatter. For example, <code>${L@foo}</code> is an inline section
 * that uses the literal "L" value of a relative argument as the default value
 * of the section and allows interceptors registered for the "foo" section to
 * make calls to the {@code CodeWriter} to modify the section.
 *
 * <pre>{@code
 * CodeWriter writer = new CodeWriter();
 * writer.onSection("example", text -> writer.write("Intercepted: " + text));
 * writer.write("Leading text...${L@example}...Trailing text...", "foo");
 * System.out.println(writer.toString());
 * // Outputs: "Leading text...Intercepted: foo...Trailing text...\n"
 * }</pre>
 *
 * Inline sections are useful for composing sets or lists from any code with access to {@code CodeWriter}:
 *
 * <pre>{@code
 * CodeWriter writer = new CodeWriter();
 * writer.onSection("example", text -> writer.write(text + "1, "));
 * writer.onSection("example", text -> writer.write(text + "2, "));
 * writer.onSection("example", text -> writer.write(text + "3"));
 * writer.write("[${L@example}]", "");
 * System.out.println(writer.toString());
 * // Outputs: "[1, 2, 3]\n"
 * }</pre>
 *
 * <h3>Inline block alignment</h3>
 *
 * <p>The long-form interpolation syntax allows for
 * <em>inline block alignment</em>, which means that any newline emitted by
 * the interpolation is automatically aligned with the column of where the
 * interpolation occurs. Inline block indentation is defined by preceding
 * the closing '}' character with '|' (e.g., <code>${L|}</code>):
 *
 * <pre>{@code
 * CodeWriter writer = new CodeWriter();
 * writer.write("$L: ${L|}", "Names", "Bob\nKaren\nLuis");
 * System.out.println(writer.toString());
 * // Outputs: "Names: Bob\n       Karen\n       Luis\n"
 * }</pre>
 */
public class CodeWriter {
    private static final Pattern LINES = Pattern.compile("\\r?\\n");
    private static final Map<Character, BiFunction<Object, String, String>> DEFAULT_FORMATTERS = MapUtils.of(
            'L', (s, i) -> formatLiteral(s),
            'S', (s, i) -> StringUtils.escapeJavaString(formatLiteral(s), i));

    private final CodeFormatter formatter = new CodeFormatter();
    private final Deque<State> states = new ArrayDeque<>();
    private State currentState;
    private boolean trailingNewline = true;
    private int trimBlankLines = -1;

    /**
     * Tracks when indentation is needed following a newline.
     *
     * <p>This is initially set to true to account for the case when a
     * code writer is initialized with indentation but hasn't written
     * anything yet.
     */
    private boolean needsIndentation = true;

    /**
     * Creates a new CodeWriter that uses "\n" for a newline, four spaces
     * for indentation, does not strip trailing whitespace, does not flatten
     * multiple successive blank lines into a single blank line, and adds no
     * trailing new line.
     */
    public CodeWriter() {
        states.push(new State());
        currentState = states.getFirst();
        currentState.builder = new StringBuilder();
        DEFAULT_FORMATTERS.forEach(formatter::putFormatter);
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
        return new CodeWriter().trimTrailingSpaces();
    }

    /**
     * Provides the default functionality for formatting literal values.
     *
     * <p>This formatter is registered by default as the literal "L" formatter,
     * and is called in the default string "S" formatter before escaping any
     * characters in the string.
     *
     * <ul>
     *     <li>{@code null}: Formatted as an empty string.</li>
     *     <li>Empty {@code Optional}: Formatted as an empty string.</li>
     *     <li>{@code Optional} with value: Formatted as the formatted value in the optional.</li>
     *     <li>Everything else: Formatted as the result of {@link String#valueOf}.</li>
     * </ul>
     *
     * @param value Value to format.
     * @return Returns the formatted value.
     */
    public static String formatLiteral(Object value) {
        if (value == null) {
            return "";
        } else if (value instanceof Optional) {
            Optional optional = (Optional) value;
            return optional.isPresent() ? formatLiteral(optional.get()) : "";
        } else {
            return String.valueOf(value);
        }
    }

    /**
     * Adds a custom formatter expression to the {@code CodeWriter}.
     *
     * <p>The provided {@code identifier} string must match the following ABNF:
     *
     * <pre>
     * %x21-23    ; ( '!' - '#' )
     * / %x25-2F  ; ( '%' - '/' )
     * / %x3A-60  ; ( ':' - '`' )
     * / %x7B-7E  ; ( '{' - '~' )
     * </pre>
     *
     * @param identifier Formatter identifier to associate with this formatter.
     * @param formatter Formatter function that formats the given object as a String.
     * @return Returns the CodeWriter.
     */
    public final CodeWriter putFormatter(char identifier, BiFunction<Object, String, String> formatter) {
        this.formatter.putFormatter(identifier, formatter);
        return this;
    }

    /**
     * Sets the character used to start expressions in the current state when calling
     * {@link #write}, {@link #writeInline}, {@link #openBlock}, etc.
     *
     * <p>By default, {@code $} is used to start expressions (for example
     * {@code $L}. However, some programming languages frequently give
     * syntactic meaning to {@code $}, making this an inconvenient syntactic
     * character for the CodeWriter. In these cases, the character used to
     * start a CodeWriter expression can be changed. Just like {@code $}, the
     * custom start character can be escaped using two subsequent start
     * characters (e.g., {@code $$}).
     *
     * @param expressionStart Character to use to start expressions.
     * @return Returns the CodeWriter.
     */
    public CodeWriter setExpressionStart(char expressionStart) {
        if (expressionStart == ' ' || expressionStart == '\n') {
            throw new IllegalArgumentException("expressionStart must not be set to " + expressionStart);
        }

        currentState.expressionStart = expressionStart;
        return this;
    }

    /**
     * Get the expression start character of the <em>current</em> state.
     *
     * <p>This value should not be cached and reused across pushed and popped
     * states. This value is "$" by default, but it can be changed using
     * {@link #setExpressionStart(char)}.
     *
     * @return Returns the expression start char of the current state.
     */
    public char getExpressionStart() {
        return currentState.expressionStart;
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
        String result = currentState.toString();

        // Trim excessive blank lines.
        if (trimBlankLines > -1) {
            StringBuilder builder = new StringBuilder(result.length());
            String[] lines = LINES.split(result);
            int blankCount = 0;

            for (String line : lines) {
                if (!StringUtils.isBlank(line)) {
                    builder.append(line).append(currentState.newline);
                    blankCount = 0;
                } else if (blankCount++ < trimBlankLines) {
                    builder.append(line).append(currentState.newline);
                }
            }

            result = builder.toString();
        }

        if (result.isEmpty()) {
            return trailingNewline ? String.valueOf(currentState.newline) : "";
        }

        // This accounts for cases where the only write on the CodeWriter was
        // an inline write, but the write ended with spaces.
        if (currentState.trimTrailingSpaces) {
            result = StringUtils.stripEnd(result, " ");
        }

        if (trailingNewline) {
            // Add a trailing newline if needed.
            return result.charAt(result.length() - 1) != currentState.newline ? result + currentState.newline : result;
        } else if (result.charAt(result.length() - 1) == currentState.newline) {
            // Strip the trailing newline if present.
            return result.substring(0, result.length() - 1);
        } else {
            return result;
        }
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
    public final CodeWriter pushState() {
        return pushState(null);
    }

    /**
     * Copies and pushes the current state to the state stack using a named
     * state that can be intercepted by functions registered with
     * {@link #onSection}.
     *
     * <p>The text written while in this state is buffered and passed to each
     * state interceptor. If no text is written by the section or an
     * interceptor, nothing is changed on the {@code CodeWriter}. This
     * behavior allows for placeholder sections to be added into
     * {@code CodeWriter} generators in order to provide extension points
     * that can be otherwise empty.
     *
     * @param sectionName Name of the section to set on the state.
     * @return Returns the code writer.
     */
    public CodeWriter pushState(String sectionName) {
        currentState = new State(currentState);
        states.push(currentState);

        // If a sectionName is specified, then capture this state separately.
        // A separate string builder is given to the state, the indentation
        // level is reset back to the root, and the newline prefix is removed.
        // Indentation and prefixes are added automatically if/when the
        // captured text is written into the parent state.
        if (sectionName != null) {
            currentState.sectionName = sectionName;
            currentState.builder = null;
            currentState.newlinePrefix = "";
            dedent(-1);
        }

        return this;
    }

    /**
     * Pushes an anonymous named state that is always passed through the given
     * filter function before being written to the writer.
     *
     * @param filter Function that maps over the entire section when popped.
     * @return Returns the code writer.
     */
    public CodeWriter pushFilteredState(Function<String, String> filter) {
        String sectionName = "__filtered_state_" + states.size() + 1;
        pushState(sectionName);
        onSection(sectionName, content -> writeWithNoFormatting(filter.apply(content.toString())));
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

        State popped = states.pop();
        currentState = states.getFirst();

        if (popped.sectionName != null) {
            String result = getTrimmedPoppedStateContents(popped);

            if (popped.interceptors.containsKey(popped.sectionName)) {
                List<Consumer<Object>> interceptors = popped.interceptors.get(popped.sectionName);
                for (Consumer<Object> interceptor : interceptors) {
                    result = expandSection("__" + popped.sectionName, result, interceptor);
                }
            }

            if (popped.isInline) {
                // Inline sections need to be written back to the original string builder,
                // and not written to the builder of the parent state. This ensures that
                // inline sections are captured inside of strings and then later written
                // back into a parent state.
                popped.builder.setLength(0);
                popped.builder.append(result);
            } else if (!result.isEmpty()) {
                // Sections can be added that are just placeholders. In those cases,
                // do not write anything unless the section emitted a non-empty string.
                // This has already been formatted, so writeWithNoFormatting is used.
                writeWithNoFormatting(result);
            }
        }

        return this;
    }

    private String getTrimmedPoppedStateContents(State state) {
        StringBuilder builder = state.builder;
        String result = "";

        // Remove the trailing newline, if present, since it gets added in the
        // final call to writeOptional.
        if (builder != null && builder.length() > 0) {
            if (builder.charAt(builder.length() - 1) == currentState.newline) {
                builder.delete(builder.length() - 1, builder.length());
            }
            result = builder.toString();
        }

        return result;
    }

    /**
     * Registers a function that intercepts the contents of a section and
     * the current context map and writes to the {@code CodeWriter} with the
     * updated contents.
     *
     * <p>These section interceptors provide a simple hook system for CodeWriters
     * that add extension points when generating code. The function has the
     * ability to completely ignore the original contents of the section, to
     * prepend text to it, and append text to it. Intercepting functions are
     * expected to have a reference to the {@code CodeWriter} and to mutate it
     * when they are invoked. Each interceptor is invoked it their own
     * isolated pushed/popped states.
     *
     * <p>Interceptors are registered on the current state of the
     * {@code CodeWriter}. When the state to which an interceptor is registered
     * is popped, the interceptor is no longer in effect.
     *
     * <p>The text provided to the intercepting function does not contain
     * a trailing new line. A trailing new line will be injected automatically
     * when the results of intercepting the contents are written to the
     * {@code CodeWriter}. A result is only written if the interceptors write
     * a non-null, non-empty string, allowing for empty placeholders to be
     * added that don't affect the resulting layout of the code.
     *
     * <pre>{@code
     * CodeWriter writer = new CodeWriter();
     *
     * // Prepend text to a section named "foo".
     * writer.onSectionPrepend("foo", () -> writer.write("A"));
     *
     * // Write text to a section, and ensure that the original
     * // text is written too.
     * writer.onSection("foo", text -> {
     *     // Write the original text of the section.
     *     writer.write(text);
     *     // Write more text to the section.
     *     writer.write("C");
     * });
     *
     * // Append text to a section.
     * writer.onSectionAppend("foo", () -> writer.write("D"));
     *
     * // Create the section, write to it, then close the section.
     * writer.pushState("foo").write("B").popState();
     *
     * assert(writer.toString().equals("A\nB\nC\nD\n"));
     * }</pre>
     *
     * @param sectionName The name of the section to intercept.
     * @param interceptor The function to intercept with.
     * @return Returns the CodeWriter.
     */
    public CodeWriter onSection(String sectionName, Consumer<Object> interceptor) {
        currentState.putInterceptor(sectionName, interceptor);
        return this;
    }

    /**
     * Prepends to the contents of a named section.
     *
     * <pre>{@code
     * writer.onSectionPrepend("foo", () -> {
     *     writer.write("This text is added before the rest of the section.");
     * });
     * }</pre>
     *
     * @param sectionName The name of the section to intercept.
     * @param writeBefore A runnable that prepends to a section by mutating the writer.
     * @return Returns the CodeWriter.
     * @see #onSection
     */
    public final CodeWriter onSectionPrepend(String sectionName, Runnable writeBefore) {
        return onSection(sectionName, contents -> {
            writeBefore.run();
            write(contents);
        });
    }

    /**
     * Appends to the contents of a named section.
     *
     * <pre>{@code
     * writer.onSectionAppend("foo", () -> {
     *     writer.write("This text is added after the rest of the section.");
     * });
     * }</pre>
     *
     * @param sectionName The name of the section to intercept.
     * @param writeAfter A runnable that appends to a section by mutating the writer.
     * @return Returns the CodeWriter.
     * @see #onSection
     */
    public final CodeWriter onSectionAppend(String sectionName, Runnable writeAfter) {
        return onSection(sectionName, contents -> {
            write(contents);
            writeAfter.run();
        });
    }

    /**
     * Disables the automatic appending of newlines in the current state.
     *
     * <p>Methods like {@link #write}, {@link #openBlock}, and {@link #closeBlock}
     * will not automatically append newlines when a state has this flag set.
     *
     * @return Returns the CodeWriter.
     */
    public CodeWriter disableNewlines() {
        currentState.disableNewline = true;
        return this;
    }

    /**
     * Enables the automatic appending of newlines in the current state.
     *
     * @return Returns the CodeWriter.
     */
    public CodeWriter enableNewlines() {
        currentState.disableNewline = false;
        return this;
    }

    /**
     * Sets the character used to represent newlines in the current state
     * ("\n" is the default).
     *
     * <p>When the provided string is empty (""), then newlines are disabled
     * in the current state. This is exactly equivalent to calling
     * {@link #disableNewlines()}, and does not actually change the newline
     * character of the current state.
     *
     * <p>When the provided string is not empty, then the string must contain
     * exactly one character. Setting the newline character to a non-empty
     * string also implicitly enables newlines in the current state.
     *
     * @param newline Newline character to use.
     * @return Returns the CodeWriter.
     */
    public final CodeWriter setNewline(String newline) {
        if (newline.isEmpty()) {
            return disableNewlines();
        } else if (newline.length() > 1) {
            throw new IllegalArgumentException("newline must be set to an empty string or a single character");
        } else {
            return setNewline(newline.charAt(0));
        }
    }

    /**
     * Sets the character used to represent newlines in the current state
     * ("\n" is the default).
     *
     * <p>This call also enables newlines in the current state by calling
     * {@link #enableNewlines()}.
     *
     * @param newline Newline character to use.
     * @return Returns the CodeWriter.
     */
    public final CodeWriter setNewline(char newline) {
        currentState.newline = newline;
        enableNewlines();
        return this;
    }

    /**
     * Sets the text used for indentation (defaults to four spaces).
     *
     * @param indentText Indentation text.
     * @return Returns the CodeWriter.
     */
    public final CodeWriter setIndentText(String indentText) {
        currentState.indent(0, indentText);
        return this;
    }

    /**
     * Enables the trimming of trailing spaces on a line.
     *
     * @return Returns the CodeWriter.
     */
    public final CodeWriter trimTrailingSpaces() {
        return trimTrailingSpaces(true);
    }

    /**
     * Configures if trailing spaces on a line are removed.
     *
     * @param trimTrailingSpaces Set to true to trim trailing spaces.
     * @return Returns the CodeWriter.
     */
    public final CodeWriter trimTrailingSpaces(boolean trimTrailingSpaces) {
        currentState.trimTrailingSpaces = trimTrailingSpaces;
        return this;
    }

    /**
     * Ensures that no more than one blank line occurs in succession.
     *
     * @return Returns the CodeWriter.
     */
    public final CodeWriter trimBlankLines() {
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
    public final CodeWriter trimBlankLines(int trimBlankLines) {
        this.trimBlankLines = trimBlankLines;
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
    public final CodeWriter insertTrailingNewline() {
        return insertTrailingNewline(true);
    }

    /**
     * Configures the CodeWriter to always append a newline at the end of
     * the text if one is not already present.
     *
     * <p>This setting is not captured as part of push/popState.
     *
     * @param trailingNewline The newline behavior. True to add, false to strip.
     *
     * @return Returns the CodeWriter.
     */
    public final CodeWriter insertTrailingNewline(boolean trailingNewline) {
        this.trailingNewline = trailingNewline;
        return this;
    }

    /**
     * Sets a prefix to prepend to every line after a new line is added
     * (except for an inserted trailing newline).
     *
     * @param newlinePrefix Newline prefix to use.
     * @return Returns the CodeWriter.
     */
    public final CodeWriter setNewlinePrefix(String newlinePrefix) {
        currentState.newlinePrefix = newlinePrefix;
        return this;
    }

    /**
     * Indents all text one level.
     *
     * @return Returns the CodeWriter.
     */
    public final CodeWriter indent() {
        return indent(1);
    }

    /**
     * Indents all text a specific number of levels.
     *
     * @param levels Number of levels to indent.
     * @return Returns the CodeWriter.
     */
    public final CodeWriter indent(int levels) {
        currentState.indent(levels, null);
        return this;
    }

    /**
     * Removes one level of indentation from all lines.
     *
     * @return Returns the CodeWriter.
     */
    public final CodeWriter dedent() {
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
    public final CodeWriter dedent(int levels) {
        int adjusted = levels == -1 ? Integer.MIN_VALUE : -1 * levels;
        currentState.indent(adjusted, null);
        return this;
    }

    /**
     * Opens a block of syntax by writing text, a newline, then indenting.
     *
     * <pre>
     * {@code
     * String result = new CodeWriter()
     *         .openBlock("public final class $L {", "Foo")
     *             .openBlock("public void main(String[] args) {")
     *                 .write("System.out.println(args[0]);")
     *             .closeBlock("}")
     *         .closeBlock("}")
     *         .toString();
     * }
     * </pre>
     *
     * @param textBeforeNewline Text to write before writing a newline and indenting.
     * @param args String arguments to use for formatting.
     * @return Returns the {@code CodeWriter}.
     */
    public final CodeWriter openBlock(String textBeforeNewline, Object... args) {
        return write(textBeforeNewline, args).indent();
    }

    /**
     * Opens a block of syntax by writing {@code textBeforeNewline}, a newline, then
     * indenting, then executes the given {@code Runnable}, then closes the block of
     * syntax by writing a newline, dedenting, then writing {@code textAfterNewline}.
     *
     * <pre>{@code
     * CodeWriter writer = new CodeWriter();
     * writer.openBlock("public final class $L {", "}", "Foo", () -> {
     *     writer.openBlock("public void main(String[] args) {", "}", () -> {
     *         writer.write("System.out.println(args[0]);");
     *     })
     * });
     * }</pre>
     *
     * @param textBeforeNewline Text to write before writing a newline and indenting.
     * @param textAfterNewline Text to write after writing a newline and indenting.
     * @param f Runnable function to execute inside of the block.
     * @return Returns the {@code CodeWriter}.
     */
    public final CodeWriter openBlock(String textBeforeNewline, String textAfterNewline, Runnable f) {
        return openBlock(textBeforeNewline, textAfterNewline, new Object[]{}, f);
    }

    /**
     * Opens a block of syntax by writing {@code textBeforeNewline}, a newline, then
     * indenting, then executes the given {@code Runnable}, then closes the block of
     * syntax by writing a newline, dedenting, then writing {@code textAfterNewline}.
     *
     * @param textBeforeNewline Text to write before writing a newline and indenting.
     * @param textAfterNewline Text to write after writing a newline and indenting.
     * @param arg1 First positional argument to substitute into {@code textBeforeNewline}.
     * @param f Runnable function to execute inside of the block.
     * @return Returns the {@code CodeWriter}.
     */
    public final CodeWriter openBlock(String textBeforeNewline, String textAfterNewline, Object arg1, Runnable f) {
        return openBlock(textBeforeNewline, textAfterNewline, new Object[]{arg1}, f);
    }

    /**
     * Opens a block of syntax by writing {@code textBeforeNewline}, a newline, then
     * indenting, then executes the given {@code Runnable}, then closes the block of
     * syntax by writing a newline, dedenting, then writing {@code textAfterNewline}.
     *
     * @param textBeforeNewline Text to write before writing a newline and indenting.
     * @param textAfterNewline Text to write after writing a newline and indenting.
     * @param arg1 First positional argument to substitute into {@code textBeforeNewline}.
     * @param arg2 Second positional argument to substitute into {@code textBeforeNewline}.
     * @param f Runnable function to execute inside of the block.
     * @return Returns the {@code CodeWriter}.
     */
    public final CodeWriter openBlock(String textBeforeNewline, String textAfterNewline,
            Object arg1, Object arg2, Runnable f) {
        return openBlock(textBeforeNewline, textAfterNewline, new Object[]{arg1, arg2}, f);
    }

    /**
     * Opens a block of syntax by writing {@code textBeforeNewline}, a newline, then
     * indenting, then executes the given {@code Runnable}, then closes the block of
     * syntax by writing a newline, dedenting, then writing {@code textAfterNewline}.
     *
     * @param textBeforeNewline Text to write before writing a newline and indenting.
     * @param textAfterNewline Text to write after writing a newline and indenting.
     * @param arg1 First positional argument to substitute into {@code textBeforeNewline}.
     * @param arg2 Second positional argument to substitute into {@code textBeforeNewline}.
     * @param arg3 Third positional argument to substitute into {@code textBeforeNewline}.
     * @param f Runnable function to execute inside of the block.
     * @return Returns the {@code CodeWriter}.
     */
    public final CodeWriter openBlock(String textBeforeNewline, String textAfterNewline,
            Object arg1, Object arg2, Object arg3, Runnable f) {
        return openBlock(textBeforeNewline, textAfterNewline, new Object[]{arg1, arg2, arg3}, f);
    }

    /**
     * Opens a block of syntax by writing {@code textBeforeNewline}, a newline, then
     * indenting, then executes the given {@code Runnable}, then closes the block of
     * syntax by writing a newline, dedenting, then writing {@code textAfterNewline}.
     *
     * @param textBeforeNewline Text to write before writing a newline and indenting.
     * @param textAfterNewline Text to write after writing a newline and indenting.
     * @param arg1 First positional argument to substitute into {@code textBeforeNewline}.
     * @param arg2 Second positional argument to substitute into {@code textBeforeNewline}.
     * @param arg3 Third positional argument to substitute into {@code textBeforeNewline}.
     * @param arg4 Fourth positional argument to substitute into {@code textBeforeNewline}.
     * @param f Runnable function to execute inside of the block.
     * @return Returns the {@code CodeWriter}.
     */
    public final CodeWriter openBlock(String textBeforeNewline, String textAfterNewline,
            Object arg1, Object arg2, Object arg3, Object arg4, Runnable f) {
        return openBlock(textBeforeNewline, textAfterNewline, new Object[]{arg1, arg2, arg3, arg4}, f);
    }

    /**
     * Opens a block of syntax by writing {@code textBeforeNewline}, a newline, then
     * indenting, then executes the given {@code Runnable}, then closes the block of
     * syntax by writing a newline, dedenting, then writing {@code textAfterNewline}.
     *
     * @param textBeforeNewline Text to write before writing a newline and indenting.
     * @param textAfterNewline Text to write after writing a newline and indenting.
     * @param arg1 First positional argument to substitute into {@code textBeforeNewline}.
     * @param arg2 Second positional argument to substitute into {@code textBeforeNewline}.
     * @param arg3 Third positional argument to substitute into {@code textBeforeNewline}.
     * @param arg4 Fourth positional argument to substitute into {@code textBeforeNewline}.
     * @param arg5 Fifth positional argument to substitute into {@code textBeforeNewline}.
     * @param f Runnable function to execute inside of the block.
     * @return Returns the {@code CodeWriter}.
     */
    public final CodeWriter openBlock(String textBeforeNewline, String textAfterNewline,
            Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Runnable f) {
        return openBlock(textBeforeNewline, textAfterNewline, new Object[]{arg1, arg2, arg3, arg4, arg5}, f);
    }

    /**
     * Opens a block of syntax by writing {@code textBeforeNewline}, a newline, then
     * indenting, then executes the given {@code Runnable}, then closes the block of
     * syntax by writing a newline, dedenting, then writing {@code textAfterNewline}.
     *
     * @param textBeforeNewline Text to write before writing a newline and indenting.
     * @param textAfterNewline Text to write after writing a newline and indenting.
     * @param args Arguments to substitute into {@code textBeforeNewline}.
     * @param f Runnable function to execute inside of the block.
     * @return Returns the {@code CodeWriter}.
     */
    public final CodeWriter openBlock(String textBeforeNewline, String textAfterNewline, Object[] args, Runnable f) {
        write(textBeforeNewline, args).indent();
        f.run();
        closeBlock(textAfterNewline);
        return this;
    }

    /**
     * Closes a block of syntax by writing a newline, dedenting, then writing text.
     *
     * @param textAfterNewline Text to write after writing a newline and dedenting.
     * @param args String arguments to use for formatting.
     * @return Returns the {@code CodeWriter}.
     */
    public final CodeWriter closeBlock(String textAfterNewline, Object... args) {
        return dedent().write(textAfterNewline, args);
    }

    /**
     * Writes text to the CodeWriter and appends a newline.
     *
     * <p>The provided text does not use any kind of expression formatting.
     *
     * <p>Indentation and the newline prefix is only prepended if the writer's
     * cursor is at the beginning of a newline.
     *
     * @param content Content to write.
     * @return Returns the CodeWriter.
     */
    public final CodeWriter writeWithNoFormatting(Object content) {
        currentState.writeLine(content.toString());
        return this;
    }

    /**
     * Creates a formatted string using formatter expressions and variadic
     * arguments.
     *
     * <p>Important: if the formatters that are executed while formatting the
     * given {@code content} string mutate the CodeWriter, it could leave the
     * CodeWriter in an inconsistent state. For example, some CodeWriter
     * implementations manage imports and dependencies automatically based on
     * code that is referenced by formatters. If such an expression is used
     * with this format method but the returned String is never written to the
     * CodeWriter, then the CodeWriter might be mutated to track dependencies
     * that aren't actually necessary.
     *
     * <pre>{@code
     * CodeWriter writer = new CodeWriter();
     * String name = "Person";
     * String formatted = writer.format("Hello, $L", name);
     * assert(formatted.equals("Hello, Person"));
     * }</pre>
     *
     * @param content Content to format.
     * @param args String arguments to use for formatting.
     * @return Returns the formatted string.
     * @see #write
     * @see #putFormatter
     */
    public final String format(Object content, Object... args) {
        return formatter.format(currentState.expressionStart, content, currentState.indentText, this, args);
    }

    /**
     * Writes text to the CodeWriter and appends a newline.
     *
     * <p>The provided text is automatically formatted using variadic
     * arguments.
     *
     * <p>Indentation and the newline prefix is only prepended if the writer's
     * cursor is at the beginning of a newline.
     *
     * @param content Content to write.
     * @param args String arguments to use for formatting.
     * @return Returns the CodeWriter.
     */
    public final CodeWriter write(Object content, Object... args) {
        String value = format(content, args);
        currentState.writeLine(value);
        return this;
    }

    /**
     * Writes text to the CodeWriter without appending a newline.
     *
     * <p>The provided text is automatically formatted using variadic
     * arguments.
     *
     * <p>Indentation and the newline prefix is only prepended if the writer's
     * cursor is at the beginning of a newline.
     *
     * <p>If newlines are present in the given string, each of those lines will receive proper indentation.
     *
     * @param content Content to write.
     * @param args String arguments to use for formatting.
     * @return Returns the CodeWriter.
     */
    public final CodeWriter writeInline(Object content, Object... args) {
        String value = format(content, args);
        currentState.write(value);
        return this;
    }

    /**
     * Optionally writes text to the CodeWriter and appends a newline
     * if a value is present.
     *
     * <p>If the provided {@code content} value is {@code null}, nothing is
     * written. If the provided {@code content} value is an empty
     * {@code Optional}, nothing is written. If the result of calling
     * {@code toString} on {@code content} results in an empty string,
     * nothing is written. Finally, if the value is a non-empty string,
     * the content is written to the {@code CodeWriter} at the current
     * level of indentation, and a newline is appended.
     *
     * @param content Content to write if present.
     * @return Returns the CodeWriter.
     */
    public final CodeWriter writeOptional(Object content) {
        if (content == null) {
            return this;
        } else if (content instanceof Optional) {
            return writeOptional(((Optional<?>) content).orElse(null));
        } else {
            String value = content.toString();
            return !value.isEmpty() ? write(value) : this;
        }
    }

    /**
     * Allows calling out to arbitrary code for things like looping or
     * conditional writes without breaking method chaining.
     *
     * @param task Method to invoke.
     * @return Returns the CodeWriter.
     */
    public final CodeWriter call(Runnable task) {
        task.run();
        return this;
    }

    /**
     * Adds a named key-value pair to the context of the current state.
     *
     * <p>These context values can be referenced by named interpolated
     * parameters.
     *
     * @param key Key to add to the context.
     * @param value Value to associate with the key.
     * @return Returns the CodeWriter.
     */
    public CodeWriter putContext(String key, Object value) {
        currentState.putContext(key, value);
        return this;
    }

    /**
     * Adds a map of named key-value pair to the context of the current state.
     *
     * <p>These context values can be referenced by named interpolated
     * parameters.
     *
     * @param mappings Key value pairs to add.
     * @return Returns the CodeWriter.
     */
    public final CodeWriter putContext(Map<String, Object> mappings) {
        mappings.forEach(currentState::putContext);
        return this;
    }

    /**
     * Removes a named key-value pair from the context of the current state.
     *
     * @param key Key to add to remove from the current context.
     * @return Returns the CodeWriter.
     */
    public CodeWriter removeContext(String key) {
        currentState.removeContext(key);
        return this;
    }

    /**
     * Gets a named contextual key-value pair from the current state.
     *
     * @param key Key to retrieve.
     * @return Returns the associated value or null if not present.
     */
    public Object getContext(String key) {
        return currentState.context.get(key);
    }

    // Used only by CodeFormatter to expand inline argument sections.
    String expandSection(String sectionName, String defaultContent, Consumer<Object> writerConsumer) {
        StringBuilder buffer = new StringBuilder();
        pushState(sectionName);
        currentState.isInline = true;
        currentState.builder = buffer;
        writerConsumer.accept(defaultContent);
        popState();
        return buffer.toString();
    }

    private final class State {
        private String indentText = "    ";
        private String leadingIndentString = "";
        private String newlinePrefix = "";
        private int indentation;
        private boolean trimTrailingSpaces;
        private boolean disableNewline;
        private char newline = '\n';
        private char expressionStart = '$';

        private transient String sectionName;

        /**
         * Inline states are created when formatting text. They aren't written
         * directly to the CodeWriter, but rather captured as part of the
         * process of expanding a template argument.
         */
        private transient boolean isInline;

        /** This StringBuilder, if null, will only be created lazily when needed. */
        private StringBuilder builder;

        /** The context map implements a simple copy on write pattern. */
        private Map<String, Object> context = MapUtils.of();
        private transient boolean copiedContext = false;

        /** The interceptors map implements a simple copy on write pattern. */
        private Map<String, List<Consumer<Object>>> interceptors = MapUtils.of();
        private transient boolean copiedInterceptors = false;

        State() {}

        private State(State copy) {
            this.newline = copy.newline;
            this.expressionStart = copy.expressionStart;
            this.builder = copy.builder;
            this.context = copy.context;
            this.indentText = copy.indentText;
            this.leadingIndentString = copy.leadingIndentString;
            this.indentation = copy.indentation;
            this.newlinePrefix = copy.newlinePrefix;
            this.trimTrailingSpaces = copy.trimTrailingSpaces;
            this.interceptors = copy.interceptors;
            this.disableNewline = copy.disableNewline;
        }

        @Override
        public String toString() {
            return builder == null ? "" : builder.toString();
        }

        private void mutateContext() {
            if (!copiedContext) {
                context = new HashMap<>(context);
                copiedContext = true;
            }
        }

        void putContext(String key, Object value) {
            mutateContext();
            context.put(key, value);
        }

        void removeContext(String key) {
            if (context.containsKey(key)) {
                mutateContext();
                context.remove(key);
            }
        }

        void putInterceptor(String section, Consumer<Object> interceptor) {
            if (!copiedInterceptors) {
                interceptors = new HashMap<>(interceptors);
                copiedInterceptors = true;
            }

            interceptors.computeIfAbsent(section, s -> new ArrayList<>()).add(interceptor);
        }

        void write(String contents) {
            if (builder == null) {
                builder = new StringBuilder();
            }

            // Write each character, accounting for newlines along the way.
            for (int i = 0; i < contents.length(); i++) {
                append(contents.charAt(i));
            }
        }

        void append(char c) {
            if (needsIndentation) {
                builder.append(leadingIndentString);
                builder.append(newlinePrefix);
                needsIndentation = false;
            }

            if (c == newline) {
                // The next appended character will get indentation and a
                // leading prefix string.
                needsIndentation = true;
                // Trim spaces before each newline. This only mutates the builder
                // if space trimming is enabled.
                trimSpaces();
            }

            builder.append(c);
        }

        void writeLine(String line) {
            write(line);

            if (!disableNewline) {
                append(newline);
            }
        }

        private void trimSpaces() {
            if (!trimTrailingSpaces) {
                return;
            }

            int toRemove = 0;
            for (int i = builder.length() - 1; i > 0; i--) {
                if (builder.charAt(i) == ' ') {
                    toRemove++;
                } else {
                    break;
                }
            }

            if (toRemove > 0) {
                builder.delete(builder.length() - toRemove, builder.length());
            }
        }

        private void indent(int levels, String indentText) {
            // Set to Integer.MIN_VALUE to indent back to root.
            if (levels == Integer.MIN_VALUE) {
                indentation = 0;
            } else if (levels + indentation < 0) {
                throw new IllegalStateException(String.format("Cannot dedent CodeWriter %d levels", levels));
            } else {
                indentation += levels;
            }

            if (indentText != null) {
                this.indentText = indentText;
            }

            leadingIndentString = StringUtils.repeat(this.indentText, indentation);
        }
    }
}
