/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

public class PluginTest {

    private static final URL SNIPPETS = Objects.requireNonNull(PluginTest.class.getResource("snippets.json"));

    private static FileManifest manifest;
    private static FileManifest sharedManifest;
    private static Model model;
    private static SmithyBuildPlugin plugin;

    @BeforeAll
    public static void setup(@TempDir Path tempDir) {
        manifest = FileManifest.create(tempDir.resolve("main"));
        sharedManifest = FileManifest.create(tempDir.resolve("shared"));
        model = getModel("main.smithy");
        plugin = new SmithyDocPlugin();
    }

    @Test
    public void pluginGeneratesMarkdown() throws Exception {
        ObjectNode settings = Node.objectNodeBuilder()
                .withMember("service", "com.example#DocumentedService")
                .withMember("format", "markdown")
                .withMember(
                        "references",
                        Node.objectNodeBuilder()
                                .withMember("com.example#ExternalResource", "https://aws.amazon.com")
                                .build())
                .withMember("snippetConfigs", Node.fromStrings(Paths.get(SNIPPETS.toURI()).toString()))
                .build();
        PluginContext context = getPluginContext(model, settings);

        plugin.execute(context);

        assertFalse(manifest.getFiles().isEmpty());
        Set<Path> files = manifest.getFiles()
                .stream()
                .map(path -> manifest.getBaseDir().relativize(path))
                .collect(Collectors.toSet());
        assertTrue(files.contains(Path.of("content", "index.md")));
    }

    @Test
    public void pluginGeneratesSphinxMarkdown() throws Exception {
        Model model = getModel("main.smithy");
        ObjectNode settings = Node.objectNodeBuilder()
                .withMember("service", "com.example#DocumentedService")
                .withMember("format", "sphinx-markdown")
                .withMember(
                        "references",
                        Node.objectNodeBuilder()
                                .withMember("com.example#ExternalResource", "https://aws.amazon.com")
                                .build())
                .withMember("snippetConfigs", Node.fromStrings(Paths.get(SNIPPETS.toURI()).toString()))
                .build();
        PluginContext context = getPluginContext(model, settings);

        plugin.execute(context);
        assertFalse(manifest.getFiles().isEmpty());
        Set<Path> files = manifest.getFiles()
                .stream()
                .map(path -> manifest.getBaseDir().relativize(path))
                .collect(Collectors.toSet());
        assertTrue(files.contains(Path.of("requirements.txt")));
        assertTrue(files.contains(Path.of("content", "conf.py")));
        assertTrue(files.contains(Path.of("content", "index.md")));

        // Assert that the transform to upgrade enum strings is applied.
        assertTrue(files.contains(Path.of("content", "shapes", "LegacyEnumTrait.md")));
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
                .sharedFileManifest(sharedManifest)
                .model(model)
                .settings(settings)
                .build();
    }
}
