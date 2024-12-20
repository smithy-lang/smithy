/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Marks a resource shape as taggable for further model validation.
 */
public final class TaggableTrait extends AbstractTrait implements ToSmithyBuilder<TaggableTrait> {
    public static final ShapeId ID = ShapeId.from("aws.api#taggable");
    private final String property;
    private final TaggableApiConfig apiConfig;
    private final boolean disableSystemTags;

    private TaggableTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        property = builder.property;
        apiConfig = builder.apiConfig;
        disableSystemTags = builder.disableSystemTags;
    }

    /**
     * Gets a boolean indicating whether or not the service supports the resource carrying system tags.
     *
     * @return Returns true if the service does not support the resource carrying system tags.
     */
    public boolean getDisableSystemTags() {
        return disableSystemTags;
    }

    /**
     * Gets the TaggableApiConfig if the resource has its own APIs for tagging.
     *
     * @return the TaggableApiConfig for the resource.
     */
    public Optional<TaggableApiConfig> getApiConfig() {
        return Optional.ofNullable(apiConfig);
    }

    /**
     * Gets the name of the property that represents the tags on the resource, if any.
     *
     * @return the name of the property that represents the tags of the resource.
     */
    public Optional<String> getProperty() {
        return Optional.ofNullable(property);
    }

    @Override
    protected Node createNode() {
        return new ObjectNode(MapUtils.of(), getSourceLocation())
                .withOptionalMember("property", getProperty().map(Node::from))
                .withOptionalMember("apiConfig", getApiConfig().map(TaggableApiConfig::toNode))
                .withMember("disableSystemTags", getDisableSystemTags());
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .property(property)
                .apiConfig(apiConfig)
                .disableSystemTags(disableSystemTags)
                .sourceLocation(getSourceLocation());
    }

    public static final class Builder extends AbstractTraitBuilder<TaggableTrait, Builder> {
        private String property;
        private TaggableApiConfig apiConfig;
        private boolean disableSystemTags = false;

        public Builder property(String property) {
            this.property = property;
            return this;
        }

        public Builder apiConfig(TaggableApiConfig apiConfig) {
            this.apiConfig = apiConfig;
            return this;
        }

        public Builder disableSystemTags(Boolean disableSystemTags) {
            this.disableSystemTags = disableSystemTags;
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
            TaggableTrait.Builder builder = TaggableTrait.builder();
            ObjectNode valueObjectNode = value.expectObjectNode();
            if (valueObjectNode.containsMember("property")) {
                builder.property(valueObjectNode.expectStringMember("property").getValue());
            }
            if (valueObjectNode.containsMember("disableSystemTags")) {
                builder.disableSystemTags(valueObjectNode.expectBooleanMember("disableSystemTags").getValue());
            }
            if (valueObjectNode.containsMember("apiConfig")) {
                TaggableApiConfig.Builder apiConfigBuilder = TaggableApiConfig.builder();
                apiConfigBuilder.tagApi(ShapeId.from(valueObjectNode.expectObjectMember("apiConfig")
                        .expectStringMember("tagApi")
                        .getValue()));
                apiConfigBuilder.untagApi(ShapeId.from(valueObjectNode.expectObjectMember("apiConfig")
                        .expectStringMember("untagApi")
                        .getValue()));
                apiConfigBuilder.listTagsApi(ShapeId.from(valueObjectNode.expectObjectMember("apiConfig")
                        .expectStringMember("listTagsApi")
                        .getValue()));
                builder.apiConfig(apiConfigBuilder.build());
            }
            TaggableTrait result = builder.build();
            result.setNodeCache(value);
            return result;
        }
    }
}
