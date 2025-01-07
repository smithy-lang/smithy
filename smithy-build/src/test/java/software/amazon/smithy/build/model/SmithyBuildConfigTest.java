/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.SmithyBuild;
import software.amazon.smithy.build.SmithyBuildException;
import software.amazon.smithy.build.SmithyBuildTest;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.utils.ListUtils;

public class SmithyBuildConfigTest {
    @Test
    public void loadsFromJson() {
        SmithyBuildConfig.load(Paths.get(getResourcePath("special-syntax.json")));
    }

    @Test
    public void throwsWithCorrectSyntaxErrorMessage() {
        Exception thrown = Assertions.assertThrows(SmithyBuildException.class, () -> {
            SmithyBuildConfig.load(Paths.get(getResourcePath("bad-syntax.json")));
        });

        assertThat(thrown.getMessage(), containsString("Error parsing"));
    }

    @Test
    public void requiresVersion() {
        Exception thrown = Assertions.assertThrows(SourceException.class, () -> {
            SmithyBuildConfig.load(Paths.get(getResourcePath("missing-version.json")));
        });

        assertThat(thrown.getMessage(), containsString("version"));
    }

    @Test
    public void canBeAbstract() {
        SmithyBuildConfig config = SmithyBuildConfig.load(
                Paths.get(getResourcePath("config-with-abstract.json")));

        assertThat(config.getProjections().get("abstract").isAbstract(), is(true));
    }

    @Test
    public void canLoadJsonDataIntoBuilder() {
        Path outputDir = Paths.get("/foo/baz");
        SmithyBuildConfig config = SmithyBuildConfig.builder()
                .load(Paths.get(getResourcePath(("config-with-abstract.json"))))
                .load(Paths.get(getResourcePath("simple-config.json")))
                .outputDirectory(outputDir.toString())
                .build();

        assertThat(config.getProjections().get("a"), notNullValue());
        assertThat(config.getProjections().get("b"), notNullValue());
        assertThat(config.getProjections().get("abstract").isAbstract(), is(true));
        assertThat(config.getOutputDirectory(), equalTo(Optional.of(outputDir.toString())));
    }

    @Test
    public void canAddImports() {
        String importPath = getResourcePath("simple-model.json");
        SmithyBuildConfig config = SmithyBuildConfig.builder()
                .version(SmithyBuild.VERSION)
                .imports(ListUtils.of(importPath))
                .build();

        assertThat(config.getImports(), containsInAnyOrder(importPath));
    }

    @Test
    public void canAddImportsAndOutputDirViaJson() {
        SmithyBuildConfig config = SmithyBuildConfig.load(
                Paths.get(getResourcePath("config-with-imports-and-output-dir.json")));

        assertThat(config.getOutputDirectory(), not(Optional.empty()));
        assertThat(config.getImports(), not(empty()));
    }

    @Test
    public void addsBuiltinPlugins() {
        SmithyBuildConfig config = SmithyBuildConfig.builder()
                .version(SmithyBuild.VERSION)
                .build();

        assertThat(config.getPlugins(), hasKey("build-info"));
        assertThat(config.getPlugins(), hasKey("model"));
        assertThat(config.getPlugins(), hasKey("sources"));
    }

    @Test
    public void expandsEnvironmentVariables() {
        System.setProperty("FOO", "Hi");
        System.setProperty("BAR", "TagFromEnv");
        System.setProperty("NAME_KEY", "name");
        SmithyBuildConfig config = SmithyBuildConfig.load(
                Paths.get(getResourcePath("config-with-env.json")));
        TransformConfig transform = config.getProjections().get("a").getTransforms().get(0);

        // Did the key expand?
        assertThat(transform.getName(), equalTo("includeShapesByTag"));
        // Did the array and string values in it expand?
        assertThat(transform.getArgs(),
                equalTo(Node.objectNode()
                        .withMember("tags",
                                Node.fromStrings("Hi", "compoundTagFromEnv", "HiAndTagFromEnv", "${BAZ}"))));
    }

    @Test
    public void throwsForUnknownEnvironmentVariables() {
        Assertions.assertThrows(SmithyBuildException.class, () -> {
            SmithyBuildConfig.load(Paths.get(getResourcePath("config-with-invalid-env.json")));
        });
    }

