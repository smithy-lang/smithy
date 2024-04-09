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
                                                    "--old", oldModel.toString(),
                                                    "--new", newModel.toString(),
                                                    "--format", "csv");

        assertThat(result.code(), not(0));

        // Make sure FAILURE is sent to stderr.
        assertThat(result.stderr(), containsString("FAILURE"));
        assertThat(result.stdout(), not(containsString("FAILURE")));

        String[] lines = result.stdout().split("(\\r\\n|\\r|\\n)");
        assertThat(lines.length, is(2));
        assertThat(lines[0], containsString("severity,id,shape,file,line,column,message,hint,suppressionReason"));
        assertThat(lines[1], containsString("\"ERROR\",\"ChangedShapeType\",\"smithy.example#Hello\""));
    }
}
