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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import software.amazon.smithy.jsonschema.Schema;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.traits.CorsTrait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.OpenApiMapper;
import software.amazon.smithy.openapi.model.ParameterObject;
import software.amazon.smithy.openapi.model.Ref;
import software.amazon.smithy.openapi.model.ResponseObject;

/**
 * Adds CORS-specific headers to every response in the API.
 *
 * <p>Values will be provided for the headers added to each operation by updating
 * the API Gateway integration of each operation to provide a static value for
 * each CORS header. This mapping is performed in {@link AddIntegrations}.
 *
 * <p>This extension only takes effect if the service being converted to
 * OpenAPI has the CORS trait.
 */
final class AddCorsResponseHeaders implements OpenApiMapper {
    private static final Logger LOGGER = Logger.getLogger(AddCorsResponseHeaders.class.getName());

    @Override
    public ResponseObject updateResponse(
            Context context, String status, OperationShape shape, ResponseObject response) {
        return context.getService().getTrait(CorsTrait.class)
                .map(corsTrait -> addCorsHeadersToResponse(context, shape, response, corsTrait))
                .orElse(response);
    }

    private ResponseObject addCorsHeadersToResponse(
            Context context, OperationShape operation, ResponseObject response, CorsTrait corsTrait) {
        // Determine which headers have been added to the response.
        List<String> headers = new ArrayList<>();
        headers.add(CorsHeader.ALLOW_ORIGIN.toString());
        if (!CorsHeader.deduceOperationHeaders(context, operation, corsTrait).isEmpty()) {
            headers.add(CorsHeader.EXPOSE_HEADERS.toString());
        }

        // Only add the "Access-Control-Allow-Credentials" header if one of the authentications schemes
        // of the service uses HTTP credentials such as cookies or browser-managed usernames as specified
        // in https://fetch.spec.whatwg.org/#credentials.
        if (context.usesHttpCredentials()) {
            headers.add(CorsHeader.ALLOW_CREDENTIALS.toString());
        }

        LOGGER.finer(() -> String.format("Adding CORS headers to `%s` response: %s", operation.getId(), headers));

        ResponseObject.Builder builder = response.toBuilder();
        Schema headerSchema = Schema.builder().type("string").build();

        // Inject the header parameters into the response IFF the header does not already exist.
        for (String headerName : headers) {
            if (!response.getHeader(headerName).isPresent()) {
                ParameterObject headerParam = ParameterObject.builder()
                        .schema(headerSchema)
                        .build();
                builder.putHeader(headerName, Ref.local(headerParam));
            }
        }

        return builder.build();
    }
}
