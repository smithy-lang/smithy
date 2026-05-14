/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.tagging;

import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Structure representing the configuration of service-wide tagging APIs when non-default operation names are used.
 * All members are optional; an unset member implies the default-named operation
 * (TagResource, UntagResource, ListTagsForResource respectively) is used.
 */
public final class TaggableServiceApiConfig
        implements FromSourceLocation, ToNode, ToSmithyBuilder<TaggableServiceApiConfig> {
    private final ShapeId tagApi;
    private final ShapeId untagApi;
    private final ShapeId listTagsApi;
    private final SourceLocation sourceLocation;

    private TaggableServiceApiConfig(Builder builder) {
        tagApi = builder.tagApi;
        untagApi = builder.untagApi;
        listTagsApi = builder.listTagsApi;
        sourceLocation = Objects.requireNonNull(builder.sourceLocation);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the ShapeId of the operation that implements service-wide TagResource behavior, if specified.
     *
     * @return Optional ShapeId of the configured tag operation.
     */
    public Optional<ShapeId> getTagApi() {
        return Optional.ofNullable(tagApi);
    }

    /**
     * Gets the ShapeId of the operation that implements service-wide UntagResource behavior, if specified.
     *
     * @return Optional ShapeId of the configured untag operation.
     */
    public Optional<ShapeId> getUntagApi() {
        return Optional.ofNullable(untagApi);
    }

    /**
     * Gets the ShapeId of the operation that implements service-wide ListTagsForResource behavior, if specified.
     *
     * @return Optional ShapeId of the configured list tags operation.
     */
    public Optional<ShapeId> getListTagsApi() {
        return Optional.ofNullable(listTagsApi);
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .tagApi(tagApi)
                .untagApi(untagApi)
                .listTagsApi(listTagsApi)
                .sourceLocation(sourceLocation);
    }

    @Override
    public Node toNode() {
        return Node.objectNodeBuilder()
                .sourceLocation(getSourceLocation())
                .withOptionalMember("tagApi", getTagApi().map(id -> Node.from(id.toString())))
                .withOptionalMember("untagApi", getUntagApi().map(id -> Node.from(id.toString())))
                .withOptionalMember("listTagsApi", getListTagsApi().map(id -> Node.from(id.toString())))
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof TaggableServiceApiConfig)) {
            return false;
        }
        TaggableServiceApiConfig other = (TaggableServiceApiConfig) o;
        return toNode().equals(other.toNode());
    }

    @Override
    public int hashCode() {
        return toNode().hashCode();
    }

    public static final class Builder implements SmithyBuilder<TaggableServiceApiConfig> {
        private ShapeId tagApi;
        private ShapeId untagApi;
        private ShapeId listTagsApi;
        private SourceLocation sourceLocation = SourceLocation.none();

        public Builder tagApi(ShapeId tagApi) {
            this.tagApi = tagApi;
            return this;
        }

        public Builder untagApi(ShapeId untagApi) {
            this.untagApi = untagApi;
            return this;
        }

        public Builder listTagsApi(ShapeId listTagsApi) {
            this.listTagsApi = listTagsApi;
            return this;
        }

        public Builder sourceLocation(SourceLocation sourceLocation) {
            this.sourceLocation = sourceLocation;
            return this;
        }

        @Override
        public TaggableServiceApiConfig build() {
            return new TaggableServiceApiConfig(this);
        }
    }
}
