/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.aws.smoketests.model;

import java.util.List;
import java.util.Optional;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * Base vendor params for all AWS services.
 */
public abstract class BaseAwsVendorParams {
    private static final String DEFAULT_REGION = "us-west-2";
    private static final boolean DEFAULT_USE_FIPS = false;
    private static final boolean DEFAULT_USE_DUALSTACK = false;
    private static final boolean DEFAULT_USE_ACCOUNT_ID_ROUTING = true;

    private final String region;
    private final List<String> sigv4aRegionSet;
    private final String uri;
    private final boolean useFips;
    private final boolean useDualstack;
    private final boolean useAccountIdRouting;

    BaseAwsVendorParams(ObjectNode node) {
        this.region = node.getStringMemberOrDefault("region", DEFAULT_REGION);
        this.sigv4aRegionSet = node.getArrayMember("sigv4aRegionSet")
                .map(a -> a.getElementsAs(el -> el.expectStringNode().getValue()))
                .orElse(null);
        this.uri = node.getStringMemberOrDefault("uri", null);
        this.useFips = node.getBooleanMemberOrDefault("useFips", DEFAULT_USE_FIPS);
        this.useDualstack = node.getBooleanMemberOrDefault("useDualstack", DEFAULT_USE_DUALSTACK);
        this.useAccountIdRouting = node.getBooleanMemberOrDefault(
                "useAccountIdRouting", DEFAULT_USE_ACCOUNT_ID_ROUTING);
    }

    /**
     * @return The AWS region to sign the request for and to resolve the default
     * endpoint with. Defaults to {@code us-west-2}.
     */
    public String getRegion() {
        return region;
    }

    /**
     * @return The set of regions to sign a sigv4a request with, if present.
     */
    public Optional<List<String>> getSigv4aRegionSet() {
        return Optional.ofNullable(sigv4aRegionSet);
    }

    /**
     * @return The static endpoint to send the request to, if present.
     */
    public Optional<String> getUri() {
        return Optional.ofNullable(uri);
    }

    /**
     * @return Whether to resolve a FIPS compliant endpoint or not. Defaults to
     * {@code false}.
     */
    public boolean useFips() {
        return useFips;
    }

    /**
     * @return Whether to resolve a dualstack endpoint or not. Defaults to
     * {@code false}.
     */
    public boolean useDualstack() {
        return useDualstack;
    }

    /**
     * @return Whether to use account ID based routing where applicable.
     * Defaults to {@code true}.
     */
    public boolean useAccountIdRouting() {
        return useAccountIdRouting;
    }
}
