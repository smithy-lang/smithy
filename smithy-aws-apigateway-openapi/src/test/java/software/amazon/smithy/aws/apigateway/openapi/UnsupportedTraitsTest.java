/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.openapi;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.OpenApiException;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;

public class UnsupportedTraitsTest {
    @Test
    public void failsOnHttpChecksumTrait() {
        Exception thrown = Assertions.assertThrows(OpenApiException.class, () -> {
            Model model = Model.assembler(getClass().getClassLoader())
                    .discoverModels(getClass().getClassLoader())
                    .addImport(getClass().getResource("unsupported-http-checksum.smithy"))
                    .assemble()
                    .unwrap();
            OpenApiConfig config = new OpenApiConfig();
            config.setService(ShapeId.from("example.smithy#MyService"));
            OpenApiConverter.create().config(config).convertToNode(model);
        });

        Assertions.assertTrue(thrown.getMessage().contains("httpChecksum"));
    }
}
