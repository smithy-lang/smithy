/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import software.amazon.smithy.cli.CliUtils;

public class AiInstallCommandTest {

    private static final String SKILL_REL = ".claude/skills/smithy-docs-navigator/SKILL.md";

    @Test
    public void hasLongHelpCommand() {
        CliUtils.Result result = CliUtils.runSmithy("ai", "install", "--help");

        assertThat(result.code(), equalTo(0));
        assertThat(result.stdout(), containsString("Install"));
    }

    @Test
    public void aiParentPrintsHelpListingSubcommands() {
        CliUtils.Result result = CliUtils.runSmithy("ai", "--help");

        assertThat(result.code(), equalTo(0));
        assertThat(result.stdout(), containsString("install"));
        assertThat(result.stdout(), containsString("list"));
    }

    @Test
    public void installsWithExplicitHarness(@TempDir Path dir) throws IOException {
        CliUtils.Result result = CliUtils.runSmithy(
                "ai",
                "install",
                "--harness",
                "claude",
                "--dir",
                dir.toString());

        assertThat(result.code(), equalTo(0));
        Path skill = dir.resolve(SKILL_REL);
        assertThat(Files.exists(skill), equalTo(true));
        // The thin skill points at the published llms.txt index rather than bundling a map.
        assertThat(new String(Files.readAllBytes(skill), StandardCharsets.UTF_8), containsString("llms.txt"));
        // A write should report where it landed.
        assertThat(result.stdout(), containsString("Installed"));
        assertThat(result.stdout(), containsString(skill.toString()));
    }

    @Test
    public void shortDirFlagIsDashC(@TempDir Path dir) {
        CliUtils.Result result = CliUtils.runSmithy(
                "ai",
                "install",
                "--harness",
                "claude",
                "-C",
                dir.toString());

        assertThat(result.code(), equalTo(0));
        assertThat(Files.exists(dir.resolve(SKILL_REL)), equalTo(true));
    }

    @Test
    public void autoDetectsClaudeFromExistingDir(@TempDir Path dir) throws IOException {
        // A project that already uses Claude Code (has a .claude directory).
        Files.createDirectories(dir.resolve(".claude"));

        CliUtils.Result result = CliUtils.runSmithy("ai", "install", "--dir", dir.toString());

        assertThat(result.code(), equalTo(0));
        // Detection must be announced, not silent.
        assertThat(result.stdout().toLowerCase(Locale.ENGLISH), containsString("detected"));
        assertThat(Files.exists(dir.resolve(SKILL_REL)), equalTo(true));
    }

    @Test
    public void installsToAllDetectedHarnesses(@TempDir Path dir) throws IOException {
        // Both markers present: install to every detected harness, not just one.
        Files.createDirectories(dir.resolve(".claude"));
        Files.createDirectories(dir.resolve(".kiro"));

        CliUtils.Result result = CliUtils.runSmithy("ai", "install", "--dir", dir.toString());

        assertThat(result.code(), equalTo(0));
        assertThat(Files.exists(dir.resolve(".claude/skills/smithy-docs-navigator/SKILL.md")), equalTo(true));
        assertThat(Files.exists(dir.resolve(".kiro/skills/smithy-docs-navigator/SKILL.md")), equalTo(true));
    }

    @Test
    public void failsWhenHarnessCannotBeDetected(@TempDir Path dir) {
        // Explicit --dir with no harness markers: cannot detect, must not guess.
        CliUtils.Result result = CliUtils.runSmithy("ai", "install", "--dir", dir.toString());

        assertThat(result.code(), not(0));
        assertThat(result.stderr(), containsString("--harness"));
        // Nothing should have been written.
        assertThat(Files.exists(dir.resolve(SKILL_REL)), equalTo(false));
    }

    @Test
    public void skipsExistingWithoutForceAndSucceeds(@TempDir Path dir) throws IOException {
        Path skill = dir.resolve(SKILL_REL);
        Files.createDirectories(skill.getParent());
        Files.write(skill, "sentinel".getBytes(StandardCharsets.UTF_8));

        CliUtils.Result result = CliUtils.runSmithy(
                "ai",
                "install",
                "--harness",
                "claude",
                "--dir",
                dir.toString());

        // Idempotent: already-installed is success, not an error.
        assertThat(result.code(), equalTo(0));
        assertThat(result.stdout() + result.stderr(), containsString("--force"));
        assertThat(new String(Files.readAllBytes(skill), StandardCharsets.UTF_8), equalTo("sentinel"));
    }

    @Test
    public void overwritesWithForce(@TempDir Path dir) throws IOException {
        Path skill = dir.resolve(SKILL_REL);
        Files.createDirectories(skill.getParent());
        Files.write(skill, "sentinel".getBytes(StandardCharsets.UTF_8));

        CliUtils.Result result = CliUtils.runSmithy(
                "ai",
                "install",
                "--harness",
                "claude",
                "--dir",
                dir.toString(),
                "--force");

        assertThat(result.code(), equalTo(0));
        String updated = new String(Files.readAllBytes(skill), StandardCharsets.UTF_8);
        assertThat(updated, not(equalTo("sentinel")));
        assertThat(updated, containsString("smithy-docs-navigator"));
    }

