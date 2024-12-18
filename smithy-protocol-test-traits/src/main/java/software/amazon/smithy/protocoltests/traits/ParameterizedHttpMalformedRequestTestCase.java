/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.protocoltests.traits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SimpleCodeWriter;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.Tagged;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Defines a parameterized test case for malformed HTTP requests.
 *
 * This class is *not* preferred for code generation - use {@link HttpMalformedRequestTestCase}
 * instances instead, retrieved from {@link HttpMalformedRequestTestsTrait#getTestCases()}.
 */
@SmithyUnstableApi
final class ParameterizedHttpMalformedRequestTestCase
        implements Tagged, ToNode, ToSmithyBuilder<ParameterizedHttpMalformedRequestTestCase> {

    private static final String DOCUMENTATION = "documentation";
    private static final String ID = "id";
    private static final String PROTOCOL = "protocol";
    private static final String REQUEST = "request";
    private static final String RESPONSE = "response";
    private static final String TAGS = "tags";
    private static final String TEST_PARAMETERS = "testParameters";

    private final String documentation;
    private final String id;
    private final ShapeId protocol;
    private final HttpMalformedRequestDefinition request;
    private final HttpMalformedResponseDefinition response;
    private final List<String> tags;
    private final Map<String, List<String>> testParameters;

    private ParameterizedHttpMalformedRequestTestCase(Builder builder) {
        documentation = builder.documentation;
        id = SmithyBuilder.requiredState(ID, builder.id);
        protocol = SmithyBuilder.requiredState(PROTOCOL, builder.protocol);
        request = SmithyBuilder.requiredState(REQUEST, builder.request);
        response = SmithyBuilder.requiredState(RESPONSE, builder.response);
        tags = ListUtils.copyOf(builder.tags);
        testParameters = MapUtils.copyOf(builder.testParameters);
    }

    public Optional<String> getDocumentation() {
        return Optional.ofNullable(documentation);
    }

    public String getId() {
        return id;
    }

    public ShapeId getProtocol() {
        return protocol;
    }

    public HttpMalformedRequestDefinition getRequest() {
        return request;
    }

    public HttpMalformedResponseDefinition getResponse() {
        return response;
    }

    @Override
    public List<String> getTags() {
        return tags;
    }

    public Map<String, List<String>> getTestParameters() {
        return testParameters;
    }

    public List<HttpMalformedRequestTestCase> generateTestCasesFromParameters() {
        if (testParameters.isEmpty()) {
            HttpMalformedRequestTestCase.Builder builder = HttpMalformedRequestTestCase.builder()
                    .id(getId())
                    .protocol(getProtocol())
                    .request(request)
                    .response(response)
                    .tags(getTags());
            getDocumentation().ifPresent(builder::documentation);
            return ListUtils.of(builder.build());
        }

        int paramLength = testParameters.values()
                .stream()
                .findFirst()
                .map(List::size)
                .orElseThrow(IllegalStateException::new);
        final List<HttpMalformedRequestTestCase> testCases = new ArrayList<>(paramLength);
        for (int i = 0; i < paramLength; i++) {
            final SimpleCodeWriter writer = new SimpleCodeWriter();
            for (Map.Entry<String, List<String>> e : testParameters.entrySet()) {
                writer.putContext(e.getKey(), e.getValue().get(i));
            }

            HttpMalformedRequestTestCase.Builder builder = HttpMalformedRequestTestCase.builder()
                    .id(String.format(getId() + "_case%d", i))
                    .protocol(getProtocol())
                    .tags(getTags().stream().map(writer::format).collect(Collectors.toList()));
            getDocumentation().map(writer::format).ifPresent(builder::documentation);

            testCases.add(builder.request(interpolateRequest(request, writer))
                    .response(interpolateResponse(response, writer))
                    .build());
        }
        return testCases;
    }

    private static HttpMalformedResponseDefinition interpolateResponse(
            HttpMalformedResponseDefinition response,
            SimpleCodeWriter writer
    ) {
        HttpMalformedResponseDefinition.Builder responseBuilder =
                response.toBuilder().headers(formatHeaders(writer, response.getHeaders()));
        response.getBody()
                .map(responseBody -> {
                    HttpMalformedResponseBodyDefinition.Builder bodyBuilder = responseBody.toBuilder()
                            .mediaType(writer.format(responseBody.getMediaType()));
                    responseBody.getContents().map(writer::format).ifPresent(bodyBuilder::contents);
                    responseBody.getMessageRegex().map(writer::format).ifPresent(bodyBuilder::messageRegex);
                    return bodyBuilder.build();
                })
                .ifPresent(responseBuilder::body);
        return responseBuilder.build();
    }

    private static HttpMalformedRequestDefinition interpolateRequest(
            HttpMalformedRequestDefinition request,
            SimpleCodeWriter writer
    ) {
        HttpMalformedRequestDefinition.Builder requestBuilder = request.toBuilder()
                .headers(formatHeaders(writer, request.getHeaders()))
                .queryParams(request.getQueryParams().stream().map(writer::format).collect(Collectors.toList()));
        request.getBody().map(writer::format).ifPresent(requestBuilder::body);
        request.getUri().map(writer::format).ifPresent(requestBuilder::uri);
        return requestBuilder.build();
    }

    private static Map<String, String> formatHeaders(SimpleCodeWriter writer, Map<String, String> headers) {
        Map<String, String> newHeaders = new HashMap<>();
        for (Map.Entry<String, String> headerEntry : headers.entrySet()) {
            newHeaders.put(writer.format(headerEntry.getKey()), writer.format(headerEntry.getValue()));
        }
        return newHeaders;
    }

    public static ParameterizedHttpMalformedRequestTestCase fromNode(Node node) {
        ParameterizedHttpMalformedRequestTestCase.Builder builder = builder();
        ObjectNode o = node.expectObjectNode();
        o.getStringMember(DOCUMENTATION).map(StringNode::getValue).ifPresent(builder::documentation);
        builder.id(o.expectStringMember(ID).getValue());
        builder.protocol(o.expectStringMember(PROTOCOL).expectShapeId());
        builder.request(HttpMalformedRequestDefinition.fromNode(o.expectObjectMember(REQUEST)));
        builder.response(HttpMalformedResponseDefinition.fromNode(o.expectObjectMember(RESPONSE)));
        o.getArrayMember(TAGS).ifPresent(tags -> {
            builder.tags(tags.getElementsAs(StringNode::getValue));
        });
        o.getObjectMember(TEST_PARAMETERS).ifPresent(params -> {
            Map<String, List<String>> paramsMap = new HashMap<>();
            for (Map.Entry<String, Node> e : params.getStringMap().entrySet()) {
                paramsMap.put(e.getKey(),
                        e.getValue().expectArrayNode().getElementsAs(n -> n.expectStringNode().getValue()));
            }
            builder.testParameters(paramsMap);
        });
        return builder.build();
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder builder = Node.objectNodeBuilder()
                .withOptionalMember(DOCUMENTATION, getDocumentation().map(Node::from))
                .withMember(ID, getId())
                .withMember(PROTOCOL, getProtocol().toString())
                .withMember(REQUEST, getRequest().toNode())
                .withMember(RESPONSE, getResponse().toNode());

        if (!tags.isEmpty()) {
            builder.withMember(TAGS, ArrayNode.fromStrings(getTags()));
        }
        if (!testParameters.isEmpty()) {
            ObjectNode.Builder paramBuilder = ObjectNode.objectNodeBuilder();
            for (Map.Entry<String, List<String>> e : getTestParameters().entrySet()) {
                paramBuilder.withMember(e.getKey(), ArrayNode.fromStrings(e.getValue()));
            }
            builder.withMember(TEST_PARAMETERS, paramBuilder.build());
        }
        return builder.build();
    }

    @Override
    public Builder toBuilder() {
        Builder builder = builder()
                .id(getId())
                .protocol(getProtocol())
                .request(getRequest())
                .response(getResponse())
                .tags(getTags())
                .testParameters(getTestParameters());
        getDocumentation().ifPresent(builder::documentation);
        return builder;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder used to create a ParameterizedHttpMalformedRequestTestCase.
     */
    public static final class Builder implements SmithyBuilder<ParameterizedHttpMalformedRequestTestCase> {

        private String documentation;
        private String id;
        private ShapeId protocol;
        private HttpMalformedRequestDefinition request;
        private HttpMalformedResponseDefinition response;
        private final List<String> tags = new ArrayList<>();
        private final Map<String, List<String>> testParameters = new HashMap<>();

        private Builder() {}

        public Builder documentation(String documentation) {
            this.documentation = documentation;
            return this;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder protocol(ToShapeId protocol) {
            this.protocol = protocol.toShapeId();
            return this;
        }

        public Builder request(HttpMalformedRequestDefinition request) {
            this.request = request;
            return this;
        }

        public Builder response(HttpMalformedResponseDefinition response) {
            this.response = response;
            return this;
        }

        public Builder tags(List<String> tags) {
            this.tags.clear();
            this.tags.addAll(tags);
            return this;
        }

        public Builder testParameters(Map<String, List<String>> testParameters) {
            this.testParameters.clear();
            this.testParameters.putAll(testParameters);
            return this;
        }

        @Override
        public ParameterizedHttpMalformedRequestTestCase build() {
            return new ParameterizedHttpMalformedRequestTestCase(this);
        }
    }
}
