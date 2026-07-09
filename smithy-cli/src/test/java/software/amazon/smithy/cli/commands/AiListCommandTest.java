/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.smithy.cli.CliUtils;

public class AiListCommandTest {

    @Test
    public void hasHelp() {
        CliUtils.Result result = CliUtils.runSmithy("ai", "list", "--help");

        assertThat(result.code(), equalTo(0));
        assertThat(result.stdout(), containsString("List"));
    }

    @Test
    public void listsBundledSkills() {
        CliUtils.Result result = CliUtils.runSmithy("ai", "list");

        assertThat(result.code(), equalTo(0));
        assertThat(result.stdout(), containsString("smithy-docs-navigator"));
    }

    @Test
    public void listsSupportedHarnesses() {
        CliUtils.Result result = CliUtils.runSmithy("ai", "list");

        assertThat(result.code(), equalTo(0));
        assertThat(result.stdout(), containsString("claude"));
        assertThat(result.stdout(), containsString("kiro"));
    }

    @Test
    public void showsInstalledStatePerHarness(@TempDir Path dir) throws IOException {
        // Install for claude only; list should mark claude installed and kiro not.
        Path skill = dir.resolve(".claude/skills/smithy-docs-navigator/SKILL.md");
        Files.createDirectories(skill.getParent());
        Files.write(skill, "x".getBytes(StandardCharsets.UTF_8));

        CliUtils.Result result = CliUtils.runSmithy("ai", "list", "--dir", dir.toString());

        assertThat(result.code(), equalTo(0));
        // Distinct installed vs not-installed markers appear.
        assertThat(result.stdout(), containsString("installed"));
        assertThat(result.stdout(), containsString("not installed"));
    }
}