    @Test
    public void dryRunWritesNothing(@TempDir Path dir) {
        CliUtils.Result result = CliUtils.runSmithy(
                "ai",
                "install",
                "--harness",
                "claude",
                "--dir",
                dir.toString(),
                "--dry-run");

        assertThat(result.code(), equalTo(0));
        assertThat(Files.exists(dir.resolve(SKILL_REL)), equalTo(false));
        assertThat(result.stdout(), containsString(dir.resolve(SKILL_REL).toString()));
    }

    @Test
    public void installsForKiro(@TempDir Path dir) throws IOException {
        CliUtils.Result result = CliUtils.runSmithy(
                "ai",
                "install",
                "--harness",
                "kiro",
                "--dir",
                dir.toString());

        assertThat(result.code(), equalTo(0));
        Path skill = dir.resolve(".kiro/skills/smithy-docs-navigator/SKILL.md");
        assertThat(Files.exists(skill), equalTo(true));
        assertThat(new String(Files.readAllBytes(skill), StandardCharsets.UTF_8), containsString("llms.txt"));
    }

    @Test
    public void autoDetectsKiroFromExistingDir(@TempDir Path dir) throws IOException {
        Files.createDirectories(dir.resolve(".kiro"));

        CliUtils.Result result = CliUtils.runSmithy("ai", "install", "--dir", dir.toString());

        assertThat(result.code(), equalTo(0));
        assertThat(result.stdout().toLowerCase(Locale.ENGLISH), containsString("detected"));
        assertThat(Files.exists(dir.resolve(".kiro/skills/smithy-docs-navigator/SKILL.md")), equalTo(true));
    }

    @Test
    public void dryRunReflectsAlreadyInstalledState(@TempDir Path dir) throws IOException {
        Path skill = dir.resolve(SKILL_REL);
        Files.createDirectories(skill.getParent());
        Files.write(skill, "sentinel".getBytes(StandardCharsets.UTF_8));

        CliUtils.Result result = CliUtils.runSmithy(
                "ai",
                "install",
                "--harness",
                "claude",
                "--dir",
                dir.toString(),
                "--dry-run");

        assertThat(result.code(), equalTo(0));
        // Preview must match what a real run would do: skip, not install.
        assertThat(result.stdout(), containsString("already installed"));
        // And it must still not touch the file.
        assertThat(new String(Files.readAllBytes(skill), StandardCharsets.UTF_8), equalTo("sentinel"));
    }

    @Test
    public void installsNamedBundledSkill(@TempDir Path dir) {
        CliUtils.Result result = CliUtils.runSmithy(
                "ai",
                "install",
                "--harness",
                "claude",
                "--dir",
                dir.toString(),
                "--skill",
                "smithy-docs-navigator");

        assertThat(result.code(), equalTo(0));
        assertThat(Files.exists(dir.resolve(SKILL_REL)), equalTo(true));
    }

    @Test
    public void installsExternalSkillFromPath(@TempDir Path dir, @TempDir Path src) throws IOException {
        // A user-authored skill on disk: <src>/my-skill/SKILL.md
        Path external = src.resolve("my-skill").resolve("SKILL.md");
        Files.createDirectories(external.getParent());
        Files.write(external, "custom skill body".getBytes(StandardCharsets.UTF_8));

        CliUtils.Result result = CliUtils.runSmithy(
                "ai",
                "install",
                "--harness",
                "claude",
                "--dir",
                dir.toString(),
                "--skill",
                external.toString());

        assertThat(result.code(), equalTo(0));
        Path installed = dir.resolve(".claude/skills/my-skill/SKILL.md");
        assertThat(Files.exists(installed), equalTo(true));
        assertThat(new String(Files.readAllBytes(installed), StandardCharsets.UTF_8),
                containsString("custom skill body"));
    }

    @Test
    public void installsExternalSkillSupportingFiles(@TempDir Path dir, @TempDir Path src) throws IOException {
        // A skill is SKILL.md plus any supporting files the author bundles (reference.md,
        // scripts/, ...). Install must copy the whole directory, not just a fixed subdirectory.
        Path skillRoot = src.resolve("tree-skill");
        Files.createDirectories(skillRoot.resolve("scripts"));
        Files.write(skillRoot.resolve("SKILL.md"), "root body".getBytes(StandardCharsets.UTF_8));
        Files.write(skillRoot.resolve("reference.md"), "the reference".getBytes(StandardCharsets.UTF_8));
        Files.write(skillRoot.resolve("scripts/helper.py"), "print('hi')".getBytes(StandardCharsets.UTF_8));

        CliUtils.Result result = CliUtils.runSmithy(
                "ai",
                "install",
                "--harness",
                "claude",
                "--dir",
                dir.toString(),
                "--skill",
                skillRoot.toString());

        assertThat(result.code(), equalTo(0));
        Path installed = dir.resolve(".claude/skills/tree-skill");
        assertThat(Files.exists(installed.resolve("SKILL.md")), equalTo(true));
        assertThat(Files.exists(installed.resolve("reference.md")), equalTo(true));
        assertThat(Files.exists(installed.resolve("scripts/helper.py")), equalTo(true));
        assertThat(new String(Files.readAllBytes(installed.resolve("reference.md")), StandardCharsets.UTF_8),
                equalTo("the reference"));
    }

