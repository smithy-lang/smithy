/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.model;

import java.util.Comparator;
import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

public final class TagObject extends Component implements ToSmithyBuilder<TagObject>, Comparable<TagObject> {
    private static final Comparator<String> STRING_COMPARATOR = Comparator
            .nullsFirst(String::compareToIgnoreCase);

    private static final Comparator<ExternalDocumentation> EXTERNAL_DOCUMENTATION_COMPARATOR = Comparator
            .nullsFirst(ExternalDocumentation::compareTo);

    private final String name;
    private final String description;
    private final ExternalDocumentation externalDocs;

    private TagObject(Builder builder) {
        super(builder);
        this.name = SmithyBuilder.requiredState("name", builder.name);
        this.description = builder.description;
        this.externalDocs = builder.externalDocs;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    public Optional<ExternalDocumentation> getExternalDocs() {
        return Optional.ofNullable(externalDocs);
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .extensions(getExtensions())
                .name(name)
                .description(description)
                .externalDocs(externalDocs);
    }

    @Override
    protected ObjectNode.Builder createNodeBuilder() {
        return Node.objectNodeBuilder()
                .withMember("name", Node.from(getName()))
                .withOptionalMember("description", getDescription().map(Node::from))
                .withOptionalMember("externalDocs", getExternalDocs().map(ExternalDocumentation::toNode));
    }

    @Override
    public int compareTo(TagObject that) {
        return Comparator.comparing(TagObject::getName, STRING_COMPARATOR)
                .thenComparing(to -> to.description, STRING_COMPARATOR)
                .thenComparing(to -> to.externalDocs, EXTERNAL_DOCUMENTATION_COMPARATOR)
                .compare(this, that);
    }

    public static final class Builder extends Component.Builder<Builder, TagObject> {
        private String name;
        private String description;
        private ExternalDocumentation externalDocs;

        private Builder() {}

        @Override
        public TagObject build() {
            return new TagObject(this);
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder externalDocs(ExternalDocumentation externalDocs) {
            this.externalDocs = externalDocs;
            return this;
        }
    }
}
