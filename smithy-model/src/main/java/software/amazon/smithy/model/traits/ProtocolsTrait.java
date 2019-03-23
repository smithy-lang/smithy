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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.SmithyBuilder;
import software.amazon.smithy.model.Tagged;
import software.amazon.smithy.model.ToSmithyBuilder;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;

/**
 * Defines the protocols supported by a service.
 */
public final class ProtocolsTrait extends AbstractTrait implements ToSmithyBuilder<ProtocolsTrait> {
    private static final String TRAIT = "smithy.api#protocols";
    private static final String AUTHENTICATION = "authentication";
    private static final String DEPRECATED = "deprecated";
    private static final String DEPRECATION_REASON = "deprecationReason";
    private static final String Q = "q";
    private static final String SETTINGS = "settings";
    private static final String TAGS = "tags";
    private static final List<String> PROPERTIES = List.of(
            AUTHENTICATION, DEPRECATED, DEPRECATION_REASON, Q, SETTINGS, TAGS);

    private final Map<String, Protocol> protocols;

    private ProtocolsTrait(Builder builder) {
        super(TRAIT, builder.sourceLocation);
        this.protocols = createProtocolMap(builder.protocols.entrySet());
    }

    public static TraitService provider() {
        return TraitService.createProvider(TRAIT, (target, value) -> {
            Builder builder = builder().sourceLocation(value);

            for (var entry : value.expectObjectNode().getMembers().entrySet()) {
                var key = entry.getKey().getValue();
                var protocol = entry.getValue().expectObjectNode();
                protocol.warnIfAdditionalProperties(PROPERTIES);
                Protocol.Builder protocolBuilder = Protocol.builder();
                protocol.getMember(Q)
                        .map(Node::expectNumberNode)
                        .map(NumberNode::getValue)
                        .map(Number::doubleValue)
                        .ifPresent(protocolBuilder::weight);
                protocol.getMember(DEPRECATED)
                        .map(Node::expectBooleanNode)
                        .map(BooleanNode::getValue)
                        .ifPresent(protocolBuilder::deprecated);
                protocol.getMember(DEPRECATION_REASON)
                        .map(Node::expectStringNode)
                        .map(StringNode::getValue)
                        .ifPresent(protocolBuilder::deprecationReason);
                protocol.getMember(SETTINGS).map(Node::expectObjectNode).ifPresent(settingsNode -> {
                    for (var e : settingsNode.getMembers().entrySet()) {
                        protocolBuilder.putSetting(e.getKey().getValue(), e.getValue().expectStringNode().getValue());
                    }
                });
                protocol.getMember(TAGS).map(Node::expectArrayNode).ifPresent(tagsNode -> {
                    tagsNode.getElements().stream()
                            .map(Node::expectStringNode)
                            .map(StringNode::getValue)
                            .forEach(protocolBuilder::addTag);
                });
                protocol.getArrayMember(AUTHENTICATION).ifPresent(auth -> {
                    Node.loadArrayOfString(AUTHENTICATION, auth).forEach(protocolBuilder::addAuthentication);
                });
                builder.putProtocol(key, protocolBuilder.build());
            }

            return builder.build();
        });
    }

    /**
     * Returns a map of protocol names to protocols.
     *
     * <p>Iteration over the map will provide protocols in priority order.
     *
     * @return Returns the protocols
     */
    public Map<String, Protocol> getProtocols() {
        return protocols;
    }

