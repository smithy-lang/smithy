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

public class CheckForGreedyLabelsTest {
    private static Model model;

    @BeforeAll
    public static void before() {
        model = Model.assembler()
                .addImport(RemoveUnusedComponentsTest.class.getResource("greedy-labels.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
    }

    @AfterAll
    public static void after() {
        model = null;
    }

    @Test
    public void logsInsteadOfThrows() {
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Greedy"));
        OpenApiConverter.create().config(config).convert(model);
    }

    @Test
    public void keepsUnusedSchemas() {
        OpenApiConfig config = new OpenApiConfig();
        config.setService(ShapeId.from("smithy.example#Greedy"));
        config.setForbidGreedyLabels(true);

        Exception thrown = Assertions.assertThrows(OpenApiException.class, () -> {
            OpenApiConverter.create()
                    .config(config)
                    .convert(model);
        });

        Assertions.assertTrue(thrown.getMessage().contains("greedy"));
    }
}
