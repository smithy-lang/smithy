/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiConfig;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that service-level errors don't override operation-specific error examples.
 */
public class ErrorDeconflictingExamplesTest {

    @Test
    public void serviceLevelErrorsShouldNotOverrideOperationSpecificExamples() {
        String modelText = "$version: \"2.0\"\n" +
            "namespace test.service.error\n" +
            "use aws.protocols#restJson1\n" +
            "\n" +
            "@restJson1\n" +
            "service TestService {\n" +
            "    version: \"1.0.0\",\n" +
            "    operations: [TestOp]\n" +
            "    errors: [ServiceError]  // Service-level 400 error\n" +
            "}\n" +
            "\n" +
            "@http(method: \"POST\", uri: \"/test\")\n" +
            "operation TestOp {\n" +
            "    input: TestInput,\n" +
            "    output: TestOutput,\n" +
            "    errors: [CustomError]  // Operation-specific 400 error\n" +
            "}\n" +
            "\n" +
            "structure TestInput { value: String }\n" +
            "structure TestOutput { result: String }\n" +
            "\n" +
            "// Service-level 400 error\n" +
            "@error(\"client\")\n" +
            "@httpError(400)\n" +
            "structure ServiceError {\n" +
            "    message: String\n" +
            "}\n" +
            "\n" +
            "// Operation-specific 400 error - should have examples generated\n" +
            "@error(\"client\")\n" +
            "@httpError(400)\n" +
            "structure CustomError {\n" +
            "    message: String,\n" +
            "    code: String,\n" +
            "    details: String\n" +
            "}\n" +
            "\n" +
            "apply TestOp @examples([\n" +
            "    {\n" +
            "        title: \"Success example\"\n" +
            "        input: { value: \"good\" }\n" +
            "        output: { result: \"success\" }\n" +
            "    },\n" +
            "    {\n" +
            "        title: \"Custom error example\"\n" +
            "        input: { value: \"bad\" }\n" +
            "        error: {\n" +
            "            shapeId: CustomError\n" +
            "            content: {\n" +
            "                message: \"Custom error occurred\"\n" +
            "                code: \"CUSTOM_ERROR\"\n" +
            "                details: \"This should appear in 400 response examples\"\n" +
            "            }\n" +
            "        }\n" +
            "        allowConstraintErrors: true\n" +
            "    }\n" +
            "])";

        Model model = Model.assembler()
                .addImport(getClass().getResource("/META-INF/smithy/aws.protocols.smithy"))
                .addUnparsedModel("test.smithy", modelText)
                .assemble()
                .unwrap();

        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("test.service.error#TestService"));
        config.setOnErrorStatusConflict(OpenApiConfig.ErrorStatusConflictHandlingStrategy.ONE_OF);

        Node result = OpenApiConverter.create().config(config).convertToNode(model);
        ObjectNode openApi = result.expectObjectNode();

        System.out.println("Generated OpenAPI:");
        System.out.println(Node.prettyPrintJson(result));

        ObjectNode paths = openApi.expectMember("paths").expectObjectNode();
        ObjectNode testPath = paths.expectMember("/test").expectObjectNode();
        ObjectNode postOp = testPath.expectMember("post").expectObjectNode();
        ObjectNode responses = postOp.expectMember("responses").expectObjectNode();

        assertTrue(responses.getMember("400").isPresent(), "Expected 400 response for client errors");

        ObjectNode response400 = responses.expectMember("400").expectObjectNode();
        ObjectNode content = response400.expectMember("content").expectObjectNode();
        ObjectNode jsonContent = content.expectMember("application/json").expectObjectNode();

        // The bug: When ValidationException (service-level) and CustomError (operation-level)
        // both map to 400, the service-level error takes precedence and operation-specific
        // examples are lost
        assertTrue(jsonContent.getMember("examples").isPresent(),
            "BUG: Service-level ValidationException overrides operation-specific CustomError examples. " +
            "Expected CustomError examples to appear in 400 response but they are missing.");

        ObjectNode examples = jsonContent.expectMember("examples").expectObjectNode();
        assertFalse(examples.getMembers().isEmpty(),
            "Expected CustomError examples in 400 response content");

        // Look for our specific custom error example content
        boolean foundCustomErrorExample = examples.getMembers().values().stream()
            .filter(Node::isObjectNode)
            .map(node -> node.expectObjectNode())
            .filter(example -> example.getMember("value").isPresent())
            .map(example -> example.expectMember("value").expectObjectNode())
            .anyMatch(value ->
                value.getMember("code").filter(node -> "CUSTOM_ERROR".equals(node.expectStringNode().getValue())).isPresent() &&
                value.getMember("message").filter(node -> "Custom error occurred".equals(node.expectStringNode().getValue())).isPresent()
            );

        assertTrue(foundCustomErrorExample,
            "Expected to find CustomError example with code 'CUSTOM_ERROR' and message 'Custom error occurred' " +
            "but service-level ValidationException appears to be overriding it");
    }
}
