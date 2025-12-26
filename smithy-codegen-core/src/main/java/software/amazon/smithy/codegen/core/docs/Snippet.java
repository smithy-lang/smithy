/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.docs;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.StringUtils;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Represents a generated snippet, potentially containing multiple logical files.
 *
 * <p>Snippets are generated code based on some trait or other shared definition,
 * such as the {@link software.amazon.smithy.model.traits.ExamplesTrait}. These are
 * created by code generators and consumed by documentation tools, such as
 * smithy-docgen.
 */
@SmithyUnstableApi
public final class Snippet implements ToSmithyBuilder<Snippet> {
    private final String targetId;
    private final String title;
    private final ShapeId protocol;
    private final List<SnippetFile> files;

    private Snippet(Builder builder) {
        this.targetId = SmithyBuilder.requiredState("targetId", builder.targetId);
        if (StringUtils.isBlank(targetId)) {
            throw new IllegalStateException("Snippet target id must not be blank");
        }
        this.title = SmithyBuilder.requiredState("title", builder.title);
        if (StringUtils.isBlank(title)) {
            throw new IllegalStateException("Snippet title must not be blank");
        }
        this.protocol = builder.protocol;
        this.files = builder.files.copy();
        if (this.files.isEmpty()) {
            throw new IllegalStateException("Snippets must contain at least one file");
        }
    }

    /**
     * Gets the identifier of what the snippet was generated for.
     *
     * <p>If this snippet represents a generated example from the
     * {@link software.amazon.smithy.model.traits.ExamplesTrait}, this
     * will be the title of the example.
     *
     * @return Returns the target identifier of the snippet.
     */
    public String getTargetId() {
        return targetId;
    }

    /**
     * Gets the title of the snippet.
     *
     * <p>This is distinct from the target ID as it identifies the specific snippet.
     *
     * <p>Generally, the title should reflect the primary language of the snippet,
     * such as "Python" for a Python snippet.
     *
     * @return Returns the title of the snippet.
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return Optionally returns the ShapeId of the protocol associated with this snippet.
     */
    public Optional<ShapeId> getProtocol() {
        return Optional.ofNullable(protocol);
    }

    /**
     * @return Returns the files that comprise the snippet.
     */
    public List<SnippetFile> getFiles() {
        return files;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Snippet)) {
            return false;
        }
        Snippet snippet = (Snippet) o;
        return Objects.equals(targetId, snippet.targetId)
                && Objects.equals(protocol, snippet.protocol)
                && Objects.equals(files, snippet.files);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetId, protocol, files);
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .targetId(targetId)
                .protocol(protocol)
                .files(files);
    }

    /**
     * @return Returns a new Snippet builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder implements SmithyBuilder<Snippet> {
        private String targetId;
        private String title;
        private ShapeId protocol;
        private final BuilderRef<List<SnippetFile>> files = BuilderRef.forList();

        @Override
        public Snippet build() {
            return new Snippet(this);
        }

        /**
         * Sets the target id of the snippet.
         *
         * @param id The id to set as the target of the snippet.
         * @return Returns the builder.
         */
        public Builder targetId(String id) {
            this.targetId = id;
            return this;
        }

        /**
         * Sets the title of the snippet.
         *
         * @param title The title to set for the snippet.
         * @return Returns the builder.
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * Sets the protocol of the snippet.
         *
         * <p>To remove the protocol, set a null value.
         *
         * @param protocol The shape id of the protocol the snippet is tied to.
         * @return Returns the builder.
         */
        public Builder protocol(ShapeId protocol) {
            this.protocol = protocol;
            return this;
        }

        /**
         * Sets the files that make up the snippet.
         *
         * @param files A list of files that make up the snippet.
         * @return Returns the builder.
         */
        public Builder files(Collection<SnippetFile> files) {
            this.files.clear();
            this.files.get().addAll(files);
            return this;
        }

        /**
         * Adds a file to the snippet.
         *
         * @param file A file to add to the snippet.
         * @return Returns the builder.
         */
        public Builder addFile(SnippetFile file) {
            this.files.get().add(file);
            return this;
        }
    }
}
