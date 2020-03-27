/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.Set;
import java.util.TreeSet;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.traits.CorsTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.SecuritySchemeConverter;

enum CorsHeader {

    ALLOW_CREDENTIALS("Access-Control-Allow-Credentials"),
    ALLOW_HEADERS("Access-Control-Allow-Headers"),
    ALLOW_METHODS("Access-Control-Allow-Methods"),
    ALLOW_ORIGIN("Access-Control-Allow-Origin"),
    EXPOSE_HEADERS("Access-Control-Expose-Headers"),
    MAX_AGE("Access-Control-Max-Age");

    private final String headerName;

    CorsHeader(String headerName) {
        this.headerName = headerName;
    }

    @Override
    public String toString() {
        return headerName;
    }

    static <T extends Trait> Set<String> deduceOperationHeaders(
            Context<T> context,
            OperationShape shape,
            CorsTrait cors
    ) {
        // The deduced response headers of an operation consist of any headers
        // returned by security schemes, any headers returned by the protocol,
        // and any headers explicitly modeled on the operation.
        Set<String> result = new TreeSet<>(cors.getAdditionalExposedHeaders());
        result.addAll(context.getOpenApiProtocol().getProtocolResponseHeaders(context, shape));

        for (SecuritySchemeConverter<? extends Trait> converter : context.getSecuritySchemeConverters()) {
            result.addAll(getSecuritySchemeResponseHeaders(context, converter));
        }

        return result;
    }

    private static <T extends Trait> Set<String> getSecuritySchemeResponseHeaders(
            Context<? extends Trait> context,
            SecuritySchemeConverter<T> converter
    ) {
        T t = context.getService().expectTrait(converter.getAuthSchemeType());
        return converter.getAuthResponseHeaders(t);
    }
}
