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

package software.amazon.smithy.openapi.fromsmithy.protocols;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.jsonschema.Schema;
import software.amazon.smithy.model.knowledge.HttpBinding;
import software.amazon.smithy.model.knowledge.HttpBindingIndex;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.shapes.BlobShape;
import software.amazon.smithy.model.shapes.CollectionShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.SetShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.MediaTypeTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.OpenApiProtocol;
import software.amazon.smithy.openapi.model.MediaTypeObject;
import software.amazon.smithy.openapi.model.OperationObject;
import software.amazon.smithy.openapi.model.ParameterObject;
import software.amazon.smithy.openapi.model.Ref;
import software.amazon.smithy.openapi.model.RequestBodyObject;
import software.amazon.smithy.openapi.model.ResponseObject;
import software.amazon.smithy.utils.OptionalUtils;
import software.amazon.smithy.utils.Pair;

/**
 * Provides the shared functionality used across protocols that use Smithy's
 * HTTP binding traits.
 *
 * <p>This class handles adding query string, path, header, payload, and
 * document bodies to HTTP messages using an {@link HttpBindingIndex}.
 * Inline schemas as created for query string, headers, and path
 * parameters that do not utilize the correct types or set an explicit
 * type/format (for example, this class ensures that a timestamp shape
 * serialized in the query string is serialized using the date-time
 * format).
 *
 * <p>This class is currently package-private, but may be made public in the
 * future when we're sure about its API.
 */
abstract class AbstractRestProtocol<T extends Trait> implements OpenApiProtocol<T> {

    /** The type of message being created. */
    enum MessageType { REQUEST, RESPONSE, ERROR }

    /**
     * Gets the media type of a document sent in a request or response.
     *
     * <p>This method may be invoked even for operations that do not send a
     * document payload, and in these cases, this method should return a
     * {@code String} and not throw.
     *
     * @param context Conversion context.
     * @param operationOrError Operation shape or error shape.
     * @param messageType The type of message being created (request, response, or error).
     * @return Returns the media type of the document payload.
     */
    abstract String getDocumentMediaType(Context<T> context, Shape operationOrError, MessageType messageType);

    /**
     * Creates a schema to send a document payload in the request,
     * response, or error of an operation.
     *
     * @param context Conversion context.
     * @param operationOrError Operation shape or error shape.
     * @param bindings HTTP bindings of this shape.
     * @param messageType The message type (request, response, or error).
     * @return Returns the created document schema.
     */
    abstract Schema createDocumentSchema(
            Context<T> context,
            Shape operationOrError,
            List<HttpBinding> bindings,
            MessageType messageType
    );

    @Override
    public Optional<Operation> createOperation(Context<T> context, OperationShape operation) {
        return operation.getTrait(HttpTrait.class).map(httpTrait -> {
            String method = context.getOpenApiProtocol().getOperationMethod(context, operation);
            String uri = context.getOpenApiProtocol().getOperationUri(context, operation);
            OperationObject.Builder builder = OperationObject.builder().operationId(operation.getId().getName());
            HttpBindingIndex bindingIndex = context.getModel().getKnowledge(HttpBindingIndex.class);
            createPathParameters(context, operation).forEach(builder::addParameter);
            createQueryParameters(context, operation).forEach(builder::addParameter);
            createRequestHeaderParameters(context, operation).forEach(builder::addParameter);
            createRequestBody(context, bindingIndex, operation).ifPresent(builder::requestBody);
            createResponses(context, bindingIndex, operation).forEach(builder::putResponse);
            return Operation.create(method, uri, builder);
        });
    }

    private List<ParameterObject> createPathParameters(Context<T> context, OperationShape operation) {
        return context.getModel().getKnowledge(HttpBindingIndex.class)
                .getRequestBindings(operation, HttpBinding.Location.LABEL).stream()
                .map(binding -> {
                    MemberShape member = binding.getMember();
                    Schema schema = context.createRef(binding.getMember());
                    ParameterObject.Builder paramBuilder = ModelUtils.createParameterMember(
                            context, member).in("path");
                    // Timestamps sent in the URI are serialized as a date-time string by default.
                    if (needsInlineSchema(context, member)) {
                        // Create a copy of the targeted schema and remove any possible numeric keywords.
                        Schema.Builder copiedBuilder = ModelUtils.convertSchemaToStringBuilder(
                                context.getSchema(context.getPointer(member)));
                        schema = copiedBuilder.format("date-time").build();
                    }
                    return paramBuilder.schema(schema).build();
                })
                .collect(Collectors.toList());
    }

