/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.SmithyBuild;
import software.amazon.smithy.build.model.SmithyBuildConfig;

public class BuildOptionsTest {
    @Test
    public void usesExplicitOutputArgument() {
        BuildOptions options = new BuildOptions();
        options.testParameter("--output").accept("test");
        SmithyBuildConfig config = SmithyBuildConfig.builder().version("1").outputDirectory("foo").build();

        assertThat(options.resolveOutput(config), equalTo(Paths.get("test")));
    }

    @Test
    public void usesConfigOutputDirectory() {
        BuildOptions options = new BuildOptions();
        SmithyBuildConfig config = SmithyBuildConfig.builder().version("1").outputDirectory("foo").build();

        assertThat(options.resolveOutput(config), equalTo(Paths.get("foo")));
    }

    @Test
    public void usesDefaultBuildDirectory() {
        BuildOptions options = new BuildOptions();
        SmithyBuildConfig config = SmithyBuildConfig.builder().version("1").build();

        assertThat(options.resolveOutput(config), equalTo(SmithyBuild.getDefaultOutputDirectory()));
    }
}
