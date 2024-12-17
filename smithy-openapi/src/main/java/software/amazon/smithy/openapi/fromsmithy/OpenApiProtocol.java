/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy;

import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.HttpBindingIndex;
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.OpenApiException;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.openapi.model.OperationObject;
import software.amazon.smithy.utils.SetUtils;

/**
 * An OpenAPI protocol is used to convert a Smithy protocol into
 * HTTP-specific bindings.
 *
 * <p>Instances of {@code OpenApiProtocol} are discovered using SPI and
 * matched with configuration settings based on the result of matching
 * a protocol against {@link #getProtocolType()}.
 *
 * @param <T> Type of Smithy protocol to convert.
 */
public interface OpenApiProtocol<T extends Trait> {
    /**
     * Gets the protocol type that this converter handles.
     *
     * @return Returns the shape ID.
     */
    Class<T> getProtocolType();

    /**
     * Sets protocol-specific default values on the OpenAPI configuration
     * object.
     *
     * @param model Model being converted.
     * @param config Configuration object to modify.
     */
    default void updateDefaultSettings(Model model, OpenApiConfig config) {}

    /**
     * Creates an operation entry, including the method, URI, and operation
     * object builder.
     *
     * The returned operation object builder should contain protocol-specific fields of an
     * OpenAPI Operation object, such as: [parameters, requestBody, responses] and [examples for any of the above,
     * contained by respective objects that the example values are for].
     * The returned operation object builder should not contain protocol-agnostic fields of an OpenAPI
     * Operation object, such as: tags, summary, description, externalDocs, deprecated, security.
     *
     * <p>The operation is returned as an empty Optional if the operation is
     * not supported by the protocol. This method should make calls to
     * {@link #getOperationUri} and {@link #getOperationMethod} when creating
     * the Operation object.
     *
     * @param context The build context.
     * @param operation The operation shape to create.
     * @return Returns the optionally created operation entry.
     */
    Optional<Operation> createOperation(Context<T> context, OperationShape operation);

    /**
     * Gets the URI of an operation.
     *
     * <p>The default implementation will attempt to get the HTTP URI
     * defined by the {@link HttpTrait} trait. If no HTTP trait can be
     * found, the default implementation will throw an exception.
     *
     * @param context The build context.
     * @param operation The operation to get the URI of.
     * @return Returns the operation URI.
     */
    default String getOperationUri(Context<T> context, OperationShape operation) {
        return operation.getTrait(HttpTrait.class)
                .map(HttpTrait::getUri)
                .map(UriPattern::toString)
                .orElseThrow(() -> new OpenApiException(
                        "The `" + operation.getId() + "` operation has no `http` binding trait, which is "
                                + "required to compute a URI (using the default protocol implementation)"));
    }

    /**
     * Gets the HTTP method of an operation.
     *
     * <p>The default implementation will attempt to get the HTTP method
     * defined by the {@link HttpTrait} trait. If no HTTP trait can be
     * found, the default implementation will throw an exception.
     *
     * @param context The build context.
     * @param operation The operation to get the method of.
     * @return Returns the method.
     */
    default String getOperationMethod(Context<T> context, OperationShape operation) {
        return operation.getTrait(HttpTrait.class)
                .map(HttpTrait::getMethod)
                .orElseThrow(() -> new OpenApiException(
                        "The `" + operation.getId() + "` operation has no `http` binding trait, which is "
                                + "required to compute a method (using the default protocol implementation)"));
    }

    /**
     * Gets the response status code of an operation or error shape.
     *
     * <p>The default implementation will attempt to use HTTP binding traits
     * to determine the HTTP status code of an operation or error structure.
     *
     * @param context The build context.
     * @param operationOrError Operation or error shape ID.
     * @return Returns the status code as a string.
     */
    default String getOperationResponseStatusCode(Context<T> context, ToShapeId operationOrError) {
        return String.valueOf(HttpBindingIndex.of(context.getModel()).getResponseCode(operationOrError));
    }

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
    default Set<String> getProtocolRequestHeaders(Context<T> context, OperationShape operationShape) {
        return SetUtils.of();
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
    default Set<String> getProtocolResponseHeaders(Context<T> context, OperationShape operationShape) {
        return SetUtils.of();
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
