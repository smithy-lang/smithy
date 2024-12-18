/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.smoketests.model;

import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.smoketests.traits.SmokeTestCase;

/**
 * Provides methods for interfacing with Java representations of the different
 * kinds of vendor params shapes used in smoke tests for AWS services.
 */
public final class AwsSmokeTestModel {
    private AwsSmokeTestModel() {}

    /**
     * @param testCase The test case to check.
     * @return {@code true} if the {@code testCase}'s {@link SmokeTestCase#getVendorParams()}
     * are {@link AwsVendorParams}.
     */
    public static boolean hasAwsVendorParams(SmokeTestCase testCase) {
        return testCase.getVendorParamsShape()
                .filter(AwsVendorParams.ID::equals)
                .isPresent();
    }

    /**
     * @param testCase The test case to check.
     * @return {@code true} if the {@code testCase}'s {@link SmokeTestCase#getVendorParams()}
     * are {@link S3VendorParams}.
     */
    public static boolean hasS3VendorParams(SmokeTestCase testCase) {
        return testCase.getVendorParamsShape()
                .filter(S3VendorParams.ID::equals)
                .isPresent();
    }

    /**
     * Gets the vendor params for the given {@code forTestCase} as {@link AwsVendorParams}.
     *
     * <p>The vendor params will be present if {@link #hasAwsVendorParams(SmokeTestCase)}
     * was {@code true} for the given {@code forTestCase}.
     *
     * @param forTestCase The test case to get vendor params for.
     * @return The optionally present vendor params as {@link S3VendorParams}.
     */
    public static Optional<AwsVendorParams> getAwsVendorParams(SmokeTestCase forTestCase) {
        if (!hasAwsVendorParams(forTestCase)) {
            return Optional.empty();
        }

        ObjectNode vendorParams = forTestCase.getVendorParams().orElse(Node.objectNode());
        return Optional.of(new AwsVendorParams(vendorParams));
    }

    /**
     * Gets the vendor params for the given {@code forTestCase} as {@link S3VendorParams}.
     *
     * <p>The vendor params will be present if {@link #hasS3VendorParams(SmokeTestCase)}
     * was {@code true} for the given {@code forTestCase}.
     *
     * @param forTestCase The test case to get vendor params for.
     * @return The optionally present vendor params as {@link S3VendorParams}.
     */
    public static Optional<S3VendorParams> getS3VendorParams(SmokeTestCase forTestCase) {
        if (!hasS3VendorParams(forTestCase)) {
            return Optional.empty();
        }

        ObjectNode vendorParams = forTestCase.getVendorParams().orElse(Node.objectNode());
        return Optional.of(new S3VendorParams(vendorParams));
    }
}
