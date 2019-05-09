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

    private Consumer<Integer> exitFunction = System::exit;
    private ClassLoader classLoader = getClass().getClassLoader();

    private SmithyCli() {}

    public static SmithyCli create() {
        return new SmithyCli();
    }

    public SmithyCli exitFunction(Consumer<Integer> exitFunction) {
        this.exitFunction = exitFunction;
        return this;
    }

    public SmithyCli classLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    public static void main(String... args) {
        SmithyCli.create().run(args);
    }

    public int run(String... args) {
        Cli cli = new Cli("smithy");
        cli.addCommand(new ValidateCommand(classLoader));
        cli.addCommand(new BuildCommand(classLoader));
        cli.addCommand(new DiffCommand(classLoader));
        cli.addCommand(new GenerateCommand(classLoader));
        cli.addCommand(new OptimizeCommand());

        int code = cli.run(args);
        if (code != 0) {
            exitFunction.accept(code);
        }

        return 0;
    }
}
