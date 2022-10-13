/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.rulesengine.language.model;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.rulesengine.language.util.SourceLocationTrackingBuilder;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Describes an AWS partition, it's regions, and the outputs to be provided by the rule-set aws.partition function.
 */
@SmithyUnstableApi
public final class Partition implements ToSmithyBuilder<Partition>, FromSourceLocation, ToNode {
    private static final String ID = "id";
    private static final String REGION_REGEX = "regionRegex";
    private static final String REGIONS = "regions";
    private static final String OUTPUTS = "outputs";

    private final String id;
    private final String regionRegex;
    private final Map<String, RegionOverride> regions;
    private final PartitionOutputs partitionOutputs;

    private final SourceLocation sourceLocation;

    private Partition(Builder builder) {
        this.id = builder.id;
        this.regionRegex = builder.regionRegex;
        this.regions = builder.regions.copy();
        this.partitionOutputs = builder.partitionOutputs;
        this.sourceLocation = builder.getSourceLocation();
    }

    public static Builder builder() {
        return new Builder(SourceLocation.none());
    }

    public static Partition fromNode(Node node) {
        Builder b = new Builder(node);

        ObjectNode objNode = node.expectObjectNode();

        objNode.expectObjectNode()
                .expectNoAdditionalProperties(Arrays.asList(ID, REGION_REGEX, REGIONS, OUTPUTS));

        objNode.getStringMember(ID).ifPresent(n -> b.id(n.toString()));
        objNode.getStringMember(REGION_REGEX).ifPresent(n -> b.regionRegex(n.toString()));

        objNode.getObjectMember(REGIONS).ifPresent(regionsNode ->
                regionsNode.getMembers().forEach((k, v) -> b.putRegion(k.toString(), RegionOverride.fromNode(v))));

        objNode.getObjectMember(OUTPUTS).ifPresent(outputsNode -> b.outputs(PartitionOutputs.fromNode(outputsNode)));

        return b.build();
    }

    public String id() {
        return id;
    }

    public String regionRegex() {
        return regionRegex;
    }

    public Map<String, RegionOverride> regions() {
        return regions;
    }

    public PartitionOutputs getOutputs() {
        return partitionOutputs;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public SmithyBuilder<Partition> toBuilder() {
        return new Builder(getSourceLocation())
                .id(id)
                .regionRegex(regionRegex);
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
               && Objects.equals(partitionOutputs, partition.partitionOutputs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, regionRegex, regions, partitionOutputs);
    }

    @Override
    public Node toNode() {
        return Node.objectNodeBuilder()
                .withMember(ID, Node.from(id))
                .withMember(REGION_REGEX, Node.from(regionRegex))
                .withMember(REGIONS, regionsNode())
                .withMember(OUTPUTS, partitionOutputs.toNode())
                .build();
    }

    private Node regionsNode() {
        ObjectNode.Builder on = ObjectNode.builder();
        regions.forEach((k, v) ->
                on.withMember(k, v.toNode())
        );
        return on.build();
    }

    public static class Builder extends SourceLocationTrackingBuilder<Builder, Partition> {
        private String id;
        private String regionRegex;
        private final BuilderRef<Map<String, RegionOverride>> regions = BuilderRef.forOrderedMap();
        private PartitionOutputs partitionOutputs;

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

        public Builder outputs(PartitionOutputs partitionOutputs) {
            this.partitionOutputs = partitionOutputs;
            return this;
        }

        @Override
        public Partition build() {
            return new Partition(this);
        }
    }
}
