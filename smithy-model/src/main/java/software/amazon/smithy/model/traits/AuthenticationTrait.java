/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.traits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.SmithyBuilder;
import software.amazon.smithy.model.Tagged;
import software.amazon.smithy.model.ToSmithyBuilder;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Defines the authentication schemes supported by a service.
 */
public final class AuthenticationTrait extends AbstractTrait implements ToSmithyBuilder<AuthenticationTrait> {
    private static final String TRAIT = "smithy.api#authentication";
    private static final List<String> PROPERTIES = List.of("deprecated", "deprecationReason", "settings", "tags");

    private final Map<String, AuthenticationTrait.AuthScheme> authenticationSchemes;

    private AuthenticationTrait(Builder builder) {
        super(TRAIT, builder.sourceLocation);
        this.authenticationSchemes = new LinkedHashMap<>(builder.authenticationSchemes);
    }

    /**
     * @return Returns the authentication schemes
     */
    public Map<String, AuthenticationTrait.AuthScheme> getAuthenticationSchemes() {
        return authenticationSchemes;
    }

    @Override
    protected Node createNode() {
        return authenticationSchemes.entrySet().stream()
                .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, entry -> entry.getValue().toNode()));
    }

    @Override
    public Builder toBuilder() {
        Builder builder = new Builder().sourceLocation(getSourceLocation());
        authenticationSchemes.forEach(builder::putAuthenticationScheme);
        return builder;
    }

    /**
     * @return Returns an authentication trait builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds an authentication trait.
     */
    public static final class Builder extends AbstractTraitBuilder<AuthenticationTrait, Builder> {
        private Map<String, AuthScheme> authenticationSchemes = new LinkedHashMap<>();

        public Builder putAuthenticationScheme(String name, AuthScheme scheme) {
            authenticationSchemes.put(name, Objects.requireNonNull(scheme));
            return this;
        }

        public Builder clearAuthenticationSchemes() {
            authenticationSchemes.clear();
            return this;
        }

        @Override
        public AuthenticationTrait build() {
            return new AuthenticationTrait(this);
        }
    }

    /**
     * Represents an authentication scheme.
     */
    public static final class AuthScheme implements ToSmithyBuilder<AuthScheme>, Tagged, ToNode {
        private final boolean deprecated;
        private final String deprecationReason;
        private final Map<String, String> settings;
        private final List<String> tags;

        AuthScheme(Builder builder) {
            deprecated = builder.deprecated;
            deprecationReason = builder.deprecationReason;
            settings = Map.copyOf(builder.settings);
            tags = List.copyOf(builder.tags);

            if (!deprecated && deprecationReason != null) {
                throw new IllegalStateException("Deprecation reasons may only be provided for deprecated elements");
            }
        }

        public static Builder builder() {
            return new Builder();
        }

        /**
         * @return Whether the element should be considered deprecated.
         */
        public boolean isDeprecated() {
            return deprecated;
        }

        /**
         * @return The reason (if any) for the element's status as deprecated.
         */
        public Optional<String> getDeprecationReason() {
            return Optional.ofNullable(deprecationReason);
        }

        /**
         * Get a specific setting of the scheme.
         *
         * @param key Setting key to retrieve.
         * @return A particular additional setting (if provided).
         */
        public Optional<String> getSetting(String key) {
            return Optional.ofNullable(settings.get(key));
        }

        /**
         * @return The unmodifiable additional settings of the element.
         */
        public Map<String, String> getAllSettings() {
            return settings;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            AuthScheme that = (AuthScheme) o;
            return deprecated == that.deprecated
                    && Objects.equals(deprecationReason, that.deprecationReason)
                    && Objects.equals(settings, that.settings)
                    && Objects.equals(tags, that.tags);
        }

        @Override
        public int hashCode() {
            return Objects.hash(deprecated, deprecationReason, settings, tags);
        }

        @Override
        public Builder toBuilder() {
            Builder builder = builder()
                    .deprecated(deprecated)
                    .deprecationReason(deprecationReason);
            settings.forEach(builder::putSetting);
            tags.forEach(builder::addTag);
            return builder;
        }

        @Override
        public Node toNode() {
            return Node.objectNodeBuilder()
                    .withOptionalMember("deprecated", deprecated ? Optional.of(Node.from(true)) : Optional.empty())
                    .withOptionalMember("deprecationReason", getDeprecationReason().map(Node::from))
                    .withOptionalMember("settings", !settings.isEmpty()
                            ? Optional.of(ObjectNode.fromStringMap(settings))
                            : Optional.empty())
                    .withOptionalMember("tags", !tags.isEmpty()
                            ? Optional.of(ArrayNode.fromStrings(tags))
                            : Optional.empty())
                    .build();
        }

        public static final class Builder implements SmithyBuilder<AuthScheme> {
            private boolean deprecated;
            private String deprecationReason;
            private final Map<String, String> settings = new HashMap<>();
            private final List<String> tags = new ArrayList<>();

            public AuthScheme.Builder deprecated(boolean deprecated) {
                this.deprecated = deprecated;
                return this;
            }

            public AuthScheme.Builder deprecationReason(String deprecationReason) {
                this.deprecationReason = deprecationReason;
                return this;
            }

            public AuthScheme.Builder putSetting(String key, String value) {
                settings.put(key, value);
                return this;
            }

            public AuthScheme.Builder removeSetting(String key) {
                settings.remove(key);
                return this;
            }

            public AuthScheme.Builder clearSettings() {
                settings.clear();
                return this;
            }

            public AuthScheme.Builder addTag(String tag) {
                tags.add(tag);
                return this;
            }

            public AuthScheme.Builder removeTag(String tag) {
                tags.remove(tag);
                return this;
            }

            public AuthScheme.Builder clearTags() {
                tags.clear();
                return this;
            }

            @Override
            public AuthScheme build() {
                return new AuthScheme(this);
            }
        }
    }

    public static final class Provider implements TraitService {
        @Override
        public String getTraitName() {
            return TRAIT;
        }

        @Override
        public AuthenticationTrait createTrait(ShapeId target, Node value) {
            Builder builder = builder().sourceLocation(value);
            value.expectObjectNode().getMembers()
                    .forEach((key, val) -> builder.putAuthenticationScheme(key.getValue(), authSchemeFromNode(val)));
            return builder.build();
        }

        private AuthScheme authSchemeFromNode(Node node) {
            ObjectNode scheme = node.expectObjectNode();
            scheme.warnIfAdditionalProperties(PROPERTIES);
            AuthScheme.Builder builder = AuthScheme.builder();
            scheme.getMember("deprecated")
                    .map(Node::expectBooleanNode)
                    .map(BooleanNode::getValue)
                    .ifPresent(builder::deprecated);
            scheme.getMember("deprecationReason")
                    .map(Node::expectStringNode)
                    .map(StringNode::getValue)
                    .ifPresent(builder::deprecationReason);
            scheme.getMember("settings")
                    .map(Node::expectObjectNode)
                    .ifPresent(settingsNode -> settingsNode.getMembers()
                            .forEach((key, val) -> builder.putSetting(key.getValue(),
                                    val.expectStringNode().getValue())));
            scheme.getMember("tags")
                    .map(Node::expectArrayNode)
                    .ifPresent(tagsNode -> tagsNode.getElements().stream()
                            .map(Node::expectStringNode)
                            .map(StringNode::getValue)
                            .forEach(builder::addTag));
            return builder.build();
        }
    }
}
