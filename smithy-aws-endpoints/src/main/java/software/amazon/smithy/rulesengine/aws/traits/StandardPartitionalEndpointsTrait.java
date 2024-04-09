/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.aws.traits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * An endpoints modifier trait that indicates that a service is partitional
 * and a single endpoint should resolve per partition.
 */
public final class StandardPartitionalEndpointsTrait extends AbstractTrait
        implements ToSmithyBuilder<StandardPartitionalEndpointsTrait> {
    public static final ShapeId ID = ShapeId.from("aws.endpoints#standardPartitionalEndpoints");
    public static final String PARTITION_ENDPOINT_SPECIAL_CASES = "partitionEndpointSpecialCases";
    public static final String ENDPOINT_PATTERN_TYPE = "endpointPatternType";

    private final Map<String, List<PartitionEndpointSpecialCase>> partitionEndpointSpecialCases;

    private final EndpointPatternType endpointPatternType;

    public StandardPartitionalEndpointsTrait(StandardPartitionalEndpointsTrait.Builder builder) {
        super(ID, builder.getSourceLocation());
        partitionEndpointSpecialCases = builder.partitionEndpointSpecialCases.copy();
        endpointPatternType = Objects.requireNonNull(builder.endpointPatternType);
    }

    /**
     * Gets the map of partition string to a list of partition endpoint special cases defined in the trait.
     *
     * @return Returns a map of partition string to a list of {@link PartitionEndpointSpecialCase}
     */
    public Map<String, List<PartitionEndpointSpecialCase>> getPartitionEndpointSpecialCases() {
        return partitionEndpointSpecialCases;
    }

    /**
     * Gets the endpoint pattern type defined in the trait.
     *
     * @return Returns a {@link EndpointPatternType}
     */
    public EndpointPatternType getEndpointPatternType() {
        return endpointPatternType;
    }

    @Override
    protected Node createNode() {
        ObjectNode.Builder partitionEndpointSpecialCasesNodeBuilder = ObjectNode.objectNodeBuilder();
        for (Map.Entry<String, List<PartitionEndpointSpecialCase>> entry
                : partitionEndpointSpecialCases.entrySet()) {
            List<Node> nodes = new ArrayList<>();
            for (PartitionEndpointSpecialCase partitionEndpointSpecialCase : entry.getValue()) {
                nodes.add(partitionEndpointSpecialCase.toNode());
            }
            partitionEndpointSpecialCasesNodeBuilder.withMember(entry.getKey(), Node.fromNodes(nodes));
        }

        return ObjectNode.objectNodeBuilder()
                .sourceLocation(getSourceLocation())
                .withMember(PARTITION_ENDPOINT_SPECIAL_CASES, partitionEndpointSpecialCasesNodeBuilder.build())
                .withMember(ENDPOINT_PATTERN_TYPE, endpointPatternType.getName())
                .build();
    }

    @Override
    public SmithyBuilder<StandardPartitionalEndpointsTrait> toBuilder() {
        return new Builder()
                .partitionEndpointSpecialCases(partitionEndpointSpecialCases)
                .endpointPatternType(endpointPatternType);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            ObjectNode objectNode = value.expectObjectNode();

            EndpointPatternType endpointPatternType = EndpointPatternType
                .fromNode(objectNode.expectStringMember(ENDPOINT_PATTERN_TYPE));

            StandardPartitionalEndpointsTrait.Builder builder = builder()
                    .sourceLocation(value)
                    .endpointPatternType(endpointPatternType);

            if (objectNode.containsMember(PARTITION_ENDPOINT_SPECIAL_CASES)) {
                for (Map.Entry<String, Node> entry
                        : objectNode.expectObjectMember(PARTITION_ENDPOINT_SPECIAL_CASES).getStringMap().entrySet()) {
                    List<PartitionEndpointSpecialCase> partitionEndpointSpecialCases = new ArrayList<>();
                    for (Node node: entry.getValue().expectArrayNode().getElements()) {
                        partitionEndpointSpecialCases.add(PartitionEndpointSpecialCase.fromNode(node));
                    }
                    builder.putPartitionEndpointSpecialCase(
                        entry.getKey(), Collections.unmodifiableList(partitionEndpointSpecialCases));
                }
            }

            StandardPartitionalEndpointsTrait result = builder.build();
            result.setNodeCache(value);
            return result;
        }
    }

    public static final class Builder extends AbstractTraitBuilder<StandardPartitionalEndpointsTrait, Builder> {
        private final BuilderRef<Map<String, List<PartitionEndpointSpecialCase>>> partitionEndpointSpecialCases =
            BuilderRef.forOrderedMap();
        private EndpointPatternType endpointPatternType;

        /**
         * Sets the partition endpoint special cases.
         *
         * @param partitionEndpointSpecialCases Map of partition string to list of {@link PartitionEndpointSpecialCase}
         * @return Returns the builder.
         */
        public Builder partitionEndpointSpecialCases(
            Map<String, List<PartitionEndpointSpecialCase>> partitionEndpointSpecialCases
        ) {
            this.partitionEndpointSpecialCases.clear();
            this.partitionEndpointSpecialCases.get().putAll(partitionEndpointSpecialCases);
            return this;
        }

        /**
         * Sets the list of partition endpoint special cases for a partition.
         *
         * @param partition the partition to use
         * @param partitionEndpointSpecialCases Map of partition string to list of {@link PartitionEndpointSpecialCase}
         * @return Returns the builder.
         */
        public Builder putPartitionEndpointSpecialCase(
            String partition,
            List<PartitionEndpointSpecialCase> partitionEndpointSpecialCases
        ) {
            this.partitionEndpointSpecialCases.get().put(partition, partitionEndpointSpecialCases);
            return this;
        }

        /**
         * Sets the endpoint pattern type.
         *
         * @param endpointPatternType the endpoint pattern type to use
         * @return Returns the builder.
         */
        public Builder endpointPatternType(EndpointPatternType endpointPatternType) {
            this.endpointPatternType = endpointPatternType;
            return this;
        }

        @Override
        public StandardPartitionalEndpointsTrait build() {
            return new StandardPartitionalEndpointsTrait(this);
        }
    }
}
