/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;

public class RootCommandTest {
    @Test
    public void providingNoInputPrintsHelp() {
        IntegUtils.run("simple-config-sources", ListUtils.of(), result -> {
            assertThat(result.getExitCode(), equalTo(1));
            ensureHelpOutput(result);
        });
    }

    @Test
    public void passing_h_printsHelp() {
        IntegUtils.run("simple-config-sources", ListUtils.of("-h"), result -> {
            assertThat(result.getExitCode(), equalTo(0));
            ensureHelpOutput(result);

            // We force NO_COLOR by default in the run method. Test that's honored here.
            assertThat(result.getOutput(), not(containsString("[0m")));
        });
    }

    @Test
    public void passingHelpPrintsHelp() {
        IntegUtils.run("simple-config-sources", ListUtils.of("--help"), result -> {
            assertThat(result.getExitCode(), equalTo(0));
            ensureHelpOutput(result);
        });
    }

    @Test
    public void supportsVersionPseudoCommand() {
        IntegUtils.run("simple-config-sources", ListUtils.of("--version"), result -> {
            assertThat(result.getExitCode(), equalTo(0));
            assertThat(result.getOutput().trim(), equalTo(SmithyCli.getVersion()));
        });
    }

    @Test
    public void errorsOnInvalidCommand() {
        IntegUtils.run("simple-config-sources", ListUtils.of("doesNotExist"), result -> {
            assertThat(result.getExitCode(), equalTo(1));
            assertThat(result.getOutput(), containsString("Unknown argument or command: doesNotExist"));
        });
    }

    @Test
    public void errorsOnInvalidArgument() {
        IntegUtils.run("simple-config-sources", ListUtils.of("--foo"), result -> {
            assertThat(result.getExitCode(), equalTo(1));
            assertThat(result.getOutput(), containsString("Unknown argument or command: --foo"));
        });
    }

    @Test
    public void runsWithColors() {
        IntegUtils.run("simple-config-sources",
                ListUtils.of("--help"),
                MapUtils.of(EnvironmentVariable.FORCE_COLOR.toString(), "true"),
                result -> {
                    assertThat(result.getExitCode(), equalTo(0));
                    assertThat(result.getOutput(), containsString("[0m"));
                });
    }

    private void ensureHelpOutput(RunResult result) {
        // Make sure it's the help output.
        assertThat(result.getOutput(),
                containsString("Usage: smithy [-h | --help] [--version] <command> [<args>]"));
        // Make sure commands are listed.
        assertThat(result.getOutput(), containsString("Available commands:"));
        // Check on one of the command's help summary.
        assertThat(result.getOutput(), containsString("Validates Smithy models"));
    }
}
