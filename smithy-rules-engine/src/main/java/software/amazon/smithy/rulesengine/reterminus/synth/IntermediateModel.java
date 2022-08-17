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

package software.amazon.smithy.rulesengine.reterminus.synth;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.rulesengine.v1.PartitionV1;

/**
 * Contains preprocessed endpoint information for a single service and partition.
 */
public final class IntermediateModel {
    private final String serviceId;
    private final PartitionV1 partition;
    private final EndpointModel defaultEndpointModel;
    private final Map<String, EndpointModel> regionEndpoints;
    private final Set<String> uniqueFipsRegions;
    private final Set<String> uniqueDefaultRegions;

    private IntermediateModel(Builder b) {
        this.serviceId = b.serviceId;
        this.partition = b.partition;
        this.defaultEndpointModel = b.defaultEndpointModel;
        this.regionEndpoints = b.endpoints;
        this.uniqueFipsRegions = b.uniqueFipsRegions;
        this.uniqueDefaultRegions = b.uniqueDefaultRegions;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String serviceId() {
        return serviceId;
    }

    public String partitionId() {
        return partition.partition();
    }

    public EndpointModel defaultEndpointModel() {
        return defaultEndpointModel;
    }

    public Set<String> uniqueFipsRegions() {
        return uniqueFipsRegions;
    }

    public Set<String> uniqueDefaultRegions() {
        return uniqueDefaultRegions;
    }

    public Optional<EndpointModel> regionEndpointModel(String regionName) {
        EndpointModel em = regionEndpoints.get(regionName);
        return Optional.ofNullable(em);
    }

    public static class Builder {
        private final Map<String, EndpointModel> endpoints = new HashMap<>();
        private final Set<String> uniqueFipsRegions = new HashSet<>();
        private final Set<String> uniqueDefaultRegions = new HashSet<>();
        private PartitionV1 partition;
        private String serviceId;
        private EndpointModel defaultEndpointModel;

        public Builder serviceId(String serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        public Builder partition(PartitionV1 partition) {
            this.partition = partition;
            return this;
        }

        public Builder defaultEndpointModel(EndpointModel defaultEndpointModel) {
            this.defaultEndpointModel = defaultEndpointModel;
            return this;
        }

        public Builder putRegionEndpoint(String region, EndpointModel endpoint) {
            this.endpoints.put(region, endpoint);
            return this;
        }

        public Builder addUniqueFipsRegion(String region) {
            this.uniqueFipsRegions.add(region);
            return this;
        }

        public Builder addUniqueDefaultRegion(String region) {
            this.uniqueDefaultRegions.add(region);
            return this;
        }

        public IntermediateModel build() {
            return new IntermediateModel(this);
        }
    }
}
