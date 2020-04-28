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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import software.amazon.smithy.jsonschema.Schema;
import software.amazon.smithy.model.knowledge.EventStreamIndex;
import software.amazon.smithy.model.knowledge.EventStreamInfo;
import software.amazon.smithy.model.knowledge.HttpBinding;
import software.amazon.smithy.model.knowledge.HttpBindingIndex;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.shapes.CollectionShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.HttpTrait;
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

    private static final String AWS_EVENT_STREAM_CONTENT_TYPE = "application/vnd.amazon.eventstream";

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
            EventStreamIndex eventStreamIndex = context.getModel().getKnowledge(EventStreamIndex.class);
            createPathParameters(context, operation).forEach(builder::addParameter);
            createQueryParameters(context, operation).forEach(builder::addParameter);
            createRequestHeaderParameters(context, operation).forEach(builder::addParameter);
            createRequestBody(context, bindingIndex, eventStreamIndex, operation).ifPresent(builder::requestBody);
            createResponses(context, bindingIndex, eventStreamIndex, operation).forEach(builder::putResponse);
            return Operation.create(method, uri, builder);
        });
    }

    private List<ParameterObject> createPathParameters(Context<T> context, OperationShape operation) {
        List<ParameterObject> result = new ArrayList<>();
        HttpBindingIndex bindingIndex = context.getModel().getKnowledge(HttpBindingIndex.class);

        for (HttpBinding binding : bindingIndex.getRequestBindings(operation, HttpBinding.Location.LABEL)) {
            Schema schema = createPathParameterSchema(context, binding);
            result.add(ModelUtils.createParameterMember(context, binding.getMember())
                    .in("path")
                    .schema(schema)
                    .build());
        }

        return result;
    }

    private Schema createPathParameterSchema(Context<T> context, HttpBinding binding) {
        MemberShape member = binding.getMember();

        // Timestamps sent in the URI are serialized as a date-time string by default.
        if (needsInlineTimestampSchema(context, member)) {
            // Create a copy of the targeted schema and remove any possible numeric keywords.
            Schema.Builder copiedBuilder = ModelUtils.convertSchemaToStringBuilder(
                    context.getSchema(context.getPointer(member)));
            return copiedBuilder.format("date-time").build();
        } else if (context.getJsonSchemaConverter().isInlined(member)) {
            return context.getJsonSchemaConverter().convertShape(member).getRootSchema();
        } else {
            return context.createRef(binding.getMember());
        }
    }

    private boolean needsInlineTimestampSchema(Context<? extends Trait> context, MemberShape member) {
        if (member.getMemberTrait(context.getModel(), TimestampFormatTrait.class).isPresent()) {
            return false;
        }

        return context.getModel()
                .getShape(member.getTarget())
                .filter(Shape::isTimestampShape)
                .isPresent();
    }

    // Creates parameters that appear in the query string. Each input member
    // bound to the QUERY location will generate a new ParameterObject that
    // has a location of "query".
    private List<ParameterObject> createQueryParameters(Context<T> context, OperationShape operation) {
        HttpBindingIndex httpBindingIndex = context.getModel().getKnowledge(HttpBindingIndex.class);
        List<ParameterObject> result = new ArrayList<>();

        for (HttpBinding binding : httpBindingIndex.getRequestBindings(operation, HttpBinding.Location.QUERY)) {
            MemberShape member = binding.getMember();
            ParameterObject.Builder param = ModelUtils.createParameterMember(context, member)
                    .in("query")
                    .name(binding.getLocationName());
            Shape target = context.getModel().expectShape(member.getTarget());

            // List and set shapes in the query string are repeated, so we need to "explode" them
            // using the "form" style (e.g., "foo=bar&foo=baz").
            // See https://swagger.io/specification/#style-examples
            if (target instanceof CollectionShape) {
                param.style("form").explode(true);
            }

            // Create the appropriate schema based on the shape type.
            Schema refSchema = context.inlineOrReferenceSchema(member);
            QuerySchemaVisitor<T> visitor = new QuerySchemaVisitor<>(context, refSchema, member);
            param.schema(target.accept(visitor));
            result.add(param.build());
        }

        return result;
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
        Map<String, ParameterObject> result = new TreeMap<>();

        for (HttpBinding binding : bindings) {
            MemberShape member = binding.getMember();
            ParameterObject.Builder param = ModelUtils.createParameterMember(context, member);

            if (messageType == MessageType.REQUEST) {
                param.in("header").name(binding.getLocationName());
            } else {
                // Response headers don't use "in" or "name".
                param.in(null).name(null);
            }

            // Create the appropriate schema based on the shape type.
            Shape target = context.getModel().expectShape(member.getTarget());
            Schema refSchema = context.inlineOrReferenceSchema(member);
            HeaderSchemaVisitor<T> visitor = new HeaderSchemaVisitor<>(context, refSchema, member);
            param.schema(target.accept(visitor));

            result.put(binding.getLocationName(), param.build());
        }

        return result;
    }

    private Optional<RequestBodyObject> createRequestBody(
            Context<T> context,
            HttpBindingIndex bindingIndex,
            EventStreamIndex eventStreamIndex,
            OperationShape operation
    ) {
        List<HttpBinding> payloadBindings = bindingIndex.getRequestBindings(
                operation, HttpBinding.Location.PAYLOAD);

        // Get the default media type if one cannot be resolved.
        String documentMediaType = getDocumentMediaType(context, operation, MessageType.REQUEST);

        // Get the event stream media type if an event stream is in use.
        String eventStreamMediaType = eventStreamIndex.getInputInfo(operation)
                .map(info -> getEventStreamMediaType(context, info))
                .orElse(null);

        String mediaType = bindingIndex
                .determineRequestContentType(operation, documentMediaType, eventStreamMediaType)
                .orElse(null);

        return payloadBindings.isEmpty()
               ? createRequestDocument(mediaType, context, bindingIndex, operation)
               : createRequestPayload(mediaType, context, payloadBindings.get(0), operation);
    }

    /**
     * Gets the media type of an event stream for the protocol.
     *
     * <p>By default, this method returns the binary AWS event stream
     * media type, {@code application/vnd.amazon.eventstream}.
     *
     * @param context Conversion context.
     * @param info Event stream info to provide the media type for.
     * @return Returns the media type of the event stream.
     */
    protected String getEventStreamMediaType(Context<T> context, EventStreamInfo info) {
        return AWS_EVENT_STREAM_CONTENT_TYPE;
    }

    private Optional<RequestBodyObject> createRequestPayload(
            String mediaTypeRange,
            Context<T> context,
            HttpBinding binding,
            OperationShape operation
    ) {
        Schema schema = context.inlineOrReferenceSchema(binding.getMember());
        String synthesizedName = operation.getId().getName() + "InputPayload";
        String pointer = context.putSynthesizedSchema(synthesizedName, schema);
        MediaTypeObject mediaTypeObject = MediaTypeObject.builder()
                .schema(Schema.builder().ref(pointer).build())
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
        List<HttpBinding> bindings = bindingIndex.getRequestBindings(operation, HttpBinding.Location.DOCUMENT);

        // If nothing is bound to the document, then no schema needs to be synthesized.
        if (bindings.isEmpty()) {
            return Optional.empty();
        }

        // Synthesize a schema for the body of the request.
        Schema schema = createDocumentSchema(context, operation, bindings, MessageType.REQUEST);
        String synthesizedName = operation.getId().getName() + "RequestContent";
        String pointer = context.putSynthesizedSchema(synthesizedName, schema);
        MediaTypeObject mediaTypeObject = MediaTypeObject.builder()
                .schema(Schema.builder().ref(pointer).build())
                .build();

        return Optional.of(RequestBodyObject.builder()
                .putContent(mediaType, mediaTypeObject)
                .build());
    }

    private Map<String, ResponseObject> createResponses(
            Context<T> context,
            HttpBindingIndex bindingIndex,
            EventStreamIndex eventStreamIndex,
            OperationShape operation
    ) {
        Map<String, ResponseObject> result = new TreeMap<>();
        OperationIndex operationIndex = context.getModel().getKnowledge(OperationIndex.class);

        operationIndex.getOutput(operation).ifPresent(output -> {
            updateResponsesMapWithResponseStatusAndObject(
                    context, bindingIndex, eventStreamIndex, operation, output, result);
        });

        for (StructureShape error : operationIndex.getErrors(operation)) {
            updateResponsesMapWithResponseStatusAndObject(
                    context, bindingIndex, eventStreamIndex, operation, error, result);
        }
        return result;
    }

    private void updateResponsesMapWithResponseStatusAndObject(
            Context<T> context,
            HttpBindingIndex bindingIndex,
            EventStreamIndex eventStreamIndex,
            OperationShape operation,
            StructureShape shape,
            Map<String, ResponseObject> responses
    ) {
        Shape operationOrError = shape.hasTrait(ErrorTrait.class) ? shape : operation;
        String statusCode = context.getOpenApiProtocol().getOperationResponseStatusCode(context, operationOrError);
        ResponseObject response = createResponse(
                context, bindingIndex, eventStreamIndex, statusCode, operationOrError);
        responses.put(statusCode, response);
    }

    private ResponseObject createResponse(
            Context<T> context,
            HttpBindingIndex bindingIndex,
            EventStreamIndex eventStreamIndex,
            String statusCode,
            Shape operationOrError
    ) {
        ResponseObject.Builder responseBuilder = ResponseObject.builder();
        responseBuilder.description(String.format("%s %s response", operationOrError.getId().getName(), statusCode));
        createResponseHeaderParameters(context, operationOrError)
                .forEach((k, v) -> responseBuilder.putHeader(k, Ref.local(v)));
        addResponseContent(context, bindingIndex, eventStreamIndex, responseBuilder, statusCode, operationOrError);
        return responseBuilder.build();
    }

    private Map<String, ParameterObject> createResponseHeaderParameters(
            Context<T> context,
            Shape operationOrError
    ) {
        List<HttpBinding> bindings = context.getModel().getKnowledge(HttpBindingIndex.class)
                .getResponseBindings(operationOrError, HttpBinding.Location.HEADER);
        return createHeaderParameters(context, bindings, MessageType.RESPONSE);
    }

    private void addResponseContent(
            Context<T> context,
            HttpBindingIndex bindingIndex,
            EventStreamIndex eventStreamIndex,
            ResponseObject.Builder responseBuilder,
            String statusCode,
            Shape operationOrError
    ) {
        List<HttpBinding> payloadBindings = bindingIndex.getResponseBindings(
                operationOrError, HttpBinding.Location.PAYLOAD);

        // Get the default media type if one cannot be resolved.
        String documentMediaType = getDocumentMediaType(context, operationOrError, MessageType.RESPONSE);

        // Get the event stream media type if an event stream is in use.
        String eventStreamMediaType = eventStreamIndex.getOutputInfo(operationOrError)
                .map(info -> getEventStreamMediaType(context, info))
                .orElse(null);

        String mediaType = bindingIndex
                .determineResponseContentType(operationOrError, documentMediaType, eventStreamMediaType)
                .orElse(null);

        if (!payloadBindings.isEmpty()) {
            createResponsePayload(mediaType, context, payloadBindings.get(0), responseBuilder, operationOrError);
        } else {
            createResponseDocumentIfNeeded(mediaType, context, bindingIndex, responseBuilder, operationOrError);
        }
    }

    private void createResponsePayload(
            String mediaType,
            Context<T> context,
            HttpBinding binding,
            ResponseObject.Builder responseBuilder,
            Shape operationOrError
    ) {
        // API Gateway validation requires that in-line schemas must be objects
        // or arrays. These schemas are synthesized as references so that
        // any schemas with string types will pass validation.
        Schema schema = context.inlineOrReferenceSchema(binding.getMember());
        String shapeName = operationOrError.getId().getName();
        String synthesizedName = operationOrError instanceof OperationShape
                ? shapeName + "OutputPayload"
                : shapeName + "ErrorPayload";
        String pointer = context.putSynthesizedSchema(synthesizedName, schema);
        MediaTypeObject mediaTypeObject = MediaTypeObject.builder()
                .schema(Schema.builder().ref(pointer).build())
                .build();
        responseBuilder.putContent(mediaType, mediaTypeObject);
    }

    private void createResponseDocumentIfNeeded(
            String mediaType,
            Context<T> context,
            HttpBindingIndex bindingIndex,
            ResponseObject.Builder responseBuilder,
            Shape operationOrError
    ) {
        List<HttpBinding> bindings = bindingIndex.getResponseBindings(
                operationOrError, HttpBinding.Location.DOCUMENT);

        // If the operation doesn't have any document bindings, then do nothing.
        if (bindings.isEmpty()) {
            return;
        }

        // Document bindings needs to be synthesized into a new schema that contains
        // just the document bindings separate from other parameters.
        MessageType messageType = operationOrError instanceof OperationShape
                ? MessageType.RESPONSE
                : MessageType.ERROR;

        // This "synthesizes" a new schema that just contains the document bindings.
        // While we *could* just use the referenced output/error shape as-is, that
        // would be a bad idea; traits applied to shapes in Smithy can contextually
        // influence what the resulting JSON schema or OpenAPI. Consider the
        // following examples:
        //
        // 1. If the same shape is reused as input and output, then some members
        //    might be bound to query string parameters, and query string params
        //    aren't relevant on output. Trying to use the same schema derived
        //    from the reused input/output shape would result in a broken API.
        // 2. What if the input/output shape doesn't bind anything to the query
        //    string, headers, path, etc? Couldn't it then be used as-is with
        //    the name given in the Smithy model? Yes, technically it could, but
        //    that's also a bad idea. If/when you want to add a header or query
        //    string parameter, then you now need to break your generated OpenAPI
        //    schema, particularly if the shapes was reused throughout your model
        //    outside of top-level inputs, outputs, and errors.
        //
        // The safest thing to do here is to always synthesize a new schema that
        // just includes the document bindings.
        //
        // **NOTE: this same blurb applies to why we do this on input.**
        Schema schema = createDocumentSchema(context, operationOrError, bindings, messageType);
        String synthesizedName = operationOrError.getId().getName() + "ResponseContent";
        String pointer = context.putSynthesizedSchema(synthesizedName, schema);
        MediaTypeObject mediaTypeObject = MediaTypeObject.builder()
                .schema(Schema.builder().ref(pointer).build())
                .build();

        responseBuilder.putContent(mediaType, mediaTypeObject);
    }
}
