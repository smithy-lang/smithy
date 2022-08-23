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

package software.amazon.smithy.rulesengine.v1;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.utils.SmithyUnstableApi;

//    "dnsSuffix" : "amazonaws.com",
//    "partition" : "aws",
//    "partitionName" : "AWS Standard",
//    "regionRegex" : "^(us|eu|ap|sa|ca|me|af)\\-\\w+\\-\\d+$"
@SmithyUnstableApi
public final class PartitionV1 {
    private static final String DEFAULTS = "defaults";
    private static final String DNS_SUFFIX = "dnsSuffix";
    private static final String PARTITION = "partition";
    private static final String PARTITION_NAME = "partitionName";
    private static final String REGION_REGEX = "regionRegex";

    private static final String SERVICES = "services";
    private static final String REGIONS = "regions";
    private static final String VARIANTS = "variants";

    private DefaultsV1 defaults;
    private String dnsSuffix;
    private String partition;
    private String partitionName;
    private String regionRegex;

    private List<VariantV1> variants;
    private Map<String, ServiceV1> services;
    private Map<String, Node> regions;

    public static PartitionV1 fromNode(Node node) {
        ObjectNode on = node.expectObjectNode();

        PartitionV1 partitionV1 = new PartitionV1();

        on.getObjectMember(DEFAULTS).ifPresent(defaultsNode ->
                partitionV1.defaults = DefaultsV1.fromNode(defaultsNode));
        on.getStringMember(DNS_SUFFIX).ifPresent(s -> partitionV1.dnsSuffix = s.getValue());
        on.getStringMember(PARTITION).ifPresent(s -> partitionV1.partition = s.getValue());
        on.getStringMember(PARTITION_NAME).ifPresent(s -> partitionV1.partitionName = s.getValue());
        on.getStringMember(REGION_REGEX).ifPresent(s -> partitionV1.regionRegex = s.getValue());

        ObjectNode servicesNode = on.expectObjectMember(SERVICES);
        partitionV1.services = servicesNode.getStringMap()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> ServiceV1.fromNode(e.getValue())));

        partitionV1.variants = on.getArrayMember(VARIANTS).map(variantsNode ->
                variantsNode.getElements().stream().map(VariantV1::fromNode).collect(Collectors.toList())
        ).orElse(null);

        ObjectNode regionsNode = on.expectObjectMember(REGIONS);

        partitionV1.regions = regionsNode.getStringMap();

        return partitionV1;
    }

    public DefaultsV1 defaults() {
        return defaults;
    }

    public String dnsSuffix() {
        return dnsSuffix;
    }

    public String partition() {
        return partition;
    }

    public String partitionName() {
        return partitionName;
    }

    public String regionRegex() {
        return regionRegex;
    }

    public List<VariantV1> variants() {
        return variants;
    }

    public Map<String, ServiceV1> services() {
        return services;
    }

    public Optional<ServiceV1> service(String serviceId) {
        return Optional.ofNullable(services.get(serviceId));
    }

    public Map<String, Node> regions() {
        return regions;
    }


}
