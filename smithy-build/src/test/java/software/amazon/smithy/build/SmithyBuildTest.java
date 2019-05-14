/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.build;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.OptionalUtils;

public class SmithyBuildTest {
    private Path outputDirectory;

    @BeforeEach
    public void before() throws IOException {
        outputDirectory = Files.createTempDirectory(getClass().getName());
    }

    @AfterEach
    public void after() throws IOException {
        Files.walk(outputDirectory).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }

    @Test
    public void loadsEmptyObject() throws Exception {
        SmithyBuildConfig.load(Paths.get(getClass().getResource("empty-config.json").toURI()));
    }

    @Test
    public void throwsForUnknownTransform() throws Exception {
        Assertions.assertThrows(UnknownTransformException.class, () -> {
            SmithyBuildConfig config = SmithyBuildConfig.load(
                    Paths.get(getClass().getResource("unknown-transform.json").toURI()));
            new SmithyBuild().config(config).build();
        });
    }

    @Test
    public void appliesAllProjections() throws Exception {
        SmithyBuildConfig config = SmithyBuildConfig.builder()
                .load(Paths.get(getClass().getResource("simple-config.json").toURI()))
                .outputDirectory(outputDirectory)
                .build();
        Model model = Model.assembler()
                .addImport(Paths.get(getClass().getResource("simple-model.json").toURI()))
                .assemble()
                .unwrap();
        SmithyBuild builder = new SmithyBuild().config(config).model(model);
        SmithyBuildResult results = builder.build();
        Model resultA = results.getProjectionResult("a").get().getModel();
        Model resultB = results.getProjectionResult("b").get().getModel();

        assertThat(resultA.getShapeIndex().getShape(ShapeId.from("ns.foo#String1")), not(Optional.empty()));
        assertThat(resultA.getShapeIndex().getShape(ShapeId.from("ns.foo#String2")), is(Optional.empty()));
        assertThat(resultA.getShapeIndex().getShape(ShapeId.from("ns.foo#String3")), not(Optional.empty()));

        assertThat(resultB.getShapeIndex().getShape(ShapeId.from("ns.foo#String1")), not(Optional.empty()));
        assertThat(resultB.getShapeIndex().getShape(ShapeId.from("ns.foo#String2")), not(Optional.empty()));
        assertThat(resultB.getShapeIndex().getShape(ShapeId.from("ns.foo#String3")), not(Optional.empty()));
    }

    @Test
    public void buildsModels() throws Exception {
        SmithyBuildConfig config = SmithyBuildConfig.builder()
                .load(Paths.get(getClass().getResource("simple-config.json").toURI()))
                .outputDirectory(outputDirectory)
                .build();
        Model model = Model.assembler()
                .addImport(Paths.get(getClass().getResource("simple-model.json").toURI()))
                .assemble()
                .unwrap();
        SmithyBuild builder = new SmithyBuild().config(config).model(model);
        builder.build();

        assertThat(Files.isDirectory(outputDirectory.resolve("source")), is(true));
        assertThat(Files.isRegularFile(outputDirectory.resolve("source/model/model.json")), is(true));
        assertThat(Files.isRegularFile(outputDirectory.resolve("source/build-info/smithy-build-info.json")), is(true));

        assertThat(Files.isDirectory(outputDirectory.resolve("a")), is(true));
        assertThat(Files.isRegularFile(outputDirectory.resolve("a/build-info/smithy-build-info.json")), is(true));
        assertThat(Files.isRegularFile(outputDirectory.resolve("a/model/model.json")), is(true));

        assertThat(Files.isDirectory(outputDirectory.resolve("b")), is(true));
        assertThat(Files.isRegularFile(outputDirectory.resolve("b/build-info/smithy-build-info.json")), is(true));
        assertThat(Files.isRegularFile(outputDirectory.resolve("b/model/model.json")), is(true));
    }

