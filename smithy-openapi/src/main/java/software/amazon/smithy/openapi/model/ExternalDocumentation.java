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

public final class ExternalDocumentation extends Component
        implements ToSmithyBuilder<ExternalDocumentation>, Comparable<ExternalDocumentation> {

    private static final Comparator<String> STRING_COMPARATOR = Comparator
            .nullsFirst(String::compareTo);

    private final String description;
    private final String url;

    private ExternalDocumentation(Builder builder) {
        super(builder);
        this.url = SmithyBuilder.requiredState("url", builder.url);
        this.description = builder.description;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public String getUrl() {
        return url;
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .extensions(getExtensions())
                .description(description)
                .url(url);
    }

    @Override
    protected ObjectNode.Builder createNodeBuilder() {
        return Node.objectNodeBuilder()
                .withOptionalMember("description", getDescription().map(Node::from))
                .withMember("url", url);
    }

    @Override
    public int compareTo(ExternalDocumentation that) {
        return Comparator.comparing(ExternalDocumentation::getUrl, STRING_COMPARATOR)
                .thenComparing(ed -> ed.description, STRING_COMPARATOR)
                .compare(this, that);
    }

    public static final class Builder extends Component.Builder<Builder, ExternalDocumentation> {
        private String description;
        private String url;

        private Builder() {}

        @Override
        public ExternalDocumentation build() {
            return new ExternalDocumentation(this);
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }
    }
}
