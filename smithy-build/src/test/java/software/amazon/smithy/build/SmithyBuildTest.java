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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.model.ProjectionConfig;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.build.model.TransformConfig;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.traits.TagsTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.ListUtils;
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
            SmithyBuildConfig config = SmithyBuildConfig
                    .load(Paths.get(getClass().getResource("unknown-transform.json").toURI()));
            new SmithyBuild().config(config).build();
        });
    }

    @Test
    public void appliesAllProjections() throws Exception {
        SmithyBuildConfig config = SmithyBuildConfig.builder()
                .load(Paths.get(getClass().getResource("simple-config.json").toURI()))
                .outputDirectory(outputDirectory.toString())
                .build();
        Model model = Model.assembler()
                .addImport(Paths.get(getClass().getResource("simple-model.json").toURI()))
                .assemble()
                .unwrap();
        SmithyBuild builder = new SmithyBuild().config(config).model(model);
        SmithyBuildResult results = builder.build();
        Model resultA = results.getProjectionResult("a").get().getModel();
        Model resultB = results.getProjectionResult("b").get().getModel();

        assertThat(resultA.getShape(ShapeId.from("ns.foo#String1")), not(Optional.empty()));
        assertThat(resultA.getShape(ShapeId.from("ns.foo#String2")), is(Optional.empty()));
        assertThat(resultA.getShape(ShapeId.from("ns.foo#String3")), not(Optional.empty()));

        assertThat(resultB.getShape(ShapeId.from("ns.foo#String1")), not(Optional.empty()));
        assertThat(resultB.getShape(ShapeId.from("ns.foo#String2")), not(Optional.empty()));
        assertThat(resultB.getShape(ShapeId.from("ns.foo#String3")), not(Optional.empty()));
    }

    @Test
    public void buildsModels() throws Exception {
        SmithyBuildConfig config = SmithyBuildConfig.builder()
                .load(Paths.get(getClass().getResource("simple-config.json").toURI()))
                .outputDirectory(outputDirectory.toString())
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
    public void createsEmptyManifest() throws Exception {
        SmithyBuildConfig config = SmithyBuildConfig.builder()
                .load(Paths.get(getClass().getResource("empty-config.json").toURI()))
                .outputDirectory(outputDirectory.toString())
                .build();
        Model model = Model.assembler()
                .assemble()
                .unwrap();
        SmithyBuild builder = new SmithyBuild().config(config).model(model);
        SmithyBuildResult results = builder.build();
        List<Path> files = results.allArtifacts().collect(Collectors.toList());

        assertThat(files, hasItem(outputDirectory.resolve("source/sources/manifest")));
        assertThat("\n", equalTo(IoUtils.readUtf8File(results.allArtifacts()
                .filter(path -> path.toString().endsWith(System.getProperty("file.separator") + "manifest"))
                .findFirst()
                .get())));
    }

    @Test
    public void doesNotCopyErroneousModelsToBuildOutput() throws Exception {
        SmithyBuildConfig config = SmithyBuildConfig.builder()
                .load(Paths.get(getClass().getResource("resource-model-config.json").toURI()))
                .outputDirectory(outputDirectory.toString())
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
        assertThat(badBuildInfo.expectMember("projectionName").expectStringNode().getValue(), equalTo("source"));
        badBuildInfo.expectMember("validationEvents").expectArrayNode();
    }

    @Test
    public void canIgnoreUnknownPlugins() throws Exception {
        SmithyBuildConfig config = SmithyBuildConfig.builder()
                .load(Paths.get(getClass().getResource("unknown-plugin-ignored.json").toURI()))
                .outputDirectory(outputDirectory.toString())
                .build();
        Model model = Model.assembler()
                .addImport(Paths.get(getClass().getResource("resource-model.json").toURI()))
                .assemble()
                .unwrap();
        SmithyBuild builder = new SmithyBuild(config).model(model);
        builder.build();
    }

    @Test
    public void failsByDefaultForUnknownPlugins() throws Exception {
        SmithyBuildConfig config = SmithyBuildConfig.builder()
                .load(Paths.get(getClass().getResource("unknown-plugin.json").toURI()))
                .outputDirectory(outputDirectory.toString())
                .build();
        Model model = Model.assembler()
                .addImport(Paths.get(getClass().getResource("resource-model.json").toURI()))
                .assemble()
                .unwrap();

        SmithyBuildException e = Assertions.assertThrows(SmithyBuildException.class, () -> {
            SmithyBuild builder = new SmithyBuild(config).model(model);
            builder.build();
        });

        assertThat(e.getMessage(), containsString("Unable to find a plugin for `unknown1`"));
    }

    @Test
    public void cannotSetFiltersOrMappersOnSourceProjection() {
        Throwable thrown = Assertions.assertThrows(SmithyBuildException.class, () -> {
            SmithyBuildConfig config = SmithyBuildConfig.builder()
                    .version(SmithyBuild.VERSION)
                    .projections(MapUtils.of("source", ProjectionConfig.builder()
                            .transforms(ListUtils.of(TransformConfig.builder().name("foo").build()))
                            .build()))
                    .build();
            new SmithyBuild().config(config).build();
        });

        assertThat(thrown.getMessage(), containsString("The source projection cannot contain any transforms"));
    }

    @Test
    public void loadsImports() throws Exception {
        SmithyBuildConfig config = SmithyBuildConfig.builder()
                .load(Paths.get(getClass().getResource("imports/smithy-build.json").toURI()))
                .load(Paths.get(getClass().getResource("otherimports/smithy-build.json").toURI()))
                .outputDirectory(outputDirectory.toString())
                .build();
        SmithyBuild builder = new SmithyBuild(config);
        SmithyBuildResult results = builder.build();

        Model resultA = results.getProjectionResult("source").get().getModel();
        Model resultB = results.getProjectionResult("b").get().getModel();
        Model resultC = results.getProjectionResult("c").get().getModel();

        assertTrue(resultA.getShape(ShapeId.from("com.foo#String")).get()
                           .getTrait(SensitiveTrait.class).isPresent());
        assertFalse(resultA.getShape(ShapeId.from("com.foo#String")).get()
                           .getTrait(DocumentationTrait.class).isPresent());
        assertTrue(resultA.getShape(ShapeId.from("com.foo#String")).get()
                .getTrait(TagsTrait.class).isPresent());
        assertThat(resultA.getShape(ShapeId.from("com.foo#String")).get()
                .getTrait(TagsTrait.class).get().getValues().get(0), equalTo("multi-import"));

        assertTrue(resultB.getShape(ShapeId.from("com.foo#String")).get()
                           .getTrait(SensitiveTrait.class).isPresent());
        assertTrue(resultB.getShape(ShapeId.from("com.foo#String")).get()
                           .getTrait(DocumentationTrait.class).isPresent());
        assertThat(resultB.getShape(ShapeId.from("com.foo#String")).get()
                           .getTrait(DocumentationTrait.class).get().getValue(), equalTo("b.json"));
        assertTrue(resultB.getShape(ShapeId.from("com.foo#String")).get()
                .getTrait(TagsTrait.class).isPresent());
        assertThat(resultB.getShape(ShapeId.from("com.foo#String")).get()
                .getTrait(TagsTrait.class).get().getValues().get(0), equalTo("multi-import"));

        assertTrue(resultC.getShape(ShapeId.from("com.foo#String")).get()
                           .getTrait(SensitiveTrait.class).isPresent());
        assertTrue(resultC.getShape(ShapeId.from("com.foo#String")).get()
                           .getTrait(DocumentationTrait.class).isPresent());
        assertThat(resultC.getShape(ShapeId.from("com.foo#String")).get()
                           .getTrait(DocumentationTrait.class).get().getValue(), equalTo("c.json"));
        assertTrue(resultC.getShape(ShapeId.from("com.foo#String")).get()
                .getTrait(TagsTrait.class).isPresent());
        assertThat(resultC.getShape(ShapeId.from("com.foo#String")).get()
                .getTrait(TagsTrait.class).get().getValues().get(0), equalTo("multi-import"));
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
    public void appliesSerialPlugins() throws Exception {
        Map<String, SmithyBuildPlugin> plugins = new LinkedHashMap<>();
        plugins.put("test1Serial", new Test1SerialPlugin());
        plugins.put("test2Serial", new Test2SerialPlugin());
        plugins.put("test1Parallel", new Test1ParallelPlugin());
        plugins.put("test2Parallel", new Test2ParallelPlugin());

        Function<String, Optional<SmithyBuildPlugin>> factory = SmithyBuildPlugin.createServiceFactory();
        Function<String, Optional<SmithyBuildPlugin>> composed = name -> OptionalUtils.or(
                Optional.ofNullable(plugins.get(name)), () -> factory.apply(name));

        SmithyBuild builder = new SmithyBuild().pluginFactory(composed);
        builder.fileManifestFactory(MockManifest::new);
        builder.config(SmithyBuildConfig.builder()
                .load(Paths.get(getClass().getResource("applies-serial-plugins.json").toURI()))
                .outputDirectory("/foo")
                .build());

        SmithyBuildResult results = builder.build();
        ProjectionResult source = results.getProjectionResult("source").get();
        ProjectionResult a = results.getProjectionResult("a").get();
        ProjectionResult b = results.getProjectionResult("b").get();

        assertPluginPresent("test1Serial", "hello1Serial", source, a);
        assertPluginPresent("test2Serial", "hello2Serial", a);
        assertPluginPresent("test1Parallel", "hello1Parallel", source, b);
        assertPluginPresent("test2Parallel", "hello2Parallel", source);

        // "source" contains serial and parallel plugins, so it runs serially and in insetion order.
        // This test will need to be changed in the future if we ever optimize how plugins are run in the future.
        assertTrue(getPluginFileContents(source, "test1Parallel") < getPluginFileContents(source, "test1Serial"));
        // The "b" projection has only parallel plugins, so it's a parallel projection. Parallel projections are run
        // after all the serial projections.
        assertTrue(getPluginFileContents(source, "test1Serial") < getPluginFileContents(b, "test1Parallel"));
    }

    @Test
    public void appliesGlobalSerialPlugins() throws Exception {
        Map<String, SmithyBuildPlugin> plugins = MapUtils.of(
                "test1Serial", new Test1SerialPlugin(),
                "test1Parallel", new Test1ParallelPlugin(),
                "test2Parallel", new Test2ParallelPlugin()
        );
        Function<String, Optional<SmithyBuildPlugin>> factory = SmithyBuildPlugin.createServiceFactory();
        Function<String, Optional<SmithyBuildPlugin>> composed = name -> OptionalUtils.or(
                Optional.ofNullable(plugins.get(name)), () -> factory.apply(name));

        SmithyBuild builder = new SmithyBuild().pluginFactory(composed);
        builder.fileManifestFactory(MockManifest::new);
        builder.config(SmithyBuildConfig.builder()
                .load(Paths.get(getClass().getResource("applies-global-serial-plugins.json").toURI()))
                .outputDirectory("/foo")
                .build());

        SmithyBuildResult results = builder.build();
        ProjectionResult a = results.getProjectionResult("a").get();
        ProjectionResult b = results.getProjectionResult("b").get();

        assertPluginPresent("test1Serial", "hello1Serial", a, b);
        assertPluginPresent("test1Parallel", "hello1Parallel", a);
        assertPluginPresent("test2Parallel", "hello2Parallel", b);
    }

    private long getPluginFileContents(ProjectionResult projection, String pluginName) {
        MockManifest manifest = (MockManifest) projection.getPluginManifest(pluginName).get();
        return Long.parseLong(manifest.getFileString(manifest.getFiles().iterator().next()).get());
    }

    private void assertPluginPresent(String pluginName, String outputFileName, ProjectionResult...results) {
        for (ProjectionResult result : results) {
            assertTrue(result.getPluginManifest(pluginName).isPresent());
            assertTrue(result.getPluginManifest(pluginName).get().hasFile(outputFileName));
        }
    }

    @Test
    public void buildCanOverrideConfigOutputDirectory() throws Exception {
        Path outputDirectory = Paths.get("/custom/foo");
        SmithyBuildConfig config = SmithyBuildConfig.builder()
                .load(Paths.get(getClass().getResource("simple-config.json").toURI()))
                // Note: this is not the same as the setting on SmithyBuild.
                .outputDirectory("/foo/baz/bar")
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
                outputDirectory.resolve("source/sources/manifest"),
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

        assertThat(thrown.getMessage(),
                   containsString("Unable to find projection named `bar` referenced by the `foo` projection"));
    }

    @Test
    public void detectsDirectlyRecursiveApply() throws Exception {
        Throwable thrown = Assertions.assertThrows(SmithyBuildException.class, () -> {
            SmithyBuildConfig config = SmithyBuildConfig.builder()
                    .load(Paths.get(getClass().getResource("apply-direct-recursion.json").toURI()))
                    .build();
            new SmithyBuild().config(config).build();
        });

        assertThat(thrown.getMessage(), containsString("Cannot recursively apply the same projection: foo"));
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

    @Test
    public void pluginsMustHaveValidNames() {
        Throwable thrown = Assertions.assertThrows(SmithyBuildException.class, () -> {
            SmithyBuildConfig config = SmithyBuildConfig.builder()
                    .version(SmithyBuild.VERSION)
                    .plugins(MapUtils.of("!invalid", Node.objectNode()))
                    .build();
            new SmithyBuild().config(config).build();
        });

        assertThat(thrown.getMessage(), containsString(
                "Invalid plugin name `!invalid` found in the `[top-level]` projection"));
    }

    @Test
    public void canFilterProjections() throws URISyntaxException {
        Model model = Model.assembler()
                .addImport(Paths.get(getClass().getResource("simple-model.json").toURI()))
                .assemble()
                .unwrap();

        SmithyBuild builder = new SmithyBuild()
                .model(model)
                .fileManifestFactory(MockManifest::new)
                .projectionFilter(name -> name.equals("a"));
        builder.config(SmithyBuildConfig.builder()
                .load(Paths.get(getClass().getResource("apply-multiple-projections.json").toURI()))
                .outputDirectory("/foo")
                .build());

        SmithyBuildResult results = builder.build();
        assertFalse(results.getProjectionResult("source").isPresent());
        assertTrue(results.getProjectionResult("a").isPresent());
    }

    @Test
    public void canFilterPlugins() throws URISyntaxException {
        Model model = Model.assembler()
                .addImport(Paths.get(getClass().getResource("simple-model.json").toURI()))
                .assemble()
                .unwrap();

        SmithyBuild builder = new SmithyBuild()
                .model(model)
                .fileManifestFactory(MockManifest::new)
                .pluginFilter(name -> name.equals("build-info"));
        builder.config(SmithyBuildConfig.builder()
                .load(Paths.get(getClass().getResource("apply-multiple-projections.json").toURI()))
                .outputDirectory("/foo")
                .build());

        SmithyBuildResult results = builder.build();
        assertTrue(results.getProjectionResult("source").isPresent());
        assertTrue(results.getProjectionResult("a").isPresent());
        ProjectionResult a = results.getProjectionResult("a").get();
        assertFalse(a.getPluginManifest("model").isPresent());
        assertTrue(a.getPluginManifest("build-info").isPresent());
    }

    @Test
    public void throwsWhenErrorsOccur() throws Exception {
        Path badConfig = Paths.get(getClass().getResource("trigger-plugin-error.json").toURI());
        Model model = Model.assembler()
                .addImport(getClass().getResource("simple-model.json"))
                .assemble()
                .unwrap();

        RuntimeException canned = new RuntimeException("Hi");
        Map<String, SmithyBuildPlugin> plugins = new HashMap<>();
        plugins.put("foo", new SmithyBuildPlugin() {
            @Override
            public String getName() {
                return "foo";
            }

            @Override
            public void execute(PluginContext context) {
                throw canned;
            }
        });

        Function<String, Optional<SmithyBuildPlugin>> factory = SmithyBuildPlugin.createServiceFactory();
        Function<String, Optional<SmithyBuildPlugin>> composed = name -> OptionalUtils.or(
                Optional.ofNullable(plugins.get(name)), () -> factory.apply(name));

        SmithyBuild builder = new SmithyBuild()
                .model(model)
                .fileManifestFactory(MockManifest::new)
                .pluginFactory(composed)
                .config(SmithyBuildConfig.load(badConfig));

        SmithyBuildException e = Assertions.assertThrows(SmithyBuildException.class, builder::build);
        assertThat(e.getMessage(), containsString("1 Smithy build projections failed"));
        assertThat(e.getMessage(), containsString("(exampleProjection): java.lang.RuntimeException: Hi"));
        assertThat(e.getSuppressed(), equalTo(new Throwable[]{canned}));
    }

    /*
     This test causes SmithyBuild to fail in a way that it's unable to create Model as part of the
     projection while using imports. This trigger a specific code path that needs to be able to create
     a ProjectionResult with no model, so an empty model is provided. To achieve this, a test trait is
     used with a custom trait factory that always throws when toNode is called on the trait (this is
     something that's used for lots of stuff like trait hash code and equality). Smithy's model
     assembler will catch the thrown SourceException, turn it into a validation event, and then bail
     on trying to finish to build the model, triggering the code path under test here.
     */
    @Test
    public void projectionsThatCannotCreateModelsFailGracefully() throws URISyntaxException {
        Path config = Paths.get(getClass().getResource("test-bad-trait-serializer-config.json").toURI());
        Model model = Model.assembler()
                .addImport(getClass().getResource("test-bad-trait-serializer.smithy"))
                .assemble()
                .unwrap();
        ShapeId badId = ShapeId.from("smithy.test#badTrait");
        TraitFactory baseFactory = TraitFactory.createServiceFactory();
        SmithyBuild builder = new SmithyBuild()
                .config(config)
                .model(model)
                .fileManifestFactory(MockManifest::new)
                .modelAssemblerSupplier(() -> {
                    // Hook in a TraitFactory that knows about our custom, failing trait.
                    return Model.assembler()
                            .traitFactory((id, target, node) -> {
                                if (id.equals(badId)) {
                                    return Optional.of(new BadCustomTrait());
                                } else {
                                    return baseFactory.createTrait(id, target, node);
                                }
                            });
                });

        SmithyBuildResult result = builder.build();

        // The project must have failed.
        assertTrue(result.anyBroken());

        // Now validate that we got the failure to serialize the model.
        List<ValidationEvent> events = result.getProjectionResultsMap().get("foo").getEvents();
        assertThat(events, not(empty()));
        assertThat(events.get(0).getSeverity(), is(Severity.ERROR));
        assertThat(events.get(0).getMessage(), containsString("Unable to serialize trait"));
    }

    // A test trait that always throws when toNode is called.
    private static final class BadCustomTrait implements Trait {
        @Override
        public ShapeId toShapeId() {
            return ShapeId.from("smithy.test#badTrait");
        }

        @Override
        public Node toNode() {
            throw new SourceException("Unable to serialize trait!", SourceLocation.none());
        }
    }

    @Test
    public void canRunMultiplePluginsWithDifferentArtifactNames() throws URISyntaxException {
        Map<String, SmithyBuildPlugin> plugins = MapUtils.of("test1", new Test1SerialPlugin(),
                                                             "test2", new Test2ParallelPlugin());
        Function<String, Optional<SmithyBuildPlugin>> factory = SmithyBuildPlugin.createServiceFactory();
        Function<String, Optional<SmithyBuildPlugin>> composed = name -> OptionalUtils.or(
                Optional.ofNullable(plugins.get(name)), () -> factory.apply(name));

        SmithyBuild builder = new SmithyBuild().pluginFactory(composed);
        builder.fileManifestFactory(MockManifest::new);
        builder.config(SmithyBuildConfig.builder()
                .load(Paths.get(getClass().getResource("artifact-names.json").toURI()))
                .outputDirectory("/foo")
                .build());

        SmithyBuildResult results = builder.build();
        ProjectionResult source = results.getProjectionResult("source").get();
        ProjectionResult a = results.getProjectionResult("a").get();
        ProjectionResult b = results.getProjectionResult("b").get();

        assertPluginPresent("test1", "hello1Serial", source, a, b);
        assertPluginPresent("foo1", "hello1Serial", source, a, b);
        assertPluginPresent("foo2", "hello1Serial", source, a, b);
        assertPluginPresent("foo3", "hello2Parallel", source, a, b);

        assertPluginPresent("test2", "hello2Parallel", a, b);
    }

    @Test
    public void runsCommand() throws Exception {
        Assumptions.assumeTrue(isPosix());

        String output = runPosixTestAndGetOutput("run-plugin/run-plugin-test.json", "test-process");

        assertThat(output, containsString("argv1: a"));
        assertThat(output, containsString("SMITHY_ROOT_DIR: " + Paths.get(".").toAbsolutePath().normalize()));
        assertThat(output, containsString("SMITHY_PLUGIN_DIR: " + outputDirectory.toString()));
        assertThat(output, containsString("SMITHY_PROJECTION_NAME: source"));
        assertThat(output, containsString("SMITHY_ARTIFACT_NAME: test-process"));
        assertThat(output, containsString("SMITHY_INCLUDES_PRELUDE: false"));
        assertThat(output, containsString("FOO_BAR: BAZ"));
        assertThat(output, containsString("FOO_PATH: "));
        assertThat(output, containsString("\"smithy\":\"" + Model.MODEL_VERSION + "\",\"shapes\":{}}"));
        // No prelude by default.
        assertThat(output, not(containsString("smithy.api#String")));
    }

    @Test
    public void runsCommandAndSendsPrelude() throws Exception {
        Assumptions.assumeTrue(isPosix());

        String output = runPosixTestAndGetOutput("run-plugin/run-plugin-with-sendprelude.json", "test-process");

        assertThat(output, containsString("SMITHY_INCLUDES_PRELUDE: true"));
        assertThat(output, containsString("smithy.api#String"));
    }

    // Assumes that `Assumptions.assumeTrue(isPosix());` was already called.
    private String runPosixTestAndGetOutput(String buildFile, String artifactName) throws Exception {
        SmithyBuild builder = new SmithyBuild();

        // Use a real working temporary directory.
        // Note that this won't change the directory based on the artifact name.
        FileManifest manifest = FileManifest.create(outputDirectory);
        builder.fileManifestFactory(path -> manifest);

        // Copy the test shell files to the output directory.
        Path testProcessSh = manifest.getBaseDir().resolve("test-process.sh");
        Path returnErrorSh = manifest.getBaseDir().resolve("return-error.sh");
        Files.createDirectories(testProcessSh.getParent());
        Files.copy(getClass().getResource("run-plugin/test-process.sh").openStream(), testProcessSh);
        Files.copy(getClass().getResource("run-plugin/return-error.sh").openStream(), returnErrorSh);

        builder.config(SmithyBuildConfig.builder()
                .load(Paths.get(getClass().getResource(buildFile).toURI()))
                .build());

        SmithyBuildResult results = builder.build();
        ProjectionResult result = results.getProjectionResultsMap().get("source");
        Path outputFile = result.getPluginManifest(artifactName).get().getBaseDir().resolve("output.txt");

        return IoUtils.readUtf8File(outputFile);
    }

    private boolean isPosix() {
        return FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
    }

    @Test
    public void invalidArtifactName() throws URISyntaxException {
        simpleAssertThrows("run-plugin/invalid-artifact-name-malformed.json");
    }

    @Test
    public void commandNotArray() throws URISyntaxException {
        simpleAssertThrows("run-plugin/invalid-command-not-array.json");
    }

    @Test
    public void missingCommand() throws URISyntaxException {
        simpleAssertThrows("run-plugin/invalid-missing-command.json");
    }

    @Test
    public void exitsNonZero() {
        Assumptions.assumeTrue(isPosix());

        SmithyBuildException e = Assertions.assertThrows(
            SmithyBuildException.class,
            () -> runPosixTestAndGetOutput("run-plugin/invalid-exit-code.json", "invalid-exit-code")
        );

        assertThat(e.getMessage(), containsString("Error exit code 2 returned from: `sh return-error.sh 2`"));
    }

    private SmithyBuildException simpleAssertThrows(String invalidCase) throws URISyntaxException {
        SmithyBuild builder = new SmithyBuild();
        builder.config(SmithyBuildConfig.builder()
                .load(Paths.get(getClass().getResource(invalidCase).toURI()))
                .build());

        return Assertions.assertThrows(SmithyBuildException.class, builder::build);
    }

    @Test
    public void detectsConflictingArtifactNames() throws Exception {
        // Setup fake test1 and test2 plugins just to create a conflict in the test between artifact names
        // but without conflicting JSON keys.
        Map<String, SmithyBuildPlugin> plugins = MapUtils.of(
                "test1", new Test1SerialPlugin(),
                "test2", new Test2ParallelPlugin());
        Function<String, Optional<SmithyBuildPlugin>> factory = SmithyBuildPlugin.createServiceFactory();
        Function<String, Optional<SmithyBuildPlugin>> composed = name -> OptionalUtils.or(
                Optional.ofNullable(plugins.get(name)), () -> factory.apply(name));

        URI build = getClass().getResource("run-plugin/invalid-conflicting-artifact-names.json").toURI();
        SmithyBuild builder = new SmithyBuild()
                .pluginFactory(composed)
                .fileManifestFactory(MockManifest::new)
                .config(SmithyBuildConfig.builder().load(Paths.get(build)).build());

        SmithyBuildException e = Assertions.assertThrows(SmithyBuildException.class, builder::build);

        assertThat(e.getMessage(), containsString("Multiple plugins use the same artifact name 'foo' in "
                                                  + "the 'source' projection"));
    }
}

