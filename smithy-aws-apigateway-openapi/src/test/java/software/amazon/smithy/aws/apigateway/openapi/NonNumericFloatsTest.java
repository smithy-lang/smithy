/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;
import software.amazon.smithy.utils.IoUtils;

public class NonNumericFloatsTest {
    @Test
    public void handlesNonNumericFloats() {
        Model model = Model.assembler(getClass().getClassLoader())
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("non-numeric-floats.json"))
                .assemble()
                .unwrap();
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("example.smithy#MyService"));
        config.setSupportNonNumericFloats(true);
        ObjectNode result = OpenApiConverter.create().config(config).convertToNode(model);
        Node expectedNode = Node.parse(IoUtils.toUtf8String(
                getClass().getResourceAsStream("non-numeric-floats.openapi.json")));

        Node.assertEquals(result, expectedNode);
    }
}
