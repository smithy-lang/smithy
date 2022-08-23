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
import java.util.List;
import java.util.Objects;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.rulesengine.language.SourceAwareBuilder;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

@SmithyUnstableApi
public final class Partitions implements ToSmithyBuilder<Partitions>, FromSourceLocation {
    private static final String VERSION = "version";
    private static final String PARTITIONS = "partitions";
    private final String version;
    private final List<Partition> partitions;
    private final SourceLocation sourceLocation;

    private Partitions(Builder builder) {
        this.version = builder.version;
        this.partitions = builder.partitions.copy();
        this.sourceLocation = builder.getSourceLocation();
    }

    public static Partitions fromNode(Node node) {
        ObjectNode objNode = node.expectObjectNode();

        Builder b = new Builder(node);

        objNode.expectNoAdditionalProperties(Arrays.asList(VERSION, PARTITIONS));

        objNode.getStringMember(VERSION).ifPresent(v -> b.version(v.toString()));
        objNode.getArrayMember(PARTITIONS).ifPresent(partitionsNode ->
                partitionsNode.forEach(partNode ->
                        b.addPartition(Partition.fromNode(partNode))));

        return b.build();
    }

    public static Builder builder() {
        return new Builder(SourceLocation.none());
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, partitions, sourceLocation);
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
    public String toString() {
        return "Partitions{"
               + "version='" + version + '\''
               + ", partitions=" + partitions
               + ", sourceLocation=" + sourceLocation
               + '}';
    }

    public String version() {
        return version;
    }

    public List<Partition> partitions() {
        return partitions;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public SmithyBuilder<Partitions> toBuilder() {
        return new Builder(getSourceLocation())
                .version(version)
                .partitions(partitions);
    }

    public static class Builder extends SourceAwareBuilder<Builder, Partitions> {
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
