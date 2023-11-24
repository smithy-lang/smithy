/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.cli.commands;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.cli.CliUtils;

public class SmithyCommandTest {
    @Test
    public void noArgsPrintsMainHelp() {
        CliUtils.Result result = CliUtils.runSmithy();

        assertThat(result.code(), equalTo(1));
        assertThat(result.stderr(), containsString("Usage: "));
    }

    @Test
    public void hasLongHelpCommand() {
        CliUtils.Result result = CliUtils.runSmithy("--help");

        assertThat(result.code(), equalTo(0));
        assertThat(result.stdout(), containsString("Usage: "));
    }

    @Test
    public void hasShortHelpCommand() {
        CliUtils.Result result = CliUtils.runSmithy("-h");

        assertThat(result.code(), equalTo(0));
        assertThat(result.stdout(), containsString("Usage: "));
    }

    @Test
    public void printsValidateSubcommandHelp() {
        CliUtils.Result result = CliUtils.runSmithy("validate", "--help");

        assertThat(result.code(), equalTo(0));
        assertThat(result.stdout(), containsString("Validates"));
    }

    @Test
    public void printsBuildSubcommandHelp() {
        CliUtils.Result result = CliUtils.runSmithy("build", "--help");

        assertThat(result.code(), equalTo(0));
        assertThat(result.stdout(), containsString("Builds"));
    }

    @Test
    public void supportsTopLevelVersion() {
        CliUtils.Result result = CliUtils.runSmithy("--version");

        assertThat(result.code(), equalTo(0));
    }

    @Test
    public void doesNotAllowVersionArgumentToSubcommands() {
        CliUtils.Result result = CliUtils.runSmithy("validate", "--version");

        assertThat(result.code(), equalTo(1));
    }
}