    @Test
    public void doesNotCopyErroneousModelsToBuildOutput() throws Exception {
        SmithyBuildConfig config = SmithyBuildConfig.builder()
                .load(Paths.get(getClass().getResource("resource-model-config.json").toURI()))
                .outputDirectory(outputDirectory)
                .build();
        Model model = Model.assembler()
                .addImport(Paths.get(getClass().getResource("resource-model.json").toURI()))
                .assemble()
                .unwrap();
        SmithyBuild builder = new SmithyBuild(config).model(model);
        builder.build();

        assertThat(Files.isDirectory(outputDirectory.resolve("source")), is(true));
        assertThat(Files.isRegularFile(outputDirectory.resolve("source/build-info/smithy-build-info.json")), is(true));
        assertThat(Files.isRegularFile(outputDirectory.resolve("source/model/model.json")), is(true));

        assertThat(Files.isDirectory(outputDirectory.resolve("valid")), is(true));
        assertThat(Files.isRegularFile(outputDirectory.resolve("valid/build-info/smithy-build-info.json")), is(true));
        assertThat(Files.isRegularFile(outputDirectory.resolve("valid/model/model.json")), is(true));

        assertThat(Files.isDirectory(outputDirectory.resolve("invalid")), is(true));
        assertThat(Files.isDirectory(outputDirectory.resolve("invalid/model")), is(false));
        assertThat(Files.isDirectory(outputDirectory.resolve("invalid/build-info")), is(true));

        String contents = IoUtils.readUtf8File(outputDirectory.resolve("source/build-info/smithy-build-info.json"));
        ObjectNode badBuildInfo = Node.parse(contents).expectObjectNode();
        assertTrue(badBuildInfo.expectMember("version").isStringNode());
        assertTrue(badBuildInfo.expectMember("smithyVersion").isStringNode());
        assertThat(badBuildInfo.expectMember("projection")
                           .expectObjectNode()
                           .expectMember("name")
                           .expectStringNode()
                           .getValue(),
                   equalTo("source"));
        badBuildInfo.expectMember("validationEvents").expectArrayNode();
    }

    @Test
    public void ignoresUnknownPlugins() throws Exception {
        SmithyBuildConfig config = SmithyBuildConfig.builder()
                .load(Paths.get(getClass().getResource("unknown-plugin.json").toURI()))
                .outputDirectory(outputDirectory)
                .build();
        Model model = Model.assembler()
                .addImport(Paths.get(getClass().getResource("resource-model.json").toURI()))
                .assemble()
                .unwrap();
        SmithyBuild builder = new SmithyBuild(config).model(model);
        builder.build();
    }

    @Test
    public void cannotSetFiltersOrMappersOnSourceProjection() {
        Throwable thrown = Assertions.assertThrows(SmithyBuildException.class, () -> {
            SmithyBuildConfig config = SmithyBuildConfig.builder()
                    .addProjection(Projection.builder()
                                           .name("source")
                                           .addTransform(TransformConfiguration.builder().name("foo").build())
                                           .build())
                    .build();
            new SmithyBuild().config(config).build();
        });

        assertThat(thrown.getMessage(), containsString("The source projection cannot contain any transforms"));
    }

    @Test
    public void loadsImports() throws Exception {
        SmithyBuild builder = new SmithyBuild(SmithyBuildConfig.builder()
                .load(Paths.get(getClass().getResource("imports/smithy-build.json").toURI()))
                .outputDirectory(outputDirectory)
                .build());
        SmithyBuildResult results = builder.build();

        Model resultA = results.getProjectionResult("source").get().getModel();
        Model resultB = results.getProjectionResult("b").get().getModel();
        Model resultC = results.getProjectionResult("c").get().getModel();

        assertTrue(resultA.getShapeIndex().getShape(ShapeId.from("com.foo#String")).get()
                           .getTrait(SensitiveTrait.class).isPresent());
        assertFalse(resultA.getShapeIndex().getShape(ShapeId.from("com.foo#String")).get()
                           .getTrait(DocumentationTrait.class).isPresent());

        assertTrue(resultB.getShapeIndex().getShape(ShapeId.from("com.foo#String")).get()
                           .getTrait(SensitiveTrait.class).isPresent());
        assertTrue(resultB.getShapeIndex().getShape(ShapeId.from("com.foo#String")).get()
                           .getTrait(DocumentationTrait.class).isPresent());
        assertThat(resultB.getShapeIndex().getShape(ShapeId.from("com.foo#String")).get()
                           .getTrait(DocumentationTrait.class).get().getValue(), equalTo("b.json"));

        assertTrue(resultC.getShapeIndex().getShape(ShapeId.from("com.foo#String")).get()
                           .getTrait(SensitiveTrait.class).isPresent());
        assertTrue(resultC.getShapeIndex().getShape(ShapeId.from("com.foo#String")).get()
                           .getTrait(DocumentationTrait.class).isPresent());
        assertThat(resultC.getShapeIndex().getShape(ShapeId.from("com.foo#String")).get()
                           .getTrait(DocumentationTrait.class).get().getValue(), equalTo("c.json"));
    }

