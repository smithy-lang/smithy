/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.function.Consumer;
import java.util.stream.Stream;
import software.amazon.smithy.cli.ArgumentReceiver;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.CliPrinter;
import software.amazon.smithy.cli.StandardOptions;
import software.amazon.smithy.cli.Style;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

@SmithyInternalApi
public final class SelectCommand extends CommandWithHelp {
    @Override
    public String getName() {
        return "select";
    }

    @Override
    public String getSummary() {
        return "Queries a model using a selector";
    }

    @Override
    public void printHelp(CliPrinter printer) {
        printer.println("Usage: smithy validate [-h | --help] [--allow-unknown-traits] [-d | --discover]");
        printer.println("                       [--discover-classpath JAVA_CLASSPATH]");
        printer.println("                       [--severity SEVERITY] [--debug] [--quiet] [--stacktrace]");
        printer.println("                       [--no-color] [--force-color] [--logging LOG_LEVEL]");
        printer.println("                       <MODELS>...");
        printer.println("");
        printer.println(getSummary());
        printer.println("");
        StandardOptions.printHelp(printer);
        BuildOptions.printHelp(printer);
        printer.println(printer.style("    --selector <SELECTOR>", Style.YELLOW));
        printer.println("        The Smithy selector to execute. Reads from STDIN when not provided.");
        printer.println(printer.style("    --vars", Style.YELLOW));
        printer.println("        Include the variables that were captured when the shape was matched.");
        printer.println("        The output of the command is JSON when --vars is passed.");
        printer.println(printer.style("    <MODELS>...", Style.YELLOW));
        printer.println("        Path to Smithy models or directories to load.");
        printer.println("");
        printer.println("This command prints the shapes in a model that match a selector. The ");
        printer.println("selector can be passed in using --selector or through stdin.");
        printer.println("");
        printer.println("By default, each matching shape ID is printed to stdout on a new line. ");
        printer.println("Pass --vars to print out a JSON array that contains a 'shape' and 'vars' ");
        printer.println("property, where the 'vars' property is a map of each variable that was ");
        printer.println("captured when the shape was matched.");
    }

    private static final class Options implements ArgumentReceiver {
        private boolean vars;
        private Selector selector;

        @Override
        public boolean testOption(String name) {
            if (name.equals("--vars")) {
                vars = true;
                return true;
            }
            return false;
        }

        @Override
        public Consumer<String> testParameter(String name) {
            if (name.equals("--selector")) {
                return value -> selector = Selector.parse(value);
            }
            return null;
        }

        public boolean vars() {
            return vars;
        }

        public Selector selector() {
            if (selector == null) {
                selector = Selector.parse(IoUtils.toUtf8String(System.in));
            }
            return selector;
        }
    }

    @Override
    protected List<String> parseArguments(Arguments arguments, Env env) {
        arguments.addReceiver(new BuildOptions());
        arguments.addReceiver(new Options());
        return arguments.finishParsing();
    }

    @Override
    protected int run(Arguments arguments, Env env, List<String> models) {
        CliPrinter stdout = env.stdout();
        Options options = arguments.getReceiver(Options.class);

        // Don't write the summary, but do write danger/errors to STDERR.
        Model model = CommandUtils.buildModel(arguments, models, env, env.stderr(), true);
        Selector selector = options.selector();

        if (!options.vars()) {
            sortShapeIds(selector.select(model)).forEach(stdout::println);
        } else {
            // Show the JSON output for writing with --vars.
            List<Node> result = new ArrayList<>();
            selector.consumeMatches(model, match -> {
                result.add(Node.objectNodeBuilder()
                        .withMember("shape", Node.from(match.getShape().getId().toString()))
                        .withMember("vars", collectVars(match))
                        .build());
            });
            stdout.println(Node.prettyPrintJson(new ArrayNode(result, SourceLocation.NONE)));
        }

        return 0;
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
