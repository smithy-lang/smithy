/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SmithyUnstableApi;

public enum ApiGatewayDefaults {
    VERSION_2023_08_11("2023-08-11") {
        @Override
        public void setDefaults(OpenApiConfig config) {
            config.setAlphanumericOnlyRefs(true);
            config.setDisableDefaultValues(true);
            config.setDisableIntegerFormat(true);
            config.setDisableFeatures(SetUtils.of("default"));
        }
    },
    DISABLED("DISABLED") {
        @Override
        public void setDefaults(OpenApiConfig config) {}
    };

    private final String version;

    ApiGatewayDefaults(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return version;
    }

    @SmithyUnstableApi
    public abstract void setDefaults(OpenApiConfig config);
}
