/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.FunctionalUtils;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.ToSmithyBuilder;

public final class CorsTrait extends AbstractTrait implements ToSmithyBuilder<CorsTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.api#cors");

    private static final String DEFAULT_ORIGIN = "*";
    private static final int DEFAULT_MAX_AGE = 600;

    private final String specifiedOrigin;
    private final String origin;
    private final Map<String, String> origins;
    private final int maxAge;
    private final Set<String> additionalAllowedHeaders;
    private final Set<String> additionalExposedHeaders;

    private CorsTrait(Builder builder) {
        super(ID, builder.sourceLocation);
        specifiedOrigin = builder.origin;
        origin = specifiedOrigin == null ? DEFAULT_ORIGIN : specifiedOrigin;
        origins = builder.origins.copy();
        maxAge = builder.maxAge;
        additionalAllowedHeaders = builder.additionalAllowedHeaders.copy();
        additionalExposedHeaders = builder.additionalExposedHeaders.copy();
    }

    /**
     * Gets the single {@code origin} value of the cors trait.
     *
     * <p>Defaults to {@code "*"} when not explicitly set. Use
     * {@link #resolveOrigin(String)} to resolve the effective origin
     * when the {@code origins} map may be in use.
     *
     * @return the origin value.
     * @throws IllegalStateException if the {@code origins} map is set.
     * @deprecated Use {@link #resolveOrigin(String)} instead.
     */
    @Deprecated
    public String getOrigin() {
        if (!origins.isEmpty()) {
            throw new IllegalStateException(
                    "Tried to access `@cors` trait's `origin` property when the mutually exclusive `origins` property is set.");
        }
        return origin;
    }

    /**
     * Gets the explicitly set {@code origin} value, if any.
     *
     * @return the specified origin, or empty if not explicitly set.
     */
    public Optional<String> getSpecifiedOrigin() {
        return Optional.ofNullable(specifiedOrigin);
    }

    /**
     * Gets the map of named origins.
     *
     * <p>Each key is a user-defined name for the origin, and each
     * value is the origin URL. Returns an empty map when not set.
     *
     * @return the named origins map.
     */
    public Map<String, String> getOrigins() {
        return origins;
    }

    /**
     * Resolves the effective origin value using the given key.
     *
     * <p>When {@code originKey} is null and the {@code origins} map is
     * empty, the single {@code origin} value is returned. When the
     * {@code origins} map is populated, the key is required and must
     * match an entry in the map.
     *
     * @param originKey the key to look up in the {@code origins} map,
     *     or null to use the single {@code origin} value.
     * @return the resolved origin URL.
     * @throws IllegalStateException if the key is null when origins is populated.
     * @throws NoSuchElementException if the key is not found in the origins map.
     */
    public String resolveOrigin(String originKey) {
        if (originKey == null) {
            if (origins.isEmpty()) {
                return origin;
            }
            throw new IllegalStateException(String.format(
                    "Cors key was `null` when resolving the `@cors` origin from the `origins` map. Available keys: [%s]",
                    ValidationUtils.tickedList(origins.keySet())));
        }

        if (origins.containsKey(originKey)) {
            return origins.get(originKey);
        }

        throw new NoSuchElementException(String.format(
                "Cors key `%s` not found in the `@cors` trait `origins` map. Available keys: [%s]",
                originKey,
                ValidationUtils.tickedList(origins.keySet())));
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
        Builder b = builder()
                .sourceLocation(getSourceLocation())
                .origins(origins)
                .maxAge(maxAge)
                .additionalAllowedHeaders(additionalAllowedHeaders)
                .additionalExposedHeaders(additionalExposedHeaders);
        if (specifiedOrigin != null) {
            b.origin(specifiedOrigin);
        }
        return b;
    }

    @Override
    protected Node createNode() {
        return new ObjectNode(MapUtils.of(), getSourceLocation())
                .withOptionalMember("origin",
                        getSpecifiedOrigin()
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

        private String origin;
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
