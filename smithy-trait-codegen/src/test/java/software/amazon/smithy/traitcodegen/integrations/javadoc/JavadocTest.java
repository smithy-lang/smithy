/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen.integrations.javadoc;

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

public class JavadocTest {

    private static final URL TEST_FILE = Objects.requireNonNull(JavadocTest.class.getResource("javadoc-test.smithy"));
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
                                .withMember("namespace", "com.example.javadoc")
                                .withMember("header", ArrayNode.fromStrings("Header line One"))
                                .build())
                .model(model)
                .build();
        SmithyBuildPlugin plugin = new TraitCodegenPlugin();
        plugin.execute(context);

        assertFalse(manifest.getFiles().isEmpty());
    }

    @Test
    void hasClassLevelDocstring() {
        String fileContents = getFileContentsFromShapeName("DocumentationWrapping", true);
        String expected = "/**\n" +
                " * Basic class-level documentation\n" +
                " */\n" +
                "@SmithyGenerated\n" +
                "public final class DocumentationWrappingTrait extends AbstractTrait implements ToSmithyBuilder<DocumentationWrappingTrait> {";
        assertTrue(fileContents.contains(expected));
    }

    @Test
    void wrapsBasicTextString() {
        String fileContents = getFileContentsFromShapeName("DocumentationWrapping", true);
        String expected = "    /**\n" +
                "     * This is a long long docstring that should be wrapped. Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do\n"
                +
                "     * eiusmod tempor incididunt ut labore et dolore magna aliqua.\n" +
                "     */\n" +
                "    public Optional<String> getShouldBeWrapped() {\n" +
                "        return Optional.ofNullable(shouldBeWrapped);\n" +
                "    }";
        assertTrue(fileContents.contains(expected));
    }

    @Test
    void doesNotFormatContentInsideHtmlTags() {
        String fileContents = getFileContentsFromShapeName("DocumentationWrapping", true);
        String expected = "    /**\n" +
                "     * Documentation includes preformatted text that should not be messed with. This sentence should still be partially\n"
                +
                "     * wrapped.\n" +
                "     * For example:\n" +
                "     * <pre>\n" +
                "     * Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.\n"
                +
                "     * </pre>\n" +
                "     * <ul>\n" +
                "     *     <li> Lorem ipsum dolor sit amet, consectetur adipiscing elit Lorem ipsum dolor sit amet, consectetur adipiscing elit consectetur adipiscing </li>\n"
                +
                "     *     <li> Lorem ipsum dolor sit amet, consectetur adipiscing elit Lorem ipsum dolor sit amet, consectetur adipiscing elit consectetur adipiscing </li>\n"
                +
                "     * </ul>\n" +
                "     */\n" +
                "    public Optional<String> getPreformattedText() {\n" +
                "        return Optional.ofNullable(preformattedText);\n" +
                "    }";
        assertTrue(fileContents.contains(expected));
    }

    @Test
    void deprecatedAnnotationAndNoteOnClass() {
        String fileContents = getFileContentsFromShapeName("DeprecatedStructure", true);
        String expected = "/**\n" +
                " * @deprecated As of yesterday. A message\n" +
                " */\n" +
                "@Deprecated\n" +
                "@SmithyGenerated\n" +
                "public final class DeprecatedStructureTrait";
        assertTrue(fileContents.contains(expected));
    }

    @Test
    void deprecatedAnnotationAndNoteOnMember() {
        String fileContents = getFileContentsFromShapeName("DeprecatedStructure", true);
        String expected = "    /**\n" +
                "     * @deprecated As of yesterday. A message\n" +
                "     */\n" +
                "    @Deprecated\n" +
                "    public Optional<String> getDeprecatedMember() {";
        assertTrue(fileContents.contains(expected));
    }

    @Test
    void deprecatedAnnotationAndNoteWithDocsOnMember() {
        String fileContents = getFileContentsFromShapeName("DeprecatedStructure", true);
        String expected = "    /**\n" +
                "     * Has docs in addition to deprecated\n" +
                "     *\n" +
                "     * @deprecated As of yesterday. A message\n" +
                "     */\n" +
                "    @Deprecated\n" +
                "    public Optional<String> getDeprecatedWithDocs() {";
        assertTrue(fileContents.contains(expected));
    }

    @Test
    void sinceJavadocTagAddedToClass() {
        String fileContents = getFileContentsFromShapeName("SinceStructure", true);
        String expected = "/**\n" +
                " * @since 1.2\n" +
                " */\n" +
                "@SmithyGenerated\n" +
                "public final class SinceStructureTrait";
        assertTrue(fileContents.contains(expected));
    }

    @Test
    void sinceJavaDocOnMember() {
        String fileContents = getFileContentsFromShapeName("SinceStructure", true);
        String expected = "    /**\n" +
                "     * @since 1.2\n" +
                "     */\n" +
                "    public Optional<String> getSinceMember() {";
        assertTrue(fileContents.contains(expected));
    }

    @Test
    void externalDocumentationOnClass() {
        String fileContents = getFileContentsFromShapeName("ExternalDocumentation", true);
        String expected = "/**\n" +
                " * @see <a href=\"https://example.com\">Example</a>\n" +
                " * @see <a href=\"https://example.com\">Example2</a>\n" +
                " */\n" +
                "@SmithyGenerated\n" +
                "public final class ExternalDocumentationTrait extends AbstractTrait";
        assertTrue(fileContents.contains(expected));

    }

    @Test
    void externalDocumentationOnMember() {
        String fileContents = getFileContentsFromShapeName("ExternalDocumentation", true);
        String expected = "    /**\n" +
                "     * @see <a href=\"https://example.com\">Example</a>\n" +
                "     */\n" +
                "    public Optional<String> getMemberWithExternalDocumentation() {";
        assertTrue(fileContents.contains(expected));
    }

    @Test
    void allDocumentationIncludedTogetherOnClass() {
        String fileContents = getFileContentsFromShapeName("Rollup", true);
        String expected = "/**\n" +
                " * This structure applies all documentation traits\n" +
                " *\n" +
                " * @see <a href=\"https://example.com\">Example</a>\n" +
                " * @since 4.5\n" +
                " * @deprecated As of sometime.\n" +
                " */\n" +
                "@SmithyUnstableApi\n" +
                "@Deprecated\n" +
                "@SmithyGenerated\n" +
                "public final class RollupTrait extends AbstractTrait";
        assertTrue(fileContents.contains(expected));
    }

    @Test
    void allDocumentationIncludedTogetherOnMember() {
        String fileContents = getFileContentsFromShapeName("Rollup", true);
        String expected = "    /**\n" +
                "     * This member applies all documentation traits\n" +
                "     *\n" +
                "     * @see <a href=\"https://example.com\">Example</a>\n" +
                "     * @since 4.5\n" +
                "     * @deprecated As of sometime.\n" +
                "     */\n" +
                "    @SmithyUnstableApi\n" +
                "    @Deprecated\n" +
                "    public Optional<String> getRollupMember() {";
        assertTrue(fileContents.contains(expected));
    }

    @Test
    void allDocumentationIncludedTogetherEnumVariant() {
        String fileContents = getFileContentsFromShapeName("EnumVariantsTest", true);
        String expected = "        /**\n" +
                "         * Just a documented variant\n" +
                "         *\n" +
                "         * @see <a href=\"https://example.com\">Example</a>\n" +
                "         * @since 4.5\n" +
                "         * @deprecated Really. Dont use this.\n" +
                "         */\n" +
                "        @SmithyUnstableApi\n" +
                "        @Deprecated\n" +
                "        A(\"A\"),";
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