    @Test
    public void rewritesArgsArrayToUnderscoreArgs() {
        SmithyBuildConfig config = SmithyBuildConfig.load(
                Paths.get(getResourcePath("rewrites-args-array.json")));
        ObjectNode node = config.getProjections().get("rewrite").getTransforms().get(0).getArgs();
        ObjectNode expected = Node.objectNode().withMember("__args", Node.fromStrings("sensitive"));

        Node.assertEquals(node, expected);
    }

    private String getResourcePath(String name) {
        try {
            return Paths.get(SmithyBuildTest.class.getResource(name).toURI()).toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void convertingToBuilderRetainsIgnoreMissingPlugins() {
        SmithyBuildConfig a = SmithyBuildConfig.builder()
                .version("1")
                .ignoreMissingPlugins(true)
                .build();

        assertThat(a.toBuilder().build().isIgnoreMissingPlugins(), equalTo(true));
    }

    @Test
    public void mergingTakesIgnoreMissingPluginsFromEither() {
        SmithyBuildConfig a = SmithyBuildConfig.builder()
                .version("1")
                .ignoreMissingPlugins(true)
                .build();
        SmithyBuildConfig b = SmithyBuildConfig.builder().version("1").build();

        assertThat(a.toBuilder().merge(b).build().isIgnoreMissingPlugins(), equalTo(true));
        assertThat(b.toBuilder().merge(a).build().isIgnoreMissingPlugins(), equalTo(true));
    }

    @Test
    public void loadsFromNode() throws IOException {
        Path root = Paths.get("/");
        ObjectNode value = Node.parse(getClass().getResource("loads-from-node.json").openStream())
                .expectObjectNode()
                .toBuilder()
                .sourceLocation(new SourceLocation(
                        root.resolve("hello").resolve("smithy-build.json").toString(),
                        1,
                        1))
                .build();
        SmithyBuildConfig config = SmithyBuildConfig.fromNode(value);

        assertThat(config.getImports(), contains(root.resolve("hello").resolve("foo.json").toString()));
        assertThat(config.getProjections().get("a").getImports(),
                contains(root.resolve("hello").resolve("baz.json").toString()));
    }

    @Test
    public void loadsFromNodeIgnoringBadSourceLocations() throws IOException {
        ObjectNode value = Node.parse(getClass().getResource("loads-from-node.json").openStream())
                .expectObjectNode();
        SmithyBuildConfig config = SmithyBuildConfig.fromNode(value);

        Path cwd = SmithyBuildUtils.getCurrentWorkingDirectory();

        assertThat(config.getImports(), contains(cwd.resolve("foo.json").toString()));
        assertThat(config.getProjections().get("a").getImports(), contains(cwd.resolve("baz.json").toString()));
    }

    @Test
    public void outputDirCannotBeEmpty() throws IOException {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            SmithyBuildConfig.builder()
                    .version("1.0")
                    .outputDirectory("")
                    .build();
        });
    }

    @Test
    public void mergingTakesOtherMavenConfigWhenHasNone() {
        SmithyBuildConfig a = SmithyBuildConfig.builder().version("1").build();
        SmithyBuildConfig b = SmithyBuildConfig.builder()
                .version("1")
                .maven(MavenConfig.builder().dependencies(ListUtils.of("a:b:1.0.0")).build())
                .build();

        assertThat(a.toBuilder().merge(b).build().getMaven(), equalTo(b.getMaven()));
    }

    @Test
    public void mergingTakesSelfMavenConfigWhenOtherHasNone() {
        SmithyBuildConfig a = SmithyBuildConfig.builder()
                .version("1")
                .maven(MavenConfig.builder().dependencies(ListUtils.of("a:b:1.0.0")).build())
                .build();
        SmithyBuildConfig b = SmithyBuildConfig.builder().version("1").build();

        assertThat(a.toBuilder().merge(b).build().getMaven(), equalTo(a.getMaven()));
    }

    @Test
    public void mergingCombinesMavenConfigsWhenBothPresent() {
        SmithyBuildConfig a = SmithyBuildConfig.builder()
                .version("1")
                .maven(MavenConfig.builder().dependencies(ListUtils.of("c:d:1.0.0")).build())
                .build();
        SmithyBuildConfig b = SmithyBuildConfig.builder()
                .version("1")
                .maven(MavenConfig.builder().dependencies(ListUtils.of("a:b:1.0.0", "c:d:1.0.0")).build())
                .build();

        assertThat(a.toBuilder().merge(b).build().getMaven().get().getDependencies(),
                contains("c:d:1.0.0", "a:b:1.0.0"));
    }
}
