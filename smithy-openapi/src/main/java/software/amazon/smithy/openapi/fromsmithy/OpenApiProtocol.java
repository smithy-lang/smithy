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

package software.amazon.smithy.openapi.fromsmithy;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.openapi.model.OperationObject;

/**
 * An OpenAPI protocol is used to convert a Smithy protocol into
 * HTTP-specific bindings.
 *
 * <p>Instances of {@code OpenApiProtocol} are discovered using SPI and
 * matched with configuration settings based on the result of matching
 * a protocol against {@link #getProtocolNamePattern()}.
 */
public interface OpenApiProtocol {
    /**
     * Gets a pattern used to match protocol names.
     *
     * @return Returns the protocol name pattern.
     */
    Pattern getProtocolNamePattern();

    /**
     * Creates an operation entry, including the method, URI, and operation
     * object builder.
     *
     * <p>The operation is returned as an empty Optional if the operation is
     * not supported by the protocol.
     *
     * @param context The build context.
     * @param operation The operation shape to create.
     * @return Returns the optionally created operation entry.
     */
    Optional<Operation> createOperation(Context context, OperationShape operation);

    /**
     * Gets the unmodeled protocol-specific HTTP headers of a request that are
     * considered significant for the provided operation.
     *
     * <p>These protocol specific headers are not automatically added to
     * requests, but are used when integrating with things like CORS.
     *
     * @param context OpenAPI context
     * @param operationShape Smithy operation
     * @return Returns a set of header names.
     */
    default Set<String> getProtocolRequestHeaders(Context context, OperationShape operationShape) {
        return Set.of();
    }

    /**
     * Gets the unmodeled protocol-specific HTTP headers of a response that
     * are considered significant for the provided operation.
     *
     * <p>These protocol specific headers are not automatically added to
     * responses, but are used when integrating with things like CORS.
     *
     * @param context OpenAPI context
     * @param operationShape Smithy operation
     * @return Returns a set of header names.
     */
    default Set<String> getProtocolResponseHeaders(Context context, OperationShape operationShape) {
        return Set.of();
    }

    /**
     * Represents an operation entry to add to an {@link OpenApi.Builder}.
     */
    final class Operation {
        private final String method;
        private final String uri;
        private final OperationObject.Builder operation;

        private Operation(String method, String uri, OperationObject.Builder operation) {
            this.method = method;
            this.uri = uri;
            this.operation = operation;
        }

        /**
         * Creates a new operation entry.
         *
         * @param method HTTP method used for the operation.
         * @param uri HTTP URI of the operation.
         * @param operation Operation builder to return.
         * @return Returns the created Operation entry.
         */
        public static Operation create(String method, String uri, OperationObject.Builder operation) {
            return new Operation(method, uri, operation);
        }

        /**
         * @return Gets the HTTP method.
         */
        public String getMethod() {
            return method;
        }

        /**
         * @return Gets the HTTP URI.
         */
        public String getUri() {
            return uri;
        }

        /**
         * @return Gets the OperationOperation builder.
         */
        public OperationObject.Builder getOperation() {
            return operation;
        }
    }
}
