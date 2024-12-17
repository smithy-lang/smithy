/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Helper class for generating code.
 *
 * <p>An AbstractCodeWriter can be used to write basically any kind of code, including
 * whitespace sensitive and brace-based.
 *
 * <p>The following example generates some Python code:
 *
 * <pre>{@code
 * SimpleCodeWriter writer = new SimpleCodeWriter();
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
 * SimpleCodeWriter = new SimpleCodeWriter();
 * writer.write("Hello, $L", "there!");
 * String code = writer.toString();
 * }</pre>
 *
 * <p>In the above example, {@code $L} is interpolated and replaced with the
 * relative argument {@code there!}.
 *
 * <p>An AbstractCodeWriter supports three kinds of interpolations: relative,
 * positional, and named. Each of these kinds of interpolations pass a value
 * to a <em>formatter</em>.</p>
 *
 * <h3>Formatters</h3>
 *
 * <p>Formatters are named functions that accept an object as input, accepts a
 * string that contains the current indentation (it can be ignored if not useful),
 * and returns a string as output. {@code AbstractCodeWriter} registers two built-in
 * formatters:
 *
 * <ul>
 *     <li>{@code L} (literal): Outputs a literal value of an {@code Object} using
 *     the following implementation: (1) A null value is formatted as "".
 *     (2) An empty {@code Optional} value is formatted as "". (3) A non-empty
 *     {@code Optional} value is recursively formatted using the value inside
 *     of the {@code Optional}. (3) All other valeus are formatted using the
 *     result of calling {@link String#valueOf}.</li>
 *
 *     <li>{@code C} (call): Runs a {@link Runnable} or {@link Consumer} argument
 *     that is expected to write to the same writer. Any text written to the AbstractCodeWriter
 *     inside of the Runnable is used as the value of the argument. Note that a
 *     single trailing newline is removed from the captured text. If a Runnable is
 *     provided, it is required to have a reference to the AbstractCodeWriter. A Consumer
 *     is provided a reference to the AbstractCodeWriter as a single argument.
 *
 *     <pre>{@code
 *     SimpleCodeWriter writer = new SimpleCodeWriter();
 *     writer.write("Hello, $C.", (Runnable) () -> writer.write("there"));
 *     assert(writer.toString().equals("Hello, there.\n"));
 *     }</pre></li>
 *
 *     <li>{@code S} (string): Adds double quotes around the result of formatting a
 *     value first using the default literal "L" implementation described
 *     above and then wrapping the value in an escaped string safe for use in
 *     Java according to https://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6.
 *     This formatter can be overridden if needed to support other
 *     programming languages.</li>
 * </ul>
 *
 * <p>Custom formatters can be registered using {@link #putFormatter}. Custom
 * formatters can be used only within the state they were added. Because states
 * inherit the formatters of parent states, adding a formatter to the root state
 * of the AbstractCodeWriter allows the formatter to be used in any state.
 *
 * <p>The identifier given to a formatter must match one of the following
 * characters:
 *
 * <pre>
 *    "!" / "#" / "%" / "&amp;" / "*" / "+" / "," / "-" / "." / "/" / ";"
 *  / "=" / "?" / "@" / "A" / "B" / "C" / "D" / "E" / "F" / "G" / "H"
 *  / "I" / "J" / "K" / "L" / "M" / "N" / "O" / "P" / "Q" / "R" / "S"
 *  / "T" / "U" / "V" / "W" / "X" / "Y" / "Z" / "^" / "_" / "`" / "~"
 * </pre>
 *
 * <h3>Relative parameters</h3>
 *
 * <p>Placeholders in the form of "$" followed by a formatter name are treated
 * as relative parameters. The first instance of a relative parameter
 * interpolates the first positional argument, the second the second, etc.
 *
 * <pre>{@code
 * SimpleCodeWriter = new SimpleCodeWriter();
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
 * SimpleCodeWriter = new SimpleCodeWriter();
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
 * <p>Named parameters are parameters that take a value from the context bag of
 * the current state or using getters of the {@link CodeSection} associated with
 * the current state. They take the following form {@code $<variable>:<formatter>},
 * where {@code <variable>} is a string that starts with a lowercase letter,
 * followed by any number of {@code [A-Za-z0-9_#$.]} characters, and
 * {@code <formatter>} is the name of a formatter.
 *
 * <pre>{@code
 * SimpleCodeWriter = new SimpleCodeWriter();
 * writer.putContext("foo", "a");
 * writer.putContext("baz.bar", "b");
 * writer.write("$foo:L $baz.bar:L");
 * System.out.println(writer.toString());
 * // Outputs: "a b"
 * }</pre>
 *
 * <p>The context bag is checked first, and then if the parameter is not found,
 * getters of the currently associated CodeSection are checked. If a getter is
 * found that matches the key exactly, then that getter is invoked and used as
 * the named parameter. If a getter is found that matches
 * "get" + uppercase_first_letter(key), then that getter is used as the named
 * parameter.
 *
 * <h3>Escaping interpolation</h3>
 *
 * <p>You can escape the "$" character using two "$$".
 *
 * <pre>{@code
 * SimpleCodeWriter = new SimpleCodeWriter().write("$$L");
 * System.out.println(writer.toString());
 * // Outputs: "$L"
 * }</pre>
 *
 * <h3>Custom expression characters</h3>
 *
 * <p>The character used to start a code block expression can be customized
 * to make it easier to write code that makes heavy use of {@code $}. The
 * default character used to start an expression is, {@code $}, but this can
 * be changed for the current state of the AbstractCodeWriter by calling
 * {@link #setExpressionStart(char)}. A custom start character can be escaped
 * using two start characters in a row. For example, given a custom start
 * character of {@code #}, {@code #} can be escaped using {@code ##}.
 *
 * <pre>{@code
 * SimpleCodeWriter = new SimpleCodeWriter();
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
 * <p>{@code AbstractCodeWriter} provides a short cut for opening code blocks that
 * have an opening an closing delimiter (for example, "{" and "}") and that
 * require indentation inside of the delimiters. Calling {@link #openBlock}
 * and providing the opening statement will write and format a line followed
 * by indenting one level. Calling {@link #closeBlock} will first dedent and
 * then print a formatted statement.
 *
 * <pre>{@code
 * SimpleCodeWriter = new SimpleCodeWriter()
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
 * <p>AbstractCodeWriter can maintain a stack of transformation states, including
 * the text used to indent, a prefix to add before each line, newline character,
 * the number of times to indent, a map of context values, whether or not
 * whitespace is trimmed from the end of newlines, whether or not the automatic
 * insertion of newlines is disabled, the character used to start code
 * expressions (defaults to {@code $}), and formatters. State can be pushed onto
 * the stack using {@link #pushState} which copies the current state. Mutations
 * can then be made to the top-most state of the AbstractCodeWriter and do not affect
 * previous states. The previous transformation state of the AbstractCodeWriter can later
 * be restored using {@link #popState}.
 *
 * <p>AbstractCodeWriter is stateful, and a prefix can be added before each line.
 * This is useful for doing things like create Javadoc strings:
 *
 * <pre>{@code
 * SimpleCodeWriter = new SimpleCodeWriter();
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
 * <p>AbstractCodeWriter maintains some global state that is not affected by
 * {@link #pushState} and {@link #popState}:
 *
 * <ul>
 *     <li>The number of successive blank lines to trim.</li>
 *     <li>Whether or not a trailing newline is inserted or removed from
 *     the result of converting the {@code AbstractCodeWriter} to a string.</li>
 * </ul>
 *
 * <h2>Limiting blank lines</h2>
 *
 * <p>Many coding standards recommend limiting the number of successive blank
 * lines. This can be handled automatically by {@code AbstractCodeWriter} by calling
 * {@link #trimBlankLines}. The removal of blank lines is handled when the
 * {@code AbstractCodeWriter} is converted to a string. Lines that consist solely
 * of spaces or tabs are considered blank. If the number of blank lines
 * exceeds the allowed threshold, they are omitted from the result.
 *
 * <h2>Trimming trailing spaces</h2>
 *
 * <p>Trailing spaces can be automatically trimmed from each line by calling
 * {@link #trimTrailingSpaces}.
 *
 * <p>In the following example:
 *
 * <pre>{@code
 * SimpleCodeWriter = new SimpleCodeWriter();
 * String result = writer.trimTrailingSpaces().write("hello  ").toString();
 * }</pre>
 *
 * <p>The value of {@code result} contains {@code "hello"}
 *
 * <h2>Extending AbstractCodeWriter</h2>
 *
 * <p>{@code AbstractCodeWriter} can be extended to add functionality for specific
 * programming languages. For example, Java specific code generator could
 * be implemented that makes it easier to write Javadocs.
 *
 * <pre>{@code
 * class JavaCodeWriter extends AbstractCodeWriter<JavaCodeWriter> {
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
 * {@code AbstractCodeWriter} a lightweight extension system for augmenting generated
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
 * content. Interceptors are expected to make calls to the {@code AbstractCodeWriter}
 * in order to emit content. Interceptors need to have a reference to the
 * {@code AbstractCodeWriter} as one is not provided to them when they are invoked.
 * Interceptors are invoked in the order in which they are added to the
 * {@code CodeBuilder}.
 *
 * <pre>{@code
 * SimpleCodeWriter = new SimpleCodeWriter();
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
 * other content passed in calls to {@link AbstractCodeWriter#write}. An inline section
 * that makes no calls to {@link AbstractCodeWriter#write} expands to an empty string.
 *
 * <p>Inline sections are created in a format string inside of braced arguments
 * after the formatter. For example, <code>${L@foo}</code> is an inline section
 * that uses the literal "L" value of a relative argument as the default value
 * of the section and allows AbstractCodeWriter registered for the "foo" section to
 * make calls to the {@code CodeWriter} to modify the section.
 *
 * <pre>{@code
 * SimpleCodeWriter = new SimpleCodeWriter();
 * writer.onSection("example", text -> writer.write("Intercepted: " + text));
 * writer.write("Leading text...${L@example}...Trailing text...", "foo");
 * System.out.println(writer.toString());
 * // Outputs: "Leading text...Intercepted: foo...Trailing text...\n"
 * }</pre>
 *
 * Inline sections are useful for composing sets or lists from any code with access to {@code AbstractCodeWriter}:
 *
 * <pre>{@code
 * SimpleCodeWriter = new SimpleCodeWriter();
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
 * SimpleCodeWriter = new SimpleCodeWriter();
 * writer.write("$L: ${L|}", "Names", "Bob\nKaren\nLuis");
 * System.out.println(writer.toString());
 * // Outputs: "Names: Bob\n       Karen\n       Luis\n"
 * }</pre>
 *
 * <p>Alignment occurs either statically or dynamically based on the characters
 * that come before interpolation. If all of the characters in the literal
 * template that come before interpolation are spaces and tabs, then those
 * characters are used when indenting newlines. Otherwise, the number of
 * characters written as the template result that come before interpolation
 * are used when indenting (this takes into account any interpolation that
 * may precede block interpolation).
 *
 * <p>Block interpolation is particularly used when using text blocks in Java
 * because it allows templates to more closely match their end result.
 *
 * <pre>{@code
 * // Assume handleNull, handleA, and handleB are Runnable.
 * writer.write("""
 *     if (foo == null) {
 *         ${C|}
 *     } else if (foo == "a") {
 *         ${C|}
 *     } else if (foo == "b") {
 *         ${C|}
 *     }
 *     """,
 *     handleNull,
 *     handleA,
 *     handleB);
 * }</pre>
 *
 * <h3>Template conditionals and loops</h3>
 *
 * <p>AbstractCodeWriter is a lightweight template engine that supports conditional
 * blocks and loops.
 *
 * <h4>Conditional blocks</h4>
 *
 * <p>Conditional blocks are defined using the following syntax:
 *
 * <pre>{@code
 * ${?foo}
 * Foo is set: ${foo:L}
 * ${/foo}
 * }</pre>
 *
 * <p>Assuming {@code foo} is truthy and set to "hi", then the above template
 * outputs: "Foo is set: hi"
 *
 * <p>In the above example, "?" indicates that the expression is a conditional block
 * to check if the named parameter "foo" is truthy. If it is, then the contents of the
 * block up to the matching closing block, {@code ${/foo}}, are evaluated. If the
 * condition is not satisfied, then contents of the block are skipped.
 *
 * <p>You can check if a named property is falsey using "^":
 *
 * <pre>{@code
 * ${^foo}
 * Foo is not set
 * ${/foo}
 * }</pre>
 *
 * <p>Assuming {@code foo} is set to "hi", then the above template outputs nothing.
 * If {@code foo} is falsey, then the above template output "Foo is not set".
 *
 * <h4>Truthy and falsey</h4>
 *
 * <p>The following values are considered falsey:
 *
 * <ul>
 *     <li>values that are not found</li>
 *     <li>null values</li>
 *     <li>false</li>
 *     <li>empty {@link String}</li>
 *     <li>empty {@link Iterable}</li>
 *     <li>empty {@link Map}</li>
 *     <li>empty {@link Optional}</li>
 * </ul>
 *
 * <p>Values that are not falsey are considered truthy.
 *
 * <h4>Loops</h4>
 *
 * <p>Loops can be created to repeat a section of a template for each value stored in
 * a list or each each key value pair stored in a map. Loops are created using "#".
 *
 * <p>The following template with a "foo" value of {"key1": "a", "key2": "b", "key3": "c"}:
 *
 * <pre>{@code
 * ${#foo}
 * - ${key:L}: ${value:L} (first: ${key.first:L}, last: ${key.last:L})
 * ${/foo}
 * }</pre>
 *
 * <p>Evaluates to:</p>
 *
 * <pre>{@code
 * - key1: a (first: true, last: false)
 * - key2: b (first: false, last: false)
 * - key3: c (first: false, last: true)
 * }</pre>
 *
 * <p>Each iteration of the loop pushes a new state in the writer that sets the following
 * context properties:
 *
 * <ul>
 *     <li>key: contains the current 0-based index of an iterator or the current key of a map entry</li>
 *     <li>value: contains the current value of an iterator or current value of a map entry</li>
 *     <li>key.first: set to true if the loop is on the first iteration</li>
 *     <li>key.false: set to true if the loop is on the last iteration</li>
 * </ul>
 *
 * <p>A custom variable name can be used in loops. For example:
 *
 * <pre>{@code
 * ${#foo as key1, value1}
 *     - ${key1:L}: ${value1:L} (first: ${key1.first:L}, last: ${key1.last:L})
 * ${/foo}
 * }</pre>
 *
 * <h4>Whitespace control</h4>
 *
 * <p>Conditional blocks that occur on lines that only contain whitespace are not written
 * to the template output. For example, if the condition in the following template evaluates
 * to falsey, then the template expands to an empty string:
 *
 * <pre>{@code
 * ${?foo}
 * Foo is set: ${foo:L}
 * ${/foo}
 * }</pre>
 *
 * <p>Whitespace that comes before an expression can be removed by putting "~" at the beginning of an expression.
 *
 * <p>Assuming that the first positional argument is "hi":
 *
 * <pre>{@code
 * Greeting:
 *     ${~L}
 * }</pre>
 *
 * <p>Expands to:
 *
 * <pre>{@code
 * Greeting:hi
 * }</pre>
 *
 * <p>Whitespace that comes after an expression can be removed by adding "~" to the end of the expression:
 *
 * <pre>{@code
 * ${L~}
 *
 * .
 * }</pre>
 *
 * <p>Expands to:
 *
 * <pre>{@code
 * hi.
 * }</pre>
 *
 * <p>Leading whitespace cannot be removed when using inline block alignment ('|'). The following is invalid:
 *
 * <pre>{@code
 * ${~C|}
 * }</pre>
 */
public abstract class AbstractCodeWriter<T extends AbstractCodeWriter<T>> {

    // Valid formatter characters that can be registered. Must be sorted for binary search to work.
    static final char[] VALID_FORMATTER_CHARS = {
            '!',
            '%',
            '&',
            '*',
            '+',
            ',',
            '-',
            '.',
            ';',
            '=',
            '@',
            'A',
            'B',
            'C',
            'D',
            'E',
            'F',
            'G',
            'H',
            'I',
            'J',
            'K',
            'L',
            'M',
            'N',
            'O',
            'P',
            'Q',
            'R',
            'S',
            'T',
            'U',
            'V',
            'W',
            'X',
            'Y',
            'Z',
            '_',
            '`'};

    private static final Pattern LINES = Pattern.compile("\\r?\\n");
    private static final Map<Character, BiFunction<Object, String, String>> DEFAULT_FORMATTERS = MapUtils.of(
            'L',
            (s, i) -> formatLiteral(s),
            'S',
            (s, i) -> StringUtils.escapeJavaString(formatLiteral(s), i));

    private final Deque<State> states = new ArrayDeque<>();
    private State currentState;
    private boolean trailingNewline = true;
    private int trimBlankLines = -1;
    private boolean enableStackTraceComments;

    /**
     * Creates a new SimpleCodeWriter that uses "\n" for a newline, four spaces
     * for indentation, does not strip trailing whitespace, does not flatten
     * multiple successive blank lines into a single blank line, and adds no
     * trailing new line.
     */
    public AbstractCodeWriter() {
        states.push(new State());
        currentState = states.getFirst();
        currentState.builder = new StringBuilder();
        // This is initially set to true to account for the case when a code writer is
        // initialized with indentation but hasn't written anything yet.
        currentState.needsIndentation = true;
    }

    /**
     * Copies settings from the given AbstractCodeWriter into this AbstractCodeWriter.
     *
     * <p>The settings of the {@code other} AbstractCodeWriter will overwrite both global and state-based settings
     * of this AbstractCodeWriter.
     *
     * <p>Stateful settings of the {@code other} AbstractCodeWriter like formatters, interceptors, and context are
     * flattened and then copied into the <em>current</em> state of this AbstractCodeWriter. Any conflicts between
     * formatters, interceptors, or context of the current writer are overwritten by the other writer. The stack of
     * states and the contents written to {@code other} are not copied.
     *
     * <pre>{@code
     * SimpleCodeWriter a = new SimpleCodeWriter();
     * a.setExpressionStart('#');
     *
     * SimpleCodeWriter b = new SimpleCodeWriter();
     * b.copySettingsFrom(a);
     *
     * assert(b.getExpressionStart() == '#');
     * }</pre>
     *
     * @param other CodeWriter to copy settings from.
     */
    public void copySettingsFrom(AbstractCodeWriter<T> other) {
        // Copy global settings.
        trailingNewline = other.trailingNewline;
        trimBlankLines = other.trimBlankLines;
        enableStackTraceComments = other.enableStackTraceComments;

        // Copy the current state settings of other into the current state.
        currentState.copyStateFrom(other.currentState);

        // Flatten containers into the current state. This is done in reverse order to ensure that more recent
        // state changes supersede earlier changes.
        Iterator<State> reverseOtherStates = other.states.descendingIterator();
        while (reverseOtherStates.hasNext()) {
            State otherState = reverseOtherStates.next();
            currentState.interceptors.addAll(otherState.interceptors);
            currentState.formatters.putAll(otherState.formatters);
            currentState.context.putAll(otherState.context);
        }
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
            Optional<?> optional = (Optional<?>) value;
            return optional.isPresent() ? formatLiteral(optional.get()) : "";
        } else {
            return String.valueOf(value);
        }
    }

    /**
     * Adds a custom formatter expression to the current state of the
     * {@code AbstractCodeWriter}.
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
     * @param formatFunction Formatter function that formats the given object as a String.
     *                       The formatter is give the value to format as an object
     *                       (use .toString to access the string contents) and the
     *                       current indentation string of the AbstractCodeWriter.
     * @return Returns self.
     */
    @SuppressWarnings("unchecked")
    public T putFormatter(char identifier, BiFunction<Object, String, String> formatFunction) {
        this.currentState.putFormatter(identifier, formatFunction);
        return (T) this;
    }

    /**
     * Sets the character used to start expressions in the current state when calling
     * {@link #write}, {@link #writeInline}, {@link #openBlock}, etc.
     *
     * <p>By default, {@code $} is used to start expressions (for example
     * {@code $L}. However, some programming languages frequently give
     * syntactic meaning to {@code $}, making this an inconvenient syntactic
     * character for the AbstractCodeWriter. In these cases, the character used to
     * start a AbstractCodeWriter expression can be changed. Just like {@code $}, the
     * custom start character can be escaped using two subsequent start
     * characters (e.g., {@code $$}).
     *
     * @param expressionStart Character to use to start expressions.
     * @return Returns self.
     */
    @SuppressWarnings("unchecked")
    public T setExpressionStart(char expressionStart) {
        if (expressionStart == ' ' || expressionStart == '\n') {
            throw new IllegalArgumentException("expressionStart must not be set to " + expressionStart);
        }

        currentState.expressionStart = expressionStart;
        return (T) this;
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
     * <p>The result will have an appended newline if the AbstractCodeWriter is
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
            return trailingNewline ? currentState.newline : "";
        }

        // This accounts for cases where the only write on the AbstractCodeWriter was
        // an inline write, but the write ended with spaces.
        if (currentState.trimTrailingSpaces) {
            result = StringUtils.stripEnd(result, " ");
        }

        if (trailingNewline) {
            // Add a trailing newline if needed.
            return result.endsWith(currentState.newline) ? result : result + currentState.newline;
        } else if (result.endsWith(currentState.newline)) {
            // Strip the trailing newline if present.
            return result.substring(0, result.length() - currentState.newline.length());
        } else {
            return result;
        }
    }

    /**
     * Copies and pushes the current state to the state stack.
     *
     * <p>This method is used to prepare for a corresponding {@link #popState}
     * operation later. It stores the current state of the AbstractCodeWriter into a
     * stack and keeps it active. After pushing, mutations can be made to the
     * state of the AbstractCodeWriter without affecting the previous state on the
     * stack. Changes to the state of the AbstractCodeWriter can be undone by using
     * {@link #popState()}, which Returns self state to the state
     * it was in before calling {@code pushState}.
     *
     * @return Returns the code writer.
     */
    @SuppressWarnings("unchecked")
    public T pushState() {
        currentState = new State(currentState);
        states.push(currentState);
        return (T) this;
    }

    /**
     * Copies and pushes the current state to the state stack using a named
     * state that can be intercepted by functions registered with
     * {@link #onSection(CodeInterceptor)}.
     *
     * <p>The text written while in this state is buffered and passed to each
     * state interceptor. If no text is written by the section or an
     * interceptor, nothing is changed on the {@code AbstractCodeWriter}. This
     * behavior allows for placeholder sections to be added into
     * {@code AbstractCodeWriter} generators in order to provide extension points
     * that can be otherwise empty.
     *
     * @param sectionName Name of the section to set on the state.
     * @return Returns the code writer.
     */
    public T pushState(String sectionName) {
        return pushState(CodeSection.forName(sectionName));
    }

    /**
     * Pushes a strongly typed section extension point.
     *
     * <p>Interceptors can be registered to intercept this specific type
     * of CodeSection using a {@link CodeInterceptor} and providing a
     * class for which {@code section} is an instance.
     *
     * @param section The section value to push.
     * @return Returns self.
     * @see #onSection(CodeInterceptor)
     */
    @SuppressWarnings("unchecked")
    public T pushState(CodeSection section) {
        pushState();
        currentState.makeInterceptableCodeSection(section);
        return (T) this;
    }

    /**
     * Creates a section that contains no content used to allow {@link CodeInterceptor}s
     * to inject content at specific locations.
     *
     * @param section The code section to register that can be intercepted by type.
     * @return Returns self.
     */
    public T injectSection(CodeSection section) {
        return pushState(section).popState();
    }

    /**
     * Gets the debug path to the current state so that errors encountered while
     * using AbstractCodeWriter are easier to track down.
     *
     * <p>For example, if state "Foo" is entered, and then an unnamed state is
     * entered, the return value is "ROOT/Foo/UNNAMED".
     *
     * @return Returns a "/" separated path to the current state.
     */
    private String getStateDebugPath() {
        StringJoiner result = new StringJoiner("/");
        Iterator<State> iterator = states.descendingIterator();
        while (iterator.hasNext()) {
            result.add(iterator.next().getSectionName());
        }
        return result.toString();
    }

    /**
     * Gets debug information about the current state of the AbstractCodeWriter, including
     * the path to the current state as returned by {@link #getStateDebugPath()},
     * and up to the last two lines of text written to the AbstractCodeWriter.
     *
     * <p>This debug information is used in most exceptions thrown by AbstractCodeWriter to
     * provide additional context when something goes wrong. It can also be used
     * by subclasses and collaborators to aid in debugging codegen issues.
     *
     * @return Returns debug info as a string.
     * @see #getDebugInfo(int)
     */
    public final CodeWriterDebugInfo getDebugInfo() {
        return getDebugInfo(2);
    }

    /**
     * Gets debug information about the current state of the AbstractCodeWriter.
     *
     * <p>This method can be overridden in order to add more metadata to the created
     * debug info object.
     *
     * @param numberOfContextLines Include the last N lines in the output. Set to 0 to omit lines.
     * @return Returns debug info as a string.
     * @see #getDebugInfo
     */
    public CodeWriterDebugInfo getDebugInfo(int numberOfContextLines) {
        // Implementer's note: a snapshot of AbstractCodeWriter is used rather than just
        // querying data from AbstractCodeWriter from within CodeWriterDebugInfo each time
        // toString is called on it. This ensures that debug info is immutable, at
        // least in terms of the state path and the most recent lines written.
        CodeWriterDebugInfo info = new CodeWriterDebugInfo();
        info.putMetadata("path", getStateDebugPath());

        if (numberOfContextLines < 0) {
            throw new IllegalArgumentException("Cannot get fewer than 0Lines");
        } else if (numberOfContextLines > 0) {
            StringBuilder lastLines = new StringBuilder();
            // Get the last N lines of text written.
            String str = toString();
            if (!str.isEmpty()) {
                String[] lines = str.split("\r?\n", 0);
                int startPosition = Math.max(0, lines.length - numberOfContextLines);
                for (int i = startPosition; i < lines.length; i++) {
                    lastLines.append(lines[i]).append("\\n");
                }
            }
            info.putMetadata("near", lastLines.toString());
        }

        return info;
    }

    /**
     * Pushes an anonymous named state that is always passed through the given
     * filter function before being written to the writer.
     *
     * @param filter Function that maps over the entire section when popped.
     * @return Returns the code writer.
     */
    @SuppressWarnings("unchecked")
    public T pushFilteredState(Function<String, String> filter) {
        CodeSection section = CodeSection.forName("__filtered_state_" + states.size() + 1);
        pushState(section);
        onSection(CodeInterceptor.forName(section.sectionName(), (w, content) -> {
            writeInlineWithNoFormatting(filter.apply(content));
        }));
        return (T) this;
    }

    /**
     * Pops the current AbstractCodeWriter state from the state stack.
     *
     * <p>This method is used to reverse a previous {@link #pushState}
     * operation. It configures the current AbstractCodeWriter state to what it was
     * before the last preceding {@code pushState} call.
     *
     * @return Returns self.
     * @throws IllegalStateException if there a no states to pop.
     */
    @SuppressWarnings("unchecked")
    public T popState() {
        if (states.size() == 1) {
            throw new IllegalStateException("Cannot pop writer state because at the root state");
        }

        State popped = states.pop();
        currentState = states.getFirst();
        CodeSection sectionValue = popped.sectionValue;

        if (sectionValue != null) {
            // Get the contents of the current state as a string so it can be filtered.
            String result = popped.toString();

            // Don't attempt to intercept anonymous sections.
            if (!(sectionValue instanceof AnonymousCodeSection)) {
                // Ensure the remaining parent interceptors are applied in the order they were inserted.
                // This is the reverse order used when normally iterating over the states deque.
                Iterator<State> insertionOrderedStates = states.descendingIterator();
                while (insertionOrderedStates.hasNext()) {
                    State state = insertionOrderedStates.next();
                    result = applyPoppedInterceptors(popped, state, sectionValue, result);
                }
                // Now ensure the popped state's interceptors are applied.
                result = applyPoppedInterceptors(popped, popped, sectionValue, result);
            }

            if (popped.isInline) {
                // Inline sections need to be written back to the popped state, not the parent state.
                // They also can't use write because other changes to the builder since capturing
                // the result string will alter the result.
                StringBuilder builder = popped.getBuilder();
                builder.setLength(0);
                builder.append(result);
            } else if (!result.isEmpty()) {
                writeInlineWithNoFormatting(result);
            }
        }

        if (!popped.isInline && popped.needsIndentation) {
            currentState.needsIndentation = true;
        }

        return (T) this;
    }

    private String applyPoppedInterceptors(State popped, State state, CodeSection sectionValue, String result) {
        for (CodeInterceptor<CodeSection, T> interceptor : state.getInterceptors(sectionValue)) {
            result = interceptSection(popped, interceptor, result);
        }
        return result;
    }

    // This method exists because inlining in popSection is impossible due to needing to mutate a result variable.
    @SuppressWarnings("unchecked")
    private String interceptSection(State popped, CodeInterceptor<CodeSection, T> interceptor, String previous) {
        return expandSection(new AnonymousCodeSection("__" + popped.getSectionName()), previous, interceptorToCall -> {
            interceptor.write((T) this, previous, popped.sectionValue);
        });
    }

    /**
     * Registers a function that intercepts the contents of a section and
     * writes to the {@code AbstractCodeWriter} with the updated contents.
     *
     * <p>The {@code interceptor} function is expected to have a reference to
     * the {@code AbstractCodeWriter} and to mutate it when they are invoked. Each
     * interceptor is invoked in their own isolated pushed/popped states.
     *
     * <p>The text provided to {@code interceptor} does not contain a trailing
     * new line. A trailing new line is expected to be injected automatically
     * when the results of intercepting the contents are written to the
     * {@code AbstractCodeWriter}. A result is only written if the interceptors write
     * a non-null, non-empty string, allowing for empty placeholders to be
     * added that don't affect the resulting layout of the code.
     *
     * <pre>{@code
     * SimpleCodeWriter = new SimpleCodeWriter();
     *
     * // Prepend text to a section named "foo".
     * writer.onSectionPrepend("foo", () -> writer.write("A"));
     *
     * // Write text to a section, and ensure that the original
     * // text is written too.
     * writer.onSection("foo", text -> {
     *     // Write before the original text.
     *     writer.write("A");
     *     // Write the original text of the section.
     *     writer.writeWithNoFormatting(text);
     *     // Write more text to the section.
     *     writer.write("C");
     * });
     *
     * // Create the section, write to it, then close the section.
     * writer.pushState("foo").write("B").popState();
     *
     * assert(writer.toString().equals("A\nB\nC\n"));
     * }</pre>
     *
     * <h3>Newline handling</h3>
     *
     * <p>This method is a wrapper around {@link #onSection(CodeInterceptor)}
     * that has several limitations:
     *
     * <ul>
     *     <li>The provided {@code interceptor} is expected to have a reference
     *     to an {@link AbstractCodeWriter} so that write calls can be made.</li>
     *     <li>The handling of newlines is much less precise. If you want to
     *     give interceptors full control over how newlines are injected, then
     *     {@link #onSection(CodeInterceptor)} must be used directly and
     *     careful use of {@link #writeInlineWithNoFormatting(Object)} is
     *     required when writing the previous contents to the interceptor.</li>
     *     <li>Interceptors do not have access to strongly typed event data
     *     like {@link CodeInterceptor}s do.
     * </ul>
     *
     * <p>The newline handling functionality provided by this method can be
     * reproduced using a {@link CodeInterceptor} by removing trailing newlines
     * using {@link #removeTrailingNewline(String)}.
     *
     * <pre>{@code
     * SimpleCodeWriter = new SimpleCodeWriter();
     *
     * CodeInterceptor<CodeSection, SimpleCodeWriter> interceptor = CodeInterceptor.forName(sectionName, (w, p) -> {
     *     String trimmedContent = removeTrailingNewline(p);
     *     interceptor.accept(trimmedContent);
     * })
     *
     * writer.onSection(interceptor);
     * }</pre>
     *
     * @param sectionName The name of the section to intercept.
     * @param interceptor The function to intercept with.
     * @return Returns self.
     */
    @SuppressWarnings("unchecked")
    public T onSection(String sectionName, Consumer<Object> interceptor) {
        currentState.putInterceptor(CodeInterceptor.forName(sectionName, (w, p) -> {
            String trimmedContent = removeTrailingNewline(p);
            interceptor.accept(trimmedContent);
        }));
        return (T) this;
    }

    /**
     * Intercepts a section of code emitted for a strongly typed {@link CodeSection}.
     *
     * <p>These section interceptors provide a kind of event-based hook system for
     * AbstractCodeWriters that add extension points when generating code. The function has
     * the ability to completely ignore the original contents of the section, to
     * prepend text to it, and append text to it. Intercepting functions are
     * expected to have a reference to the {@code AbstractCodeWriter} and to mutate it
     * when they are invoked. Each interceptor is invoked in their own
     * isolated pushed/popped states.
     *
     * <p>Interceptors are registered on the current state of the
     * {@code AbstractCodeWriter}. When the state to which an interceptor is registered
     * is popped, the interceptor is no longer in scope.
     *
     * @param interceptor A consumer that takes the writer and strongly typed section.
     * @param <S> The type of section being intercepted.
     * @return Returns self.
     */
    @SuppressWarnings("unchecked")
    public <S extends CodeSection> T onSection(CodeInterceptor<S, T> interceptor) {
        currentState.putInterceptor(interceptor);
        return (T) this;
    }

    /**
     * Disables the automatic appending of newlines in the current state.
     *
     * <p>Methods like {@link #write}, {@link #openBlock}, and {@link #closeBlock}
     * will not automatically append newlines when a state has this flag set.
     *
     * @return Returns self.
     */
    @SuppressWarnings("unchecked")
    public T disableNewlines() {
        currentState.disableNewline = true;
        return (T) this;
    }

    /**
     * Enables the automatic appending of newlines in the current state.
     *
     * @return Returns self.
     */
    @SuppressWarnings("unchecked")
    public T enableNewlines() {
        currentState.disableNewline = false;
        return (T) this;
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
     * <p>Setting the newline character to a non-empty string implicitly
     * enables newlines in the current state.
     *
     * @param newline Newline character to use.
     * @return Returns self.
     */
    public T setNewline(String newline) {
        if (newline.isEmpty()) {
            return disableNewlines();
        } else {
            currentState.newline = newline;
            return enableNewlines();
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
     * @return Returns self.
     */
    public T setNewline(char newline) {
        return setNewline(String.valueOf(newline));
    }

    /**
     * Gets the character used to represent newlines in the current state.
     *
     * @return Returns the newline string.
     */
    public String getNewline() {
        return currentState.newline;
    }

    /**
     * Sets the text used for indentation (defaults to four spaces).
     *
     * @param indentText Indentation text.
     * @return Returns self.
     */
    @SuppressWarnings("unchecked")
    public T setIndentText(String indentText) {
        currentState.indent(0, indentText);
        return (T) this;
    }

    /**
     * Gets the text used for indentation (defaults to four spaces).
     *
     * @return Returns the indentation string.
     */
    public final String getIndentText() {
        return currentState.indentText;
    }

    /**
     * Enables the trimming of trailing spaces on a line.
     *
     * @return Returns self.
     */
    public T trimTrailingSpaces() {
        return trimTrailingSpaces(true);
    }

    /**
     * Configures if trailing spaces on a line are removed.
     *
     * @param trimTrailingSpaces Set to true to trim trailing spaces.
     * @return Returns self.
     */
    @SuppressWarnings("unchecked")
    public T trimTrailingSpaces(boolean trimTrailingSpaces) {
        currentState.trimTrailingSpaces = trimTrailingSpaces;
        return (T) this;
    }

    /**
     * Returns true if the trailing spaces in the current state are trimmed.
     *
     * @return Returns the trailing spaces setting of the current state.
     */
    public boolean getTrimTrailingSpaces() {
        return currentState.trimTrailingSpaces;
    }

    /**
     * Ensures that no more than one blank line occurs in succession.
     *
     * @return Returns self.
     */
    public T trimBlankLines() {
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
     * @return Returns self.
     */
    @SuppressWarnings("unchecked")
    public T trimBlankLines(int trimBlankLines) {
        this.trimBlankLines = trimBlankLines;
        return (T) this;
    }

    /**
     * Returns the number of allowed consecutive newlines that are not
     * trimmed by the AbstractCodeWriter when written to a string.
     *
     * @return Returns the number of allowed consecutive newlines. -1 means
     *   that no newlines are trimmed. 0 allows no blank lines. 1 or more
     *   allows for no more than N consecutive blank lines.
     */
    public int getTrimBlankLines() {
        return trimBlankLines;
    }

    /**
     * Configures the AbstractCodeWriter to always append a newline at the end of
     * the text if one is not already present.
     *
     * <p>This setting is not captured as part of push/popState.
     *
     * @return Returns self.
     */
    public T insertTrailingNewline() {
        return insertTrailingNewline(true);
    }

    /**
     * Configures the AbstractCodeWriter to always append a newline at the end of
     * the text if one is not already present.
     *
     * <p>This setting is not captured as part of push/popState.
     *
     * @param trailingNewline True if a newline is added.
     *
     * @return Returns self.
     */
    @SuppressWarnings("unchecked")
    public T insertTrailingNewline(boolean trailingNewline) {
        this.trailingNewline = trailingNewline;
        return (T) this;
    }

    /**
     * Checks if the AbstractCodeWriter inserts a trailing newline (if necessary) when
     * converted to a string.
     *
     * @return The newline behavior (true to insert a trailing newline).
     */
    public boolean getInsertTrailingNewline() {
        return trailingNewline;
    }

    /**
     * Sets a prefix to prepend to every line after a new line is added
     * (except for an inserted trailing newline).
     *
     * @param newlinePrefix Newline prefix to use.
     * @return Returns self.
     */
    @SuppressWarnings("unchecked")
    public T setNewlinePrefix(String newlinePrefix) {
        currentState.newlinePrefix = newlinePrefix;
        return (T) this;
    }

    /**
     * Gets the prefix to prepend to every line after a new line is added
     * (except for an inserted trailing newline).
     *
     * @return Returns the newline prefix string.
     */
    public String getNewlinePrefix() {
        return currentState.newlinePrefix;
    }

    /**
     * Indents all text one level.
     *
     * @return Returns self.
     */
    public T indent() {
        return indent(1);
    }

    /**
     * Indents all text a specific number of levels.
     *
     * @param levels Number of levels to indent.
     * @return Returns self.
     */
    @SuppressWarnings("unchecked")
    public T indent(int levels) {
        currentState.indent(levels, null);
        return (T) this;
    }

    /**
     * Gets the indentation level of the current state.
     *
     * @return Returns the indentation level of the current state.
     */
    public int getIndentLevel() {
        return currentState.indentation;
    }

    /**
     * Removes one level of indentation from all lines.
     *
     * @return Returns self.
     */
    public T dedent() {
        return dedent(1);
    }

    /**
     * Removes a specific number of indentations from all lines.
     *
     * <p>Set to -1 to dedent back to 0 (root).
     *
     * @param levels Number of levels to remove.
     * @return Returns self.
     * @throws IllegalStateException when trying to dedent too far.
     */
    @SuppressWarnings("unchecked")
    public T dedent(int levels) {
        int adjusted = levels == -1 ? Integer.MIN_VALUE : -1 * levels;
        currentState.indent(adjusted, null);
        return (T) this;
    }

    /**
     * Opens a block of syntax by writing text, a newline, then indenting.
     *
     * <pre>
     * {@code
     * String result = new SimpleCodeWriter()
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
     * @return Returns this.
     */
    public T openBlock(String textBeforeNewline, Object... args) {
        return write(textBeforeNewline, args).indent();
    }

    /**
     * Opens a block of syntax by writing {@code textBeforeNewline}, a newline, then
     * indenting, then executes the given {@code Runnable}, then closes the block of
     * syntax by writing a newline, dedenting, then writing {@code textAfterNewline}.
     *
     * <pre>{@code
     * SimpleCodeWriter = new SimpleCodeWriter();
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
     * @return Returns this.
     */
    public T openBlock(String textBeforeNewline, String textAfterNewline, Runnable f) {
        return openBlock(textBeforeNewline, textAfterNewline, new Object[] {}, f);
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
     * @return Returns this.
     */
    public T openBlock(String textBeforeNewline, String textAfterNewline, Object arg1, Runnable f) {
        return openBlock(textBeforeNewline, textAfterNewline, new Object[] {arg1}, f);
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
     * @return Returns this.
     */
    public T openBlock(
            String textBeforeNewline,
            String textAfterNewline,
            Object arg1,
            Object arg2,
            Runnable f
    ) {
        return openBlock(textBeforeNewline, textAfterNewline, new Object[] {arg1, arg2}, f);
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
     * @return Returns this.
     */
    public T openBlock(
            String textBeforeNewline,
            String textAfterNewline,
            Object arg1,
            Object arg2,
            Object arg3,
            Runnable f
    ) {
        return openBlock(textBeforeNewline, textAfterNewline, new Object[] {arg1, arg2, arg3}, f);
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
     * @return Returns this.
     */
    public T openBlock(
            String textBeforeNewline,
            String textAfterNewline,
            Object arg1,
            Object arg2,
            Object arg3,
            Object arg4,
            Runnable f
    ) {
        return openBlock(textBeforeNewline, textAfterNewline, new Object[] {arg1, arg2, arg3, arg4}, f);
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
     * @return Returns this.
     */
    public T openBlock(
            String textBeforeNewline,
            String textAfterNewline,
            Object arg1,
            Object arg2,
            Object arg3,
            Object arg4,
            Object arg5,
            Runnable f
    ) {
        return openBlock(textBeforeNewline, textAfterNewline, new Object[] {arg1, arg2, arg3, arg4, arg5}, f);
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
     * @return Returns this.
     */
    @SuppressWarnings("unchecked")
    public T openBlock(String textBeforeNewline, String textAfterNewline, Object[] args, Runnable f) {
        write(textBeforeNewline, args).indent();
        f.run();
        closeBlock(textAfterNewline);
        return (T) this;
    }

    /**
     * Closes a block of syntax by writing a newline, dedenting, then writing text.
     *
     * @param textAfterNewline Text to write after writing a newline and dedenting.
     * @param args String arguments to use for formatting.
     * @return Returns this.
     */
    public T closeBlock(String textAfterNewline, Object... args) {
        return dedent().write(textAfterNewline, args);
    }

    /**
     * Writes text to the AbstractCodeWriter and appends a newline.
     *
     * <p>The provided text does not use any kind of expression formatting.
     *
     * <p>Indentation and the newline prefix is only prepended if the writer's
     * cursor is at the beginning of a newline.
     *
     * <p>Stack trace comments are written along with the given content if
     * {@link #enableStackTraceComments(boolean)} was called with {@code true}.
     *
     * @param content Content to write.
     * @return Returns self.
     */
    @SuppressWarnings("unchecked")
    public T writeWithNoFormatting(Object content) {
        currentState.writeLine(findAndFormatStackTraceElement(content.toString(), false));
        return (T) this;
    }

    private String findAndFormatStackTraceElement(String content, boolean inline) {
        if (enableStackTraceComments) {
            for (StackTraceElement e : Thread.currentThread().getStackTrace()) {
                if (isStackTraceRelevant(e)) {
                    return formatWithStackTraceElement(content, e, inline);
                }
            }
        }

        return content;
    }

    /**
     * Tests if the given {@code StackTraceElement} is relevant for a comment
     * used when writing debug information before calls to write.
     *
     * <p>The default implementation filters out all methods in "java.*",
     * AbstractCodeWriter, software.amazon.smithy.utils.SymbolWriter,
     * SimpleCodeWriter, and methods of the implementing subclass of
     * AbstractCodeWriter. This method can be overridden to further filter
     * stack frames as needed.
     *
     * @param e StackTraceElement to test.
     * @return Returns true if this element should be in a comment.
     */
    protected boolean isStackTraceRelevant(StackTraceElement e) {
        String normalized = e.getClassName().replace("$", ".");
        return !normalized.startsWith("java.")
                // Ignore writes made by AbstractCodeWriter or AbstractCodeWriter$State.
                && !normalized.startsWith(AbstractCodeWriter.class.getCanonicalName())
                // Ignore writes made by subclasses of this class.
                && !normalized.startsWith(getClass().getCanonicalName())
                // Ignore writes made by SimpleCodeWriter.
                && !normalized.equals(SimpleCodeWriter.class.getCanonicalName())
                // Ignore any writes made by the well-known SymbolWriter from smithy-codegen-core.
                && !normalized.equals("software.amazon.smithy.utils.SymbolWriter");
    }

    /**
     * Formats content for the given stack frame.
     *
     * <p>Subclasses are expected to override this method as needed to handle
     * language-specific comment requirements. By default, this class will use
     * C/Java style "traditional" comments that come on the same line before
     * both calls to writeInline and calls to write with a newline
     * {@see https://docs.oracle.com/javase/specs/jls/se18/html/jls-3.html#jls-3.7}.
     *
     * <p>Programming languages that do not support inline comments should return
     * the given {@code content} string as-is when {@code writingInline} is set
     * to {@code true}.
     *
     * @param content The content about to be written.
     * @param element The {@code StackFrameElement} to format.
     * @param inline Set to true when this is a comment intended to appear before inline content.
     * @return Returns the formatted content that includes a leading comment.
     */
    protected String formatWithStackTraceElement(String content, StackTraceElement element, boolean inline) {
        return "/* " + element + " */ " + content;
    }

    /**
     * Writes inline text to the AbstractCodeWriter with no formatting.
     *
     * <p>The provided text does not use any kind of expression formatting.
     * Indentation and the newline prefix is only prepended if the writer's
     * cursor is at the beginning of a newline.
     *
     * <p>Stack trace comments are written along with the given content if
     * {@link #enableStackTraceComments(boolean)} was called with {@code true}.
     *
     * @param content Inline content to write.
     * @return Returns self.
     */
    @SuppressWarnings("unchecked")
    public final T writeInlineWithNoFormatting(Object content) {
        currentState.write(findAndFormatStackTraceElement(content.toString(), true));
        return (T) this;
    }

    /**
     * Creates a formatted string using formatter expressions and variadic
     * arguments.
     *
     * <p>Important: if the formatters that are executed while formatting the
     * given {@code content} string mutate the AbstractCodeWriter, it could leave the
     * SimpleCodeWriter in an inconsistent state. For example, some AbstractCodeWriter
     * implementations manage imports and dependencies automatically based on
     * code that is referenced by formatters. If such an expression is used
     * with this format method but the returned String is never written to the
     * AbstractCodeWriter, then the AbstractCodeWriter might be mutated to track dependencies
     * that aren't actually necessary.
     *
     * <pre>{@code
     * SimpleCodeWriter = new SimpleCodeWriter();
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
        StringBuilder result = new StringBuilder();
        CodeFormatter.run(result, this, Objects.requireNonNull(content).toString(), args);
        return result.toString();
    }

    /**
     * A simple helper method that makes it easier to invoke the built-in {@code C}
     * (call) formatter using a {@link Consumer} where {@code T} is the specific type
     * of {@link AbstractCodeWriter}.
     *
     * <p>Instead of having to type this:
     *
     * <pre>{@code
     * writer.write("$C", (Consumer<MyWriter>) (w) -> w.write("Hi"));
     * }</pre>
     *
     * <p>You can write:
     *
     * <pre>{@code
     * writer.write("$C", writer.consumer(w -> w.write("Hi"));
     * }</pre>
     *
     * @param consumer The consumer to call.
     * @return Returns the consumer as-is, but cast as the appropriate Java type.
     */
    public Consumer<T> consumer(Consumer<T> consumer) {
        return consumer;
    }

    /**
     * Allows calling out to arbitrary code for things like looping or
     * conditional writes without breaking method chaining.
     *
     * @param task Method to invoke.
     * @return Returns this.
     */
    @SuppressWarnings("unchecked")
    public T call(Runnable task) {
        task.run();
        return (T) this;
    }

    /**
     * Writes text to the AbstractCodeWriter and appends a newline.
     *
     * <p>The provided text is automatically formatted using variadic
     * arguments.
     *
     * <p>Indentation and the newline prefix is only prepended if the writer's
     * cursor is at the beginning of a newline.
     *
     * <p>If a subclass overrides this method, it <em>should</em> first
     * perform formatting and then delegate to {@link #writeWithNoFormatting}
     * to perform the actual write.
     *
     * @param content Content to write.
     * @param args String arguments to use for formatting.
     * @return Returns self.
     */
    public T write(Object content, Object... args) {
        return writeWithNoFormatting(format(content, args));
    }

    /**
     * Writes text to the AbstractCodeWriter without appending a newline.
     *
     * <p>The provided text is automatically formatted using variadic
     * arguments.
     *
     * <p>Indentation and the newline prefix is only prepended if the writer's
     * cursor is at the beginning of a newline.
     *
     * <p>If newlines are present in the given string, each of those lines will receive proper indentation.
     *
     * <p>If a subclass overrides this method, it <em>should</em> first
     * perform formatting and then delegate to {@link #writeInlineWithNoFormatting}
     * to perform the actual write.
     *
     * @param content Content to write.
     * @param args String arguments to use for formatting.
     * @return Returns self.
     */
    public T writeInline(Object content, Object... args) {
        return writeInlineWithNoFormatting(format(content, args));
    }

    /**
     * Ensures that the last text written to the writer was a newline as defined in
     * the current state and inserts one if necessary.
     *
     * @return Returns self.
     */
    @SuppressWarnings("unchecked")
    public T ensureNewline() {
        if (!builderEndsWith(currentState.getBuilder(), getNewline())) {
            write("");
        }
        return (T) this;
    }

    private boolean builderEndsWith(StringBuilder builder, String check) {
        return builder.length() > check.length()
                && builder.substring(builder.length() - check.length(), builder.length()).equals(check);
    }

    /**
     * Optionally writes text to the AbstractCodeWriter and appends a newline
     * if a value is present.
     *
     * <p>If the provided {@code content} value is {@code null}, nothing is
     * written. If the provided {@code content} value is an empty
     * {@code Optional}, nothing is written. If the result of calling
     * {@code toString} on {@code content} results in an empty string,
     * nothing is written. Finally, if the value is a non-empty string,
     * the content is written to the {@code AbstractCodeWriter} at the current
     * level of indentation, and a newline is appended.
     *
     * @param content Content to write if present.
     * @return Returns self.
     */
    @SuppressWarnings("unchecked")
    public T writeOptional(Object content) {
        if (content == null) {
            return (T) this;
        } else if (content instanceof Optional) {
            return writeOptional(((Optional<?>) content).orElse(null));
        } else {
            String value = content.toString();
            return !value.isEmpty() ? write(value) : (T) this;
        }
    }

    /**
     * Remove the most recent text written to the AbstractCodeWriter if and only
     * if the last written text is exactly equal to the given expanded
     * content string.
     *
     * <p>This can be useful, for example, for use cases like removing
     * trailing commas from lists of values.
     *
     * <p>For example, the following will remove ", there." from the
     * end of the AbstractCodeWriter:
     *
     * <pre>{@code
     * SimpleCodeWriter = new SimpleCodeWriter();
     * writer.writeInline("Hello, there.");
     * writer.unwrite(", there.");
     * assert(writer.toString().equals("Hello\n"));
     * }</pre>
     *
     * <p>However, the following call to unwrite will do nothing because
     * the last text written to the AbstractCodeWriter does not match:
     *
     * <pre>{@code
     * SimpleCodeWriter = new SimpleCodeWriter();
     * writer.writeInline("Hello.");
     * writer.unwrite("there.");
     * assert(writer.toString().equals("Hello.\n"));
     * }</pre>
     *
     * @param content Content to write.
     * @param args String arguments to use for formatting.
     * @return Returns self.
     */
    @SuppressWarnings("unchecked")
    public T unwrite(Object content, Object... args) {
        String value = format(content, args);
        int currentLength = currentState.builder.length();

        if (currentState.builder.lastIndexOf(value) == currentLength - value.length()) {
            currentState.builder.setLength(currentLength - value.length());
        }

        return (T) this;
    }

    /**
     * Adds a named key-value pair to the context of the current state.
     *
     * <p>These context values can be referenced by named interpolated
     * parameters.
     *
     * @param key Key to add to the context.
     * @param value Value to associate with the key.
     * @return Returns self.
     */
    @SuppressWarnings("unchecked")
    public T putContext(String key, Object value) {
        currentState.context.put(key, value);
        return (T) this;
    }

    /**
     * Adds a map of named key-value pair to the context of the current state.
     *
     * <p>These context values can be referenced by named interpolated
     * parameters.
     *
     * @param mappings Key value pairs to add.
     * @return Returns self.
     */
    @SuppressWarnings("unchecked")
    public T putContext(Map<String, Object> mappings) {
        mappings.forEach(this::putContext);
        return (T) this;
    }

    /**
     * Removes a named key-value pair from the context of the current state.
     *
     * <p>This method has no effect if the parent state defines the context key value pair.
     *
     * @param key Key to add to remove from the current context.
     * @return Returns self.
     */
    @SuppressWarnings("unchecked")
    public T removeContext(String key) {
        if (currentState.context.containsKey(key)) {
            currentState.context.remove(key);
        } else {
            // Parent states might have a value for this context key, so explicitly set it to null in this context.
            currentState.context.put(key, null);
        }
        return (T) this;
    }

    /**
     * Gets a named contextual key-value pair from the current state or any parent states.
     *
     * @param key Key to retrieve.
     * @return Returns the associated value or null if not present.
     */
    public Object getContext(String key) {
        for (State state : states) {
            if (state.context.containsKey(key)) {
                return state.context.get(key);
            } else if (state.sectionValue != null) {
                Method method = findContextMethod(state.sectionValue, key);
                if (method != null) {
                    try {
                        return method.invoke(state.sectionValue);
                    } catch (ReflectiveOperationException e) {
                        String message = String.format(
                                "Unable to get context '%s' from a matching method of the current CodeSection: %s %s",
                                key,
                                e.getCause() != null ? e.getCause().getMessage() : e.getMessage(),
                                getDebugInfo());
                        throw new RuntimeException(message, e);
                    }
                }
            }
        }
        return null;
    }

    private Method findContextMethod(CodeSection section, String key) {
        for (Method method : section.getClass().getMethods()) {
            if (method.getName().equals(key) || method.getName().equals("get" + StringUtils.capitalize(key))) {
                if (!method.getReturnType().equals(Void.TYPE)) {
                    return method;
                }
            }
        }
        return null;
    }

    /**
     * Gets a named context key-value pair from the current state and
     * casts the value to the given type.
     *
     * @param key Key to retrieve.
     * @param type The type of value expected.
     * @return Returns the associated value or null if not present.
     * @throws ClassCastException if the stored value is not null and does not match {@code type}.
     */
    @SuppressWarnings("unchecked")
    public <C> C getContext(String key, Class<C> type) {
        Object value = getContext(key);
        if (value == null) {
            return null;
        } else if (type.isInstance(value)) {
            return (C) value;
        } else {
            throw new ClassCastException(String.format(
                    "Expected context value '%s' to be an instance of %s, but found %s %s",
                    key,
                    type.getName(),
                    value.getClass().getName(),
                    getDebugInfo()));
        }
    }

    /**
     * Enable or disable writing stack trace comments before each call to
     * {@link #write}, {@link #writeWithNoFormatting}, {@link #writeInline},
     * and {@link #writeInlineWithNoFormatting}.
     *
     * <p>It's sometimes useful to know where in a code generator a line of code
     * generated text came from. Enabling stack trace comments will output
     * the last relevant stack trace information caused text to appear in the
     * code writer's output.
     *
     * @param enableStackTraceComments Set to true to enable stack trace comments.
     * @return Returns self.
     */
    @SuppressWarnings("unchecked")
    public final T enableStackTraceComments(boolean enableStackTraceComments) {
        this.enableStackTraceComments = enableStackTraceComments;
        return (T) this;
    }

    String expandSection(CodeSection section, String previousContent, Consumer<String> consumer) {
        StringBuilder buffer = new StringBuilder();
        pushState(section);
        currentState.makeSectionInline(buffer);
        consumer.accept(previousContent);
        popState();
        return buffer.toString();
    }

    // Used only by CodeFormatter to apply formatters.
    @SuppressWarnings("unchecked")
    String applyFormatter(char identifier, Object value) {
        BiFunction<Object, String, String> f = resolveFormatter(identifier);
        if (f != null) {
            return f.apply(value, getIndentText());
        } else if (identifier == 'C') {
            // The default C formatter is evaluated dynamically to prevent cyclic references in CodeFormatter.
            CodeSection section = new AnonymousCodeSection("__C_formatter_" + states.size());
            // A single trailing newline is stripped from the result, if present. This is because the $C formatter
            // is often used on the same line as other text like:
            //
            //     w.write("Hi, $C.", w.consumer(writer -> writer.write("name");
            //     // Results in "Hi, name.\n" and not "Hi, name\n.\n"
            //
            // In these cases, the trailing newline introduced in the $C formatter by writer.write() is removed
            // since it's up to the surrounding text to dictate newlines. Use an explicit newline in the text to
            // inject a newline.
            if (value instanceof Runnable) {
                Runnable runnable = (Runnable) value;
                return removeTrailingNewline(expandSection(section, "", ignore -> runnable.run()));
            } else if (value instanceof Consumer) {
                Consumer<T> consumer = (Consumer<T>) value;
                return removeTrailingNewline(expandSection(section, "", ignore -> consumer.accept((T) this)));
            } else {
                throw new ClassCastException(String.format(
                        "Expected value for 'C' formatter to be an instance of %s or %s, but found %s %s",
                        Runnable.class.getName(),
                        Consumer.class.getName(),
                        value == null ? "null" : value.getClass().getName(),
                        getDebugInfo()));
            }
        } else {
            // Return null if no formatter was found.
            return null;
        }
    }

    BiFunction<Object, String, String> resolveFormatter(char identifier) {
        for (State state : states) {
            BiFunction<Object, String, String> result = state.getFormatter(identifier);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private final class State {
        private final boolean isRoot;
        private String indentText = "    ";
        private String leadingIndentString = "";
        private String newlinePrefix = "";
        private int indentation;
        private boolean trimTrailingSpaces;
        private boolean disableNewline;
        private String newline = "\n";
        private char expressionStart = '$';
        private boolean needsIndentation;

        private CodeSection sectionValue;
        private final Map<String, Object> context = new HashMap<>();
        private final Map<Character, BiFunction<Object, String, String>> formatters = new HashMap<>();
        private final List<CodeInterceptor<CodeSection, T>> interceptors = new ArrayList<>();

        private StringBuilder builder;

        /**
         * Inline states are created when formatting text. They aren't written
         * directly to the AbstractCodeWriter, but rather captured as part of the
         * process of expanding a template argument.
         */
        private boolean isInline;

        State() {
            builder = new StringBuilder();
            isRoot = true;
            DEFAULT_FORMATTERS.forEach(this::putFormatter);
        }

        @SuppressWarnings("CopyConstructorMissesField")
        State(State copy) {
            isRoot = false;
            copyStateFrom(copy);
            this.builder = copy.builder;
        }

        // This does not copy context, interceptors, or formatters.
        // State inheritance relies on stacks of States in an AbstractCodeWriter.
        private void copyStateFrom(State copy) {
            this.newline = copy.newline;
            this.expressionStart = copy.expressionStart;
            this.indentText = copy.indentText;
            this.leadingIndentString = copy.leadingIndentString;
            this.indentation = copy.indentation;
            this.newlinePrefix = copy.newlinePrefix;
            this.trimTrailingSpaces = copy.trimTrailingSpaces;
            this.disableNewline = copy.disableNewline;
            this.needsIndentation = copy.needsIndentation;
        }

        @Override
        public String toString() {
            return getBuilder().toString();
        }

        StringBuilder getBuilder() {
            return builder;
        }

        void write(String contents) {
            int position = 0;
            int nextNewline = contents.indexOf(newline);

            while (nextNewline > -1) {
                for (; position < nextNewline; position++) {
                    append(contents.charAt(position));
                }
                writeNewline();
                position += newline.length();
                nextNewline = contents.indexOf(newline, position);
            }

            // Write anything remaining in the string after the last newline.
            for (; position < contents.length(); position++) {
                append(contents.charAt(position));
            }
        }

        private void append(char c) {
            checkIndentationBeforeWriting();
            getBuilder().append(c);
        }

        private void checkIndentationBeforeWriting() {
            if (needsIndentation) {
                getBuilder().append(leadingIndentString).append(newlinePrefix);
                needsIndentation = false;
            }
        }

        private void writeNewline() {
            checkIndentationBeforeWriting();
            // Trim spaces before each newline. This only mutates the builder
            // if space trimming is enabled.
            trimSpaces();
            // Newlines are never split across writes, which could potentially cause
            // indentation logic to mess it up.
            getBuilder().append(newline);
            // The next appended character will get indentation and a
            // leading prefix string.
            needsIndentation = true;
        }

        private void writeLine(String line) {
            write(line);

            if (!disableNewline) {
                writeNewline();
            }
        }

        private void trimSpaces() {
            if (!trimTrailingSpaces) {
                return;
            }

            StringBuilder buffer = getBuilder();
            int toRemove = 0;
            for (int i = buffer.length() - 1; i > 0; i--) {
                if (buffer.charAt(i) == ' ') {
                    toRemove++;
                } else {
                    break;
                }
            }

            if (toRemove > 0) {
                buffer.delete(buffer.length() - toRemove, buffer.length());
            }
        }

        private void indent(int levels, String indentText) {
            // Set to Integer.MIN_VALUE to indent back to root.
            if (levels == Integer.MIN_VALUE) {
                indentation = 0;
            } else if (levels + indentation < 0) {
                throw new IllegalStateException(String.format("Cannot dedent writer %d levels", levels));
            } else {
                indentation += levels;
            }

            if (indentText != null) {
                this.indentText = indentText;
            }

            leadingIndentString = StringUtils.repeat(this.indentText, indentation);
        }

        private void makeSectionInline(StringBuilder builder) {
            this.isInline = true;
            this.builder = builder;
        }

        private void makeInterceptableCodeSection(CodeSection section) {
            currentState.sectionValue = section;
            // If a section value is specified, then capture this state separately.
            // A separate string builder is given to the state, the indentation
            // level is reset back to the root, and the newline prefix is removed.
            // Indentation and prefixes are added automatically if/when the
            // captured text is written into the parent state.
            currentState.builder = new StringBuilder();
            currentState.newlinePrefix = "";
            dedent(-1);
        }

        private String getSectionName() {
            if (isRoot) {
                return "ROOT";
            } else if (sectionValue == null) {
                return "UNNAMED";
            } else {
                return sectionValue.sectionName();
            }
        }

        void putFormatter(Character identifier, BiFunction<Object, String, String> formatFunction) {
            if (Arrays.binarySearch(VALID_FORMATTER_CHARS, identifier) < 0) {
                throw new IllegalArgumentException("Invalid formatter identifier: " + identifier);
            }
            formatters.put(identifier, formatFunction);
        }

        BiFunction<Object, String, String> getFormatter(char identifier) {
            return formatters.get(identifier);
        }

        @SuppressWarnings("unchecked")
        void putInterceptor(CodeInterceptor<? extends CodeSection, T> interceptor) {
            interceptors.add((CodeInterceptor<CodeSection, T>) interceptor);
        }

        /**
         * Gets a list of interceptors that match the given type and for which the
         * result of {@link CodeInterceptor#isIntercepted(CodeSection)} returns true
         * when given {@code forSection}.
         *
         * @param forSection The section that is being intercepted.
         * @param <S> The type of section being intercepted.
         * @return Returns the list of matching interceptors.
         */
        <S extends CodeSection> List<CodeInterceptor<CodeSection, T>> getInterceptors(S forSection) {
            // Add in parent interceptors.
            List<CodeInterceptor<CodeSection, T>> result = new ArrayList<>();
            // Merge in local interceptors.
            for (CodeInterceptor<CodeSection, T> interceptor : interceptors) {
                // Add the interceptor only if it's the right type.
                if (interceptor.sectionType().isInstance(forSection)) {
                    // Only add if the filter passes.
                    if (interceptor.isIntercepted(forSection)) {
                        result.add(interceptor);
                    }
                }
            }

            return result;
        }
    }

    String removeTrailingNewline(String value) {
        if (value.endsWith(currentState.newline)) {
            value = value.substring(0, value.length() - currentState.newline.length());
        }
        return value;
    }

    /**
     * An anonymous section that can never be directly intercepted.
     *
     * <p>This type of section is key to avoiding infinite recursion when an overly
     * greedy CodeInterceptor attempts to intercept all CodeSection instances with
     * no filtering by name.
     */
    private static final class AnonymousCodeSection implements CodeSection {
        private final String sectionName;

        private AnonymousCodeSection(String sectionName) {
            this.sectionName = Objects.requireNonNull(sectionName);
        }

        @Override
        public String sectionName() {
            return sectionName;
        }
    }
}
