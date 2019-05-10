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

import java.util.List;
import java.util.logging.Logger;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.Colors;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.Parser;
import software.amazon.smithy.cli.SmithyCli;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.validation.ValidatedResult;

public final class ValidateCommand implements Command {
    private static final Logger LOGGER = Logger.getLogger(ValidateCommand.class.getName());

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
                .option("--allow-unknown-traits", "Ignores unknown traits when validating models")
                .option("--discover", "-d", "Enables model discovery, merging in models found inside of jars")
                .parameter("--format", "-f", "Format to use for the out. Can currently only be set to 'text'")
                .positional("<MODELS>", "Path to Smithy models or directories")
                .build();
    }

    @Override
    public void execute(Arguments arguments, ClassLoader classLoader) {
        String format = arguments.parameter("--format", "text");
        if (!format.equals("text")) {
            throw new CliError("Invalid --format value `" + format + "`");
        }

        List<String> models = arguments.positionalArguments();
        LOGGER.info(String.format("Validating Smithy models: %s", String.join(" ", models)));

        ModelAssembler assembler = Model.assembler(classLoader);

        if (arguments.has(SmithyCli.DISCOVER)) {
            LOGGER.fine("Enabling model discovery");
            assembler.discoverModels(classLoader);
        }

        if (arguments.has("--ignore-unknown-traits")) {
            LOGGER.fine("Ignoring unknown traits");
            assembler.putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true);
        }

        models.forEach(assembler::addImport);
        ValidatedResult<Model> modelResult = assembler.assemble();
        Validator.validate(modelResult);
        Colors.out(Colors.GREEN, "Smithy validation complete");
    }
}
