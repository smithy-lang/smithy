/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.cli.commands;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Paths;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.SmithyCli;

public class BuildCommandTest {
    @Test
    public void hasBuildCommand() throws Exception {
        PrintStream out = System.out;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        System.setOut(printStream);
        SmithyCli.create().run("build", "--help");
        System.setOut(out);
        String help = outputStream.toString("UTF-8");

        assertThat(help, containsString("Builds"));
    }

    @Test
    public void dumpsOutValidationErrorsAndFails() throws Exception {
        PrintStream out = System.out;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        System.setOut(printStream);

        CliError e = Assertions.assertThrows(CliError.class, () -> {
            String model = Paths.get(getClass().getResource("unknown-trait.smithy").toURI()).toString();
            SmithyCli.create().run("build", model);
        });

        System.setOut(out);
        String output = outputStream.toString("UTF-8");

        assertThat(output, containsString("smithy.example#MyString"));
        assertThat(output, containsString("1 ERROR"));
        assertThat(e.getMessage(), containsString("The model is invalid"));
    }

    @Test
    public void printsSuccessfulProjections() throws Exception {
        String model = Paths.get(getClass().getResource("valid-model.smithy").toURI()).toString();

        PrintStream out = System.out;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        System.setOut(printStream);
        SmithyCli.create().run("build", model);
        System.setOut(out);
        String output = outputStream.toString("UTF-8");

        assertThat(output, containsString("Completed projection source"));
        assertThat(output, containsString("Smithy built "));
    }

    @Test
    public void validationFailuresCausedByProjectionsAreDetected() throws Exception {
        PrintStream out = System.out;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        System.setOut(printStream);

        CliError e = Assertions.assertThrows(CliError.class, () -> {
            String model = Paths.get(getClass().getResource("valid-model.smithy").toURI()).toString();
            String config = Paths.get(getClass().getResource("projection-build-failure.json").toURI()).toString();
            SmithyCli.create().run("build", "--debug", "--config", config, model);
        });

        System.setOut(out);
        String output = outputStream.toString("UTF-8");

        assertThat(output, containsString("ResourceLifecycle"));
        assertThat(e.getMessage(),
                   containsString("The following 1 Smithy build projection(s) failed: [exampleProjection]"));
    }

    @Test
    public void exceptionsThrownByProjectionsAreDetected() {
        // TODO: need to make a plugin throw an exception
    }
}
