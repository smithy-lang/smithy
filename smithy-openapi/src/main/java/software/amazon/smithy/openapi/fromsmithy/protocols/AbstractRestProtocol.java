/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy.protocols;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import software.amazon.smithy.jsonschema.Schema;
import software.amazon.smithy.model.knowledge.EventStreamIndex;
import software.amazon.smithy.model.knowledge.EventStreamInfo;
import software.amazon.smithy.model.knowledge.HttpBinding;
import software.amazon.smithy.model.knowledge.HttpBindingIndex;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.pattern.SmithyPattern;
import software.amazon.smithy.model.shapes.CollectionShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.ExamplesTrait;
import software.amazon.smithy.model.traits.HttpChecksumRequiredTrait;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.OpenApiException;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.OpenApiProtocol;
import software.amazon.smithy.openapi.model.ExampleObject;
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
    private static final Pattern NON_ALPHA_NUMERIC = Pattern.compile("[^A-Za-z0-9]");

    private static final Logger LOGGER = Logger.getLogger(AbstractRestProtocol.class.getName());

    /** The type of message being created. */
    enum MessageType {
        REQUEST, RESPONSE, ERROR
    }

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

    /**
     * Converts Smithy values in Node form to a data exchange format used by a protocol (e.g., XML).
     * Then returns the converted value as a long string (escaping where necessary).
     * If data exchange format is JSON (e.g., as in restJson1 protocol),
     * method should return values without any modification.
     *
     * <p> Used for the value property of OpenAPI example objects.
     * For protocols that do not use JSON as data-exchange format,
     * converts the Node object to a StringNode object that contains the same data, but represented
     * in the data representation format used by the protocol.
     * E.g., for restXML protocol, values would be converted to a large String of XML value / object,
     * escaping where necessary.
     *
     * @param value    value to be converted.
     * @return  the long string (escaped where necessary) of values in a data exchange format used by a protocol.
     */
    abstract Node transformSmithyValueToProtocolValue(Node value);

    @Override
    public Set<String> getProtocolRequestHeaders(Context<T> context, OperationShape operationShape) {
        Set<String> headers = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        headers.addAll(OpenApiProtocol.super.getProtocolRequestHeaders(context, operationShape));

        HttpBindingIndex bindingIndex = HttpBindingIndex.of(context.getModel());
        String documentMediaType = getDocumentMediaType(context, operationShape, MessageType.REQUEST);
        // If the request has a body with a content type, allow the content-type and content-length headers.
        bindingIndex.determineRequestContentType(operationShape, documentMediaType)
                .ifPresent(c -> headers.addAll(ProtocolUtils.CONTENT_HEADERS));

        if (operationShape.hasTrait(HttpChecksumRequiredTrait.class)) {
            headers.add("Content-Md5");
        }
        return headers;
    }

    @Override
    public Set<String> getProtocolResponseHeaders(Context<T> context, OperationShape operationShape) {
        Set<String> headers = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        headers.addAll(OpenApiProtocol.super.getProtocolResponseHeaders(context, operationShape));
        headers.addAll(ProtocolUtils.CONTENT_HEADERS);

        return headers;
    }

    @Override
    public Optional<Operation> createOperation(Context<T> context, OperationShape operation) {
        ServiceShape serviceShape = context.getService();
        return operation.getTrait(HttpTrait.class).map(httpTrait -> {
            HttpBindingIndex bindingIndex = HttpBindingIndex.of(context.getModel());
            EventStreamIndex eventStreamIndex = EventStreamIndex.of(context.getModel());
            String method = context.getOpenApiProtocol().getOperationMethod(context, operation);
            String uri = context.getOpenApiProtocol().getOperationUri(context, operation);
            OperationObject.Builder builder = OperationObject.builder()
                    .operationId(serviceShape.getContextualName(operation));
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
        HttpBindingIndex bindingIndex = HttpBindingIndex.of(context.getModel());
        HttpTrait httpTrait = operation.expectTrait(HttpTrait.class);

        for (HttpBinding binding : bindingIndex.getRequestBindings(operation, HttpBinding.Location.LABEL)) {
            Schema schema = createPathParameterSchema(context, binding);
            String memberName = binding.getMemberName();

            SmithyPattern.Segment label = httpTrait.getUri()
                    .getLabel(memberName)
                    .orElseThrow(() -> new OpenApiException(String.format(
                            "Unable to find URI label on %s for %s: %s",
                            operation.getId(),
                            binding.getMemberName(),
                            httpTrait.getUri())));

            // Greedy labels in OpenAPI need to include the label in the generated parameter.
            // For example, given "/{foo+}", the parameter name must be "foo+".
            // Some vendors/tooling, require the "+" suffix be excluded in the generated parameter.
            // If required, the setRemoveGreedyParameterSuffix config option should be set to `true`.
            // When this option is enabled, given "/{foo+}", the parameter name will be "foo".
            String name = label.getContent();
            if (label.isGreedyLabel() && !context.getConfig().getRemoveGreedyParameterSuffix()) {
                name = name + "+";
            }

            result.add(ModelUtils.createParameterMember(context, binding.getMember())
                    .name(name)
                    .in("path")
                    .schema(schema)
                    .examples(createExamplesForMembersWithHttpTraits(
                            operation,
                            binding,
                            MessageType.REQUEST,
                            null))
                    .build());
        }

        return result;
    }

    /*
     * This method is used for converting the Smithy examples to OpenAPI examples for:
     * path parameters, query parameters, header parameters, and payload.
     */
    private Map<String, Node> createExamplesForMembersWithHttpTraits(
            Shape operationOrError,
            HttpBinding binding,
            MessageType type,
            OperationShape operation
    ) {
        if (operation == null && type == MessageType.ERROR) {
            return Collections.emptyMap();
        }

        if (type == MessageType.ERROR) {
            return createErrorExamplesForMembersWithHttpTraits(operationOrError, binding, operation);
        } else {
            Map<String, Node> examples = new TreeMap<>();
            // unique numbering for unique example names in OpenAPI.
            int uniqueNum = 1;

            Optional<ExamplesTrait> examplesTrait = operationOrError.getTrait(ExamplesTrait.class);
            for (ExamplesTrait.Example example : examplesTrait.map(ExamplesTrait::getExamples)
                    .orElse(Collections.emptyList())) {
                ObjectNode inputOrOutput = type == MessageType.REQUEST ? example.getInput()
                        : example.getOutput().orElse(Node.objectNode());
                String name = operationOrError.getId().getName() + "_example" + uniqueNum++;

                // this if condition is needed to avoid errors when converting examples of response.
                if ((!example.getError().isPresent() || type == MessageType.REQUEST)
                        && inputOrOutput.containsMember(binding.getMemberName())) {
                    Node values = inputOrOutput.getMember(binding.getMemberName()).get();

                    examples.put(name,
                            ExampleObject.builder()
                                    .summary(example.getTitle())
                                    .description(example.getDocumentation().orElse(""))
                                    .value(transformSmithyValueToProtocolValue(values))
                                    .build()
                                    .toNode());
                }
            }
            return examples;
        }
    }

    /*
     * Helper method for createExamples() method.
     */
    private Map<String, Node> createErrorExamplesForMembersWithHttpTraits(
            Shape error,
            HttpBinding binding,
            OperationShape operation
    ) {
        Map<String, Node> examples = new TreeMap<>();

        // unique numbering for unique example names in OpenAPI.
        int uniqueNum = 1;
        Optional<ExamplesTrait> examplesTrait = operation.getTrait(ExamplesTrait.class);
        for (ExamplesTrait.Example example : examplesTrait.map(ExamplesTrait::getExamples)
                .orElse(Collections.emptyList())) {
            String name = operation.getId().getName() + "_example" + uniqueNum++;

            // this has to be checked because an operation can have more than one error linked to it.
            ExamplesTrait.ErrorExample errorExample = example.getError().orElse(null);
            if (errorExample != null
                    && errorExample.getShapeId() == error.toShapeId()
                    && errorExample.getContent().containsMember(binding.getMemberName())) {
                Node values = errorExample.getContent()
                        .getMember(binding.getMemberName())
                        .get();

                examples.put(name,
                        ExampleObject.builder()
                                .summary(example.getTitle())
                                .description(example.getDocumentation().orElse(""))
                                .value(transformSmithyValueToProtocolValue(values))
                                .build()
                                .toNode());
            }
        }
        return examples;
    }

    /*
     * This method is used for converting the Smithy examples to OpenAPI examples for non-payload HTTP message body.
     */
    private Map<String, Node> createBodyExamples(
            Shape operationOrError,
            List<HttpBinding> bindings,
            MessageType type,
            OperationShape operation
    ) {
        if (operation == null && type == MessageType.ERROR) {
            return Collections.emptyMap();
        }

        if (type == MessageType.ERROR) {
            return createErrorBodyExamples(operationOrError, bindings, operation);
        } else {
            Map<String, Node> examples = new TreeMap<>();
            // unique numbering for unique example names in OpenAPI.
            int uniqueNum = 1;

            Optional<ExamplesTrait> examplesTrait = operationOrError.getTrait(ExamplesTrait.class);
            for (ExamplesTrait.Example example : examplesTrait.map(ExamplesTrait::getExamples)
                    .orElse(Collections.emptyList())) {
                // get members included in bindings
                ObjectNode values = getMembersWithHttpBindingTrait(bindings,
                        type == MessageType.REQUEST ? example.getInput()
                                : example.getOutput().orElse(Node.objectNode()));
                String name = operationOrError.getId().getName() + "_example" + uniqueNum++;
                // this if condition is needed to avoid errors when converting examples of response.
                if (!example.getError().isPresent() || type == MessageType.REQUEST) {
                    examples.put(name,
                            ExampleObject.builder()
                                    .summary(example.getTitle())
                                    .description(example.getDocumentation().orElse(""))
                                    .value(transformSmithyValueToProtocolValue(values))
                                    .build()
                                    .toNode());
                }
            }
            return examples;
        }
    }

    private Map<String, Node> createErrorBodyExamples(
            Shape error,
            List<HttpBinding> bindings,
            OperationShape operation
    ) {
        Map<String, Node> examples = new TreeMap<>();
        // unique numbering for unique example names in OpenAPI.
        int uniqueNum = 1;
        Optional<ExamplesTrait> examplesTrait = operation.getTrait(ExamplesTrait.class);
        for (ExamplesTrait.Example example : examplesTrait.map(ExamplesTrait::getExamples)
                .orElse(Collections.emptyList())) {
            String name = operation.getId().getName() + "_example" + uniqueNum++;
            // this has to be checked because an operation can have more than one error linked to it.
            if (example.getError().isPresent()
                    && example.getError().get().getShapeId() == error.toShapeId()) {
                // get members included in bindings
                ObjectNode values = getMembersWithHttpBindingTrait(bindings, example.getError().get().getContent());
                examples.put(name,
                        ExampleObject.builder()
                                .summary(example.getTitle())
                                .description(example.getDocumentation().orElse(""))
                                .value(transformSmithyValueToProtocolValue(values))
                                .build()
                                .toNode());
            }
        }
        return examples;
    }

    /*
     * Returns a modified copy of [inputOrOutput] only containing members bound to a HttpBinding trait in [bindings].
     */
    private ObjectNode getMembersWithHttpBindingTrait(List<HttpBinding> bindings, ObjectNode inputOrOutput) {
        ObjectNode.Builder values = ObjectNode.builder();

        Set<String> memberNamesWithHttpBinding = new LinkedHashSet<>();
        for (HttpBinding binding : bindings) {
            memberNamesWithHttpBinding.add(binding.getMemberName());
        }

        for (Map.Entry<StringNode, Node> entry : inputOrOutput.getMembers().entrySet()) {
            if (memberNamesWithHttpBinding.contains(entry.getKey().toString())) {
                values.withMember(entry.getKey(), entry.getValue());
            }
        }

        return values.build();
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
    // bound to the QUERY or QUERY_PARAMS location will generate a new
    // ParameterObject that has a location of "query".
    private List<ParameterObject> createQueryParameters(Context<T> context, OperationShape operation) {
        HttpBindingIndex httpBindingIndex = HttpBindingIndex.of(context.getModel());
        List<ParameterObject> result = new ArrayList<>();

        List<HttpBinding> bindings = new ArrayList<>();
        bindings.addAll(httpBindingIndex.getRequestBindings(operation, HttpBinding.Location.QUERY));
        bindings.addAll(httpBindingIndex.getRequestBindings(operation, HttpBinding.Location.QUERY_PARAMS));

        for (HttpBinding binding : bindings) {
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

            // To allow undefined parameters of a specific type, the style is set to `form`. This is set in conjunction
            // with a schema of the `object` type.
            if (binding.getLocation().equals(HttpBinding.Location.QUERY_PARAMS)) {
                param.style("form");

                // QUERY_PARAMS necessarily target maps.  If the map value is a list or set, the query string are
                // repeated and must also be set to "explode".
                Shape shape = context.getModel().expectShape(target.asMapShape().get().getValue().getTarget());
                if (shape instanceof CollectionShape) {
                    param.explode(true);
                }
            }

            param.schema(createQuerySchema(context, member, target));
            param.examples(createExamplesForMembersWithHttpTraits(operation, binding, MessageType.REQUEST, null));
            result.add(param.build());
        }

        return result;
    }

    private Schema createQuerySchema(Context<T> context, MemberShape member, Shape target) {
        // Create the appropriate schema based on the shape type.
        Schema refSchema = context.inlineOrReferenceSchema(member);
        QuerySchemaVisitor<T> visitor = new QuerySchemaVisitor<>(context, refSchema, member);
        return target.accept(visitor);
    }

    private Collection<ParameterObject> createRequestHeaderParameters(Context<T> context, OperationShape operation) {
        List<HttpBinding> bindings = HttpBindingIndex.of(context.getModel())
                .getRequestBindings(operation, HttpBinding.Location.HEADER);
        return createHeaderParameters(context, bindings, MessageType.REQUEST, operation, null).values();
    }

    private Map<String, ParameterObject> createHeaderParameters(
            Context<T> context,
            List<HttpBinding> bindings,
            MessageType messageType,
            Shape operationOrError,
            OperationShape operation
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

            param.examples(createExamplesForMembersWithHttpTraits(operationOrError, binding, messageType, operation));

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
                operation,
                HttpBinding.Location.PAYLOAD);

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
        // API Gateway validation requires that in-line schemas must be objects
        // or arrays. These schemas are synthesized as references so that
        // any schemas with string types will pass validation.
        Schema schema = context.inlineOrReferenceSchema(binding.getMember());
        MediaTypeObject mediaTypeObject = getMediaTypeObject(context, schema, operation, shape -> {
            String shapeName = context.getService().getContextualName(shape.getId());
            return shapeName + "InputPayload";
        }).toBuilder()
                .examples(createExamplesForMembersWithHttpTraits(
                        operation,
                        binding,
                        MessageType.REQUEST,
                        null))
                .build();
        RequestBodyObject requestBodyObject = RequestBodyObject.builder()
                .putContent(Objects.requireNonNull(mediaTypeRange), mediaTypeObject)
                .required(binding.getMember().isRequired())
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
        String contextName = context.getService().getContextualName(operation);
        String synthesizedName = stripNonAlphaNumericCharsIfNecessary(context, contextName) + "RequestContent";
        String pointer = context.putSynthesizedSchema(synthesizedName, schema);
        MediaTypeObject mediaTypeObject = MediaTypeObject.builder()
                .schema(Schema.builder().ref(pointer).build())
                .examples(createBodyExamples(operation, bindings, MessageType.REQUEST, null))
                .build();

        // If any of the top level bindings are required, then the body itself must be required.
        boolean required = false;
        for (HttpBinding binding : bindings) {
            if (binding.getMember().isRequired()) {
                required = true;
                break;
            }
        }

        return Optional.of(RequestBodyObject.builder()
                .putContent(mediaType, mediaTypeObject)
                .required(required)
                .build());
    }

    private Map<String, ResponseObject> createResponses(
            Context<T> context,
            HttpBindingIndex bindingIndex,
            EventStreamIndex eventStreamIndex,
            OperationShape operation
    ) {
        Map<String, ResponseObject> result = new TreeMap<>();
        OperationIndex operationIndex = OperationIndex.of(context.getModel());
        StructureShape output = operationIndex.expectOutputShape(operation);
        updateResponsesMapWithResponseStatusAndObject(
                context,
                bindingIndex,
                eventStreamIndex,
                operation,
                output,
                result);

        for (StructureShape error : operationIndex.getErrors(operation)) {
            updateResponsesMapWithResponseStatusAndObject(
                    context,
                    bindingIndex,
                    eventStreamIndex,
                    operation,
                    error,
                    result);
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
                context,
                bindingIndex,
                eventStreamIndex,
                statusCode,
                operationOrError,
                operation);
        responses.put(statusCode, response);
    }

    private ResponseObject createResponse(
            Context<T> context,
            HttpBindingIndex bindingIndex,
            EventStreamIndex eventStreamIndex,
            String statusCode,
            Shape operationOrError,
            OperationShape operation
    ) {
        ResponseObject.Builder responseBuilder = ResponseObject.builder();
        String contextName = context.getService().getContextualName(operationOrError);
        String responseName = stripNonAlphaNumericCharsIfNecessary(context, contextName);
        responseBuilder.description(String.format("%s %s response", responseName, statusCode));
        createResponseHeaderParameters(context, operationOrError, operation)
                .forEach((k, v) -> responseBuilder.putHeader(k, Ref.local(v)));
        addResponseContent(context, bindingIndex, eventStreamIndex, responseBuilder, operationOrError, operation);
        return responseBuilder.build();
    }

    private Map<String, ParameterObject> createResponseHeaderParameters(
            Context<T> context,
            Shape operationOrError,
            OperationShape operation
    ) {
        List<HttpBinding> bindings = HttpBindingIndex.of(context.getModel())
                .getResponseBindings(operationOrError, HttpBinding.Location.HEADER);
        MessageType type = !operationOrError.hasTrait(ErrorTrait.class) ? MessageType.RESPONSE : MessageType.ERROR;
        return createHeaderParameters(context, bindings, type, operationOrError, operation);
    }

    private void addResponseContent(
            Context<T> context,
            HttpBindingIndex bindingIndex,
            EventStreamIndex eventStreamIndex,
            ResponseObject.Builder responseBuilder,
            Shape operationOrError,
            OperationShape operation
    ) {
        List<HttpBinding> payloadBindings = bindingIndex.getResponseBindings(
                operationOrError,
                HttpBinding.Location.PAYLOAD);

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
            createResponsePayload(mediaType,
                    context,
                    payloadBindings.get(0),
                    responseBuilder,
                    operationOrError,
                    operation);
        } else {
            createResponseDocumentIfNeeded(mediaType,
                    context,
                    bindingIndex,
                    responseBuilder,
                    operationOrError,
                    operation);
        }
    }

    private void createResponsePayload(
            String mediaType,
            Context<T> context,
            HttpBinding binding,
            ResponseObject.Builder responseBuilder,
            Shape operationOrError,
            OperationShape operation
    ) {
        Objects.requireNonNull(mediaType, "Unable to determine response media type for " + operationOrError);

        // API Gateway validation requires that in-line schemas must be objects
        // or arrays. These schemas are synthesized as references so that
        // any schemas with string types will pass validation.
        Schema schema = context.inlineOrReferenceSchema(binding.getMember());
        MessageType type = !operationOrError.hasTrait(ErrorTrait.class) ? MessageType.RESPONSE : MessageType.ERROR;
        MediaTypeObject mediaTypeObject = getMediaTypeObject(context, schema, operationOrError, shape -> {
            String shapeName = context.getService().getContextualName(shape.getId());
            return shape instanceof OperationShape
                    ? shapeName + "OutputPayload"
                    : shapeName + "ErrorPayload";
        }).toBuilder()
                .examples(createExamplesForMembersWithHttpTraits(
                        operationOrError,
                        binding,
                        type,
                        operation))
                .build();

        responseBuilder.putContent(mediaType, mediaTypeObject);
    }

    // If a synthetic schema is just a wrapper for another schema, create the
    // MediaTypeObject using the pointer to the existing schema, otherwise add
    // the synthetic schema and create the MediaTypeObject using a new pointer.
    private MediaTypeObject getMediaTypeObject(
            Context<T> context,
            Schema schema,
            Shape shape,
            Function<Shape, String> createSynthesizedName
    ) {
        if (!schema.getType().isPresent() && schema.getRef().isPresent()) {
            return MediaTypeObject.builder()
                    .schema(Schema.builder().ref(schema.getRef().get()).build())
                    .build();
        } else {
            String synthesizedName = createSynthesizedName.apply(shape);
            String pointer = context.putSynthesizedSchema(synthesizedName, schema);
            return MediaTypeObject.builder()
                    .schema(Schema.builder().ref(pointer).build())
                    .build();
        }
    }

    private void createResponseDocumentIfNeeded(
            String mediaType,
            Context<T> context,
            HttpBindingIndex bindingIndex,
            ResponseObject.Builder responseBuilder,
            Shape operationOrError,
            OperationShape operation
    ) {
        List<HttpBinding> bindings = bindingIndex.getResponseBindings(
                operationOrError,
                HttpBinding.Location.DOCUMENT);

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
        String contextName = context.getService().getContextualName(operationOrError);
        String synthesizedName = stripNonAlphaNumericCharsIfNecessary(context, contextName)
                + "ResponseContent";
        String pointer = context.putSynthesizedSchema(synthesizedName, schema);
        MediaTypeObject mediaTypeObject = MediaTypeObject.builder()
                .schema(Schema.builder().ref(pointer).build())
                .examples(createBodyExamples(operationOrError, bindings, messageType, operation))
                .build();

        responseBuilder.putContent(mediaType, mediaTypeObject);
    }

    private String stripNonAlphaNumericCharsIfNecessary(Context<T> context, String name) {
        String alphanumericOnly = NON_ALPHA_NUMERIC.matcher(name).replaceAll("");
        if (context.getConfig().getAlphanumericOnlyRefs() && !alphanumericOnly.equals(name)) {
            LOGGER.info(() -> String.format("Removing non-alphanumeric characters from %s to assure compatibility with"
                    + " vendors that only allow alphanumeric shape names.", name));
            return alphanumericOnly;
        }
        return name;
    }
}
