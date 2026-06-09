/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.utils.ListUtils;

public class FormatCommandTest {
    @Test
    public void failsWhenNoModelsGiven() {
        IntegUtils.run("bad-formatting", ListUtils.of("format"), result -> {
            assertThat(result.getExitCode(), equalTo(1));
            assertThat(result.getOutput(),
                    containsString("No .smithy model or directory was provided as a positional argument"));
        });
    }

    @Test
    public void failsWhenBadFileGiven() {
        IntegUtils.run("bad-formatting", ListUtils.of("format", "THIS_FILE_DOES_NOT_EXIST_1234"), result -> {
            assertThat(result.getExitCode(), equalTo(1));

            assertThat(result.getOutput(),
                    containsString("`THIS_FILE_DOES_NOT_EXIST_1234` is not a valid file or directory"));
        });
    }

    @Test
    public void formatsSingleFile() {
        IntegUtils.run("bad-formatting", ListUtils.of("format", "model/other.smithy"), result -> {
            assertThat(result.getExitCode(), equalTo(0));

            String model = result.getFile("model/other.smithy");
            assertThat(model,
                    equalTo(String.format("$version: \"2.0\"%n"
                            + "%n"
                            + "namespace smithy.example%n"
                            + "%n"
                            + "string MyString%n"
                            + "%n"
                            + "string MyString2%n")));
        });
    }

    @Test
    public void formatsDirectory() {
        IntegUtils.run("bad-formatting", ListUtils.of("format", "model"), result -> {
            assertThat(result.getExitCode(), equalTo(0));

            String main = result.getFile("model/main.smithy");
            assertThat(main,
                    equalTo(String.format("$version: \"2.0\"%n"
                            + "%n"
                            + "metadata this_is_a_long_string = {%n"
                            + "    this_is_a_long_string1: \"a\"%n"
                            + "    this_is_a_long_string2: \"b\"%n"
                            + "    this_is_a_long_string3: \"c\"%n"
                            + "    this_is_a_long_string4: \"d\"%n"
                            + "}%n")));

            String other = result.getFile("model/other.smithy");
            assertThat(other,
                    equalTo(String.format("$version: \"2.0\"%n"
                            + "%n"
                            + "namespace smithy.example%n"
                            + "%n"
                            + "string MyString%n"
                            + "%n"
                            + "string MyString2%n")));
        });
    }

    @Test
    public void checkFailsWhenFormattingWouldChangeFiles() {
        IntegUtils.run("bad-formatting", ListUtils.of("format", "--check", "model"), result -> {
            assertThat(result.getExitCode(), equalTo(1));
            assertThat(result.getOutput(), containsString("would be reformatted"));

            // --check must not actually modify the files
            String unchanged = result.getFile("model/other.smithy");
            assertThat(unchanged, containsString(String.format("string MyString%nstring MyString2")));
        });
    }

    @Test
    public void checkSucceedsWhenAlreadyFormatted() {
        IntegUtils.withProject("bad-formatting", path -> {
            RunResult formatted = IntegUtils.run(path, ListUtils.of("format", "model"));
            assertThat(formatted.getExitCode(), equalTo(0));

            RunResult checked = IntegUtils.run(path, ListUtils.of("format", "--check", "model"));
            assertThat(checked.getExitCode(), equalTo(0));
        });
    }

    @Test
    public void formatsSourcesFromSmithyBuildConfig() {
        IntegUtils.run("format-from-config", ListUtils.of("format"), result -> {
            assertThat(result.getExitCode(), equalTo(0));

            String main = result.getFile("model/main.smithy");
            assertThat(main,
                    equalTo(String.format("$version: \"2.0\"%n"
                            + "%n"
                            + "metadata this_is_a_long_string = {%n"
                            + "    this_is_a_long_string1: \"a\"%n"
                            + "    this_is_a_long_string2: \"b\"%n"
                            + "    this_is_a_long_string3: \"c\"%n"
                            + "    this_is_a_long_string4: \"d\"%n"
                            + "}%n")));

            String other = result.getFile("model/other.smithy");
            assertThat(other,
                    equalTo(String.format("$version: \"2.0\"%n"
                            + "%n"
                            + "namespace smithy.example%n"
                            + "%n"
                            + "string MyString%n"
                            + "%n"
                            + "string MyString2%n")));

            String vendored = result.getFile("vendored/vendored.smithy");
            assertThat(vendored,
                    equalTo(String.format("$version: \"2.0\"%n"
                            + "namespace smithy.vendored%n"
                            + "string VendoredString%n")));
        });
    }

    @Test
    public void failsWhenBothModelsAndConfigGiven() {
        IntegUtils.run("format-from-config",
                ListUtils.of("format", "--config", "smithy-build.json", "model"),
                result -> {
                    assertThat(result.getExitCode(), equalTo(1));
                    assertThat(result.getOutput(),
                            containsString("Cannot combine --config with positional model arguments"));
                });
    }

    @Test
    public void failsWhenNoArgsAndNoConfigFlag() {
        IntegUtils.run("format-from-config", ListUtils.of("format", "--no-config"), result -> {
            assertThat(result.getExitCode(), equalTo(1));
            assertThat(result.getOutput(),
                    containsString("No .smithy model or directory was provided as a positional argument"));
        });
    }
}
