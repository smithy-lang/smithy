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
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Trait annotating a service shape as having taggable resources. Should also contain consistent tagging operations.
 */
public final class TagEnabledTrait extends AbstractTrait implements ToSmithyBuilder<TagEnabledTrait> {
    public static final ShapeId ID = ShapeId.from("aws.api#tagEnabled");

    private final boolean disableDefaultOperations;
    private final TaggableServiceApiConfig apiConfig;

    public TagEnabledTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        disableDefaultOperations = builder.disableDefaultOperations;
        apiConfig = builder.apiConfig;
    }

    @Override
    protected Node createNode() {
        ObjectNode.Builder builder = ObjectNode.builder()
                .sourceLocation(getSourceLocation());
        if (disableDefaultOperations) {
            builder.withMember("disableDefaultOperations", true);
        }
        builder.withOptionalMember("apiConfig", getApiConfig().map(TaggableServiceApiConfig::toNode));
        return builder.build();
    }

    public boolean getDisableDefaultOperations() {
        return disableDefaultOperations;
    }

    /**
     * Gets the TaggableServiceApiConfig if the service overrides the default tagging operation names.
     *
     * @return the TaggableServiceApiConfig for the service.
     */
    public Optional<TaggableServiceApiConfig> getApiConfig() {
        return Optional.ofNullable(apiConfig);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .disableDefaultOperations(disableDefaultOperations)
                .apiConfig(apiConfig);
    }

    public static final class Builder extends AbstractTraitBuilder<TagEnabledTrait, Builder> {
        private Boolean disableDefaultOperations = false;
        private TaggableServiceApiConfig apiConfig;

        public Builder disableDefaultOperations(Boolean disableDefaultOperations) {
            this.disableDefaultOperations = disableDefaultOperations;
            return this;
        }

        public Builder apiConfig(TaggableServiceApiConfig apiConfig) {
            this.apiConfig = apiConfig;
            return this;
        }

        @Override
        public TagEnabledTrait build() {
            return new TagEnabledTrait(this);
        }
    }

    public static final class Provider implements TraitService {
        @Override
        public ShapeId getShapeId() {
            return ID;
        }

        @Override
        public TagEnabledTrait createTrait(ShapeId target, Node value) {
            ObjectNode objectNode = value.expectObjectNode();
            Builder builder = builder().sourceLocation(value);
            builder.disableDefaultOperations(
                    objectNode.getBooleanMemberOrDefault("disableDefaultOperations", false));
            objectNode.getObjectMember("apiConfig").ifPresent(apiNode -> {
                TaggableServiceApiConfig.Builder cfg = TaggableServiceApiConfig.builder()
                        .sourceLocation(apiNode.getSourceLocation());
                apiNode.getStringMember("tagApi")
                        .ifPresent(s -> cfg.tagApi(ShapeId.from(s.getValue())));
                apiNode.getStringMember("untagApi")
                        .ifPresent(s -> cfg.untagApi(ShapeId.from(s.getValue())));
                apiNode.getStringMember("listTagsApi")
                        .ifPresent(s -> cfg.listTagsApi(ShapeId.from(s.getValue())));
                builder.apiConfig(cfg.build());
            });
            TagEnabledTrait result = builder.build();
            result.setNodeCache(value);
            return result;
        }
    }
}