    private boolean needsInlineSchema(Context<? extends Trait> context, MemberShape member) {
        if (member.getMemberTrait(context.getModel(), TimestampFormatTrait.class).isPresent()) {
            return false;
        }

        return context.getModel()
                .getShape(member.getTarget())
                .filter(Shape::isTimestampShape)
                .isPresent();
    }

    private List<ParameterObject> createQueryParameters(Context<T> context, OperationShape operation) {
        return context.getModel().getKnowledge(HttpBindingIndex.class)
                .getRequestBindings(operation, HttpBinding.Location.QUERY).stream()
                .map(binding -> {
                    ParameterObject.Builder param = ModelUtils.createParameterMember(context, binding.getMember())
                            .in("query")
                            .name(binding.getLocationName());
                    Shape target = context.getModel().expectShape(binding.getMember().getTarget());

                    // List and set shapes in the query string are repeated, so we need to "explode" them.
                    if (target instanceof CollectionShape) {
                        param.style("form").explode(true);
                    }

                    Schema refSchema = context.createRef(binding.getMember());
                    param.schema(target.accept(new QuerySchemaVisitor<>(context, refSchema, binding.getMember())));
                    return param.build();
                })
                .collect(Collectors.toList());
    }

    private Collection<ParameterObject> createRequestHeaderParameters(Context<T> context, OperationShape operation) {
        List<HttpBinding> bindings = context.getModel().getKnowledge(HttpBindingIndex.class)
                .getRequestBindings(operation, HttpBinding.Location.HEADER);
        return createHeaderParameters(context, bindings, MessageType.REQUEST).values();
    }

    private Map<String, ParameterObject> createHeaderParameters(
            Context<T> context,
            List<HttpBinding> bindings,
            MessageType messageType
    ) {
        return bindings.stream()
                .map(binding -> {
                    ParameterObject.Builder param = ModelUtils.createParameterMember(context, binding.getMember());
                    if (messageType == MessageType.REQUEST) {
                        param.in("header").name(binding.getLocationName());
                    } else {
                        // Response headers don't use "in" or "name".
                        param.in(null).name(null);
                    }
                    Shape target = context.getModel().expectShape(binding.getMember().getTarget());
                    Schema refSchema = context.createRef(binding.getMember());
                    param.schema(target.accept(new HeaderSchemaVisitor<>(context, refSchema, binding.getMember())));
                    return Pair.of(binding.getLocationName(), param.build());
                })
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }

    private Optional<RequestBodyObject> createRequestBody(
            Context<T> context,
            HttpBindingIndex bindingIndex,
            OperationShape operation
    ) {
        List<HttpBinding> payloadBindings = bindingIndex.getRequestBindings(
                operation, HttpBinding.Location.PAYLOAD);
        String documentMediaType = getDocumentMediaType(context, operation, MessageType.REQUEST);
        String mediaType = bindingIndex.determineRequestContentType(operation, documentMediaType).orElse(null);
        return payloadBindings.isEmpty()
               ? createRequestDocument(mediaType, context, bindingIndex, operation)
               : createRequestPayload(mediaType, context, payloadBindings.get(0));
    }

    private Optional<RequestBodyObject> createRequestPayload(
            String mediaTypeRange,
            Context<T> context,
            HttpBinding binding
    ) {
        MediaTypeObject mediaTypeObject = MediaTypeObject.builder()
                .schema(context.createRef(binding.getMember()))
                .build();
        RequestBodyObject requestBodyObject = RequestBodyObject.builder()
                .putContent(Objects.requireNonNull(mediaTypeRange), mediaTypeObject)
                .build();
        return Optional.of(requestBodyObject);
    }

