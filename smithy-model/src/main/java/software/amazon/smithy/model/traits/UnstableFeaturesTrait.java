/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Defines the preview features offered by a service, keyed by feature id.
 */
public final class UnstableFeaturesTrait extends AbstractTrait implements ToSmithyBuilder<UnstableFeaturesTrait> {

    public static final ShapeId ID = ShapeId.from("smithy.api#unstableFeatures");

    private final Map<String, UnstableFeatureInfo> features;

    private UnstableFeaturesTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        this.features = builder.features.copy();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected Node createNode() {
        ObjectNode.Builder builder = ObjectNode.builder();
        builder.sourceLocation(getSourceLocation());
        features.forEach((k, v) -> builder.withMember(k, v.toNode()));
        return builder.build();
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * Gets the map of feature id to feature metadata.
     *
     * @return Returns an unmodifiable map.
     */
    public Map<String, UnstableFeatureInfo> getFeatures() {
        return features;
    }

    /**
     * Gets the metadata for a specific feature id.
     *
     * @param featureId Feature id to look up.
     * @return Returns the optional feature metadata.
     */
    public Optional<UnstableFeatureInfo> getFeature(String featureId) {
        return Optional.ofNullable(features.get(featureId));
    }

    /**
     * The reason why a feature is unstable.
     */
    public enum UnstableReason implements ToNode {
        PREVIEW;

        private static final String[] NAMES = names();

        private static String[] names() {
            String[] names = new String[values().length];
            for (int i = 0; i < names.length; i++) {
                names[i] = values()[i].name();
            }
            return names;
        }

        /**
         * Creates an {@code UnstableReason} from a node.
         *
         * @param node Node to parse.
         * @return Returns the parsed reason.
         */
        public static UnstableReason fromNode(Node node) {
            return UnstableReason.valueOf(node.expectStringNode().expectOneOf(NAMES));
        }

        @Override
        public Node toNode() {
            return Node.from(toString());
        }
    }

    /**
     * The metadata describing a single unstable feature.
     */
    public static final class UnstableFeatureInfo implements ToNode {

        private final String message;
        private final UnstableReason reason;

        private UnstableFeatureInfo(String message, UnstableReason reason) {
            this.message = message;
            this.reason = reason;
        }

        public static UnstableFeatureInfo fromNode(Node node) {
            ObjectNode obj = node.expectObjectNode();
            String message = obj.getStringMember("message").map(StringNode::getValue).orElse(null);
            UnstableReason reason = obj.getMember("reason").map(UnstableReason::fromNode).orElse(null);
            return new UnstableFeatureInfo(message, reason);
        }

        public static UnstableFeatureInfo of(String message, UnstableReason reason) {
            return new UnstableFeatureInfo(message, reason);
        }

        public Optional<String> getMessage() {
            return Optional.ofNullable(message);
        }

        public Optional<UnstableReason> getReason() {
            return Optional.ofNullable(reason);
        }

        @Override
        public Node toNode() {
            return Node.objectNodeBuilder()
                    .withOptionalMember("message", getMessage().map(Node::from))
                    .withOptionalMember("reason", getReason().map(UnstableReason::toNode))
                    .build();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            UnstableFeatureInfo that = (UnstableFeatureInfo) o;
            return Objects.equals(message, that.message) && reason == that.reason;
        }

        @Override
        public int hashCode() {
            return Objects.hash(message, reason);
        }
    }

    public static final class Builder extends AbstractTraitBuilder<UnstableFeaturesTrait, Builder> {

        private final BuilderRef<Map<String, UnstableFeatureInfo>> features = BuilderRef.forOrderedMap();

        private Builder() {}

        private Builder(UnstableFeaturesTrait trait) {
            sourceLocation(trait.getSourceLocation());
            this.features.setBorrowed(trait.features);
        }

        @Override
        public UnstableFeaturesTrait build() {
            return new UnstableFeaturesTrait(this);
        }

        public Builder putFeature(String featureId, UnstableFeatureInfo info) {
            features.get().put(featureId, info);
            return this;
        }

        public Builder removeFeature(String featureId) {
            features.get().remove(featureId);
            return this;
        }
    }

    public static final class Provider implements TraitService {
        @Override
        public ShapeId getShapeId() {
            return ID;
        }

        @Override
        public UnstableFeaturesTrait createTrait(ShapeId target, Node value) {
            Builder builder = builder().sourceLocation(value);
            value.expectObjectNode().getMembers().forEach((k, v) -> {
                String key = k.getValue();
                builder.putFeature(key, UnstableFeatureInfo.fromNode(v));
            });
            UnstableFeaturesTrait result = builder.build();
            result.setNodeCache(value);
            return result;
        }
    }
}
