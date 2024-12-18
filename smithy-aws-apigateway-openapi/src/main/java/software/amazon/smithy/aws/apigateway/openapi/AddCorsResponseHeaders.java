/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import software.amazon.smithy.jsonschema.Schema;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.traits.CorsTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.model.OperationObject;
import software.amazon.smithy.openapi.model.ParameterObject;
import software.amazon.smithy.openapi.model.Ref;
import software.amazon.smithy.openapi.model.ResponseObject;
import software.amazon.smithy.utils.ListUtils;

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
final class AddCorsResponseHeaders implements ApiGatewayMapper {

    private static final Logger LOGGER = Logger.getLogger(AddCorsResponseHeaders.class.getName());

    @Override
    public List<ApiGatewayConfig.ApiType> getApiTypes() {
        return ListUtils.of(ApiGatewayConfig.ApiType.REST);
    }

    @Override
    public OperationObject postProcessOperation(
            Context<? extends Trait> context,
            OperationShape shape,
            OperationObject operation,
            String method,
            String path
    ) {
        return context.getService()
                .getTrait(CorsTrait.class)
                .map(trait -> addCorsHeadersToResponses(context, shape, operation, method, trait))
                .orElse(operation);
    }

    private OperationObject addCorsHeadersToResponses(
            Context<? extends Trait> context,
            OperationShape shape,
            OperationObject operationObject,
            String method,
            CorsTrait trait
    ) {
        OperationObject.Builder builder = operationObject.toBuilder();

        for (Map.Entry<String, ResponseObject> entry : operationObject.getResponses().entrySet()) {
            ResponseObject updated = createUpdatedResponseWithCorsHeaders(
                    context,
                    shape,
                    operationObject,
                    method,
                    trait,
                    entry.getValue());
            builder.putResponse(entry.getKey(), updated);
        }

        return builder.build();
    }

    private ResponseObject createUpdatedResponseWithCorsHeaders(
            Context<? extends Trait> context,
            OperationShape operation,
            OperationObject operationObject,
            String method,
            CorsTrait trait,
            ResponseObject response
    ) {
        // Determine which headers have been added to the response.
        List<String> headers = new ArrayList<>();

        // Access-Control-Allow-Origin should be sent in response to a
        // "CORS request", and a CORS request is anything that includes an
        // Origin header. Even a preflight request could include an Origin
        // header, so it follows that this header should be sent in response
        // to preflight requests. The example Mozilla provides also seems to
        // draw this conclusion:
        //
        // > https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS
        //
        // OPTIONS /resources/post-here/ HTTP/1.1
        // Host: bar.other
        // User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10.14; rv:71.0) Gecko/20100101 Firefox/71.0
        // Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
        // Accept-Language: en-us,en;q=0.5
        // Accept-Encoding: gzip,deflate
        // Connection: keep-alive
        // Origin: http://foo.example
        // Access-Control-Request-Method: POST
        // Access-Control-Request-Headers: X-PINGOTHER, Content-Type
        //
        // HTTP/1.1 204 No Content
        // Date: Mon, 01 Dec 2008 01:15:39 GMT
        // Server: Apache/2
        // Access-Control-Allow-Origin: https://foo.example
        // Access-Control-Allow-Methods: POST, GET, OPTIONS
        // Access-Control-Allow-Headers: X-PINGOTHER, Content-Type
        // Access-Control-Max-Age: 86400
        // Vary: Accept-Encoding, Origin
        // Keep-Alive: timeout=2, max=100
        // Connection: Keep-Alive
        headers.add(CorsHeader.ALLOW_ORIGIN.toString());

        if (!method.equalsIgnoreCase("options")) {
            // From https://fetch.spec.whatwg.org/#http-responses 3.2.3:
            // An HTTP response to a CORS request that is *not* a
            // CORS-preflight request can also include the following header:
            // `Access-Control-Expose-Headers`
            if (!CorsHeader.deduceOperationResponseHeaders(context, operationObject, operation, trait).isEmpty()) {
                headers.add(CorsHeader.EXPOSE_HEADERS.toString());
            }

            // Access-Control-Allow-Credentials is only added if one of the
            // auth schemes of the service uses HTTP credentials such as cookies
            // or browser-managed usernames as specified in https://fetch.spec.whatwg.org/#credentials.
            //
            // Because the "credentials mode" is always "omit" when responding to a
            // preflight-request the Access-Control-Allow-Credentials header is omitted
            // for preflight responses. This is also what Mozilla illustrates in their
            // CORS examples:
            //
            // GET /resources/access-control-with-credentials/ HTTP/1.1
            // Host: bar.other
            // User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10.14; rv:71.0) Gecko/20100101 Firefox/71.0
            // Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
            // Accept-Language: en-us,en;q=0.5
            // Accept-Encoding: gzip,deflate
            // Connection: keep-alive
            // Referer: http://foo.example/examples/credential.html
            // Origin: http://foo.example
            // Cookie: pageAccess=2
            //
            // HTTP/1.1 200 OK
            // Date: Mon, 01 Dec 2008 01:34:52 GMT
            // Server: Apache/2
            // Access-Control-Allow-Origin: https://foo.example
            // Access-Control-Allow-Credentials: true
            // Cache-Control: no-cache
            // Pragma: no-cache
            // Set-Cookie: pageAccess=3; expires=Wed, 31-Dec-2008 01:34:53 GMT
            // Vary: Accept-Encoding, Origin
            // Content-Encoding: gzip
            // Content-Length: 106
            // Keep-Alive: timeout=2, max=100
            // Connection: Keep-Alive
            // Content-Type: text/plain
            // ...
            if (context.usesHttpCredentials()) {
                headers.add(CorsHeader.ALLOW_CREDENTIALS.toString());
            }
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
