/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.utils.ListUtils;

public class WarmupTest {
    @Test
    public void providingNoInputPrintsHelpExits0() {
        IntegUtils.run("simple-config-sources", ListUtils.of("--help"), result -> {
            assertThat(result.getOutput(), not(containsString("warmup")));
        });
    }

    @Test
    public void canCallHelpForCommand() {
        IntegUtils.run("simple-config-sources", ListUtils.of("warmup", "--help"), result -> {
            assertThat(result.getExitCode(), is(0));
        });
    }

    @Test
    public void canWarmupTheCli() throws IOException {
        Path tempDir = Files.createTempDirectory("smithy-warmup-integ");
        RunResult result = IntegUtils.run(tempDir, ListUtils.of("warmup"));

        assertThat(result.getExitCode(), equalTo(0));
    }
}
