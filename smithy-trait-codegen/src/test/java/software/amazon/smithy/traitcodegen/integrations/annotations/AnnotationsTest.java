/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen.integrations.annotations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.MockManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.traitcodegen.TraitCodegenPlugin;

public class AnnotationsTest {

    private static final URL TEST_FILE =
            Objects.requireNonNull(AnnotationsTest.class.getResource("annotations-test.smithy"));
    private final MockManifest manifest = new MockManifest();

    @BeforeEach
    void setup() {
        Model model = Model.assembler()
                .addImport(TEST_FILE)
                .assemble()
                .unwrap();
        PluginContext context = PluginContext.builder()
                .fileManifest(manifest)
                .settings(
                        ObjectNode.builder()
                                .withMember("package", "com.example.traits")
                                .withMember("namespace", "com.example.annotations")
                                .withMember("header", ArrayNode.fromStrings("Header line One"))
                                .build())
                .model(model)
                .build();
        SmithyBuildPlugin plugin = new TraitCodegenPlugin();
        plugin.execute(context);

        assertFalse(manifest.getFiles().isEmpty());
    }

    @Test
    void hasSmithyGeneratedAnnotationEvenIfNoDocstring() {
        String fileContents = getFileContentsFromShapeName("HasSmithyGeneratedClass", true);
        String fileContentsNested = getFileContentsFromShapeName("HasSmithyGeneratedNested", false);

        assertTrue(fileContents.contains("@SmithyGenerated"));
        assertTrue(fileContentsNested.contains("@SmithyGenerated"));
    }

    @Test
    void deprecatedAnnotationOnClass() {
        String fileContents = getFileContentsFromShapeName("DeprecatedStructure", true);
        String expected = "@Deprecated\n" +
                "@SmithyGenerated\n" +
                "public final class DeprecatedStructureTrait";
        assertTrue(fileContents.contains(expected));
    }

    @Test
    void deprecatedAnnotationOnMember() {
        String fileContents = getFileContentsFromShapeName("DeprecatedStructure", true);
        String expected = "    @Deprecated\n" +
                "    public Optional<String> getDeprecatedMember() {";
        assertTrue(fileContents.contains(expected));
    }

    @Test
    void deprecatedAnnotationWithDocsOnMember() {
        String fileContents = getFileContentsFromShapeName("DeprecatedStructure", true);
        String expected = "    /**\n" +
                "     * Has docs in addition to deprecated\n" +
                "     */\n" +
                "    @Deprecated\n" +
                "    public Optional<String> getDeprecatedWithDocs() {";
        assertTrue(fileContents.contains(expected));
    }

    @Test
    void unstableAnnotationOnClass() {
        String fileContents = getFileContentsFromShapeName("UnstableStructure", true);
        String expected = "@SmithyUnstableApi\n" +
                "@SmithyGenerated\n" +
                "public final class UnstableStructureTrait extends AbstractTrait";
        assertTrue(fileContents.contains(expected));
    }

    @Test
    void unstableAnnotationOnMember() {
        String fileContents = getFileContentsFromShapeName("UnstableStructure", true);
        String expected = "    @SmithyUnstableApi\n" +
                "    public Optional<String> getUnstableMember() {";
        assertTrue(fileContents.contains(expected));
    }

    @Test
    void unstableAnnotationWithDocsOnMember() {
        String fileContents = getFileContentsFromShapeName("UnstableStructure", true);
        String expected = "    /**\n" +
                "     * Has docs in addition to unstable\n" +
                "     */\n" +
                "    @SmithyUnstableApi\n" +
                "    public Optional<String> getUnstableWithDocs() {";
        assertTrue(fileContents.contains(expected));
    }

    @Test
    void deprecatedAnnotationOnEnumVariant() {
        String fileContents = getFileContentsFromShapeName("EnumWithAnnotations", true);
        String expected = "        @Deprecated\n" +
                "        DEPRECATED(\"DEPRECATED\"),";
        assertTrue(fileContents.contains(expected));
    }

    @Test
    void unstableAnnotationOnEnumVariant() {
        String fileContents = getFileContentsFromShapeName("EnumWithAnnotations", true);
        String expected = "        @SmithyUnstableApi\n" +
                "        UNSTABLE(\"UNSTABLE\"),";
        assertTrue(fileContents.contains(expected));
    }

    private String getFileContentsFromShapeName(String className, boolean isTrait) {
        String suffix = isTrait ? "Trait" : "";
        String path = String.format("com/example/traits/%s%s.java", className, suffix);
        Optional<String> fileStringOptional = manifest.getFileString(Paths.get(path).toString());
        assertTrue(fileStringOptional.isPresent());
        return fileStringOptional.get();
    }
}