    @Override
    protected Node createNode() {
        return protocols.entrySet().stream()
                .collect(ObjectNode.collectStringKeys(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Builder toBuilder() {
        Builder builder = builder();
        protocols.forEach(builder::putProtocol);
        return builder;
    }

    /**
     * @return Returns an protocols trait builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds the protocols trait.
     */
    public static final class Builder extends AbstractTraitBuilder<ProtocolsTrait, Builder> {
        private Map<String, Protocol> protocols = new HashMap<>();

        private Builder() {}

        @Override
        public ProtocolsTrait build() {
            return new ProtocolsTrait(this);
        }

        public Builder putProtocol(String name, Protocol protocol) {
            protocols.put(name, Objects.requireNonNull(protocol));
            return this;
        }

        public Builder clearProtocols() {
            protocols.clear();
            return this;
        }
    }

    /**
     * Represents a communication protocol.
     */
    public static final class Protocol implements ToNode, ToSmithyBuilder<Protocol>, Tagged {
        private List<String> authentication;
        private final boolean deprecated;
        private final String deprecationReason;
        private final Map<String, String> settings;
        private final List<String> tags;
        private final double weight;
        private Node node;

        Protocol(Builder builder) {
            authentication = List.copyOf(builder.authentication);
            deprecated = builder.deprecated;
            deprecationReason = builder.deprecationReason;
            settings = Map.copyOf(builder.settings);
            tags = List.copyOf(builder.tags);
            weight = builder.weight;

            if (!deprecated && deprecationReason != null) {
                throw new IllegalStateException("Deprecation reasons may only be provided for deprecated elements");
            }

            if (weight < 0 || 1.0 < weight) {
                throw new IllegalStateException(String.format("Weight must be between 0 and 1.0; %f received", weight));
            }
        }

        public static Builder builder() {
            return new Builder();
        }

        public List<String> getAuthentication() {
            return authentication;
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
         * Gets the quality factor / weight of the entry.
         *
         * @return Returns the "q" weight from 0.0 to 1.0.
         */
        public double getWeight() {
            return weight;
        }

        /**
         * Gets a specific setting by key.
         *
         * @param key Key to retrieve.
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
        public List<String> getTags() {
            return tags;
        }

        @Override
        public Builder toBuilder() {
            Builder builder = builder()
                    .deprecated(deprecated)
                    .deprecationReason(deprecationReason)
                    .weight(weight);
            settings.forEach(builder::putSetting);
            tags.forEach(builder::addTag);
            authentication.forEach(builder::addAuthentication);
            return builder;
        }

        @Override
        public Node toNode() {
            if (node == null) {
                node = Node.objectNodeBuilder()
                        .withOptionalMember(Q, weight < 1.0 ? Optional.of(Node.from(weight)) : Optional.empty())
                        .withOptionalMember(DEPRECATED, deprecated ? Optional.of(Node.from(true)) : Optional.empty())
                        .withOptionalMember(DEPRECATION_REASON, getDeprecationReason().map(Node::from))
                        .withOptionalMember(SETTINGS, !settings.isEmpty()
                                ? Optional.of(ObjectNode.fromStringMap(settings))
                                : Optional.empty())
                        .withOptionalMember(TAGS, !tags.isEmpty()
                                ? Optional.of(ArrayNode.fromStrings(tags))
                                : Optional.empty())
                        .withOptionalMember(AUTHENTICATION, !authentication.isEmpty()
                                ? Optional.of(ArrayNode.fromStrings(authentication))
                                : Optional.empty())
                        .build();
            }

            return node;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (!(o instanceof Protocol)) {
                return false;
            }

            Protocol protocol = (Protocol) o;
            return deprecated == protocol.deprecated
                   && Objects.equals(deprecationReason, protocol.deprecationReason)
                   && authentication.equals(protocol.authentication)
                   && settings.equals(protocol.settings)
                   && tags.equals(protocol.tags)
                   && weight == protocol.weight;
        }

        @Override
        public int hashCode() {
            return Objects.hash(authentication, deprecated, settings, tags, weight);
        }

        public static final class Builder implements SmithyBuilder<Protocol> {
            private List<String> authentication = new ArrayList<>();
            private boolean deprecated;
            private String deprecationReason;
            private final Map<String, String> settings = new HashMap<>();
            private final List<String> tags = new ArrayList<>();
            private double weight = 1.0;

            public Protocol.Builder addAuthentication(String value) {
                authentication.add(value);
                return this;
            }

            public Protocol.Builder removeAuthentication(String value) {
                authentication.remove(value);
                return this;
            }

            public Protocol.Builder clearAuthentication() {
                authentication.clear();
                return this;
            }

            public Builder deprecated(boolean deprecated) {
                this.deprecated = deprecated;
                return this;
            }

            public Builder deprecationReason(String deprecationReason) {
                this.deprecationReason = deprecationReason;
                return this;
            }

            public Builder putSetting(String key, String value) {
                settings.put(key, value);
                return this;
            }

            public Builder removeSetting(String key) {
                settings.remove(key);
                return this;
            }

            public Builder clearSettings() {
                settings.clear();
                return this;
            }

            public Builder addTag(String tag) {
                tags.add(tag);
                return this;
            }

            public Builder removeTag(String tag) {
                tags.remove(tag);
                return this;
            }

            public Builder clearTags() {
                tags.clear();
                return this;
            }

            public Builder weight(double weight) {
                this.weight = weight;
                return this;
            }

            @Override
            public Protocol build() {
                return new Protocol(this);
            }
        }
    }

    private static Map<String, Protocol> createProtocolMap(Set<Map.Entry<String, Protocol>> entries) {
        Set<Map.Entry<String, Protocol>> sorted = entries.stream()
                .sorted((a, b) -> {
                    double aWeight = a.getValue().getWeight();
                    double bWeight = b.getValue().getWeight();
                    if (Math.abs(aWeight - bWeight) < .0000001) {
                        return 0;
                    }
                    return aWeight > bWeight ? 1 : -1;
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return new AbstractMap<>() {
            @Override
            public Set<Entry<String, Protocol>> entrySet() {
                return sorted;
            }
        };
    }
}
