/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.cli.CliUtils;

public class DiffCommandTest {
    @Test
    public void canOutputCsv() throws Exception {
        Path oldModel = Paths.get(getClass().getResource("diff/old.smithy").toURI());
        Path newModel = Paths.get(getClass().getResource("diff/new.smithy").toURI());
        CliUtils.Result result = CliUtils.runSmithy("diff",
                "--old",
                oldModel.toString(),
                "--new",
                newModel.toString(),
                "--format",
                "csv");

        assertThat(result.code(), not(0));

        // Make sure FAILURE is sent to stderr.
        assertThat(result.stderr(), containsString("FAILURE"));
        assertThat(result.stdout(), not(containsString("FAILURE")));

        String[] lines = result.stdout().split("(\\r\\n|\\r|\\n)");
        assertThat(lines.length, is(2));
        assertThat(lines[0], containsString("severity,id,shape,file,line,column,message,hint,suppressionReason"));
        assertThat(lines[1], containsString("\"ERROR\",\"ChangedShapeType\",\"smithy.example#Hello\""));
    }

    @Test
    public void showsWarningEventsByDefault() throws Exception {
        Path oldModel = Paths.get(getClass().getResource("diff/old2.smithy").toURI());
        Path newModel = Paths.get(getClass().getResource("diff/new2.smithy").toURI());
        CliUtils.Result result = CliUtils.runSmithy("diff",
                "--old", oldModel.toString(),
                "--new", newModel.toString());

        assertThat(result.code(), is(0));

        String[] lines = result.stdout().split("(\\r\\n|\\r|\\n)");
        assertThat(lines[1], containsString("WARNING"));
        assertThat(lines[1], containsString("TraitBreakingChange.Add.smithy.api#pattern"));
    }

    @Test
    public void doesNotShowWarningEventsWhenSeverityIsSetToDanger() throws Exception {
        Path oldModel = Paths.get(getClass().getResource("diff/old2.smithy").toURI());
        Path newModel = Paths.get(getClass().getResource("diff/new2.smithy").toURI());
        CliUtils.Result result = CliUtils.runSmithy("diff",
                // warning events won't be shown in the output
                "--severity", "DANGER",
                "--old", oldModel.toString(),
                "--new", newModel.toString());

        assertThat(result.code(), is(0));
        assertThat(result.stdout(), is(""));
    }
}
