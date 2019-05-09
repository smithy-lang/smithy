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

import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.Parser;

/**
 * This class is really only here so that it appears in the help output of the
 * CLI. The launcher script should perform the optimization.
 *
 * TODO: implement an optimization launcher template for Windows.
 */
public final class OptimizeCommand implements Command {
    @Override
    public String getName() {
        return "optimize";
    }

    @Override
    public String getSummary() {
        return "Performs a one-time optimization to start the CLI faster";
    }

    @Override
    public String getHelp() {
        return String.format(
                "Running the optimize command will enable Java %n"
                + "Class Data Sharing (CDS). This could make the CLI run%n"
                + "up to ~20%% faster, but generates a ~20MB file.");
    }

    @Override
    public Parser getParser() {
        return Parser.builder().build();
    }

    @Override
    public void execute(Arguments arguments, ClassLoader classLoader) {
        throw new UnsupportedOperationException("Expected the CLI runner to perform optimizations");
    }
}
