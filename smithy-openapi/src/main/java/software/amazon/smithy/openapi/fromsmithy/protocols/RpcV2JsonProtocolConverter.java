/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy.protocols;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import software.amazon.smithy.jsonschema.Schema;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.OpenApiProtocol;
import software.amazon.smithy.openapi.model.MediaTypeObject;
import software.amazon.smithy.openapi.model.OperationObject;
import software.amazon.smithy.openapi.model.RequestBodyObject;
import software.amazon.smithy.openapi.model.ResponseObject;
import software.amazon.smithy.protocol.traits.Rpcv2JsonTrait;
import software.amazon.smithy.utils.SetUtils;

/**
 * Converts the {@code smithy.protocols#rpcv2Json} protocol to OpenAPI.
 *
 * <p>Each operation is mapped to a POST request with a path of
 * {@code /service/{serviceName}/operation/{operationName}}, where
 * {@code serviceName} is the service shape name (without namespace).
 */
public final class RpcV2JsonProtocolConverter implements OpenApiProtocol<Rpcv2JsonTrait> {
    private static final String STATUS_CODE = "200";
    private static final String CONTENT_TYPE = "application/json";

    private static final Set<String> REQUEST_HEADERS = SetUtils.of(
            // Since APIGW doesn't support event streaming, apply the 3 protocol headers.
            "Smithy-Protocol",
            "Content-Type",
            "Content-Length",
            // Used by clients for a purpose similar to the standard user-agent header.
            "X-Amz-User-Agent",
            // Used by clients configured to work with X-Ray.
            "X-Amzn-Trace-Id",
            // Used by clients for adaptive retry behavior.
            "Amz-Sdk-Request",
            "Amz-Sdk-Invocation-Id");

    private static final Set<String> RESPONSE_HEADERS = SetUtils.of(
            // Since APIGW doesn't support event streaming, apply the 3 protocol headers.
            "Smithy-Protocol",
            "Content-Type",
            "Content-Length",
            // Used to identify a given request/response, primarily for debugging.
            "X-Amzn-Requestid");

    @Override
    public Class<Rpcv2JsonTrait> getProtocolType() {
        return Rpcv2JsonTrait.class;
    }

    @Override
    public void updateDefaultSettings(Model model, OpenApiConfig config) {
        config.setUseStringsForArbitraryPrecision(true);
    }

    /**
     * Each operation will have a separate path in the format /service/{serviceName}/operation/{operationName}
     */
    @Override
    public Optional<OpenApiProtocol.Operation> createOperation(
            Context<Rpcv2JsonTrait> context,
            OperationShape operation
    ) {
        OperationObject.Builder builder = OperationObject.builder()
                .operationId(context.getService().getContextualName(operation));
        createRequestBody(context, operation).ifPresent(builder::requestBody);
        createResponseBody(context, operation).forEach(builder::putResponse);
        return Optional.of(OpenApiProtocol.Operation.create(
                getOperationMethod(context, operation),
                getOperationUri(context, operation),
                builder));
    }

    @Override
    public String getOperationResponseStatusCode(Context<Rpcv2JsonTrait> context, ToShapeId operationOrError) {
        if (context.getModel().expectShape(operationOrError.toShapeId()).isOperationShape()) {
            return STATUS_CODE;
        }
        return OpenApiProtocol.super.getOperationResponseStatusCode(context, operationOrError);
    }

    @Override
    public String getOperationMethod(Context<Rpcv2JsonTrait> context, OperationShape operation) {
        return "POST";
    }

    @Override
    public String getOperationUri(Context<Rpcv2JsonTrait> context, OperationShape operation) {
        return "/service/" + context.getService().getId().getName()
                + "/operation/" + context.getService().getContextualName(operation);
    }

    @Override
    public Set<String> getProtocolRequestHeaders(Context<Rpcv2JsonTrait> context, OperationShape operationShape) {
        return REQUEST_HEADERS;
    }

    @Override
    public Set<String> getProtocolResponseHeaders(Context<Rpcv2JsonTrait> context, OperationShape operationShape) {
        return RESPONSE_HEADERS;
    }

    private Map<String, ResponseObject> createResponseBody(
            Context<Rpcv2JsonTrait> context,
            OperationShape operation
    ) {
        Map<String, ResponseObject> result = new TreeMap<>();

        if (operation.getOutput().isPresent()) {
            result.put(STATUS_CODE,
                    buildResponseObject(context,
                            operation.getOutputShape(),
                            operation,
                            "ResponseContent"));

            OperationIndex operationIndex = OperationIndex.of(context.getModel());
            for (StructureShape error : operationIndex.getErrors(operation)) {
                String errorCode = context.getOpenApiProtocol().getOperationResponseStatusCode(context, error);
                result.put(errorCode, buildResponseObject(context, error.toShapeId(), error, "ErrorContent"));
            }
        }
        return result;
    }

    private ResponseObject buildResponseObject(
            Context<Rpcv2JsonTrait> context,
            ShapeId shape,
            Shape operationOrError,
            String suffix
    ) {
        Schema schema = convertToSchema(context, shape);
        MediaTypeObject mediaTypeObject = createMediaTypeObject(context, schema, operationOrError, suffix);

        return ResponseObject.builder()
                .description("Response Object")
                .putContent(CONTENT_TYPE, mediaTypeObject)
                .build();
    }

    private Optional<RequestBodyObject> createRequestBody(
            Context<Rpcv2JsonTrait> context,
            OperationShape operation
    ) {
        if (operation.getInput().isPresent()) {
            Schema schema = convertToSchema(context, operation.getInputShape());
            MediaTypeObject mediaTypeObject = createMediaTypeObject(context, schema, operation, "RequestContent");

            return Optional.of(RequestBodyObject.builder()
                    .description("Request Object")
                    .putContent(CONTENT_TYPE, mediaTypeObject)
                    .build());
        }
        return Optional.empty();
    }

    private MediaTypeObject createMediaTypeObject(
            Context<Rpcv2JsonTrait> context,
            Schema schema,
            Shape operationOrError,
            String suffix
    ) {
        return getMediaTypeObject(context, schema, operationOrError, shape -> {
            String shapeName = context.getService().getContextualName(shape.getId());
            return shapeName + suffix;
        });
    }

    private MediaTypeObject getMediaTypeObject(
            Context<Rpcv2JsonTrait> context,
            Schema schema,
            Shape shape,
            Function<Shape, String> createSynthesizedName
    ) {
        String synthesizedName = createSynthesizedName.apply(shape);
        String pointer = context.putSynthesizedSchema(synthesizedName, schema);
        return MediaTypeObject.builder()
                .schema(Schema.builder().ref(pointer).build())
                .build();
    }

    private Schema convertToSchema(Context<Rpcv2JsonTrait> context, ShapeId shape) {
        StructureShape containerShape = context.getModel().expectShape(shape, StructureShape.class);
        return context.getJsonSchemaConverter()
                .convertShape(containerShape)
                .getRootSchema();
    }
}
