package software.amazon.smithy.openapi.fromsmithy.protocols;

import java.io.InputStream;
import java.util.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiConstants;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.utils.IoUtils;

public class AwsRestJsonProtocolTest {
    private static final Logger LOGGER = Logger.getLogger(AwsRestJsonProtocolTest.class.getName());

    @ParameterizedTest
    @ValueSource(strings = {
            "adds-json-document-bodies.json",
            "adds-path-timestamp-format.json",
            "adds-query-timestamp-format.json",
            "adds-query-blob-format.json",
            "adds-header-timestamp-format.json",
            "adds-header-mediatype-format.json",
            "supports-payloads.json",
            "aws-rest-json-uses-jsonname.json"
    })

    public void testProtocolResult(String smithy) {
        Model model = Model.assembler()
                .addImport(getClass().getResource(smithy))
                .assemble()
                .unwrap();
        ObjectNode result = OpenApiConverter.create()
                .putSetting(OpenApiConstants.DISABLE_PRIMITIVE_INLINING, true)
                .convertToNode(model, ShapeId.from("smithy.example#Service"));
        String openApiModel = smithy.replace(".json", ".openapi.json");
        InputStream openApiStream = getClass().getResourceAsStream(openApiModel);

        if (openApiStream == null) {
            LOGGER.warning("OpenAPI model not found for test case: " + openApiModel);
        } else {
            Node expectedNode = Node.parse(IoUtils.toUtf8String(openApiStream));
            Node.assertEquals(result, expectedNode);
        }

        // Attempt to compare the inlined model variant if available.
        {
            String inlinedOpenApiModel = smithy.replace(".json", ".openapi.inlined.json");
            InputStream inlinedOpenApiStream = getClass().getResourceAsStream(inlinedOpenApiModel);
            if (inlinedOpenApiStream != null) {
                ObjectNode inlinedResult = OpenApiConverter.create()
                        .convertToNode(model, ShapeId.from("smithy.example#Service"));
                Node.assertEquals(inlinedResult, Node.parse(IoUtils.toUtf8String(inlinedOpenApiStream)));
            } else {
                LOGGER.info("No .inlined.json test case found for " + smithy);
            }
        }
    }

    @Test
    public void canUseCustomMediaType() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("adds-json-document-bodies.json"))
                .assemble()
                .unwrap();
        OpenApi result = OpenApiConverter.create()
                .putSetting(OpenApiConstants.AWS_JSON_CONTENT_TYPE, "application/x-amz-json-1.0")
                .convert(model, ShapeId.from("smithy.example#Service"));

        Assertions.assertTrue(Node.printJson(result.toNode()).contains("application/x-amz-json-1.0"));
    }
}
