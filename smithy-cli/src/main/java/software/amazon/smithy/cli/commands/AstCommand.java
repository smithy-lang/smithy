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

import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.Cli;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.Parser;
import software.amazon.smithy.cli.SmithyCli;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

@SmithyInternalApi
public final class AstCommand implements Command {
    @Override
    public String getName() {
        return "ast";
    }

    @Override
    public String getSummary() {
        return "Reads Smithy models in and writes out a single JSON AST model";
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
        // Don't write the summary to STDOUT, but do write errors to STDERR.
        Model model = CommandUtils.buildModel(arguments, classLoader, SetUtils.of(Validator.Feature.QUIET));
        ModelSerializer serializer = ModelSerializer.builder().build();
        Cli.stdout(Node.prettyPrintJson(serializer.serialize(model)));
    }
}
