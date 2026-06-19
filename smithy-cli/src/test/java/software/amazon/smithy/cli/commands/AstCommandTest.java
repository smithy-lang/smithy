/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.cli.CliUtils;

public class AstCommandTest {
    @Test
    public void hasLongHelpCommand() {
        CliUtils.Result result = CliUtils.runSmithy("ast", "--help");

        assertThat(result.code(), equalTo(0));
        assertThat(result.stdout(), containsString("Reads"));
    }

    @Test
    public void hasShortHelpCommand() {
        CliUtils.Result result = CliUtils.runSmithy("ast", "-h");

        assertThat(result.code(), equalTo(0));
        assertThat(result.stdout(), containsString("Reads"));
    }

    @Test
    public void failsOnUnknownTrait() throws Exception {
        String model = Paths.get(getClass().getResource("unknown-trait.smithy").toURI()).toString();
        CliUtils.Result result = CliUtils.runSmithy("ast", model);

        assertThat(result.code(), not(0));
        assertThat(result.stderr(), containsString("ERROR: 1"));
    }

    @Test
    public void allowsUnknownTrait() throws URISyntaxException {
        String model = Paths.get(getClass().getResource("unknown-trait.smithy").toURI()).toString();
        CliUtils.Result result = CliUtils.runSmithy("ast", "--allow-unknown-traits", model);

        assertThat(result.code(), equalTo(0));
    }

    @Test
    public void selectorEmitsOnlyOperationClosure() throws URISyntaxException {
        String model = Paths.get(getClass().getResource("valid-model.smithy").toURI()).toString();
        // The operation plus everything it references (input, output, and the FooId those members target).
        CliUtils.Result result = CliUtils.runSmithy(
                "ast",
                "--selector",
                "[id = smithy.example#GetFoo] :is(*, ~>)",
                model);

        assertThat(result.code(), equalTo(0));
        // Closure members are present.
        assertThat(result.stdout(), containsString("smithy.example#GetFoo"));
        assertThat(result.stdout(), containsString("smithy.example#GetFooInput"));
        assertThat(result.stdout(), containsString("smithy.example#GetFooOutput"));
        assertThat(result.stdout(), containsString("smithy.example#FooId"));
        // The containing resource is NOT reachable from the operation, so it must be excluded.
        assertThat(result.stdout(), not(containsString("smithy.example#Foo\"")));
    }

    @Test
    public void noDocsStripsDocumentationButKeepsStructure() throws URISyntaxException {
        String model = Paths.get(getClass().getResource("documented-model.smithy").toURI()).toString();
        CliUtils.Result result = CliUtils.runSmithy("ast", "--no-docs", model);

        assertThat(result.code(), equalTo(0));
        // Documentation trait values are gone...
        assertThat(result.stdout(), not(containsString("should be stripped")));
        assertThat(result.stdout(), not(containsString("smithy.api#documentation")));
        // ...but the structure (shapes, members, and functional traits) remains.
        assertThat(result.stdout(), containsString("smithy.example#DocInput"));
        assertThat(result.stdout(), containsString("smithy.api#required"));
    }

    @Test
    public void keepsDocumentationByDefault() throws URISyntaxException {
        String model = Paths.get(getClass().getResource("documented-model.smithy").toURI()).toString();
        CliUtils.Result result = CliUtils.runSmithy("ast", model);

        assertThat(result.code(), equalTo(0));
        assertThat(result.stdout(), containsString("smithy.api#documentation"));
    }

    @Test
    public void selectorlessAstEmitsWholeModel() throws URISyntaxException {
        String model = Paths.get(getClass().getResource("valid-model.smithy").toURI()).toString();
        CliUtils.Result result = CliUtils.runSmithy("ast", model);

        assertThat(result.code(), equalTo(0));
        // Without a selector the resource (not in any single operation's closure) is still present.
        assertThat(result.stdout(), containsString("smithy.example#Foo"));
        assertThat(result.stdout(), containsString("smithy.example#GetFoo"));
    }
}
