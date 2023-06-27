/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.aws.language.functions.partition;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.rulesengine.language.RulesComponentBuilder;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Describes an AWS partition, its regions, and the outputs to be provided from the rule-set aws.partition function.
 */
@SmithyUnstableApi
public final class Partition implements ToSmithyBuilder<Partition>, FromSourceLocation, ToNode {
    private static final String ID = "id";
    private static final String REGION_REGEX = "regionRegex";
    private static final String REGIONS = "regions";
    private static final String OUTPUTS = "outputs";
    private static final List<String> PROPERTIES = ListUtils.of(ID, REGION_REGEX, REGIONS, OUTPUTS);

    private final String id;
    private final String regionRegex;
    private final Map<String, RegionOverride> regions;
    private final PartitionOutputs outputs;
    private final SourceLocation sourceLocation;

    private Partition(Builder builder) {
        this.sourceLocation = builder.getSourceLocation();
        this.id = builder.id;
        this.regionRegex = builder.regionRegex;
        this.regions = builder.regions.copy();
        this.outputs = builder.outputs;
    }

    /**
     * Builder to create a {@link Partition} instance.
     *
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder(SourceLocation.none());
    }

    /**
     * Creates a {@link Partition} instance from the given Node information.
     *
     * @param node the node to deserialize.
     * @return the created Partition.
     */
    public static Partition fromNode(Node node) {
        Builder builder = new Builder(node);
        ObjectNode objectNode = node.expectObjectNode();
        objectNode.expectObjectNode().expectNoAdditionalProperties(PROPERTIES);

        objectNode.expectStringMember(ID, builder::id);
        objectNode.getStringMember(REGION_REGEX, builder::regionRegex);
        objectNode.getObjectMember(REGIONS, regionsNode -> regionsNode.getMembers().forEach((k, v) ->
                builder.putRegion(k.toString(), RegionOverride.fromNode(v))));
        objectNode.getObjectMember(OUTPUTS, outputsNode ->
                builder.outputs(PartitionOutputs.fromNode(outputsNode)));

        return builder.build();
    }

    /**
     * Gets the ID of the partition.
     *
     * @return returns the partition's ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the regular expression to match regions in this partition.
     *
     * @return returns the regular expression to match regions in this partition.
     */
    public String getRegionRegex() {
        return regionRegex;
    }

    /**
     * Gets this partition's map of region names to region-specific overriding data.
     *
     * @return returns a map of region names to region-specific overriding data.
     */
    public Map<String, RegionOverride> getRegions() {
        return regions;
    }

    /**
     * Gets the supported configuration output for this partition.
     *
     * @return returns the partition's configuration outputs.
     */
    public PartitionOutputs getOutputs() {
        return outputs;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public SmithyBuilder<Partition> toBuilder() {
        return new Builder(getSourceLocation())
                .id(id)
                .regionRegex(regionRegex)
                .regions(regions)
                .outputs(outputs);
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder regionNodeBuilder = ObjectNode.builder();
        for (Map.Entry<String, RegionOverride> entry : regions.entrySet()) {
            regionNodeBuilder.withMember(entry.getKey(), entry.getValue().toNode());
        }

        return Node.objectNodeBuilder()
                .withMember(ID, Node.from(id))
                .withMember(REGION_REGEX, Node.from(regionRegex))
                .withMember(REGIONS, regionNodeBuilder.build())
                .withMember(OUTPUTS, outputs.toNode())
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
        Partition partition = (Partition) o;
        return Objects.equals(id, partition.id) && Objects.equals(regionRegex, partition.regionRegex)
               && Objects.equals(regions, partition.regions)
               && Objects.equals(outputs, partition.outputs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, regionRegex, regions, outputs);
    }

    /**
     * A builder used to create a {@link Partition} class.
     */
    public static class Builder extends RulesComponentBuilder<Builder, Partition> {
        private String id;
        private String regionRegex;
        private final BuilderRef<Map<String, RegionOverride>> regions = BuilderRef.forOrderedMap();
        private PartitionOutputs outputs;

        public Builder(FromSourceLocation sourceLocation) {
            super(sourceLocation);
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder regionRegex(String regionRegex) {
            this.regionRegex = regionRegex;
            return this;
        }

        public Builder regions(Map<String, RegionOverride> regions) {
            this.regions.clear();
            this.regions.get().putAll(regions);
            return this;
        }

        public Builder putRegion(String name, RegionOverride regionOverride) {
            this.regions.get().put(name, regionOverride);
            return this;
        }

        public Builder outputs(PartitionOutputs outputs) {
            this.outputs = outputs;
            return this;
        }

        @Override
        public Partition build() {
            return new Partition(this);
        }
    }
}
