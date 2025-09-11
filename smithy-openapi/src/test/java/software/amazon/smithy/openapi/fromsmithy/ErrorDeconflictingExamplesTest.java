/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy;

import java.io.InputStream;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.utils.IoUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that all error examples are included in the openapi when error deconflicting (`onErrorStatusConflict`) is set to `oneOf`.
 */
public class ErrorDeconflictingExamplesTest {

    @Test
    public void serviceLevelErrorsShouldNotOverrideOperationSpecificExamples() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("/META-INF/smithy/aws.protocols.smithy"))
                .addImport(getClass().getResource("error-deconflicting-test.smithy"))
                .assemble()
                .unwrap();

        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("test.service.error#TestService"));
        config.setOnErrorStatusConflict(OpenApiConfig.ErrorStatusConflictHandlingStrategy.ONE_OF);

        Node result = OpenApiConverter.create().config(config).convertToNode(model);

        InputStream expectedStream = getClass().getResourceAsStream("error-deconflicting-test.openapi.json");
        if (expectedStream == null) {
            fail("Expected OpenAPI output file not found: error-deconflicting-test.openapi.json");
        }

        Node expected = Node.parse(IoUtils.toUtf8String(expectedStream));
        Node.assertEquals(result, expected);
    }
}
