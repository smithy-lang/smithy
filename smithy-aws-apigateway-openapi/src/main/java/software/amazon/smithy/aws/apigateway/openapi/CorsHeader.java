/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import java.util.Set;
import java.util.TreeSet;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.traits.CorsTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.model.OperationObject;
import software.amazon.smithy.openapi.model.ResponseObject;

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

    static <T extends Trait> Set<String> deduceOperationResponseHeaders(
            Context<T> context,
            OperationObject operationObject,
            OperationShape shape,
            CorsTrait cors
    ) {
        // The deduced response headers of an operation consist of any headers
        // returned by security schemes, any headers returned by the protocol,
        // and any headers explicitly modeled on the operation.
        Set<String> result = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        result.addAll(cors.getAdditionalExposedHeaders());
        result.addAll(context.getOpenApiProtocol().getProtocolResponseHeaders(context, shape));
        result.addAll(context.getAllSecuritySchemeResponseHeaders());

        // Include all headers found in the generated OpenAPI response.
        for (ResponseObject responseObject : operationObject.getResponses().values()) {
            result.addAll(responseObject.getHeaders().keySet());
        }

        return result;
    }
}
