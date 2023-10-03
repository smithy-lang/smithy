package software.amazon.smithy.openapi.fromsmithy.protocols;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.InputStream;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.smithy.aws.traits.protocols.RestJson1Trait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;
import software.amazon.smithy.openapi.fromsmithy.OpenApiMapper;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.openapi.model.OperationObject;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.SetUtils;

public class AwsRestJson1ProtocolTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "adds-json-document-bodies.json",
            "adds-path-timestamp-format.json",
            "adds-query-timestamp-format.json",
            "adds-header-timestamp-format.json",
            "adds-header-mediatype-format.json",
            "supports-payloads.json",
            "aws-rest-json-uses-jsonname.json",
            "synthesizes-contents.json",
            "greedy-labels.json"
    })

    public void testProtocolResult(String smithy) {
        Model model = Model.assembler()
                .addImport(getClass().getResource(smithy))
                .discoverModels()
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        ObjectNode result = OpenApiConverter.create()
                .config(config)
                .convertToNode(model);
        String openApiModel = smithy.replace(".json", ".openapi.json");
        InputStream openApiStream = getClass().getResourceAsStream(openApiModel);

        if (openApiStream == null) {
            fail("OpenAPI model not found for test case: " + openApiModel);
        } else {
            Node expectedNode = Node.parse(IoUtils.toUtf8String(openApiStream));
            Node.assertEquals(result, expectedNode);
        }
    }

    @Test
    public void canUseCustomMediaType() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("adds-json-document-bodies.json"))
                .discoverModels()
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        config.setJsonContentType("application/x-amz-json-1.0");
        OpenApi result = OpenApiConverter.create()
                .config(config)
                .convert(model);

        Assertions.assertTrue(Node.printJson(result.toNode()).contains("application/x-amz-json-1.0"));
    }

    @Test
    public void canRemoveGreedyLabelNameParameterSuffix() {
        String smithy = "greedy-labels-name-parameter-without-suffix.json";
        Model model = Model.assembler()
                .addImport(getClass().getResource(smithy))
                .discoverModels()
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        config.setRemoveGreedyParameterSuffix(true);
        OpenApi result = OpenApiConverter.create()
                .config(config)
                .convert(model);
        String openApiModel = smithy.replace(".json", ".openapi.json");
        InputStream openApiStream = getClass().getResourceAsStream(openApiModel);

        if (openApiStream == null) {
            fail("OpenAPI model not found for test case: " + openApiModel);
        } else {
            Node expectedNode = Node.parse(IoUtils.toUtf8String(openApiStream));
            Node.assertEquals(result, expectedNode);
        }
    }

    @Test
    public void canRemoveNonAlphaNumericDocumentNames() {
        String smithy = "non-alphanumeric-content-names.json";
        Model model = Model.assembler()
                .addImport(getClass().getResource(smithy))
                .discoverModels()
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        config.setAlphanumericOnlyRefs(true);
        OpenApi result = OpenApiConverter.create()
                .config(config)
                .convert(model);
        String openApiModel = smithy.replace(".json", ".openapi.json");
        InputStream openApiStream = getClass().getResourceAsStream(openApiModel);

        if (openApiStream == null) {
            fail("OpenAPI model not found for test case: " + openApiModel);
        } else {
            Node expectedNode = Node.parse(IoUtils.toUtf8String(openApiStream));
            Node.assertEquals(result, expectedNode);
        }
    }

    private static Stream<Arguments> protocolHeaderCases() {
        return Stream.of(
                Arguments.of(
                        "NoInputOrOutput",
                        SetUtils.of(
                                "X-Amz-User-Agent",
                                "X-Amzn-Trace-Id",
                                "Amz-Sdk-Request",
                                "Amz-Sdk-Invocation-Id"
                        ),
                        SetUtils.of(
                                "X-Amzn-Requestid",
                                "X-Amzn-Errortype",
                                "Content-Length",
                                "Content-Type"
                        )
                ),
                Arguments.of(
                        "EmptyInputAndOutput",
                        SetUtils.of(
                                "X-Amz-User-Agent",
                                "X-Amzn-Trace-Id",
                                "Amz-Sdk-Request",
                                "Amz-Sdk-Invocation-Id"
                        ),
                        SetUtils.of(
                                "X-Amzn-Requestid",
                                "X-Amzn-Errortype",
                                "Content-Length",
                                "Content-Type"
                        )
                ),
                Arguments.of(
                        "OnlyErrorOutput",
                        SetUtils.of(
                                "X-Amz-User-Agent",
                                "X-Amzn-Trace-Id",
                                "Amz-Sdk-Request",
                                "Amz-Sdk-Invocation-Id"
                        ),
                        SetUtils.of(
                                "X-Amzn-Requestid",
                                "X-Amzn-Errortype",
                                "Content-Length",
                                "Content-Type"
                        )
                ),
                Arguments.of(
                        "HttpChecksumRequired",
                        SetUtils.of(
                                "X-Amz-User-Agent",
                                "X-Amzn-Trace-Id",
                                "Amz-Sdk-Request",
                                "Amz-Sdk-Invocation-Id",
                                "Content-Md5"
                        ),
                        SetUtils.of(
                                "X-Amzn-Requestid",
                                "X-Amzn-Errortype",
                                "Content-Length",
                                "Content-Type"
                        )
                ),
                Arguments.of(
                        "HasDiscoveredEndpoint",
                        SetUtils.of(
                                "X-Amz-User-Agent",
                                "X-Amzn-Trace-Id",
                                "Amz-Sdk-Request",
                                "Amz-Sdk-Invocation-Id",
                                "X-Amz-Api-Version"
                        ),
                        SetUtils.of(
                                "X-Amzn-Requestid",
                                "X-Amzn-Errortype",
                                "Content-Length",
                                "Content-Type"
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource("protocolHeaderCases")
    public void assertProtocolHeaders(
            String operationId,
            Set<String> expectedRequestHeaders,
            Set<String> expectedResponseHeaders
    ) {
        Model model = Model.assembler()
                .addImport(getClass().getResource("rest-json-protocol-headers.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Service"));
        config.setAlphanumericOnlyRefs(true);

        AwsRestJson1Protocol protocol = new AwsRestJson1Protocol();
        OperationShape operation = model.expectShape(
                ShapeId.fromParts("smithy.example", operationId), OperationShape.class);

        ContextCapturingMapper contextCaptor = new ContextCapturingMapper();
        OpenApiConverter.create()
                .config(config)
                .addOpenApiMapper(contextCaptor)
                .convert(model);

        Context<RestJson1Trait> context = (Context<RestJson1Trait>) contextCaptor.capturedContext;

        Set<String> requestHeaders = protocol.getProtocolRequestHeaders(context, operation);
        Assertions.assertEquals(expectedRequestHeaders, requestHeaders);

        Set<String> responseHeaders = protocol.getProtocolResponseHeaders(context, operation);
        Assertions.assertEquals(expectedResponseHeaders, responseHeaders);
    }

    private static class ContextCapturingMapper implements OpenApiMapper {

        public Context<? extends Trait> capturedContext;

        @Override
        public byte getOrder() {
            return 127;
        }

        @Override
        public OperationObject updateOperation(
                Context<? extends Trait> context, OperationShape shape, OperationObject operation,
                String httpMethodName, String path
        ) {
            this.capturedContext = context;
            return OpenApiMapper.super.updateOperation(context, shape, operation, httpMethodName, path);
        }
    }
    
    @Test
    public void convertsExamples() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("examples-test.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.examplestrait#Banking"));
        ObjectNode result = OpenApiConverter.create()
                .config(config)
                .convertToNode(model);
        InputStream openApiStream = getClass().getResourceAsStream("examples-test.openapi.json");

        if (openApiStream == null) {
            throw new RuntimeException("OpenAPI model not found for test case: examples-test.openapi.json");
        } else {
            Node expectedNode = Node.parse(IoUtils.toUtf8String(openApiStream));
            Node.assertEquals(result, expectedNode);
        }
    }

    @Test
    public void combinesErrorsWithSameStatusCode() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("error-code-collision-test.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("example#Example"));
        config.setOnErrorStatusConflict(OpenApiConfig.ErrorStatusConflictHandlingStrategy.ONE_OF);
        ObjectNode result = OpenApiConverter.create()
                .config(config)
                .convertToNode(model);
        InputStream openApiStream = getClass()
                .getResourceAsStream("error-code-collision-test-use-oneof.openapi.json");

        if (openApiStream == null) {
            throw new RuntimeException("OpenAPI model not found for test case: "
                    + "error-code-collision-test-use-properties.openapi.json");
        } else {
            Node expectedNode = Node.parse(IoUtils.toUtf8String(openApiStream));
            Node.assertEquals(result, expectedNode);
        }
    }
}
