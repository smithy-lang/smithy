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

public class CliTest {

    @Test
    public void detectsInvalidArguments() {
        CliUtils.Result result = CliUtils.runSmithy("--foo");

        assertThat(result.code(), not(0));
        assertThat(result.stderr(), containsString("Unknown argument"));
    }

    @Test
    public void showsStacktrace() {
        CliUtils.Result result = CliUtils.runSmithy("--stacktrace", "--foo");

        assertThat(result.code(), not(0));
        assertThat(result.stderr(), containsString("software.amazon.smithy.cli.CliError"));
    }

    @Test
    public void canDisableLogging() {
        CliUtils.Result result = CliUtils.runSmithy("validate", "--logging", "OFF");

        assertThat(result.code(), equalTo(0));
        assertThat(result.stderr(), not(containsString("Invoking Command")));
    }

    @Test
    public void canEnableLoggingViaLogLevel() {
        CliUtils.Result result = CliUtils.runSmithy("validate", "--logging", "FINE");

        assertThat(result.code(), equalTo(0));
        assertThat(result.stderr(), containsString("Invoking Command"));
    }

    @Test
    public void canEnableDebugLogging() {
        CliUtils.Result result = CliUtils.runSmithy("validate", "--debug");

        assertThat(result.code(), equalTo(0));
        assertThat(result.stderr(), containsString("Invoking Command"));
    }

    @Test
    public void canForceColors() {
        CliUtils.Result result = CliUtils.runSmithyWithAutoColors("--force-color", "--help");

        assertThat(result.stdout(), containsString("[0m"));
    }

    @Test
    public void canForceDisableColors() {
        CliUtils.Result result = CliUtils.runSmithyWithAutoColors("--no-color", "--help");

        assertThat(result.stdout(), not(containsString("[0m")));
    }
}
