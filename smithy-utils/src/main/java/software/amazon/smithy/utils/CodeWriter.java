/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @deprecated prefer {@link SimpleCodeWriter} or a custom subclass of {@link AbstractCodeWriter}.
 */
@Deprecated
public class CodeWriter extends AbstractCodeWriter<CodeWriter> {

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
     * @see AbstractCodeWriter#onSection(CodeInterceptor) as an alternative
     *      that allows more explicit whitespace handling.
     */
    @Deprecated
    public CodeWriter onSectionPrepend(String sectionName, Runnable writeBefore) {
        return onSection(sectionName, contents -> {
            writeBefore.run();
            writeWithNoFormatting(contents);
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
     * @see AbstractCodeWriter#onSection(CodeInterceptor) as an alternative
    *          that allows more explicit whitespace handling.
     */
    @Deprecated
    public CodeWriter onSectionAppend(String sectionName, Runnable writeAfter) {
        return onSection(sectionName, contents -> {
            writeWithNoFormatting(contents);
            writeAfter.run();
        });
    }

    /**
     * @see AbstractCodeWriter#copySettingsFrom(AbstractCodeWriter)
     */
    public void copySettingsFrom(CodeWriter other) {
        super.copySettingsFrom(other);
    }

    @Override
    public CodeWriter putFormatter(char identifier, BiFunction<Object, String, String> formatFunction) {
        super.putFormatter(identifier, formatFunction);
        return this;
    }

    @Override
    public CodeWriter setExpressionStart(char expressionStart) {
        super.setExpressionStart(expressionStart);
        return this;
    }

    @Override
    public CodeWriter pushState() {
        super.pushState();
        return this;
    }

    @Override
    public CodeWriter pushState(String sectionName) {
        super.pushState(sectionName);
        return this;
    }

    @Override
    public CodeWriter pushFilteredState(Function<String, String> filter) {
        super.pushFilteredState(filter);
        return this;
    }

    @Override
    public CodeWriter popState() {
        super.popState();
        return this;
    }

    @Override
    public CodeWriter onSection(String sectionName, Consumer<Object> interceptor) {
        super.onSection(sectionName, interceptor);
        return this;
    }

    @Override
    public CodeWriter disableNewlines() {
        super.disableNewlines();
        return this;
    }

    @Override
    public CodeWriter enableNewlines() {
        super.enableNewlines();
        return this;
    }

    @Override
    public CodeWriter setNewline(String newline) {
        super.setNewline(newline);
        return this;
    }

    @Override
    public CodeWriter setNewline(char newline) {
        super.setNewline(newline);
        return this;
    }

    @Override
    public CodeWriter setIndentText(String indentText) {
        super.setIndentText(indentText);
        return this;
    }

    @Override
    public CodeWriter trimTrailingSpaces() {
        super.trimTrailingSpaces();
        return this;
    }

    @Override
    public CodeWriter trimTrailingSpaces(boolean trimTrailingSpaces) {
        super.trimTrailingSpaces(trimTrailingSpaces);
        return this;
    }

    @Override
    public CodeWriter trimBlankLines() {
        super.trimBlankLines();
        return this;
    }

    @Override
    public CodeWriter trimBlankLines(int trimBlankLines) {
        super.trimBlankLines(trimBlankLines);
        return this;
    }

    @Override
    public CodeWriter insertTrailingNewline() {
        super.insertTrailingNewline();
        return this;
    }

    @Override
    public CodeWriter insertTrailingNewline(boolean trailingNewline) {
        super.insertTrailingNewline(trailingNewline);
        return this;
    }

    @Override
    public CodeWriter setNewlinePrefix(String newlinePrefix) {
        super.setNewlinePrefix(newlinePrefix);
        return this;
    }

    @Override
    public CodeWriter indent() {
        super.indent();
        return this;
    }

    @Override
    public CodeWriter indent(int levels) {
        super.indent(levels);
        return this;
    }

    @Override
    public CodeWriter dedent() {
        super.dedent();
        return this;
    }

    @Override
    public CodeWriter dedent(int levels) {
        super.dedent(levels);
        return this;
    }

    @Override
    public CodeWriter openBlock(String textBeforeNewline, Object... args) {
        super.openBlock(textBeforeNewline, args);
        return this;
    }

    @Override
    public CodeWriter openBlock(String textBeforeNewline, String textAfterNewline, Runnable f) {
        super.openBlock(textBeforeNewline, textAfterNewline, f);
        return this;
    }

    @Override
    public CodeWriter openBlock(String textBeforeNewline, String textAfterNewline, Object arg1, Runnable f) {
        super.openBlock(textBeforeNewline, textAfterNewline, arg1, f);
        return this;
    }

    @Override
    public CodeWriter openBlock(
            String textBeforeNewline,
            String textAfterNewline,
            Object arg1,
            Object arg2,
            Runnable f
    ) {
        super.openBlock(textBeforeNewline, textAfterNewline, arg1, arg2, f);
        return this;
    }

    @Override
    public CodeWriter openBlock(
            String textBeforeNewline,
            String textAfterNewline,
            Object arg1,
            Object arg2,
            Object arg3,
            Runnable f
    ) {
        super.openBlock(textBeforeNewline, textAfterNewline, arg1, arg2, arg3, f);
        return this;
    }

    @Override
    public CodeWriter openBlock(
            String textBeforeNewline,
            String textAfterNewline,
            Object arg1,
            Object arg2,
            Object arg3,
            Object arg4,
            Runnable f
    ) {
        super.openBlock(textBeforeNewline, textAfterNewline, arg1, arg2, arg3, arg4, f);
        return this;
    }

    @Override
    public CodeWriter openBlock(
            String textBeforeNewline,
            String textAfterNewline,
            Object arg1,
            Object arg2,
            Object arg3,
            Object arg4,
            Object arg5,
            Runnable f
    ) {
        super.openBlock(textBeforeNewline, textAfterNewline, arg1, arg2, arg3, arg4, arg5, f);
        return this;
    }

    @Override
    public CodeWriter openBlock(String textBeforeNewline, String textAfterNewline, Object[] args, Runnable f) {
        super.openBlock(textBeforeNewline, textAfterNewline, args, f);
        return this;
    }

    @Override
    public CodeWriter closeBlock(String textAfterNewline, Object... args) {
        super.closeBlock(textAfterNewline, args);
        return this;
    }

    @Override
    public CodeWriter writeWithNoFormatting(Object content) {
        super.writeWithNoFormatting(content);
        return this;
    }

    @Override
    public CodeWriter call(Runnable task) {
        task.run();
        return this;
    }

    @Override
    public CodeWriter write(Object content, Object... args) {
        super.write(content, args);
        return this;
    }

    @Override
    public CodeWriter writeInline(Object content, Object... args) {
        super.writeInline(content, args);
        return this;
    }

    @Override
    public CodeWriter ensureNewline() {
        super.ensureNewline();
        return this;
    }

    @Override
    public CodeWriter writeOptional(Object content) {
        super.writeOptional(content);
        return this;
    }

    @Override
    public CodeWriter unwrite(Object content, Object... args) {
        super.unwrite(content, args);
        return this;
    }

    @Override
    public CodeWriter putContext(String key, Object value) {
        super.putContext(key, value);
        return this;
    }

    @Override
    public CodeWriter putContext(Map<String, Object> mappings) {
        super.putContext(mappings);
        return this;
    }

    @Override
    public CodeWriter removeContext(String key) {
        super.removeContext(key);
        return this;
    }
}
