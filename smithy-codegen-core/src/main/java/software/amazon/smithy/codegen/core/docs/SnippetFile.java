/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.docs;

import java.util.Objects;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.StringUtils;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Represents a single file that is part of a snippet. This could be a config file, a source code file,
 * or some logically differentiated file type.
 */
@SmithyUnstableApi
public class SnippetFile implements ToSmithyBuilder<SnippetFile> {
    private final String language;
    private final String filename;
    private final String content;

    private SnippetFile(Builder builder) {
        this.language = SmithyBuilder.requiredState("language", builder.language);
        if (StringUtils.isBlank(language)) {
            throw new IllegalStateException("SnippetFile language must not be blank");
        }
        this.filename = SmithyBuilder.requiredState("filename", builder.filename);
        if (StringUtils.isBlank(filename)) {
            throw new IllegalStateException("SnippetFile filename must not be blank");
        }
        this.content = SmithyBuilder.requiredState("content", builder.content);
    }

    /**
     * Gets the language used by the file.
     *
     * <p>This will be used for syntax highlighting.
     *
     * @return Returns a string representation of the file's language.
     */
    public String getLanguage() {
        return language;
    }

    /**
     * @return Returns the name of the file.
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @return Returns the content of the file.
     */
    public String getContent() {
        return content;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SnippetFile)) {
            return false;
        }
        SnippetFile that = (SnippetFile) o;
        return Objects.equals(language, that.language)
                && Objects.equals(filename, that.filename)
                && Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(language, filename, content);
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .language(language)
                .filename(filename)
                .content(content);
    }

    /**
     * @return Returns a new SnippetFile builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder implements SmithyBuilder<SnippetFile> {
        private String language;
        private String filename;
        private String content;

        @Override
        public SnippetFile build() {
            return new SnippetFile(this);
        }

        /**
         * Sets the language of the snippet file.
         *
         * @param language The language of the snippet file.
         * @return Returns the builder.
         */
        public Builder language(String language) {
            this.language = language;
            return this;
        }

        /**
         * Sets the name of the snippet file.
         *
         * @param filename The file name of the snippet.
         * @return Returns the builder.
         */
        public Builder filename(String filename) {
            this.filename = filename;
            return this;
        }

        /**
         * Sets the content of the snippet file.
         *
         * @param content The snippet file contents.
         * @return Returns the builder.
         */
        public Builder content(String content) {
            this.content = content;
            return this;
        }
    }
}
