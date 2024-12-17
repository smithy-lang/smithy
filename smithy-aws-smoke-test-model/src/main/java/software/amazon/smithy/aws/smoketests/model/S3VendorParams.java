/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.smoketests.model;

import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Vendor params for S3.
 */
public final class S3VendorParams extends BaseAwsVendorParams {
    public static final ShapeId ID = ShapeId.from("aws.test#S3VendorParams");

    private static final boolean DEFAULT_USE_ACCELERATE = false;
    private static final boolean DEFAULT_USE_GLOBAL_ENDPOINT = false;
    private static final boolean DEFAULT_FORCE_PATH_STYLE = false;
    private static final boolean DEFAULT_USE_ARN_REGION = true;
    private static final boolean DEFAULT_USE_MULTI_REGION_ACCESS_POINTS = true;

    private final boolean useAccelerate;
    private final boolean useGlobalEndpoint;
    private final boolean forcePathStyle;
    private final boolean useArnRegion;
    private final boolean useMultiRegionAccessPoints;

    S3VendorParams(ObjectNode node) {
        super(node);
        this.useAccelerate = node.getBooleanMemberOrDefault("useAccelerate", DEFAULT_USE_ACCELERATE);
        this.useGlobalEndpoint = node.getBooleanMemberOrDefault("useGlobalEndpoint", DEFAULT_USE_GLOBAL_ENDPOINT);
        this.forcePathStyle = node.getBooleanMemberOrDefault("forcePathStyle", DEFAULT_FORCE_PATH_STYLE);
        this.useArnRegion = node.getBooleanMemberOrDefault("useArnRegion", DEFAULT_USE_ARN_REGION);
        this.useMultiRegionAccessPoints = node.getBooleanMemberOrDefault(
                "useMultiRegionAccessPoints",
                DEFAULT_USE_MULTI_REGION_ACCESS_POINTS);
    }

    /**
     * @return Whether to resolve an accelerate endpoint or not. Defaults to
     * {@code false}.
     */
    public boolean useAccelerate() {
        return useAccelerate;
    }

    /**
     * @return Whether to use the global endpoint for {@code us-east-1}.
     * Defaults to {@code false}.
     */
    public boolean useGlobalEndpoint() {
        return useGlobalEndpoint;
    }

    /**
     * @return Whether to force path-style addressing. Defaults to {@code false}.
     */
    public boolean forcePathStyle() {
        return forcePathStyle;
    }

    /**
     * @return Whether to use the region in the bucket ARN to override the set
     * region. Defaults to {@code true}.
     */
    public boolean useArnRegion() {
        return useArnRegion;
    }

    /**
     * @return Whether to use S3's multi-region access points. Defaults to
     * {@code true}.
     */
    public boolean useMultiRegionAccessPoints() {
        return useMultiRegionAccessPoints;
    }
}
