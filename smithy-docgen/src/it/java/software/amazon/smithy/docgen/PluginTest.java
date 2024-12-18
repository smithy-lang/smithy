/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.MockManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

public class PluginTest {

    private static MockManifest manifest;
    private static Model model;
    private static SmithyBuildPlugin plugin;

    @BeforeAll
    public static void setup() {
        manifest = new MockManifest();
        model = getModel("main.smithy");
        plugin = new SmithyDocPlugin();
    }

    @Test
    public void pluginGeneratesMarkdown() {
        ObjectNode settings = Node.objectNodeBuilder()
                .withMember("service", "com.example#DocumentedService")
                .withMember("format", "markdown")
                .withMember(
                        "references",
                        Node.objectNodeBuilder()
                                .withMember("com.example#ExternalResource", "https://aws.amazon.com")
                                .build())
                .build();
        PluginContext context = getPluginContext(model, settings);

        plugin.execute(context);

        assertFalse(manifest.getFiles().isEmpty());
        Set<Path> files = manifest.getFiles();
        assertTrue(files.contains(Path.of("/content", "index.md")));
    }

    @Test
    public void pluginGeneratesSphinxMarkdown() {
        Model model = getModel("main.smithy");
        ObjectNode settings = Node.objectNodeBuilder()
                .withMember("service", "com.example#DocumentedService")
                .withMember("format", "sphinx-markdown")
                .withMember(
                        "references",
                        Node.objectNodeBuilder()
                                .withMember("com.example#ExternalResource", "https://aws.amazon.com")
                                .build())
                .build();
        PluginContext context = getPluginContext(model, settings);

        plugin.execute(context);

        assertFalse(manifest.getFiles().isEmpty());
        Set<Path> files = manifest.getFiles();
        assertTrue(files.contains(Path.of("/requirements.txt")));
        assertTrue(files.contains(Path.of("/content", "conf.py")));
        assertTrue(files.contains(Path.of("/content", "index.md")));
    }

    private static Model getModel(String path) {
        return Model.assembler()
                .addImport(PluginTest.class.getResource(path))
                .discoverModels(PluginTest.class.getClassLoader())
                .assemble()
                .unwrap();
    }

    private static PluginContext getPluginContext(Model model, ObjectNode settings) {
        return PluginContext.builder()
                .fileManifest(manifest)
                .model(model)
                .settings(settings)
                .build();
    }
}
