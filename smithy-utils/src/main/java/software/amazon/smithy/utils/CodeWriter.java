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

/**
 * Helper class for generating code.
 *
 * <p>This class is a general purpose implementation of {@link AbstractCodeWriter}.
 */
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
     * @see #onSection(CodeInterceptor) as an alternative that allows more explicit whitespace handling.
     */
    @Deprecated
    public final CodeWriter onSectionPrepend(String sectionName, Runnable writeBefore) {
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
     * @see #onSection(CodeInterceptor) as an alternative that allows more explicit whitespace handling.
     */
    @Deprecated
    public final CodeWriter onSectionAppend(String sectionName, Runnable writeAfter) {
        return onSection(sectionName, contents -> {
            writeWithNoFormatting(contents);
            writeAfter.run();
        });
    }
}
