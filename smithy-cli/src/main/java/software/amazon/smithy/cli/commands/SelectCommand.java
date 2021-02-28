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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.Cli;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.Parser;
import software.amazon.smithy.cli.SmithyCli;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

@SmithyInternalApi
public final class SelectCommand implements Command {
    @Override
    public String getName() {
        return "select";
    }

    @Override
    public String getSummary() {
        return "Queries a model using a selector";
    }

    @Override
    public String getHelp() {
        return "This command prints the shapes in a model that match a selector. The\n"
               + "selector can be passed in using --selector or through stdin.\n\n"
               + "By default, each matching shape ID is printed to stdout on a new line.\n"
               + "Pass --vars to print out a JSON array that contains a 'shape' and 'vars'\n"
               + "property, where the 'vars' property is a map of each variable that was\n"
               + "captured when the shape was matched.";
    }

    @Override
    public Parser getParser() {
        return Parser.builder()
                .parameter("--selector", "The Smithy selector to execute. Reads from STDIN when not provided.")
                .option("--vars", "Include the variables that were captured when the shape was matched. Uses JSON.")
                .option(SmithyCli.ALLOW_UNKNOWN_TRAITS, "Ignores unknown traits when validating models")
                .option(SmithyCli.DISCOVER, "-d", "Enables model discovery, merging in models found inside of jars")
                .parameter(SmithyCli.DISCOVER_CLASSPATH, "Enables model discovery using a custom classpath for models")
                .positional("<MODELS>", "Path to Smithy models or directories")
                .build();
    }

    @Override
    public void execute(Arguments arguments, ClassLoader classLoader) {
        // Get the selector from --selector or from STDIN/
        Selector selector = arguments.has("--selector")
                ? Selector.parse(arguments.parameter("--selector"))
                : Selector.parse(IoUtils.toUtf8String(System.in));

        // Don't write the summary to STDOUT, but do write errors to STDERR.
        Model model = CommandUtils.buildModel(arguments, classLoader, SetUtils.of(Validator.Feature.QUIET));

        if (!arguments.has("--vars")) {
            sortShapeIds(selector.select(model)).forEach(Cli::stdout);
        } else {
            // Show the JSON output for writing with --vars.
            List<Node> result = new ArrayList<>();
            selector.consumeMatches(model, match -> {
                result.add(Node.objectNodeBuilder()
                        .withMember("shape", Node.from(match.getShape().getId().toString()))
                        .withMember("vars", collectVars(match))
                        .build());
            });
            Cli.stdout(Node.prettyPrintJson(new ArrayNode(result, SourceLocation.NONE)));
        }
    }

    private Stream<String> sortShapeIds(Collection<Shape> shapes) {
        return shapes.stream().map(Shape::getId).map(ShapeId::toString).sorted();
    }

    private ObjectNode collectVars(Map<String, Set<Shape>> vars) {
        ObjectNode.Builder varBuilder = Node.objectNodeBuilder();
        for (Map.Entry<String, Set<Shape>> varEntry : vars.entrySet()) {
            ArrayNode value = sortShapeIds(varEntry.getValue()).map(Node::from).collect(ArrayNode.collect());
            varBuilder.withMember(varEntry.getKey(), value);
        }
        return varBuilder.build();
    }
}
