/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
        public void setDefaults(OpenApiConfig config) {
        }
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
