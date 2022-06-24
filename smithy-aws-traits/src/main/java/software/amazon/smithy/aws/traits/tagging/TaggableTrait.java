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

package software.amazon.smithy.aws.traits.tagging;

import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.TraitService;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Marks a resource shape as taggable for further model validation.
 */
public final class TaggableTrait extends AbstractTrait implements ToSmithyBuilder<TaggableTrait> {
    public static final ShapeId ID = ShapeId.from("aws.api#taggable");
    private final String property;
    private final ShapeId tagApi;
    private final ShapeId untagApi;
    private final ShapeId listTagsApi;
    private final Boolean supportsSystemTags;

    private TaggableTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        property = builder.property;
        tagApi = builder.tagApi;
        untagApi = builder.untagApi;
        listTagsApi = builder.listTagsApi;
        supportsSystemTags = builder.supportsSystemTags;
    }

    /**
     * Gets a boolean indicating whether or not the service supports the resource carrying system tags.
     *
     * @return Returns true if the service supports the resource carrying system tags.
     *
     */
    public Optional<Boolean> getSupportsSystemTags() {
        return Optional.ofNullable(supportsSystemTags);
    }

    /**
     * Gets the operation referenced that satisfies the ListTagsForResource behavior.
     *
     * @return Return the ShapeId of the operation specified for the ListTags API.
     */
    public Optional<ShapeId> getListTagsApi() {
        return Optional.ofNullable(listTagsApi);
    }

    /**
     * Gets the operation referenced that satisfies the UntagResource behavior.
     *
     * @return Return the ShapeId of the operation specified for the Untag API.
     */
    public Optional<ShapeId> getUntagApi() {
        return Optional.ofNullable(untagApi);
    }

    /**
     * Gets the operation referenced that satisfies the TagResource behavior.
     *
     * @return Return the ShapeId of the operation specified for the Tag API.
     */
    public Optional<ShapeId> getTagApi() {
        return Optional.ofNullable(tagApi);
    }

    /**
     * Gets the rame of the property that represents the tags on the resource, if any.
     *
     * @return Return the name of the property that represents the tags of the resource.
     */
    public Optional<String> getProperty() {
        return Optional.ofNullable(property);
    }

    @Override
    protected Node createNode() {
        return new ObjectNode(MapUtils.of(), getSourceLocation())
                .withOptionalMember("property", getProperty().map(Node::from))
                .withOptionalMember("tagApi", getTagApi().map(id -> Node.from(id.toString())))
                .withOptionalMember("untagApi", getUntagApi().map(id -> Node.from(id.toString())))
                .withOptionalMember("listTagsApi", getListTagsApi().map(id -> Node.from(id.toString())))
                .withOptionalMember("supportsSystemTags", getSupportsSystemTags().map(Node::from));
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public SmithyBuilder<TaggableTrait> toBuilder() {
        return builder()
                .property(property)
                .tagApi(tagApi)
                .untagApi(untagApi)
                .listTagsApi(listTagsApi)
                .supportsSystemTags(supportsSystemTags)
                .sourceLocation(getSourceLocation());
    }

    public static final class Builder extends AbstractTraitBuilder<TaggableTrait, Builder> {
        private String property;
        private ShapeId tagApi;
        private ShapeId untagApi;
        private ShapeId listTagsApi;
        private Boolean supportsSystemTags;

        public Builder property(String property) {
            this.property = property;
            return this;
        }

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

        public Builder supportsSystemTags(Boolean supportsSystemTags) {
            this.supportsSystemTags = supportsSystemTags;
            return this;
        }

        @Override
        public TaggableTrait build() {
            return new TaggableTrait(this);
        }
    }

    public static final class Provider implements TraitService {
        @Override
        public ShapeId getShapeId() {
            return ID;
        }

        @Override
        public TaggableTrait createTrait(ShapeId target, Node value) {
            NodeMapper nodeMapper = new NodeMapper();
            TaggableTrait result = nodeMapper.deserialize(value, TaggableTrait.class);
            result.setNodeCache(value);
            return result;
        }
    }
}
