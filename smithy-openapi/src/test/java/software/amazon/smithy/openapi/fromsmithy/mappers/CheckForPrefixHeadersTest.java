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

public class CheckForPrefixHeadersTest {
    private static Model model;

    @BeforeAll
    public static void before() {
        model = Model.assembler()
                .addImport(RemoveUnusedComponentsTest.class.getResource("prefix-headers.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
    }

    @AfterAll
    public static void after() {
        model = null;
    }

    @Test
    public void canIgnorePrefixHeaders() {
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#PrefixHeaders"));
        config.setOnHttpPrefixHeaders(OpenApiConfig.HttpPrefixHeadersStrategy.WARN);
        OpenApiConverter.create()
                .config(config)
                .convert(model);
    }

    @Test
    public void throwsOnPrefixHeadersByDefault() {
        Exception thrown = Assertions.assertThrows(OpenApiException.class, () -> {
            OpenApiConfig config = new OpenApiConfig();
            config.setService(ShapeId.from("smithy.example#PrefixHeaders"));
            OpenApiConverter.create().config(config).convert(model);
        });

        Assertions.assertTrue(thrown.getMessage().contains("httpPrefixHeaders"));
    }
}