    private Optional<RequestBodyObject> createRequestDocument(
            String mediaType,
            Context<T> context,
            HttpBindingIndex bindingIndex,
            OperationShape operation
    ) {
        List<HttpBinding> bindings = bindingIndex.getRequestBindings(
                operation, HttpBinding.Location.DOCUMENT);
        if (bindings.isEmpty()) {
            return Optional.empty();
        }

        Schema schema = createDocumentSchema(context, operation, bindings, MessageType.REQUEST);
        return Optional.of(RequestBodyObject.builder()
                .putContent(mediaType, MediaTypeObject.builder().schema(schema).build())
                .build());
    }

    private Map<String, ResponseObject> createResponses(
            Context<T> context,
            HttpBindingIndex bindingIndex,
            OperationShape operation
    ) {
        OperationIndex operationIndex = context.getModel().getKnowledge(OperationIndex.class);

        // Add the successful response and errors.
        return Stream.concat(
                OptionalUtils.stream(operationIndex.getOutput(operation)),
                operationIndex.getErrors(operation).stream()
        ).map(shape -> {
            Shape operationOrError = shape.hasTrait(ErrorTrait.class) ? shape : operation;
            String statusCode = context.getOpenApiProtocol().getOperationResponseStatusCode(context, operationOrError);
            ResponseObject response = createResponse(context, bindingIndex, statusCode, operation, operationOrError);
            return Pair.of(statusCode, response);
        }).collect(Collectors.toMap(Pair::getLeft, Pair::getRight, (a, b) -> b, LinkedHashMap::new));
    }

    private ResponseObject createResponse(
            Context<T> context,
            HttpBindingIndex bindingIndex,
            String statusCode,
            OperationShape operationShape,
            Shape operationOrError
    ) {
        ResponseObject.Builder responseBuilder = ResponseObject.builder();
        responseBuilder.description(String.format("%s %s response", operationOrError.getId().getName(), statusCode));
        createResponseHeaderParameters(context, operationShape)
                .forEach((k, v) -> responseBuilder.putHeader(k, Ref.local(v)));
        addResponseContent(context, bindingIndex, responseBuilder, operationOrError);
        return responseBuilder.build();
    }

    private Map<String, ParameterObject> createResponseHeaderParameters(
            Context<T> context,
            OperationShape operation
    ) {
        List<HttpBinding> bindings = context.getModel().getKnowledge(HttpBindingIndex.class)
                .getResponseBindings(operation, HttpBinding.Location.HEADER);
        return createHeaderParameters(context, bindings, MessageType.RESPONSE);
    }

    private void addResponseContent(
            Context<T> context,
            HttpBindingIndex bindingIndex,
            ResponseObject.Builder responseBuilder,
            Shape operationOrError
    ) {
        List<HttpBinding> payloadBindings = bindingIndex.getResponseBindings(
                operationOrError, HttpBinding.Location.PAYLOAD);
        String documentMediaType = getDocumentMediaType(context, operationOrError, MessageType.RESPONSE);
        String mediaType = bindingIndex.determineResponseContentType(operationOrError, documentMediaType).orElse(null);
        if (!payloadBindings.isEmpty()) {
            createResponsePayload(mediaType, context, payloadBindings.get(0), responseBuilder);
        } else {
            createResponseDocument(mediaType, context, bindingIndex, responseBuilder, operationOrError);
        }
    }

    private void createResponsePayload(
            String mediaType,
            Context<T> context,
            HttpBinding binding,
            ResponseObject.Builder responseBuilder
    ) {
        responseBuilder.putContent(mediaType, MediaTypeObject
                .builder()
                .schema(context.createRef(binding.getMember()))
                .build());
    }

    private void createResponseDocument(
            String mediaType,
            Context<T> context,
            HttpBindingIndex bindingIndex,
            ResponseObject.Builder responseBuilder,
            Shape operationOrError
    ) {
        List<HttpBinding> bindings = bindingIndex.getResponseBindings(
                operationOrError, HttpBinding.Location.DOCUMENT);
        if (!bindings.isEmpty()) {
            MessageType messageType = operationOrError instanceof OperationShape
                    ? MessageType.RESPONSE
                    : MessageType.ERROR;
            Schema schema = createDocumentSchema(context, operationOrError, bindings, messageType);
            responseBuilder.putContent(mediaType, MediaTypeObject.builder().schema(schema).build());
        }
    }

