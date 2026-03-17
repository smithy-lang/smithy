/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy.protocols;

import java.io.InputStream;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;
import software.amazon.smithy.utils.IoUtils;

public class SmithyRpcV2JsonProtocolTest {

    @Test
    public void convertsExamples() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("rpc-v2-json.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("example.smithy#MyService"));
        ObjectNode result = OpenApiConverter.create()
                .config(config)
                .convertToNode(model);
        InputStream openApiStream = getClass().getResourceAsStream("rpc-v2-json.openapi.json");

        if (openApiStream == null) {
            throw new RuntimeException("OpenAPI model not found for test case: rpc-v2-json.openapi.json");
        } else {
            Node expectedNode = Node.parse(IoUtils.toUtf8String(openApiStream));
            Node.assertEquals(result, expectedNode);
        }
    }
}
