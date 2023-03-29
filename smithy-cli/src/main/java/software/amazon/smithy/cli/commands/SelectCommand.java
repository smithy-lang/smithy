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
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.cli.ArgumentReceiver;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.CliPrinter;
import software.amazon.smithy.cli.ColorFormatter;
import software.amazon.smithy.cli.HelpPrinter;
import software.amazon.smithy.cli.dependencies.DependencyResolver;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.utils.IoUtils;

final class SelectCommand extends ClasspathCommand {

    private static final Logger LOGGER = Logger.getLogger(SelectCommand.class.getName());

    SelectCommand(String parentCommandName, DependencyResolver.Factory dependencyResolverFactory) {
        super(parentCommandName, dependencyResolverFactory);
    }

    @Override
    public String getName() {
        return "select";
    }

    @Override
    public String getSummary() {
        return "Queries a model using a selector.";
    }

    @Override
    public String getDocumentation(ColorFormatter colors) {
        return "By default, each matching shape ID is printed to stdout on a new line. Pass --vars to print out a "
               + "JSON array that contains a 'shape' and 'vars' property, where the 'vars' property is a map of "
               + "each variable that was captured when the shape was matched.";
    }

    private static final class Options implements ArgumentReceiver {
        private boolean vars;
        private Selector selector;
        private final List<ShapeId> showTraits = new ArrayList<>();

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
            switch (name) {
                case "--selector":
                    return value -> selector = Selector.parse(value);
                case "--show-traits":
                    return value -> {
                        for (String trait : value.split(",", -1)) {
                            trait = trait.trim();
                            if (trait.isEmpty()) {
                                throw new CliError("--show-traits cannot contain empty trait values");
                            }
                            showTraits.add(ShapeId.fromOptionalNamespace(Prelude.NAMESPACE, trait));
                        }
                        if (showTraits.isEmpty()) {
                            throw new CliError("--show-traits must contain traits");
                        }
                    };
                default:
                    return null;
            }
        }

        @Override
        public void registerHelp(HelpPrinter printer) {
            printer.param("--selector", null, "SELECTOR",
                          "The Smithy selector to execute. Reads from STDIN when not provided.");
            printer.param("--show-traits", null, "TRAITS",
                          "Returns JSON output that includes the values of specific traits applied to matched shapes, "
                          + "stored in a 'traits' property. Provide a comma-separated list of trait shape IDs. "
                          + "Prelude traits may omit a namespace (e.g., 'required' or 'smithy.api#required').");
            printer.option("--vars", null, "Returns JSON output that includes the variables that were captured when "
                                           + "a shape was matched, stored in a 'vars' property.");
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
    protected void addAdditionalArgumentReceivers(List<ArgumentReceiver> receivers) {
        receivers.add(new Options());
    }

    @Override
    int runWithClassLoader(SmithyBuildConfig config, Arguments arguments, Env env, List<String> models) {
        // Don't write the summary, but do write danger/errors to STDERR.
        ValidationFlag flag = ValidationFlag.DISABLE;

        // Force the severity to DANGER or ERROR to only see events if they'll fail the command.
        BuildOptions buildOptions = arguments.getReceiver(BuildOptions.class);
        buildOptions.severity(Severity.DANGER);

        Model model = CommandUtils.buildModel(arguments, models, env, env.stderr(), flag, config);

        // Flush outputs to ensure there is no interleaving with selection output.
        env.stderr().flush();
        env.stdout().flush();

        Options options = arguments.getReceiver(Options.class);
        Selector selector = options.selector();
        OutputFormat outputFormat = OutputFormat.determineFormat(options);

        long startTime = System.nanoTime();
        outputFormat.dumpResults(selector, model, options, env.stdout());
        long endTime = System.nanoTime();

        if (LOGGER.isLoggable(Level.FINE)) {
            // Ensure that the log statement isn't interlaced in the JSON output.
            env.stdout().flush();
            LOGGER.fine("Select time: " + ((endTime - startTime) / 1000000) + "ms");
        }

        return 0;
    }

    enum OutputFormat {
        SHAPE_ID_LINES {
            @Override
            void dumpResults(Selector selector, Model model, Options options, CliPrinter stdout) {
                sortShapeIds(selector.select(model)).forEach(stdout::println);
            }
        },
        JSON {
            @Override
            void dumpResults(Selector selector, Model model, Options options, CliPrinter stdout) {
                List<Node> result = selector.matches(model).map(match -> {
                    ObjectNode.Builder builder = Node.objectNodeBuilder()
                            .withMember("shape", Node.from(match.getShape().getId().toString()));

                    if (!match.isEmpty()) {
                        builder.withMember("vars", collectVars(match));
                    }

                    if (!options.showTraits.isEmpty()) {
                        Map<StringNode, Node> values = new TreeMap<>();
                        for (ShapeId trait : options.showTraits) {
                            match.getShape()
                                    .findTrait(trait)
                                    .ifPresent(found -> values.put(Node.from(trait.toString()), found.toNode()));
                        }
                        builder.withMember("traits", Node.objectNode(values));
                    }

                    return builder.build();
                }).collect(Collectors.toList());

                stdout.println(Node.prettyPrintJson(new ArrayNode(result, SourceLocation.NONE)));
            }
        };

        abstract void dumpResults(Selector selector, Model model, Options options, CliPrinter stdout);

        static OutputFormat determineFormat(Options options) {
            // If --var isn't provided and --show-traits is empty, then use the SHAPE_ID_LINES output.
            return !options.vars() && options.showTraits.isEmpty() ? SHAPE_ID_LINES : JSON;
        }

        private static Stream<String> sortShapeIds(Collection<Shape> shapes) {
            return shapes.stream().map(Shape::getId).map(ShapeId::toString).sorted();
        }

        private static ObjectNode collectVars(Map<String, Set<Shape>> vars) {
            ObjectNode.Builder varBuilder = Node.objectNodeBuilder();
            for (Map.Entry<String, Set<Shape>> varEntry : vars.entrySet()) {
                ArrayNode value = sortShapeIds(varEntry.getValue()).map(Node::from).collect(ArrayNode.collect());
                varBuilder.withMember(varEntry.getKey(), value);
            }
            return varBuilder.build();
        }
    }
}