    private static final class QuerySchemaVisitor<T extends Trait> extends ShapeVisitor.Default<Schema> {
        private Context<T> context;
        private Schema schema;
        private MemberShape member;

        private QuerySchemaVisitor(Context<T> context, Schema schema, MemberShape member) {
            this.context = context;
            this.schema = schema;
            this.member = member;
        }

        @Override
        protected Schema getDefault(Shape shape) {
            return schema;
        }

        @Override
        public Schema listShape(ListShape shape) {
            return collection(shape);
        }

        @Override
        public Schema setShape(SetShape shape) {
            return collection(shape);
        }

        private Schema collection(CollectionShape collection) {
            Shape memberTarget = context.getModel().expectShape(collection.getMember().getTarget());
            String memberPointer = context.getPointer(collection.getMember());
            Schema currentMemberSchema = context.getSchema(memberPointer);
            QuerySchemaVisitor<T> visitor = new QuerySchemaVisitor<>(
                    context, currentMemberSchema, collection.getMember());
            Schema newMemberSchema = memberTarget.accept(visitor);
            return schema.toBuilder().ref(null).type("array").items(newMemberSchema).build();
        }

        @Override
        public Schema timestampShape(TimestampShape shape) {
            // Query string timestamps in Smithy are date-time strings by default
            // unless overridden by the timestampFormat trait. This code grabs the
            // referenced shape and creates an inline schema that explicitly defines
            // the necessary styles.
            if (member.hasTrait(TimestampFormatTrait.class)) {
                return schema;
            }

            Schema.Builder copiedBuilder = ModelUtils.convertSchemaToStringBuilder(
                    context.getSchema(context.getPointer(member)));
            return copiedBuilder.format("date-time").build();
        }

        @Override
        public Schema blobShape(BlobShape shape) {
            // Query string blobs in Smithy must be base64 encoded, so this
            // code grabs the referenced shape and creates an inline schema that
            // explicitly defines the necessary styles.
            return schema.toBuilder().ref(null).type("string").format("byte").build();
        }
    }

    private static final class HeaderSchemaVisitor<T extends Trait> extends ShapeVisitor.Default<Schema> {
        private Context<T> context;
        private Schema schema;
        private MemberShape member;

        private HeaderSchemaVisitor(Context<T> context, Schema schema, MemberShape member) {
            this.context = context;
            this.schema = schema;
            this.member = member;
        }

        @Override
        protected Schema getDefault(Shape shape) {
            return schema;
        }

        @Override
        public Schema listShape(ListShape shape) {
            return collection(shape);
        }

        @Override
        public Schema setShape(SetShape shape) {
            return collection(shape);
        }

        private Schema collection(CollectionShape collection) {
            Shape memberTarget = context.getModel().expectShape(collection.getMember().getTarget());
            String memberPointer = context.getPointer(collection.getMember());
            Schema currentMemberSchema = context.getSchema(memberPointer);
            HeaderSchemaVisitor<T> visitor = new HeaderSchemaVisitor<>(
                    context, currentMemberSchema, collection.getMember());
            Schema newMemberSchema = memberTarget.accept(visitor);
            return schema.toBuilder().ref(null).type("array").items(newMemberSchema).build();
        }

        @Override
        public Schema timestampShape(TimestampShape shape) {
            // Header timestamps in Smithy use the HTTP-Date format if a
            // timestamp format is not explicitly set. An inline schema is
            // created if the format was not explicitly set.
            if (member.hasTrait(TimestampFormatTrait.class)) {
                return schema;
            }

            // Uses an HTTP-date format by default.
            Schema.Builder copiedBuilder = ModelUtils.convertSchemaToStringBuilder(
                    context.getSchema(context.getPointer(member)));
            return copiedBuilder.format(null).build();
        }

        @Override
        public Schema stringShape(StringShape shape) {
            // String shapes with the mediaType trait must be base64 encoded.
            return shape.hasTrait(MediaTypeTrait.class)
                   ? schema.toBuilder().ref(null).type("string").format("byte").build()
                   : schema;
        }
    }
}
