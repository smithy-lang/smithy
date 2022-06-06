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
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.TraitService;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 *
 */
@SmithyUnstableApi
public final class TaggableTrait extends AbstractTrait implements ToSmithyBuilder<TaggableTrait> {
    public static final ShapeId ID = ShapeId.from("aws.api#taggable");
    private final String property;
    private final String tagApi;
    private final String untagApi;
    private final String listTagsApi;
    private final Boolean supportsSystemTags;

    private TaggableTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        property = builder.property;
        tagApi = builder.tagApi;
        untagApi = builder.untagApi;
        listTagsApi = builder.listTagsApi;
        supportsSystemTags = builder.supportsSystemTags;
    }

    public Optional<Boolean> getSupportsSystemTags() {
        return Optional.ofNullable(supportsSystemTags);
    }

    public Optional<String> getListTagsApi() {
        return Optional.ofNullable(listTagsApi);
    }

    public Optional<String> getUntagApi() {
        return Optional.ofNullable(untagApi);
    }

    public Optional<String> getTagApi() {
        return Optional.ofNullable(tagApi);
    }

    public Optional<String> getProperty() {
        return Optional.ofNullable(property);
    }

    public Boolean resolveSupportsSystemTags() {
        return supportsSystemTags != null ? supportsSystemTags : Boolean.TRUE;
    }

    public String resolveListTagsApi() {
        return listTagsApi != null ? listTagsApi : "ListTagsForResource";
    }

    public String resolveUntagApi() {
        return untagApi != null ? untagApi : "UntagResource";
    }

    public String resolveTagApi() {
        return tagApi != null ? tagApi : "TagResource";
    }

    @Override
    protected Node createNode() {
        return new ObjectNode(MapUtils.of(), getSourceLocation())
                .withOptionalMember("property", getProperty().map(Node::from))
                .withOptionalMember("tagApi", getTagApi().map(Node::from))
                .withOptionalMember("untagApi", getUntagApi().map(Node::from))
                .withOptionalMember("listTagsApi", getListTagsApi().map(Node::from))
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
        private String tagApi;
        private String untagApi;
        private String listTagsApi;
        private Boolean supportsSystemTags;

        public Builder property(String property) {
            this.property = property;
            return this;
        }

        public Builder tagApi(String tagApi) {
            this.tagApi = tagApi;
            return this;
        }

        public Builder untagApi(String untagApi) {
            this.untagApi = untagApi;
            return this;
        }

        public Builder listTagsApi(String listTagsApi) {
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
            ObjectNode objectNode = value.expectObjectNode();
            String property = objectNode.getStringMemberOrDefault("property", null);
            String tagApi = objectNode.getStringMemberOrDefault("tagApi", null);
            String untagApi = objectNode.getStringMemberOrDefault("untagApi", null);
            String listTagsApi = objectNode.getStringMemberOrDefault("listTagsApi", null);
            Boolean supportsSystemTags = objectNode.getBooleanMemberOrDefault("supportsSystemTags", null);
            TaggableTrait result = builder()
                    .property(property)
                    .tagApi(tagApi)
                    .untagApi(untagApi)
                    .listTagsApi(listTagsApi)
                    .supportsSystemTags(supportsSystemTags)
                    .sourceLocation(value)
                    .build();
            result.setNodeCache(value);
            return result;
        }
    }
}
