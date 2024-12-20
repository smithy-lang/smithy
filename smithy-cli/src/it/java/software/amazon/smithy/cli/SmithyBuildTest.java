/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.utils.ListUtils;

public class SmithyBuildTest {
    @Test
    public void buildsModelsWithSourcesAndDefaultPlugins() {
        IntegUtils.run("simple-config-sources", ListUtils.of("build"), this::doSimpleBuildAssertions);
    }

    @Test
    public void canSetSpecificConfigFile() {
        IntegUtils.run("simple-config-sources",
                ListUtils.of("build", "-c", "smithy-build.json"),
                this::doSimpleBuildAssertions);
    }

    @Test
    public void registersSourcesFromArguments() {
        IntegUtils.run("simple-no-config", ListUtils.of("build", "model"), this::doSimpleBuildAssertions);
    }

    private void doSimpleBuildAssertions(RunResult result) {
        assertThat(result.getExitCode(), equalTo(0));
        assertThat(result.getOutput(), containsString("SUCCESS"));
        assertThat(result.hasProjection("source"), is(true));
        assertThat(result.hasPlugin("source", "model"), is(true));
        assertThat(result.hasPlugin("source", "sources"), is(true));
        assertThat(result.hasPlugin("source", "build-info"), is(true));
        assertThat(result.hasArtifact("source", "model", "model.json"), is(true));
        assertThat(result.hasArtifact("source", "sources", "manifest"), is(true));
        assertThat(result.hasArtifact("source", "sources", "main.smithy"), is(true));
        assertThat(result.getArtifact("source", "sources", "main.smithy"), containsString("string MyString"));
    }

    @Test
    public void canUseQuietOutput() {
        IntegUtils.run("simple-config-sources", ListUtils.of("build", "--quiet"), result -> {
            assertThat(result.getExitCode(), equalTo(0));
            assertThat(result.getOutput().trim(), emptyString());
        });
    }

    @Test
    public void showsErrorMessageWhenConfigIsMissing() {
        String path = Paths.get("does", "not", "exist1234.json").toString();
        IntegUtils.run("simple-config-sources", ListUtils.of("build", "-c", path), result -> {
            assertThat(result.getExitCode(), equalTo(1));
            assertThat(result.getOutput(), containsString(path));
            assertThat(result.getOutput(), not(containsString("java.io.UncheckedIOException")));
        });
    }

    @Test
    public void showsErrorMessageWithStacktraceWhenConfigIsMissing() {
        String path = Paths.get("does", "not", "exist1234.json").toString();
        IntegUtils.run("simple-config-sources", ListUtils.of("build", "-c", path, "--stacktrace"), result -> {
            assertThat(result.getExitCode(), equalTo(1));
            assertThat(result.getOutput(), containsString(path));
            assertThat(result.getOutput(), containsString("java.io.UncheckedIOException"));
            assertThat(result.getOutput(), containsString("Caused by"));
        });
    }

    @Test
    public void successfullyDeDupesConfigAndCliArguments() {
        // The config adds model as a source and so does the CLI. Without de-duping, this would fail due to the
        // enum being defined twice with the same members. Without de-conflicting in SmithyBuild, the sources plugin
        // would fail due to finding two files named main.smithy.
        IntegUtils.run("simple-config-sources", ListUtils.of("build", "model"), result -> {
            assertThat(result.getExitCode(), equalTo(0));
        });
    }

    @Test
    public void canDisableConfigFileDetection() {
        // Disable the config file detection and don't pass model via positional arguments.
        // This causes main.smithy to not be loaded.
        IntegUtils.run("simple-config-sources", ListUtils.of("build", "--no-config"), result -> {
            assertThat(result.getExitCode(), equalTo(0));
            assertThat(result.hasArtifact("source", "sources", "main.smithy"), is(false));
        });
    }

    @Test
    public void failsWhenConfigIsProvidedAndDisabled() {
        // Disable the config file detection and don't pass model via positional arguments.
        // This causes main.smithy to not be loaded.
        IntegUtils.run("simple-config-sources",
                ListUtils.of("build", "--no-config", "-c", "smithy-build.json"),
                result -> {
                    assertThat(result.getExitCode(), equalTo(1));
                    assertThat(result.getOutput(), containsString("Invalid combination of --no-config and --config"));
                });
    }
}
