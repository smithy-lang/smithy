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

package software.amazon.smithy.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.cli.commands.BuildCommand;
import software.amazon.smithy.cli.commands.ValidateCommand;

public class CliTest {
    @Test
    public void noArgsPrintsMainHelp() throws Exception {
        Cli cli = new Cli("mytest");
        PrintStream out = System.out;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        System.setOut(printStream);
        cli.run(new String[]{});
        System.setOut(out);
        String help = outputStream.toString("UTF-8");

        assertThat(help, containsString("mytest"));
    }

    @Test
    public void printsMainHelp() throws Exception {
        Cli cli = new Cli("mytest");
        cli.addCommand(new BuildCommand());
        cli.addCommand(new ValidateCommand());
        PrintStream out = System.out;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        System.setOut(printStream);
        cli.run(new String[]{"--help"});
        System.setOut(out);
        String help = outputStream.toString("UTF-8");

        assertThat(help, containsString("build"));
        assertThat(help, containsString("validate"));
        assertThat(help, containsString("mytest"));
    }

    @Test
    public void printsSubcommandHelp() throws Exception {
        Cli cli = new Cli("mytest");
        cli.addCommand(new BuildCommand());
        cli.addCommand(new ValidateCommand());
        PrintStream out = System.out;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        System.setOut(printStream);
        cli.run(new String[]{"validate", "--help"});
        System.setOut(out);
        String help = outputStream.toString("UTF-8");

        assertThat(help, containsString("validate"));
        assertThat(help, containsString("--help"));
        assertThat(help, containsString("--debug"));
        assertThat(help, containsString("--no-color"));
    }

    @Test
    public void showsStacktrace() throws Exception {
        Cli cli = new Cli("mytest");
        PrintStream out = System.out;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        System.setOut(printStream);

        try {
            cli.run(new String[]{"invalid", "--stacktrace"});
            Assertions.fail("Expected to throw");
        } catch (RuntimeException e) {
        }

        System.setOut(out);
        String help = outputStream.toString("UTF-8");

        assertThat(help, containsString("Unknown command or argument"));
    }
}
