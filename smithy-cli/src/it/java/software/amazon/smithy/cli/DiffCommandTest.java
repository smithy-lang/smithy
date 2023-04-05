/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import static org.hamcrest.Matchers.is;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.ListUtils;

public class DiffCommandTest {
    @Test
    public void passingDiffEventsExitZero() {
        IntegUtils.withTempDir("diff", dir -> {
            Path a = dir.resolve("a.smithy");
            writeFile(a, "$version: \"2.0\"\nnamespace example\nstring A\n");

            RunResult result = IntegUtils.run(dir, ListUtils.of("diff", "--old", a.toString(), "--new", a.toString()));
            assertThat("Not 0: output [" + result.getOutput() + ']', result.getExitCode(), is(0));
        });
    }

    @Test
    public void showsLabelForOldModelEvents() {
        IntegUtils.withTempDir("diff", dir -> {
            Path a = dir.resolve("a.smithy");
            writeFile(a, "$version: \"2.0\"\nnamespace example\n@aaaaaa\nstring A\n");

            RunResult result = IntegUtils.run(dir, ListUtils.of("diff", "--old", a.toString(), "--new", a.toString()));
            assertThat("Not 1: output [" + result.getOutput() + ']', result.getExitCode(), is(1));
            assertThat(result.getOutput(), containsString("──  OLD  ERROR  ──"));
        });
    }

    @Test
    public void showsLabelForNewModelEvents() {
        IntegUtils.withTempDir("diff", dir -> {
            Path a = dir.resolve("a.smithy");
            writeFile(a, "$version: \"2.0\"\nnamespace example\nstring A\n");

            Path b = dir.resolve("b.smithy");
            writeFile(b, "$version: \"2.0\"\nnamespace example\n@aaaaaa\nstring A\n");

            RunResult result = IntegUtils.run(dir, ListUtils.of("diff", "--old", a.toString(), "--new", b.toString()));
            assertThat("Not 1: output [" + result.getOutput() + ']', result.getExitCode(), is(1));
            assertThat(result.getOutput(), containsString("──  NEW  ERROR  ──"));
        });
    }

    @Test
    public void showsLabelForDiffEvents() {
        IntegUtils.withTempDir("diff", dir -> {
            Path a = dir.resolve("a.smithy");
            writeFile(a, "$version: \"2.0\"\nnamespace example\nstring A\n");

            Path b = dir.resolve("b.smithy");
            writeFile(b, "$version: \"2.0\"\nnamespace example\nstring A\nstring B\n"); // Added B.

            RunResult result = IntegUtils.run(dir, ListUtils.of(
                    "diff",
                    "--old", a.toString(),
                    "--new", b.toString(),
                    "--severity", "NOTE")); // Note that this is required since the default severity is WARNING.
            assertThat("Not 0: output [" + result.getOutput() + ']', result.getExitCode(), is(0));
            assertThat(result.getOutput(), containsString("──  DIFF  NOTE  ──"));
        });
    }

