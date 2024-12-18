/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy.mappers;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.openapi.OpenApiConfig;
import software.amazon.smithy.openapi.OpenApiException;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;

public class UnsupportedTraitsPluginTest {
    private static Model model;

    @BeforeAll
    public static void before() {
        model = Model.assembler()
                .addImport(UnsupportedTraitsPluginTest.class.getResource("endpoint-service.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
    }

    @AfterAll
    public static void after() {
        model = null;
    }

    @Test
    public void logsWhenUnsupportedTraitsAreFound() {
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#EndpointService"));
        config.setIgnoreUnsupportedTraits(true);
        OpenApiConverter.create()
                .config(config)
                .convert(model);
    }

    @Test
    public void throwsWhenUnsupportedTraitsAreFound() {
        Exception thrown = Assertions.assertThrows(OpenApiException.class, () -> {
            OpenApiConfig config = new OpenApiConfig();
            config.setService(ShapeId.from("smithy.example#EndpointService"));
            OpenApiConverter.create().config(config).convert(model);
        });

        Assertions.assertTrue(thrown.getMessage().contains("endpoint"));
    }
}
