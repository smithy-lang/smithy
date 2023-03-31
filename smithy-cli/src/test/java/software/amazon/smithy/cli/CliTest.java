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