    private void writeFile(Path path, String contents) {
        try {
            FileWriter fileWriter = new FileWriter(path.toString());
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.print(contents);
            printWriter.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    public void doesNotUseImportsOrSourcesWithArbitraryMode() {
        IntegUtils.withProject("simple-config-sources", dir -> {
            Path file = dir.resolve("a.smithy");
            // The model from simple-config-sources defines a MyString. This would fail if we used imports/sources.
            writeFile(file, "$version: \"2.0\"\nnamespace smithy.example\ninteger MyString\n");

            RunResult result = IntegUtils.run(dir, ListUtils.of("diff",
                                                                "-c", dir.resolve("smithy-build.json").toString(),
                                                                "--old", file.toString(),
                                                                "--new", file.toString()));
            assertThat("Not 0: output [" + result.getOutput() + ']', result.getExitCode(), is(0));
        });
    }

    @Test
    public void requiresOldAndNewForArbitraryMode() {
        RunResult result = IntegUtils.run(Paths.get("."), ListUtils.of("diff"));

        assertThat("Not 1: output [" + result.getOutput() + ']', result.getExitCode(), is(1));
        assertThat(result.getOutput(), containsString("Missing required --old argument"));
    }

    @Test
    public void doesNotAllowNewWithProjectMode() {
        RunResult result = IntegUtils.run(Paths.get("."), ListUtils.of("diff",
                                                                       "--mode", "project",
                                                                       "--new", "x",
                                                                       "--old", "y"));

        assertThat("Not 1: output [" + result.getOutput() + ']', result.getExitCode(), is(1));
        assertThat(result.getOutput(), containsString("--new cannot be used with this diff mode"));
    }

    @Test
    public void projectModeUsesConfigOfOldModel() {
        IntegUtils.withProject("diff-example-conflict-with-simple", outer -> {
            IntegUtils.withProject("simple-config-sources", dir -> {
                RunResult result = IntegUtils.run(dir, ListUtils.of(
                        "diff",
                        "--mode",
                        "project",
                        "--old",
                        outer.toString()));

                assertThat("Not 1: output [" + result.getOutput() + ']', result.getExitCode(), is(1));
                assertThat(result.getOutput(), containsString("ChangedShapeType"));
            });
        });
    }

    @Test
    public void projectModeCanDiffAgainstSingleFile() {
        // Diff against itself (the only model file of the project), so there should be no differences.
        IntegUtils.withProject("simple-config-sources", dir -> {
            RunResult result = IntegUtils.run(dir, ListUtils.of(
                    "diff",
                    "--mode",
                    "project",
                    "--old",
                    dir.resolve("model").resolve("main.smithy").toString()));

            assertThat("Not 0: output [" + result.getOutput() + ']', result.getExitCode(), is(0));
        });
    }

    @Test
    public void gitDiffAgainstLastCommit() {
        // Diff against itself (the only model file of the project), so there should be no differences.
        IntegUtils.withProject("simple-config-sources", dir -> {
            initRepo(dir);
            commitChanges(dir);

            RunResult result = runDiff(dir);

            assertThat("Not 0: output [" + result.getOutput() + ']', result.getExitCode(), is(0));
        });
    }

    private RunResult runDiff(Path dir) {
        return runDiff(dir, null);
    }

    private RunResult runDiff(Path dir, String oldCommit) {
        List<String> args = new ArrayList<>();
        args.add("diff");
        args.add("--mode");
        args.add("git");
        if (oldCommit != null) {
            args.add("--old");
            args.add(oldCommit);
        }

        return IntegUtils.run(dir, args);
    }

    @Test
    public void canDiffAgainstLastCommitWithFailure() {
        IntegUtils.withProject("simple-config-sources", dir -> {
            initRepo(dir);
            commitChanges(dir);

            Path file = dir.resolve("model").resolve("main.smithy");
            writeFile(file, "$version: \"2.0\"\nnamespace smithy.example\ninteger MyString\n");
            RunResult result = IntegUtils.run(dir, ListUtils.of("diff", "--mode", "git"));

            assertThat("Not 1: output [" + result.getOutput() + ']', result.getExitCode(), is(1));
            assertThat(result.getOutput(), containsString("ChangedShapeType"));
        });
    }

    private void initRepo(Path dir) {
        run(ListUtils.of("git", "init"), dir);
        run(ListUtils.of("git", "config", "user.email", "you@example.com"), dir);
        run(ListUtils.of("git", "config", "user.name", "Your Name"), dir);
    }

    private void commitChanges(Path dir) {
        run(ListUtils.of("git", "add", "-A"), dir);
        run(ListUtils.of("git", "commit", "-m", "Foo"), dir);
    }

    private void run(List<String> args, Path root) {
        StringBuilder output = new StringBuilder();
        int result = IoUtils.runCommand(args, root, output, Collections.emptyMap());
        if (result != 0) {
            throw new RuntimeException("Error running command: " + args + ": " + output);
        }
    }

    @Test
    public void gitDiffAgainstLastCommitAfterClean() {
        IntegUtils.withProject("simple-config-sources", dir -> {
            initRepo(dir);
            commitChanges(dir);
            RunResult result = runDiff(dir);
            assertThat("Not zero: output [" + result.getOutput() + ']', result.getExitCode(), is(0));

            // This will remove the previously created worktree.
            IntegUtils.run(dir, ListUtils.of("clean"));

            // Running diff again ensures that we handle the case where there's a prunable worktree.
            result = runDiff(dir);

            assertThat("Not zero: output [" + result.getOutput() + ']', result.getExitCode(), is(0));
        });
    }

    @Test
    public void gitDiffEnsuresHeadUpdatesAsCommitsAreMade() {
        IntegUtils.withProject("simple-config-sources", dir -> {
            initRepo(dir);
            commitChanges(dir);

            // Run with HEAD (the default)
            RunResult result = runDiff(dir);
            assertThat("Not zero: output [" + result.getOutput() + ']', result.getExitCode(), is(0));

            // Now make another commit, which updates HEAD and ensures the worktree updates too.
            Path file = dir.resolve("model").resolve("main.smithy");
            writeFile(file, "$version: \"2.0\"\nnamespace smithy.example\ninteger MyString\n");
            commitChanges(dir);

            // Run diff again, which won't fail because the last commit is the change that broke the model, meaning
            // the current state of the model is valid.
            result = runDiff(dir);

            assertThat("Not zero: output [" + result.getOutput() + ']', result.getExitCode(), is(0));
        });
    }

    @Test
    public void gitDiffAgainstSpecificCommit() {
        IntegUtils.withProject("simple-config-sources", dir -> {
            initRepo(dir);
            commitChanges(dir);

            // Run with explicit HEAD
            RunResult result = runDiff(dir, "HEAD");
            assertThat("Not zero: output [" + result.getOutput() + ']', result.getExitCode(), is(0));

            // Now make another commit, which updates HEAD and ensures the worktree updates too.
            Path file = dir.resolve("model").resolve("main.smithy");
            writeFile(file, "$version: \"2.0\"\nnamespace smithy.example\ninteger MyString\n");
            commitChanges(dir);

            // Run diff again, which won't fail because the last commit is the change that broke the model, meaning
            // the current state of the model is valid.
            result = runDiff(dir, "HEAD~1");

            assertThat("Not 1: output [" + result.getOutput() + ']', result.getExitCode(), is(1));
            assertThat(result.getOutput(), containsString("ChangedShapeType"));
        });
    }

    @Test
    public void gitModeDoesNotAllowNewArgument() {
        IntegUtils.withProject("simple-config-sources", dir -> {
            initRepo(dir);
            commitChanges(dir);

            // Now that it's running in a git repo, it won't fail due to that and will validate that --new is invalid.
            RunResult result = IntegUtils.run(dir, ListUtils.of("diff", "--mode", "git", "--new", "some-file.smithy"));

            assertThat("Not 1: output [" + result.getOutput() + ']', result.getExitCode(), is(1));
            assertThat(result.getOutput(), containsString("--new cannot be used with this diff mode"));
        });
    }
}
