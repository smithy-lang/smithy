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

import java.util.List;
import java.util.Objects;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.rulesengine.language.RulesComponentBuilder;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * A model for defining the set of partitions that are used by the rule-set aws.partition function.
 */
@SmithyUnstableApi
public final class Partitions implements ToSmithyBuilder<Partitions>, FromSourceLocation, ToNode {
    private static final String VERSION = "version";
    private static final String PARTITIONS = "partitions";
    private static final List<String> PROPERTIES = ListUtils.of(VERSION, PARTITIONS);

    private final String version;
    private final List<Partition> partitions;
    private final SourceLocation sourceLocation;

    private Partitions(Builder builder) {
        this.sourceLocation = builder.getSourceLocation();
        this.version = builder.version;
        this.partitions = builder.partitions.copy();
    }

    public static Builder builder() {
        return new Builder(SourceLocation.none());
    }

    public static Partitions fromNode(Node node) {
        Builder builder = new Builder(node);
        ObjectNode objNode = node.expectObjectNode();
        objNode.expectNoAdditionalProperties(PROPERTIES);

        objNode.getStringMember(VERSION, builder::version);
        objNode.getArrayMember(PARTITIONS, partitionsNode ->
                partitionsNode.forEach(partNode -> builder.addPartition(Partition.fromNode(partNode))));

        return builder.build();
    }

    public String getVersion() {
        return version;
    }

    public List<Partition> getPartitions() {
        return partitions;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public Builder toBuilder() {
        return new Builder(getSourceLocation())
                .version(version)
                .partitions(partitions);
    }

    @Override
    public Node toNode() {
        ArrayNode.Builder partitionsNodeBuilder = ArrayNode.builder();
        partitions.forEach(partitionsNodeBuilder::withValue);

        return Node.objectNodeBuilder()
                .withMember(VERSION, Node.from(version))
                .withMember(PARTITIONS, partitionsNodeBuilder.build())
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
        Partitions that = (Partitions) o;
        return version.equals(that.version) && partitions.equals(that.partitions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, partitions, sourceLocation);
    }

    @Override
    public String toString() {
        return "Partitions{version='" + version
               + "', partitions=" + partitions
               + ", sourceLocation=" + sourceLocation
               + '}';
    }

    public static class Builder extends RulesComponentBuilder<Builder, Partitions> {
        private String version;
        private final BuilderRef<List<Partition>> partitions = BuilderRef.forList();

        public Builder(FromSourceLocation sourceLocation) {
            super(sourceLocation);
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder partitions(List<Partition> partitions) {
            this.partitions.clear();
            this.partitions.get().addAll(partitions);
            return this;
        }

        public Builder addPartition(Partition p) {
            this.partitions.get().add(p);
            return this;
        }

        @Override
        public Partitions build() {
            return new Partitions(this);
        }
    }
}
