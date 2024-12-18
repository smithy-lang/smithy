/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.MockManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.ObjectNode;

public class TraitCodegenPluginTest {
    private static final int EXPECTED_NUMBER_OF_FILES = 60;

    private MockManifest manifest;
    private Model model;

    @BeforeEach
    void setup() {
        manifest = new MockManifest();
        model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .assemble()
                .unwrap();
    }

    @Test
    public void generatesExpectedTraitFiles() {
        PluginContext context = PluginContext.builder()
                .fileManifest(manifest)
                .settings(ObjectNode.builder()
                        .withMember("package", "com.example.traits")
                        .withMember("namespace", "test.smithy.traitcodegen")
                        .withMember("header", ArrayNode.fromStrings("Header line One"))
                        .build())
                .model(model)
                .build();

        SmithyBuildPlugin plugin = new TraitCodegenPlugin();
        plugin.execute(context);

        assertFalse(manifest.getFiles().isEmpty());
        assertEquals(EXPECTED_NUMBER_OF_FILES, manifest.getFiles().size());
        List<String> fileList = manifest.getFiles().stream().map(Path::toString).collect(Collectors.toList());
        assertThat(fileList,
                hasItem(
                        Paths.get("/META-INF/services/software.amazon.smithy.model.traits.TraitService").toString()));
        assertThat(fileList,
                hasItem(
                        Paths.get("/com/example/traits/nested/NestedNamespaceTrait.java").toString()));
        assertThat(fileList,
                hasItem(
                        Paths.get("/com/example/traits/nested/NestedNamespaceStruct.java").toString()));
    }

    @Test
    public void filtersTags() {
        PluginContext context = PluginContext.builder()
                .fileManifest(manifest)
                .settings(ObjectNode.builder()
                        .withMember("package", "com.example.traits")
                        .withMember("namespace", "test.smithy.traitcodegen")
                        .withMember("header", ArrayNode.fromStrings("Header line One"))
                        .withMember("excludeTags", ArrayNode.fromStrings("filterOut"))
                        .build())
                .model(model)
                .build();

        SmithyBuildPlugin plugin = new TraitCodegenPlugin();
        plugin.execute(context);

        assertFalse(manifest.getFiles().isEmpty());
        assertEquals(EXPECTED_NUMBER_OF_FILES - 1, manifest.getFiles().size());
    }

    @Test
    public void addsHeaderLines() {
        PluginContext context = PluginContext.builder()
                .fileManifest(manifest)
                .settings(ObjectNode.builder()
                        .withMember("package", "com.example.traits")
                        .withMember("namespace", "test.smithy.traitcodegen")
                        .withMember("header", ArrayNode.fromStrings("Header line one", "Header line two"))
                        .build())
                .model(model)
                .build();

        SmithyBuildPlugin plugin = new TraitCodegenPlugin();
        plugin.execute(context);

        assertFalse(manifest.getFiles().isEmpty());
        assertEquals(EXPECTED_NUMBER_OF_FILES, manifest.getFiles().size());
        Optional<String> fileStringOptional = manifest.getFileString(
                Paths.get("com/example/traits/idref/IdRefStructTrait.java").toString());
        assertTrue(fileStringOptional.isPresent());
        assertThat(fileStringOptional.get(),
                startsWith("/**\n" +
                        " * Header line one\n" +
                        " * Header line two\n" +
                        " */"));
    }
}
