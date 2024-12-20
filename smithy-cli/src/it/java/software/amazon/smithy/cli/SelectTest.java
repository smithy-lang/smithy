/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;

public class SelectTest {
    @Test
    public void selectsShapeIds() {
        List<String> args = Arrays.asList("select", "--selector", "string [id|namespace=smithy.example]");
        IntegUtils.run("simple-config-sources", args, result -> {
            assertThat(result.getExitCode(), equalTo(0));
            assertThat(result.getOutput().trim(), equalTo("smithy.example#MyString"));
        });
    }

    @Test
    public void doesNotShowWarnings() {
        List<String> args = Arrays.asList("select", "--selector", "string [id|namespace=smithy.example]");
        IntegUtils.run("model-with-warning", args, result -> {
            assertThat(result.getExitCode(), equalTo(0));
            assertThat(result.getOutput().trim(), equalTo("smithy.example#MyString"));
        });
    }

    @Test
    public void selectsVariables() {
        List<String> args = Arrays.asList("select", "--show", "vars", "--selector", "list $list(*) > member > string");
        IntegUtils.run("simple-config-sources", args, result -> {
            assertThat(result.getExitCode(), equalTo(0));
            String content = result.getOutput().trim();
            // Ensure it's valid JSON
            Node.parse(content);
            assertThat(content, containsString("\"shape\": \"smithy.api#String\""));
        });
    }

    @Test
    public void selectsTraits() {
        List<String> args = Arrays.asList("select",
                // Can use just shape names, or an absolute shape ID. Including a trait
                // here doesn't mean a shape in the result is required to have the trait.
                "--show-traits",
                "length, range, smithy.api#documentation",
                // Every match has to have length or range, but documentation is optional.
                "--selector",
                ":is([trait|length], [trait|range])");
        IntegUtils.run("simple-config-sources", args, result -> {
            assertThat(result.getExitCode(), equalTo(0));
            String content = result.getOutput().trim();
            // Ensure it's valid JSON
            Node.parse(content);
            assertThat(content, containsString("\"shape\": \""));
            assertThat(content, containsString("\"traits\": {"));
            assertThat(content, containsString("\"smithy.api#length\": {"));
            assertThat(content, containsString("\"smithy.api#range\": {"));
            assertThat(content, containsString("\"min\": "));
            assertThat(content, containsString("\"max\": "));
            assertThat(content, containsString("\"smithy.api#documentation\": "));
        });
    }

    @Test
    public void showTraitsCannotBeEmpty() {
        List<String> args = Arrays.asList("select", "--show-traits", "", "--selector", "service");
        IntegUtils.run("simple-config-sources", args, result -> {
            assertThat(result.getExitCode(), not(equalTo(0)));
        });
    }

    @Test
    public void showTraitsCannotHaveEmptyValues() {
        List<String> args = Arrays.asList("select", "--show-traits", "documentation,", "--selector", "service");
        IntegUtils.run("simple-config-sources", args, result -> {
            assertThat(result.getExitCode(), not(equalTo(0)));
        });
    }

    @Test
    public void showCannotBeEmpty() {
        List<String> args = Arrays.asList("select", "--show", "", "--selector", "string");
        IntegUtils.run("simple-config-sources", args, result -> {
            assertThat(result.getExitCode(), equalTo(1));
        });
    }

    @Test
    public void showCannotContainInvalidValues() {
        List<String> args = Arrays.asList("select", "--show", "foo", "--selector", "string");
        IntegUtils.run("simple-config-sources", args, result -> {
            assertThat(result.getExitCode(), equalTo(1));
        });
    }

    @Test
    public void showCannotContainInvalidValuesInCsv() {
        List<String> args = Arrays.asList("select", "--show", "vars,foo", "--selector", "string");
        IntegUtils.run("simple-config-sources", args, result -> {
            assertThat(result.getExitCode(), equalTo(1));
        });
    }

    @Test
    public void includesType() {
        List<String> args = Arrays.asList("select", "--show", "type", "--selector", "string");
        IntegUtils.run("simple-config-sources", args, result -> {
            assertThat(result.getExitCode(), equalTo(0));
            String content = result.getOutput().trim();
            // Ensure it's valid JSON
            Node.parse(content);
            assertThat(content, containsString("\"type\": \"string\""));
        });
    }

    @Test
    public void includesFile() {
        List<String> args = Arrays.asList("select", "--show", "file", "--selector", "string");
        IntegUtils.run("simple-config-sources", args, result -> {
            assertThat(result.getExitCode(), equalTo(0));
            String content = result.getOutput().trim();
            // Ensure it's valid JSON
            Node.parse(content);
            assertThat(content, containsString("\"file\": "));
        });
    }

    @Test
    public void includesFileAndType() {
        List<String> args = Arrays.asList("select", "--show", "file, type", "--selector", "string");
        IntegUtils.run("simple-config-sources", args, result -> {
            assertThat(result.getExitCode(), equalTo(0));
            String content = result.getOutput().trim();
            // Ensure it's valid JSON
            Node.parse(content);
            assertThat(content, containsString("\"type\": \"string\""));
            assertThat(content, containsString("\"file\": "));
        });
    }

    @Test
    public void includesFileAndTypeAndVars() {
        List<String> args = Arrays.asList("select", "--show", "file, type,vars", "--selector", "string $hi(*)");
        IntegUtils.run("simple-config-sources", args, result -> {
            assertThat(result.getExitCode(), equalTo(0));
            String content = result.getOutput().trim();
            // Ensure it's valid JSON
            Node.parse(content);
            assertThat(content, containsString("\"type\": \"string\""));
            assertThat(content, containsString("\"file\": "));
            assertThat(content, containsString("\"vars\": "));
            assertThat(content, containsString("\"hi\": "));
        });
    }
}
