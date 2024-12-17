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
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;
import software.amazon.smithy.openapi.fromsmithy.OpenApiMapper;
import software.amazon.smithy.openapi.model.OpenApi;
import software.amazon.smithy.openapi.model.SecurityScheme;

public class RemoveUnusedComponentsTest {
    private static Model model;

    @BeforeAll
    public static void before() {
        model = Model.assembler()
                .addImport(RemoveUnusedComponentsTest.class.getResource("small-service.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
    }

    @AfterAll
    public static void after() {
        model = null;
    }

    @Test
    public void removesUnusedSchemas() {
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Small"));
        OpenApi result = OpenApiConverter.create().config(config).convert(model);

        Assertions.assertFalse(result.getComponents().getSchemas().isEmpty());
        Assertions.assertTrue(result.getComponents().getSchemas().containsKey("SmallOperationRequestContent"));
        Assertions.assertTrue(result.getComponents().getSchemas().containsKey("StringMap"));
        Assertions
                .assertFalse(result.getComponents().getSchemas().containsKey("SmallOperationExceptionResponseContent"));
    }

    @Test
    public void keepsUnusedSchemas() {
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Small"));
        config.setKeepUnusedComponents(true);
        OpenApi result = OpenApiConverter.create()
                .config(config)
                .convert(model);

        // The input structure remains in the output even though it's unreferenced.
        Assertions.assertFalse(result.getComponents().getSchemas().isEmpty());
    }

    @Test
    public void removesUnusedSchemes() {
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Small"));

        OpenApi result = OpenApiConverter.create()
                .config(config)
                .addOpenApiMapper(new OpenApiMapper() {
                    @Override
                    public OpenApi after(Context context, OpenApi openapi) {
                        return openapi.toBuilder()
                                .components(openapi.getComponents()
                                        .toBuilder()
                                        .putSecurityScheme("foo", SecurityScheme.builder().type("apiKey").build())
                                        .build())
                                .build();
                    }
                })
                .convert(model);

        Assertions.assertFalse(result.getComponents().getSecuritySchemes().keySet().contains("foo"));
    }
}
