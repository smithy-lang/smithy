/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.FunctionalUtils;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.ToSmithyBuilder;

public final class CorsTrait extends AbstractTrait implements ToSmithyBuilder<CorsTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.api#cors");

    private static final String DEFAULT_ORIGIN = "*";
    private static final int DEFAULT_MAX_AGE = 600;

    private final String origin;
    private final Map<String, String> origins;
    private final int maxAge;
    private final Set<String> additionalAllowedHeaders;
    private final Set<String> additionalExposedHeaders;

    private CorsTrait(Builder builder) {
        super(ID, builder.sourceLocation);
        origin = builder.origin;
        origins = builder.origins.copy();
        maxAge = builder.maxAge;
        additionalAllowedHeaders = builder.additionalAllowedHeaders.copy();
        additionalExposedHeaders = builder.additionalExposedHeaders.copy();
    }

    public String getOrigin() {
        return origin;
    }

    public Map<String, String> getOrigins() {
        return origins;
    }

    public int getMaxAge() {
        return maxAge;
    }

    public Set<String> getAdditionalAllowedHeaders() {
        return additionalAllowedHeaders;
    }

    public Set<String> getAdditionalExposedHeaders() {
        return additionalExposedHeaders;
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .sourceLocation(getSourceLocation())
                .origin(origin)
                .origins(origins)
                .maxAge(maxAge)
                .additionalAllowedHeaders(additionalAllowedHeaders)
                .additionalExposedHeaders(additionalExposedHeaders);
    }

    @Override
    protected Node createNode() {
        return new ObjectNode(MapUtils.of(), getSourceLocation())
                .withOptionalMember("origin",
                        Optional.of(origin)
                                .filter(val -> !val.equals(DEFAULT_ORIGIN))
                                .map(Node::from))
                .withOptionalMember("origins",
                        Optional.of(origins)
                                .filter(FunctionalUtils.not(Map::isEmpty))
                                .map(ObjectNode::fromStringMap))
                .withOptionalMember("maxAge",
                        Optional.of(maxAge)
                                .filter(val -> !val.equals(DEFAULT_MAX_AGE))
                                .map(Node::from))
                .withOptionalMember("additionalAllowedHeaders",
                        Optional.of(additionalAllowedHeaders)
                                .filter(FunctionalUtils.not(Set::isEmpty))
                                .map(Node::fromStrings))
                .withOptionalMember("additionalExposedHeaders",
                        Optional.of(additionalExposedHeaders)
                                .filter(FunctionalUtils.not(Set::isEmpty))
                                .map(Node::fromStrings));
    }

    // Avoid inconsequential equality issues due to empty vs not empty sets.
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CorsTrait)) {
            return false;
        } else if (other == this) {
            return true;
        } else {
            CorsTrait trait = (CorsTrait) other;
            return origin.equals(trait.origin)
                    && origins.equals(trait.origins)
                    && maxAge == trait.maxAge
                    && additionalAllowedHeaders.equals(trait.additionalAllowedHeaders)
                    && additionalExposedHeaders.equals(trait.additionalExposedHeaders);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(toShapeId(), origin, origins, maxAge, additionalAllowedHeaders, additionalExposedHeaders);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends AbstractTraitBuilder<CorsTrait, Builder> {

        private String origin = DEFAULT_ORIGIN;
        private final BuilderRef<Map<String, String>> origins = BuilderRef.forOrderedMap();
        private int maxAge = DEFAULT_MAX_AGE;
        private final BuilderRef<Set<String>> additionalAllowedHeaders =
                BuilderRef.forSortedSet(String.CASE_INSENSITIVE_ORDER);
        private final BuilderRef<Set<String>> additionalExposedHeaders =
                BuilderRef.forSortedSet(String.CASE_INSENSITIVE_ORDER);

        private Builder() {}

        public Builder origin(String origin) {
            this.origin = Objects.requireNonNull(origin);
            return this;
        }

        public Builder origins(Map<String, String> origins) {
            this.origins.clear();
            this.origins.get().putAll(origins);
            return this;
        }

        public Builder maxAge(int maxAge) {
            this.maxAge = maxAge;
            return this;
        }

        public Builder additionalAllowedHeaders(Set<String> additionalAllowedHeaders) {
            this.additionalAllowedHeaders.clear();
            this.additionalAllowedHeaders.get().addAll(additionalAllowedHeaders);
            return this;
        }

        public Builder additionalExposedHeaders(Set<String> additionalExposedHeaders) {
            this.additionalExposedHeaders.clear();
            this.additionalExposedHeaders.get().addAll(additionalExposedHeaders);
            return this;
        }

        @Override
        public CorsTrait build() {
            return new CorsTrait(this);
        }
    }

    public static final class Provider implements TraitService {
        @Override
        public ShapeId getShapeId() {
            return ID;
        }

        @Override
        public CorsTrait createTrait(ShapeId target, Node value) {
            Builder builder = builder().sourceLocation(value);
            value.expectObjectNode()
                    .getStringMember("origin", builder::origin)
                    .getObjectMember("origins", o -> builder.origins(stringMapFromNode(o)))
                    .getNumberMember("maxAge", n -> builder.maxAge(n.intValue()))
                    .getMember("additionalAllowedHeaders",
                            Node::expectArrayNode,
                            a -> builder.additionalAllowedHeaders(stringSetFromNode(a)))
                    .getMember("additionalExposedHeaders",
                            Node::expectArrayNode,
                            a -> builder.additionalExposedHeaders(stringSetFromNode(a)));
            CorsTrait result = builder.build();
            result.setNodeCache(value);
            return result;
        }

        private static Map<String, String> stringMapFromNode(ObjectNode node) {
            Map<String, String> result = new LinkedHashMap<>(node.size());
            for (Map.Entry<StringNode, Node> entry : node.getMembers().entrySet()) {
                result.put(entry.getKey().getValue(), entry.getValue().expectStringNode().getValue());
            }
            return result;
        }

        private static Set<String> stringSetFromNode(ArrayNode node) {
            Set<String> result = new HashSet<>(node.size());
            for (Node value : node.getElements()) {
                result.add(value.expectStringNode().getValue());
            }
            return result;
        }
    }
}
