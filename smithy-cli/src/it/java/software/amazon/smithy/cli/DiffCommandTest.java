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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.utils.ListUtils;

public class DiffCommandTest {
    @Test
    public void passingDiffEventsExitZero() {
        IntegUtils.withTempDir("diff", dir -> {
            Path a = dir.resolve("a.smithy");
            writeFile(a, "$version: \"2.0\"\nnamespace example\nstring A\n");

            RunResult result = IntegUtils.run(dir, ListUtils.of("diff", "--old", a.toString(), "--new", a.toString()));
            assertThat(result.getExitCode(), equalTo(0));
        });
    }

    @Test
    public void showsLabelForOldModelEvents() {
        IntegUtils.withTempDir("diff", dir -> {
            Path a = dir.resolve("a.smithy");
            writeFile(a, "$version: \"2.0\"\nnamespace example\n@aaaaaa\nstring A\n");

            RunResult result = IntegUtils.run(dir, ListUtils.of("diff", "--old", a.toString(), "--new", a.toString()));
            assertThat(result.getExitCode(), equalTo(1));
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
            assertThat(result.getExitCode(), equalTo(1));
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
            assertThat(result.getExitCode(), equalTo(0));
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
            assertThat(result.getExitCode(), equalTo(0));
        });
    }

    @Test
    public void requiresOldAndNewForArbitraryMode() {
        RunResult result = IntegUtils.run(Paths.get("."), ListUtils.of("diff"));

        assertThat(result.getExitCode(), is(1));
        assertThat(result.getOutput(), containsString("Missing required --old argument"));
    }

    @Test
    public void doesNotAllowNewWithProjectMode() {
        RunResult result = IntegUtils.run(Paths.get("."), ListUtils.of("diff",
                                                                       "--mode", "project",
                                                                       "--new", "x",
                                                                       "--old", "y"));

        assertThat(result.getExitCode(), is(1));
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
                assertThat(result.getOutput(), containsString("ChangedShapeType"));
                assertThat(result.getExitCode(), equalTo(1));
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
            assertThat(result.getExitCode(), equalTo(0));
        });
    }
}
