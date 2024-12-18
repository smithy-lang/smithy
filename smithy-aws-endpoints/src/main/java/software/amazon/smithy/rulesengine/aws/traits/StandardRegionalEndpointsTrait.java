/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.aws.traits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * An endpoints modifier trait that indicates that a service's endpoints should be resolved
 * using the standard AWS regional patterns.
 */
public final class StandardRegionalEndpointsTrait extends AbstractTrait
        implements ToSmithyBuilder<StandardRegionalEndpointsTrait> {
    public static final ShapeId ID = ShapeId.from("aws.endpoints#standardRegionalEndpoints");
    public static final String PARTITION_SPECIAL_CASES = "partitionSpecialCases";
    public static final String REGION_SPECIAL_CASES = "regionSpecialCases";

    private final Map<String, List<PartitionSpecialCase>> partitionSpecialCases;
    private final Map<String, List<RegionSpecialCase>> regionSpecialCases;

    public StandardRegionalEndpointsTrait(StandardRegionalEndpointsTrait.Builder builder) {
        super(ID, builder.getSourceLocation());
        partitionSpecialCases = builder.partitionSpecialCases.copy();
        regionSpecialCases = builder.regionSpecialCases.copy();
    }

    /**
     * Gets the map of partition string to a list of partition special cases defined in the trait.
     *
     * @return Returns a map of partition string to a list of {@link PartitionSpecialCase}
     */
    public Map<String, List<PartitionSpecialCase>> getPartitionSpecialCases() {
        return partitionSpecialCases;
    }

    /**
     * Gets the map of partition string to a list of region special cases defined in the trait.
     *
     * @return Returns a map of region string to a list of {@link RegionSpecialCase}
     */
    public Map<String, List<RegionSpecialCase>> getRegionSpecialCases() {
        return regionSpecialCases;
    }

    @Override
    protected Node createNode() {
        ObjectNode.Builder partitionSpecialCasesNodeBuilder = ObjectNode.objectNodeBuilder();
        for (Map.Entry<String, List<PartitionSpecialCase>> entry : partitionSpecialCases.entrySet()) {
            List<Node> nodes = new ArrayList<>();
            for (PartitionSpecialCase partitionSpecialCase : entry.getValue()) {
                nodes.add(partitionSpecialCase.toNode());
            }
            partitionSpecialCasesNodeBuilder.withMember(entry.getKey(), Node.fromNodes(nodes));
        }

        ObjectNode.Builder regionSpecialCasesNodeBuilder = ObjectNode.objectNodeBuilder();
        for (Map.Entry<String, List<RegionSpecialCase>> entry : regionSpecialCases.entrySet()) {
            List<Node> nodes = new ArrayList<>();
            for (RegionSpecialCase regionSpecialCase : entry.getValue()) {
                nodes.add(regionSpecialCase.toNode());
            }
            regionSpecialCasesNodeBuilder.withMember(entry.getKey(), Node.fromNodes(nodes));
        }

        return ObjectNode.objectNodeBuilder()
                .sourceLocation(getSourceLocation())
                .withMember(PARTITION_SPECIAL_CASES, partitionSpecialCasesNodeBuilder.build())
                .withMember(REGION_SPECIAL_CASES, regionSpecialCasesNodeBuilder.build())
                .build();
    }

    @Override
    public Builder toBuilder() {
        return new Builder()
                .partitionSpecialCases(partitionSpecialCases)
                .regionSpecialCases(regionSpecialCases);
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

            StandardRegionalEndpointsTrait.Builder builder = builder()
                    .sourceLocation(value);

            if (objectNode.containsMember(PARTITION_SPECIAL_CASES)) {
                for (Map.Entry<String, Node> entry : objectNode.expectObjectMember(PARTITION_SPECIAL_CASES)
                        .getStringMap()
                        .entrySet()) {
                    List<PartitionSpecialCase> partitionSpecialCases = new ArrayList<>();
                    for (Node node : entry.getValue().expectArrayNode().getElements()) {
                        partitionSpecialCases.add(PartitionSpecialCase.fromNode(node));
                    }
                    builder.putPartitionSpecialCases(
                            entry.getKey(),
                            Collections.unmodifiableList(partitionSpecialCases));
                }
            }

            if (objectNode.containsMember(REGION_SPECIAL_CASES)) {
                for (Map.Entry<String, Node> entry : objectNode.expectObjectMember(REGION_SPECIAL_CASES)
                        .getStringMap()
                        .entrySet()) {
                    List<RegionSpecialCase> regionSpecialCases = new ArrayList<>();
                    for (Node node : entry.getValue().expectArrayNode().getElements()) {
                        regionSpecialCases.add(RegionSpecialCase.fromNode(node));
                    }
                    builder.putRegionSpecialCases(entry.getKey(), Collections.unmodifiableList(regionSpecialCases));
                }
            }

            StandardRegionalEndpointsTrait result = builder.build();
            result.setNodeCache(value);
            return result;
        }
    }

    public static final class Builder extends AbstractTraitBuilder<StandardRegionalEndpointsTrait, Builder> {
        private final BuilderRef<Map<String, List<PartitionSpecialCase>>> partitionSpecialCases =
                BuilderRef.forOrderedMap();
        private final BuilderRef<Map<String, List<RegionSpecialCase>>> regionSpecialCases =
                BuilderRef.forOrderedMap();

        /**
         * Sets the partition special cases.
         *
         * @param partitionSpecialCases Map of partition string to list of {@link PartitionSpecialCase}
         * @return Returns the builder.
         */
        public Builder partitionSpecialCases(Map<String, List<PartitionSpecialCase>> partitionSpecialCases) {
            this.partitionSpecialCases.clear();
            this.partitionSpecialCases.get().putAll(partitionSpecialCases);
            return this;
        }

        /**
         * Sets the list of partition special cases for a partition.
         *
         * @param partition the partition to use
         * @param partitionSpecialCases Map of partition string to list of {@link PartitionSpecialCase}
         * @return Returns the builder.
         */
        public Builder putPartitionSpecialCases(String partition, List<PartitionSpecialCase> partitionSpecialCases) {
            this.partitionSpecialCases.get().put(partition, partitionSpecialCases);
            return this;
        }

        /**
         * Sets the region special cases.
         *
         * @param regionSpecialCases Map of region string to list of {@link RegionSpecialCase}
         * @return Returns the builder.
         */
        public Builder regionSpecialCases(Map<String, List<RegionSpecialCase>> regionSpecialCases) {
            this.regionSpecialCases.clear();
            this.regionSpecialCases.get().putAll(regionSpecialCases);
            return this;
        }

        /**
         * Sets the list of region special cases for a region.
         *
         * @param region the region to use
         * @param regionSpecialCases Map of region string to list of {@link RegionSpecialCase}
         * @return Returns the builder.
         */
        public Builder putRegionSpecialCases(String region, List<RegionSpecialCase> regionSpecialCases) {
            this.regionSpecialCases.get().put(region, regionSpecialCases);
            return this;
        }

        @Override
        public StandardRegionalEndpointsTrait build() {
            return new StandardRegionalEndpointsTrait(this);
        }
    }
}
