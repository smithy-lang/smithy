/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.writer;

import java.util.function.Function;
import software.amazon.smithy.utils.CodeWriter;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A builder used to create a {@code DocumentationWriter} for Java style
 * documentation comments.
 *
 * <p>Documentation comments are automatically sanitized by escaping a
 * closing documentation comment (i.e., star (*) followed by a forward slash
 * (/)). This should also work for JavaScript, PHP, and other languages that
 * use Java-style comments.
 *
 * @deprecated this class uses CodeWriter, which is deprecated.
 */
@Deprecated
@SmithyUnstableApi
public final class JavaStyleDocumentationWriterBuilder {

    private String namedDocumentationSection;
    private Function<String, String> mappingFunction;
    private boolean escapeAtSignWithEntity;

    /**
     * A function used to escape the closing tokens of a documentation comment.
     *
     * @param contents Contents to sanitize.
     * @return Returns the sanitized contents.
     */
    public static String escapeClosingChars(String contents) {
        return contents.replace("*/", "*\\/");
    }

    /**
     * A function used to escape the {@literal @} sign of a documentation
     * comment with an HTML entity of {@literal &#064;}.
     *
     * @param contents Contents to sanitize.
     * @return Returns the sanitized contents.
     */
    public static String escapeAtSignWithEntity(String contents) {
        return contents.replace("@", "&#064;");
    }

    /**
     * Creates a {@code DocumentationWriter} configured by the builder.
     *
     * @param <T> The type of writer to create.
     * @return Returns the created documentation writer.
     */
    public <T extends CodeWriter> DocumentationWriter<T> build() {
        Function<String, String> function = resolveMappingFunction();
        String sectionName = namedDocumentationSection;

        return (writer, runnable) -> {
            if (sectionName != null) {
                writer.pushState(sectionName);
            }

            writer.pushFilteredState(function);
            writer.writeWithNoFormatting("/**");
            writer.setNewlinePrefix(" * ");
            runnable.run();
            writer.ensureNewline();
            writer.popState();
            writer.writeWithNoFormatting(" */");

            if (sectionName != null) {
                writer.popState();
            }
        };
    }

    private Function<String, String> resolveMappingFunction() {
        // Create a default mapping function that escapes closing comment
        // tokens if one was not explicitly configured.
        Function<String, String> function = mappingFunction;

        if (mappingFunction == null) {
            function = JavaStyleDocumentationWriterBuilder::escapeClosingChars;
        }

        // Always compose at-sign escaping with whatever function was resolved.
        if (escapeAtSignWithEntity) {
            function = function.andThen(JavaStyleDocumentationWriterBuilder::escapeAtSignWithEntity);
        }

        return function;
    }

    /**
     * Sets a specific named section to use when writing documentation.
     *
     * @param namedDocumentationSection The name of the state's section to use.
     * @return Returns the builder.
     */
    public JavaStyleDocumentationWriterBuilder namedDocumentationSection(String namedDocumentationSection) {
        this.namedDocumentationSection = namedDocumentationSection;
        return this;
    }

    /**
     * Sets a custom mapping function to use when filtering documentation.
     *
     * <p>Setting a custom mapping function will disable the default mapping
     * function that is used to escape the closing tokens of a block comment.
     * However, other mapping functions will still compose with a custom
     * mapping function if provided (e.g., escaping {@literal @} symbols via
     * {@link #escapeAtSignWithEntity(boolean)} still compose with a custom mapping function).
     *
     * @param mappingFunction Mapping function to use. Set to {@code null} to use the default.
     * @return Returns the builder.
     */
    public JavaStyleDocumentationWriterBuilder mappingFunction(Function<String, String> mappingFunction) {
        this.mappingFunction = mappingFunction;
        return this;
    }

    /**
     * Sets whether or not the "&#064;" sign is escaped with an HTML entity.
     *
     * <p>At signs are not escaped by default.
     *
     * @param escapeAtSignWithEntity Set to true to escape, false to not.
     * @return Returns the builder.
     */
    public JavaStyleDocumentationWriterBuilder escapeAtSignWithEntity(boolean escapeAtSignWithEntity) {
        this.escapeAtSignWithEntity = escapeAtSignWithEntity;
        return this;
    }
}