    @Test
    public void appliesPlugins() throws Exception {
        Map<String, SmithyBuildPlugin> plugins = MapUtils.of("test1", new Test1Plugin(), "test2", new Test2Plugin());
        Function<String, Optional<SmithyBuildPlugin>> factory = SmithyBuildPlugin.createServiceFactory();
        Function<String, Optional<SmithyBuildPlugin>> composed = name -> OptionalUtils.or(
                Optional.ofNullable(plugins.get(name)), () -> factory.apply(name));

        SmithyBuild builder = new SmithyBuild().pluginFactory(composed);
        builder.fileManifestFactory(MockManifest::new);
        builder.config(SmithyBuildConfig.builder()
                .load(Paths.get(getClass().getResource("applies-plugins.json").toURI()))
                .outputDirectory("/foo")
                .build());

        SmithyBuildResult results = builder.build();
        ProjectionResult source = results.getProjectionResult("source").get();
        ProjectionResult a = results.getProjectionResult("a").get();
        ProjectionResult b = results.getProjectionResult("b").get();

        assertTrue(source.getPluginManifest("test1").isPresent());
        assertTrue(a.getPluginManifest("test1").isPresent());
        assertTrue(b.getPluginManifest("test1").isPresent());

        assertTrue(source.getPluginManifest("test1").get().hasFile("hello1"));
        assertTrue(a.getPluginManifest("test1").get().hasFile("hello1"));
        assertTrue(b.getPluginManifest("test1").get().hasFile("hello1"));

        assertTrue(source.getPluginManifest("test2").isPresent());
        assertTrue(a.getPluginManifest("test2").isPresent());
        assertTrue(b.getPluginManifest("test2").isPresent());

        assertTrue(source.getPluginManifest("test2").get().hasFile("hello2"));
        assertTrue(a.getPluginManifest("test2").get().hasFile("hello2"));
        assertTrue(b.getPluginManifest("test2").get().hasFile("hello2"));
    }

    @Test
    public void buildCanOverrideConfigOutputDirectory() throws Exception {
        Path outputDirectory = Paths.get("/custom/foo");
        SmithyBuildConfig config = SmithyBuildConfig.builder()
                .load(Paths.get(getClass().getResource("simple-config.json").toURI()))
                // Note: this is not the same as the setting on SmithyBuild.
                .outputDirectory(Paths.get("/foo/baz/bar"))
                .build();
        Model model = Model.assembler()
                .addImport(Paths.get(getClass().getResource("simple-model.json").toURI()))
                .assemble()
                .unwrap();
        SmithyBuild builder = new SmithyBuild()
                .config(config)
                .model(model)
                .outputDirectory(outputDirectory)
                .fileManifestFactory(MockManifest::new);
        SmithyBuildResult results = builder.build();
        List<Path> files = results.allArtifacts().collect(Collectors.toList());

        assertThat(files, containsInAnyOrder(
                outputDirectory.resolve("source/model/model.json"),
                outputDirectory.resolve("source/build-info/smithy-build-info.json"),
                outputDirectory.resolve("a/sources/manifest"),
                outputDirectory.resolve("a/sources/model.json"),
                outputDirectory.resolve("a/model/model.json"),
                outputDirectory.resolve("a/build-info/smithy-build-info.json"),
                outputDirectory.resolve("b/sources/manifest"),
                outputDirectory.resolve("b/sources/model.json"),
                outputDirectory.resolve("b/model/model.json"),
                outputDirectory.resolve("b/build-info/smithy-build-info.json")));
    }

    @Test
    public void detectsCyclesInApplyProjection() throws Exception {
        Throwable thrown = Assertions.assertThrows(SmithyBuildException.class, () -> {
            SmithyBuildConfig config = SmithyBuildConfig.builder()
                    .load(Paths.get(getClass().getResource("apply-cycle.json").toURI()))
                    .build();
            new SmithyBuild().config(config).build();
        });

        assertThat(thrown.getMessage(), containsString("Cycle found in apply transforms:"));
    }

    @Test
    public void detectsMissingApplyProjection() throws Exception {
        Throwable thrown = Assertions.assertThrows(SmithyBuildException.class, () -> {
            SmithyBuildConfig config = SmithyBuildConfig.builder()
                    .load(Paths.get(getClass().getResource("apply-invalid-projection.json").toURI()))
                    .build();
            new SmithyBuild().config(config).build();
        });

        assertThat(thrown.getMessage(), containsString("Unable to find projection named `bar` referenced by `foo`"));
    }

    @Test
    public void appliesProjections() throws Exception {
        Model model = Model.assembler()
                .addImport(Paths.get(getClass().getResource("simple-model.json").toURI()))
                .assemble()
                .unwrap();

        SmithyBuild builder = new SmithyBuild()
                .model(model)
                .fileManifestFactory(MockManifest::new);
        builder.config(SmithyBuildConfig.builder()
                .load(Paths.get(getClass().getResource("apply-multiple-projections.json").toURI()))
                .outputDirectory("/foo")
                .build());

        SmithyBuildResult results = builder.build();
        assertTrue(results.getProjectionResult("source").isPresent());
        assertTrue(results.getProjectionResult("a").isPresent());
        ProjectionResult a = results.getProjectionResult("a").get();
        assertTrue(a.getPluginManifest("model").isPresent());
        MockManifest manifest = (MockManifest) a.getPluginManifest("model").get();
        String modelText = manifest.getFileString("model.json").get();

        assertThat(modelText, not(containsString("length\"")));
        assertThat(modelText, not(containsString("tags\"")));
    }
}
