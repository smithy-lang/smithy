/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.List;
import software.amazon.smithy.cli.commands.AstCommand;
import software.amazon.smithy.cli.commands.BuildCommand;
import software.amazon.smithy.cli.commands.DiffCommand;
import software.amazon.smithy.cli.commands.SelectCommand;
import software.amazon.smithy.cli.commands.ValidateCommand;

/**
 * Entry point of the Smithy CLI.
 */
public final class SmithyCli {
    public static final String DISCOVER = "--discover";
    public static final String DISCOVER_CLASSPATH = "--discover-classpath";
    public static final String ALLOW_UNKNOWN_TRAITS = "--allow-unknown-traits";

    private ClassLoader classLoader = getClass().getClassLoader();

    private SmithyCli() {}

    /**
     * Creates a new instance of the CLI.
     *
     * @return Returns the CLI instance.
     */
    public static SmithyCli create() {
        return new SmithyCli();
    }

    /**
     * Executes the CLI.
     *
     * @param args Arguments to parse and execute.
     */
    public static void main(String... args) {
        try {
            SmithyCli.create().run(args);
        } catch (CliError e) {
            System.exit(e.code);
        } catch (Exception e) {
            System.exit(1);
        }
    }

    /**
     * Sets a custom class loader to use when executing commands.
     *
     * @param classLoader Class loader used to find models, traits, etc.
     * @return Returns the CLI.
     */
    public SmithyCli classLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    /**
     * Runs the CLI using a list of arguments.
     *
     * @param args Arguments to parse and execute.
     */
    public void run(List<String> args) {
        run(args.toArray(new String[0]));
    }

    /**
     * Runs the CLI.
     *
     * @param args Arguments to parse and execute.
     */
    public void run(String... args) {
        Cli cli = new Cli("smithy", classLoader);
        cli.addCommand(new ValidateCommand());
        cli.addCommand(new BuildCommand());
        cli.addCommand(new DiffCommand());
        cli.addCommand(new SelectCommand());
        cli.addCommand(new AstCommand());
        cli.run(args);
    }
}
