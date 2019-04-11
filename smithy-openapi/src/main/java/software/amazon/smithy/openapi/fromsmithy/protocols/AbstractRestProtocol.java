package software.amazon.smithy.openapi.fromsmithy.protocols;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.jsonschema.Schema;
import software.amazon.smithy.model.Pair;
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
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.OpenApiProtocol;
import software.amazon.smithy.openapi.model.MediaTypeObject;
import software.amazon.smithy.openapi.model.OperationObject;
import software.amazon.smithy.openapi.model.ParameterObject;
import software.amazon.smithy.openapi.model.Ref;
import software.amazon.smithy.openapi.model.RequestBodyObject;
import software.amazon.smithy.openapi.model.ResponseObject;
import software.amazon.smithy.utils.OptionalUtils;

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
abstract class AbstractRestProtocol implements OpenApiProtocol {

    /** The type of message being created. */
    enum MessageType { REQUEST, RESPONSE, ERROR }

    /**
     * Gets the media type of a document sent in a request or response.
     *
     * @param context Conversion context.
     * @param operationOrError Operation shape or error shape.
     * @param messageType The type of message being created (request, response, or error).
     * @return Returns the media type of the document payload.
     */
    abstract String getDocumentMediaType(Context context, Shape operationOrError, MessageType messageType);

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
            Context context,
            Shape operationOrError,
            List<HttpBindingIndex.Binding> bindings,
            MessageType messageType);

    @Override
    public Optional<Operation> createOperation(Context context, OperationShape operation) {
        return operation.getTrait(HttpTrait.class).map(httpTrait -> {
            String method = httpTrait.getMethod();
            String uri = httpTrait.getUri().toString();
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

    private List<ParameterObject> createPathParameters(Context context, OperationShape operation) {
        return context.getModel().getKnowledge(HttpBindingIndex.class)
                .getRequestBindings(operation, HttpBindingIndex.Location.LABEL).stream()
                .map(binding -> {
                    MemberShape member = binding.getMember();
                    Schema schema = context.createRef(binding.getMember());
                    ParameterObject.Builder paramBuilder = ModelUtils.createParameterMember(context, member).in("path");
                    // Timestamps sent in the URI are serialized as a date-time string by default.
                    boolean needsInlineSchema = context.getModel().getShapeIndex().getShape(member.getTarget())
                            .filter(Shape::isTimestampShape)
                            .isPresent()
                            && ModelUtils.getMemberTrait(context, member, TimestampFormatTrait.class).isEmpty();
                    if (needsInlineSchema) {
                        // Create a copy of the targeted schema and remove any possible numeric keywords.
                        Schema.Builder copiedBuilder = ModelUtils.convertSchemaToStringBuilder(
                                context.getSchema(context.getPointer(member)));
                        schema = copiedBuilder.format("date-time").build();
                    }
                    return paramBuilder.schema(schema).build();
                })
                .collect(Collectors.toList());
    }

    private List<ParameterObject> createQueryParameters(Context context, OperationShape operation) {
        return context.getModel().getKnowledge(HttpBindingIndex.class)
                .getRequestBindings(operation, HttpBindingIndex.Location.QUERY).stream()
                .map(binding -> {
                    ParameterObject.Builder param = ModelUtils.createParameterMember(context, binding.getMember())
                            .in("query")
                            .name(binding.getLocationName());
                    Shape target = context.getModel().getShapeIndex().getShape(binding.getMember().getTarget()).get();

                    // List and set shapes in the query string are repeated, so we need to "explode" them.
                    if (target instanceof CollectionShape) {
                        param.style("form").explode(true);
                    }

                    Schema refSchema = context.createRef(binding.getMember());
                    param.schema(target.accept(new QuerySchemaVisitor(context, refSchema, binding.getMember())));
                    return param.build();
                })
                .collect(Collectors.toList());
    }

    private Collection<ParameterObject> createRequestHeaderParameters(Context context, OperationShape operation) {
        List<HttpBindingIndex.Binding> bindings = context.getModel().getKnowledge(HttpBindingIndex.class)
                .getRequestBindings(operation, HttpBindingIndex.Location.HEADER);
        return createHeaderParameters(context, bindings, MessageType.REQUEST).values();
    }

    private Map<String, ParameterObject> createHeaderParameters(
            Context context,
            List<HttpBindingIndex.Binding> bindings,
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
                    Shape target = context.getModel().getShapeIndex().getShape(binding.getMember().getTarget()).get();
                    Schema refSchema = context.createRef(binding.getMember());
                    param.schema(target.accept(new HeaderSchemaVisitor(context, refSchema, binding.getMember())));
                    return Pair.of(binding.getLocationName(), param.build());
                })
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }

    private Optional<RequestBodyObject> createRequestBody(
            Context context,
            HttpBindingIndex bindingIndex,
            OperationShape operation
    ) {
        List<HttpBindingIndex.Binding> payloadBindings
                = bindingIndex.getRequestBindings(operation, HttpBindingIndex.Location.PAYLOAD);
        return payloadBindings.isEmpty()
               ? createRequestDocument(context, bindingIndex, operation)
               : createRequestPayload(context, payloadBindings.get(0));
    }

    private Optional<RequestBodyObject> createRequestPayload(Context context, HttpBindingIndex.Binding binding) {
        MediaTypeObject mediaTypeObject = MediaTypeObject.builder()
                .schema(context.createRef(binding.getMember())).build();
        String mediaTypeRange = ModelUtils.getMediaType(context, binding.getMember());
        RequestBodyObject.Builder requestBuilder = RequestBodyObject.builder()
                .putContent(mediaTypeRange, mediaTypeObject);
        return Optional.of(requestBuilder.build());
    }

    private Optional<RequestBodyObject> createRequestDocument(
            Context context,
            HttpBindingIndex bindingIndex,
            OperationShape operation
    ) {
        List<HttpBindingIndex.Binding> bindings
                = bindingIndex.getRequestBindings(operation, HttpBindingIndex.Location.DOCUMENT);
        if (bindings.isEmpty()) {
            return Optional.empty();
        }

        Schema schema = createDocumentSchema(context, operation, bindings, MessageType.REQUEST);
        String mediaType = getDocumentMediaType(context, operation, MessageType.REQUEST);
        return Optional.of(RequestBodyObject.builder()
                .putContent(mediaType, MediaTypeObject.builder().schema(schema).build())
                .build());
    }

    private Map<String, ResponseObject> createResponses(
            Context context,
            HttpBindingIndex bindingIndex,
            OperationShape operation
    ) {
        OperationIndex operationIndex = context.getModel().getKnowledge(OperationIndex.class);

        // Add the successful response and errors. TODO: What about synthetic errors?
        return Stream.concat(
                OptionalUtils.stream(operationIndex.getOutput(operation)),
                operationIndex.getErrors(operation).stream()
        ).map(shape -> {
            Shape operationOrError = shape.hasTrait(ErrorTrait.class) ? shape : operation;
            String statusCode = String.valueOf(bindingIndex.getResponseCode(operationOrError));
            ResponseObject response = createResponse(context, bindingIndex, statusCode, operation, operationOrError);
            return Pair.of(statusCode, response);
        }).collect(Collectors.toMap(Pair::getLeft, Pair::getRight, (a, b) -> b, LinkedHashMap::new));
    }

    private ResponseObject createResponse(
            Context context,
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
            Context context,
            OperationShape operation
    ) {
        List<HttpBindingIndex.Binding> bindings = context.getModel().getKnowledge(HttpBindingIndex.class)
                .getResponseBindings(operation, HttpBindingIndex.Location.HEADER);
        return createHeaderParameters(context, bindings, MessageType.RESPONSE);
    }

    private void addResponseContent(
            Context context,
            HttpBindingIndex bindingIndex,
            ResponseObject.Builder responseBuilder,
            Shape operationOrError
    ) {
        List<HttpBindingIndex.Binding> payloadBindings
                = bindingIndex.getResponseBindings(operationOrError, HttpBindingIndex.Location.PAYLOAD);
        if (!payloadBindings.isEmpty()) {
            createResponsePayload(context, payloadBindings.get(0), responseBuilder);
        } else {
            createResponseDocument(context, bindingIndex, responseBuilder, operationOrError);
        }
    }

    private void createResponsePayload(
            Context context,
            HttpBindingIndex.Binding binding,
            ResponseObject.Builder responseBuilder
    ) {
        String mediaType = ModelUtils.getMediaType(context, binding.getMember());
        responseBuilder.putContent(mediaType, MediaTypeObject
                .builder()
                .schema(context.createRef(binding.getMember()))
                .build());
    }

    private void createResponseDocument(
            Context context,
            HttpBindingIndex bindingIndex,
            ResponseObject.Builder responseBuilder,
            Shape operationOrError
    ) {
        List<HttpBindingIndex.Binding> bindings
                = bindingIndex.getResponseBindings(operationOrError, HttpBindingIndex.Location.DOCUMENT);
        if (!bindings.isEmpty()) {
            MessageType messageType = operationOrError instanceof OperationShape
                    ? MessageType.RESPONSE
                    : MessageType.ERROR;
            Schema schema = createDocumentSchema(context, operationOrError, bindings, messageType);
            String mediaType = getDocumentMediaType(context, operationOrError, messageType);
            responseBuilder.putContent(mediaType, MediaTypeObject.builder().schema(schema).build());
        }
    }

    private static final class QuerySchemaVisitor extends ShapeVisitor.Default<Schema> {
        private Context context;
        private Schema schema;
        private MemberShape member;

        private QuerySchemaVisitor(Context context, Schema schema, MemberShape member) {
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
            Shape memberTarget = context.getModel().getShapeIndex().getShape(collection.getMember().getTarget()).get();
            String memberPointer = context.getPointer(collection.getMember());
            Schema currentMemberSchema = context.getSchema(memberPointer);
            Schema newMemberSchema = memberTarget.accept(
                    new QuerySchemaVisitor(context, currentMemberSchema, collection.getMember()));
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

    private static final class HeaderSchemaVisitor extends ShapeVisitor.Default<Schema> {
        private Context context;
        private Schema schema;
        private MemberShape member;

        private HeaderSchemaVisitor(Context context, Schema schema, MemberShape member) {
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
            Shape memberTarget = context.getModel().getShapeIndex().getShape(collection.getMember().getTarget()).get();
            String memberPointer = context.getPointer(collection.getMember());
            Schema currentMemberSchema = context.getSchema(memberPointer);
            Schema newMemberSchema = memberTarget.accept(
                    new HeaderSchemaVisitor(context, currentMemberSchema, collection.getMember()));
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
