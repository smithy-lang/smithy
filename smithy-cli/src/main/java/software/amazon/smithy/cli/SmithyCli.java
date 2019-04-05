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

import java.util.function.Consumer;
import software.amazon.smithy.cli.commands.BuildCommand;
import software.amazon.smithy.cli.commands.DiffCommand;
import software.amazon.smithy.cli.commands.GenerateCommand;
import software.amazon.smithy.cli.commands.OptimizeCommand;
import software.amazon.smithy.cli.commands.ValidateCommand;

/**
 * Entry point of the Smithy CLI.
 */
public final class SmithyCli {
    private SmithyCli() {}

    public static void main(String... args) {
        run(System::exit, args);
    }

    public static int run(Consumer<Integer> exitFunction, String... args) {
        Cli cli = new Cli("smithy");
        cli.addCommand(new ValidateCommand());
        cli.addCommand(new BuildCommand());
        cli.addCommand(new DiffCommand());
        cli.addCommand(new GenerateCommand());
        cli.addCommand(new OptimizeCommand());
        int code = cli.run(args);
        if (code != 0) {
            exitFunction.accept(code);
        }

        return 0;
    }

    public static ClassLoader getConfiguredClassLoader() {
        return SmithyCli.class.getClassLoader();
    }
}
