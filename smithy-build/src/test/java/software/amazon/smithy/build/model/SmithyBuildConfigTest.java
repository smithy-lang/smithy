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

package software.amazon.smithy.build.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

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
        assertThat(transform.getArgs(), equalTo(Node.objectNode()
                .withMember("tags", Node.fromStrings("Hi", "compoundTagFromEnv", "${BAZ}"))));
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
}