    @Test
    public void forceOverwritesEntireTree(@TempDir Path dir, @TempDir Path src) throws IOException {
        Path skillRoot = src.resolve("tree-skill");
        Files.createDirectories(skillRoot.resolve("scripts"));
        Files.write(skillRoot.resolve("SKILL.md"), "v1 root".getBytes(StandardCharsets.UTF_8));
        Files.write(skillRoot.resolve("scripts/note.py"), "v1 note".getBytes(StandardCharsets.UTF_8));

        // First install seeds the tree.
        CliUtils.runSmithy("ai",
                "install",
                "--harness",
                "claude",
                "--dir",
                dir.toString(),
                "--skill",
                skillRoot.toString());

        // Tamper with an installed supporting file and bump the source; --force must overwrite.
        Path installedNote = dir.resolve(".claude/skills/tree-skill/scripts/note.py");
        Files.write(installedNote, "tampered".getBytes(StandardCharsets.UTF_8));
        Files.write(skillRoot.resolve("scripts/note.py"), "v2 note".getBytes(StandardCharsets.UTF_8));

        CliUtils.Result result = CliUtils.runSmithy(
                "ai",
                "install",
                "--harness",
                "claude",
                "--dir",
                dir.toString(),
                "--skill",
                skillRoot.toString(),
                "--force");

        assertThat(result.code(), equalTo(0));
        assertThat(new String(Files.readAllBytes(installedNote), StandardCharsets.UTF_8),
                equalTo("v2 note"));
    }

    @Test
    public void failsOnUnknownSkillName(@TempDir Path dir) {
        CliUtils.Result result = CliUtils.runSmithy(
                "ai",
                "install",
                "--harness",
                "claude",
                "--dir",
                dir.toString(),
                "--skill",
                "no-such-skill");

        assertThat(result.code(), not(0));
        assertThat(result.stderr(), containsString("no-such-skill"));
    }

    @Test
    public void failsOnUnsupportedHarnessWithHonestMessage(@TempDir Path dir) {
        // Gemini reads a GEMINI.md instructions file, not a SKILL.md skills directory, so a
        // skills-dir install would be a silent no-op; refuse instead.
        CliUtils.Result result = CliUtils.runSmithy(
                "ai",
                "install",
                "--harness",
                "gemini",
                "--dir",
                dir.toString());

        assertThat(result.code(), not(0));
        assertThat(result.stderr(), containsString("gemini"));
        assertThat(Files.exists(dir.resolve(".agents/skills/smithy-docs-navigator/SKILL.md")), equalTo(false));
    }

    @Test
    public void failsOnUnknownHarness(@TempDir Path dir) {
        CliUtils.Result result = CliUtils.runSmithy(
                "ai",
                "install",
                "--harness",
                "bogus",
                "--dir",
                dir.toString());

        assertThat(result.code(), not(0));
        assertThat(result.stderr(), containsString("bogus"));
    }

    // These two tests mutate the process-global user.home system property to exercise the
    // home-default install path in-process, so they are isolated here and pinned to SAME_THREAD
    // to avoid cross-test interference. The rest of the class passes --dir and is parallel-safe.
    @Nested
    @Execution(ExecutionMode.SAME_THREAD)
    class HomeDefaultInstall {

        /** Runs Smithy with {@code user.home} pointed at a scratch dir so home-default installs are testable. */
        private CliUtils.Result runWithHome(Path home, String... args) {
            String previous = System.getProperty("user.home");
            System.setProperty("user.home", home.toString());
            try {
                return CliUtils.runSmithy(args);
            } finally {
                if (previous != null) {
                    System.setProperty("user.home", previous);
                }
            }
        }

        @Test
        public void installsToUserHomeByDefault(@TempDir Path home) {
            // No --dir: the skill is a cross-project tool, so it installs under the user's home.
            CliUtils.Result result = runWithHome(home, "ai", "install", "--harness", "claude");

            assertThat(result.code(), equalTo(0));
            Path skill = home.resolve(SKILL_REL);
            assertThat(Files.exists(skill), equalTo(true));
            assertThat(result.stdout(), containsString(skill.toString()));
        }

        @Test
        public void dirOverridesHomeForProjectLocalInstall(@TempDir Path home, @TempDir Path project) {
            CliUtils.Result result = runWithHome(home,
                    "ai",
                    "install",
                    "--harness",
                    "claude",
                    "--dir",
                    project.toString());

            assertThat(result.code(), equalTo(0));
            // Installs into the project, not home.
            assertThat(Files.exists(project.resolve(SKILL_REL)), equalTo(true));
            assertThat(Files.exists(home.resolve(SKILL_REL)), equalTo(false));
        }
    }
}
