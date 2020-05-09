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

package software.amazon.smithy.cli.commands;

import java.util.List;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.Colors;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.Parser;
import software.amazon.smithy.cli.SmithyCli;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

@SmithyInternalApi
public final class ValidateCommand implements Command {
    @Override
    public String getName() {
        return "validate";
    }

    @Override
    public String getSummary() {
        return "Validates Smithy models";
    }

    @Override
    public Parser getParser() {
        return Parser.builder()
                .option(SmithyCli.ALLOW_UNKNOWN_TRAITS, "Ignores unknown traits when validating models")
                .option(SmithyCli.DISCOVER, "-d", "Enables model discovery, merging in models found inside of jars")
                .parameter(SmithyCli.DISCOVER_CLASSPATH, "Enables model discovery using a custom classpath for models")
                .positional("<MODELS>", "Path to Smithy models or directories")
                .build();
    }

    @Override
    public void execute(Arguments arguments, ClassLoader classLoader) {
        List<String> models = arguments.positionalArguments();
        Colors.BRIGHT_WHITE.out(String.format("Validating Smithy model sources: %s", models));
        CommandUtils.buildModel(arguments, classLoader, SetUtils.of());
        Colors.BRIGHT_BOLD_GREEN.out("Smithy validation complete");
    }
}
