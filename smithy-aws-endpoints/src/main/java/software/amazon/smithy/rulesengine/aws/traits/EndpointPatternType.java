/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.aws.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.StringNode;

/**
 * The pattern type to use for the partitional services.
 */
public enum EndpointPatternType {
    /** An endpoint with pattern `{service}.{dnsSuffix}`.*/
    SERVICE_DNSSUFFIX("service_dnsSuffix"),

    /** An endpoint with pattern `{service}.{region}.{dnsSuffix}`. */
    SERVICE_REGION_DNSSUFFI("service_region_dnsSuffix");

    private final String name;

    EndpointPatternType(String name) {
        this.name = name;
    }

    /**
     * Gets the name of a partitional service pattern type.
     *
     * @return Returns a partitional service pattern type name.
     */
    public String getName() {
        return name;
    }

    public static EndpointPatternType fromNode(Node node) {
        StringNode value = node.expectStringNode();
        for (EndpointPatternType type: EndpointPatternType.values()) {
            if (type.name.equals(value.getValue())) {
                return type;
            }
        }
        throw new RuntimeException(String.format(
            "Unable to find EndpointPatternType enum with value [%s]", value.getValue()));
    }
}
