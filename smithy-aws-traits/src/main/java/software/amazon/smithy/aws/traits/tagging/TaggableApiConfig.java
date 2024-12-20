/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.tagging;

import java.util.Objects;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Structure representing the configuration of resource specific tagging APIs.
 */
public final class TaggableApiConfig implements FromSourceLocation, ToNode, ToSmithyBuilder<TaggableApiConfig> {
    private final ShapeId tagApi;
    private final ShapeId untagApi;
    private final ShapeId listTagsApi;
    private final SourceLocation sourceLocation;

    private TaggableApiConfig(Builder builder) {
        tagApi = builder.tagApi;
        untagApi = builder.untagApi;
        listTagsApi = builder.listTagsApi;
        sourceLocation = Objects.requireNonNull(builder.sourceLocation);
    }

    /**
     * Creates a builder used to build an TaggableApiConfig.
     *
     * @return Returns the created builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the ShapeId of the operation which implements TagResource behavior.
     *
     * @return the ShapeId of the tag operation for the resource
     */
    public ShapeId getTagApi() {
        return tagApi;
    }

    /**
     * Gets the ShapeId of the operation which implements UntagResource behavior.
     *
     * @return the ShapeId of the untag operation for the resource
     */
    public ShapeId getUntagApi() {
        return untagApi;
    }

    /**
     * Gets the ShapeId of the operation which implements ListTagsForResource
     * behavior for the resource.
     *
     * @return the ShapeId of the list tags operation for the resource
     */
    public ShapeId getListTagsApi() {
        return listTagsApi;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public Builder toBuilder() {
        Builder builder = builder();
        return builder
                .tagApi(tagApi)
                .untagApi(untagApi)
                .listTagsApi(listTagsApi)
                .sourceLocation(sourceLocation);
    }

    @Override
    public Node toNode() {
        return Node.objectNodeBuilder()
                .sourceLocation(getSourceLocation())
                .withMember("tagApi", tagApi.toString())
                .withMember("untagApi", untagApi.toString())
                .withMember("listTagsApi", listTagsApi.toString())
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof TaggableApiConfig)) {
            return false;
        }

        TaggableApiConfig other = (TaggableApiConfig) o;
        return toNode().equals(other.toNode());
    }

    @Override
    public int hashCode() {
        return toNode().hashCode();
    }

    /**
     * Builds a {@link TaggableApiConfig}.
     */
    public static final class Builder implements SmithyBuilder<TaggableApiConfig> {
        private ShapeId tagApi;
        private ShapeId untagApi;
        private ShapeId listTagsApi;
        private SourceLocation sourceLocation = SourceLocation.none();

        Builder tagApi(ShapeId tagApi) {
            this.tagApi = tagApi;
            return this;
        }

        Builder untagApi(ShapeId untagApi) {
            this.untagApi = untagApi;
            return this;
        }

        Builder listTagsApi(ShapeId listTagsApi) {
            this.listTagsApi = listTagsApi;
            return this;
        }

        Builder sourceLocation(SourceLocation sourceLocation) {
            this.sourceLocation = sourceLocation;
            return this;
        }

        @Override
        public TaggableApiConfig build() {
            return new TaggableApiConfig(this);
        }
    }
}
