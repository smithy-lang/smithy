/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.fromsmithy;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.MockManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.openapi.OpenApiException;

public class Smithy2OpenApiTest {
    @Test
    public void pluginConvertsModel() {
        Model model = Model.assembler()
                .addImport(OpenApiConverterTest.class.getResource("test-service.json"))
                .discoverModels()
                .assemble()
                .unwrap();
        MockManifest manifest = new MockManifest();
        PluginContext context = PluginContext.builder()
                .settings(Node.objectNode()
                        .withMember("service", "example.rest#RestService"))
                .fileManifest(manifest)
                .model(model)
                .originalModel(model)
                .build();
        new Smithy2OpenApi().execute(context);
        assertTrue(manifest.hasFile("RestService.openapi.json"));
    }

    @Test
    public void throwsWhenServiceNotConfigured() {
        Model model = Model.assembler()
                .addImport(OpenApiConverterTest.class.getResource("test-service.json"))
                .discoverModels()
                .assemble()
                .unwrap();
        MockManifest manifest = new MockManifest();
        PluginContext context = PluginContext.builder()
                .settings(Node.objectNode())
                .fileManifest(manifest)
                .model(model)
                .originalModel(model)
                .build();
        assertThrows(OpenApiException.class, () -> {
            new Smithy2OpenApi().execute(context);
        });
    }
}
