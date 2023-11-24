/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import software.amazon.smithy.utils.SetUtils;

/**
 * API Gateway OpenAPI configuration.
 */
public final class ApiGatewayConfig {

    /**
     * The type of API Gateway service to generate.
     */
    public enum ApiType {
        /**
         * Generates an OpenAPI model for an API Gateway "REST" API.
         *
         * <p>This is the default type of OpenAPI model to generate when not
         * specified.
         *
         * @see <a href="https://docs.aws.amazon.com/apigateway/latest/developerguide/apigateway-rest-api.html">REST API</a>
         */
        REST,

        /**
         * Generates an OpenAPI model for an API Gateway "HTTP" API.
         *
         * <p>This converter does not currently support for HTTP APIs.
         *
         * @see <a href="https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api.html">HTTP API</a>
         */
        HTTP,

        /**
         * Disables API Gateway OpenAPI plugins.
         */
        DISABLED
    }

    private ApiType apiGatewayType = ApiType.REST;
    private boolean disableCloudFormationSubstitution;
    private Set<String> additionalAllowedCorsHeaders = Collections.emptySet();
    private ApiGatewayDefaults apiGatewayDefaults;

    /**
     * @return Returns true if CloudFormation substitutions are disabled.
     */
    public boolean getDisableCloudFormationSubstitution() {
        return disableCloudFormationSubstitution;
    }

    /**
     * Disables CloudFormation substitutions of specific paths when they contain
     * ${} placeholders. When found, these are expanded into CloudFormation Fn::Sub
     * intrinsic functions.
     *
     * @param disableCloudFormationSubstitution Set to true to disable intrinsics.
     */
    public void setDisableCloudFormationSubstitution(boolean disableCloudFormationSubstitution) {
        this.disableCloudFormationSubstitution = disableCloudFormationSubstitution;
    }

    /**
     * @return the type of API Gateway API to generate.
     */
    public ApiType getApiGatewayType() {
        return apiGatewayType;
    }

    /**
     * Sets the type of API Gateway API to generate.
     *
     * <p>If not set, this value defaults to a "REST" API.
     *
     * @param apiGatewayType API type to set.
     */
    public void setApiGatewayType(ApiType apiGatewayType) {
        this.apiGatewayType = Objects.requireNonNull(apiGatewayType);
    }

    /**
     * @deprecated Use {@link ApiGatewayConfig#getAdditionalAllowedCorsHeadersSet}
     */
    @Deprecated
    public List<String> getAdditionalAllowedCorsHeaders() {
        return new ArrayList<>(additionalAllowedCorsHeaders);
    }

    /**
     * @return the set of additional allowed CORS headers.
     */
    public Set<String> getAdditionalAllowedCorsHeadersSet() {
        return additionalAllowedCorsHeaders;
    }

    /**
     * Sets the additional allowed CORS headers.
     *
     * @param additionalAllowedCorsHeaders additional cors headers to be allowed.
     */
    public void setAdditionalAllowedCorsHeaders(Collection<String> additionalAllowedCorsHeaders) {
        this.additionalAllowedCorsHeaders = SetUtils.caseInsensitiveCopyOf(additionalAllowedCorsHeaders);
    }

    public ApiGatewayDefaults getApiGatewayDefaults() {
        return this.apiGatewayDefaults;
    }

    public void setApiGatewayDefaults(ApiGatewayDefaults apiGatewayDefaults) {
        this.apiGatewayDefaults = apiGatewayDefaults;
    }
}
