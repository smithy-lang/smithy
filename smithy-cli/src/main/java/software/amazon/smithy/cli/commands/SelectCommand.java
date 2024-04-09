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
import java.util.TreeSet;
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
import software.amazon.smithy.cli.Command;
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

final class SelectCommand implements Command {

    private static final Logger LOGGER = Logger.getLogger(SelectCommand.class.getName());
    private final String parentCommandName;
    private final DependencyResolver.Factory dependencyResolverFactory;

    SelectCommand(String parentCommandName, DependencyResolver.Factory dependencyResolverFactory) {
        this.parentCommandName = parentCommandName;;
        this.dependencyResolverFactory = dependencyResolverFactory;
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
    public int execute(Arguments arguments, Env env) {
        arguments.addReceiver(new ConfigOptions());
        arguments.addReceiver(new BuildOptions());
        arguments.addReceiver(new Options());

        CommandAction action = HelpActionWrapper.fromCommand(
            this,
            parentCommandName,
            this::getDocumentation,
            new ClasspathAction(dependencyResolverFactory, this::runWithClassLoader)
        );

        return action.apply(arguments, env);
    }

    private String getDocumentation(ColorFormatter colors) {
        return "By default, each matching shape ID is printed to stdout on a new line. Pass --show or --show-traits "
               + "to get JSON array output.";
    }

    private static final class Options implements ArgumentReceiver {
        private Selector selector;
        private final List<ShapeId> showTraits = new ArrayList<>();
        private final Set<Show> show = new TreeSet<>();

        private enum Show {
            TYPE("type") {
                @Override
                protected void inject(Selector.ShapeMatch match, ObjectNode.Builder builder) {
                    builder.withMember("type", match.getShape().getType().toString());
                }
            },

            FILE("file") {
                @Override
                protected void inject(Selector.ShapeMatch match, ObjectNode.Builder builder) {
                    SourceLocation source = match.getShape().getSourceLocation();
                    // Only shapes with a real source location add a file.
                    if (!source.getFilename().equals(SourceLocation.NONE.getFilename())) {
                        builder.withMember("file", source.getFilename()
                                                   + ':' + source.getLine()
                                                   + ':' + source.getColumn());
                    }
                }
            },

            VARS("vars") {
                @Override
                protected void inject(Selector.ShapeMatch match, ObjectNode.Builder builder) {
                    if (!match.isEmpty()) {
                        ObjectNode.Builder varBuilder = Node.objectNodeBuilder();
                        for (Map.Entry<String, Set<Shape>> varEntry : match.entrySet()) {
                            varBuilder.withMember(
                                varEntry.getKey(),
                                sortShapeIds(varEntry.getValue()).map(Node::from).collect(ArrayNode.collect())
                            );
                        }
                        ObjectNode collectedVars = varBuilder.build();
                        builder.withMember("vars", collectedVars);
                    }
                }
            };

            private final String value;

            Show(String value) {
                this.value = value;
            }

            protected abstract void inject(Selector.ShapeMatch match, ObjectNode.Builder builder);

            private static Show from(String value) {
                for (Show variant : values()) {
                    if (variant.value.equals(value)) {
                        return variant;
                    }
                }
                throw new CliError("Invalid value given to --show: `" + value + "`");
            }
        }

        @Override
        public boolean testOption(String name) {
            switch (name) {
                case "--vars":
                case "--show-vars":
                    return deprecatedVars(name);
                default:
                    return false;
            }
        }

        private boolean deprecatedVars(String name) {
            LOGGER.warning(name + " is deprecated. Use `--show vars` instead.");
            show.add(Show.VARS);
            return true;
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
                case "--show":
                    return value -> {
                        String[] parts = value.split("\\s*,\\s*");
                        for (String part : parts) {
                            show.add(Show.from(part));
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
            printer.param("--show", null, "DATA",
                          "Displays additional top-level members in each match and forces JSON output. This parameter "
                          + "accepts a comma-separated list of values, including 'type', 'file', and 'vars'. 'type' "
                          + "adds a string member containing the shape type of each match. 'file' adds a string "
                          + "member containing the absolute path to where the shape is defined followed by the line "
                          + "number then column (e.g., '/path/example.smithy:10:1'). 'vars' adds an object containing "
                          + "the variables that were captured when a shape was matched.");
            printer.param("--show-traits", null, "TRAITS",
                          "Returns JSON output that includes the values of specific traits applied to matched shapes, "
                          + "stored in a 'traits' property. Provide a comma-separated list of trait shape IDs. "
                          + "Prelude traits may omit a namespace (e.g., 'required' or 'smithy.api#required').");
        }

        public Selector selector() {
            if (selector == null) {
                selector = Selector.parse(IoUtils.toUtf8String(System.in));
            }
            return selector;
        }
    }

    private int runWithClassLoader(SmithyBuildConfig config, Arguments arguments, Env env) {
        Model model = new ModelBuilder()
                .config(config)
                .arguments(arguments)
                .env(env)
                .models(arguments.getPositional())
                .validationPrinter(env.stderr())
                .validationMode(Validator.Mode.QUIET_CORE_ONLY)
                .defaultSeverity(Severity.DANGER)
                .build();

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

                    for (Options.Show showData : options.show) {
                        showData.inject(match, builder);
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
            return options.showTraits.isEmpty() && options.show.isEmpty() ? SHAPE_ID_LINES : JSON;
        }
    }

    private static Stream<String> sortShapeIds(Collection<Shape> shapes) {
        return shapes.stream().map(Shape::getId).map(ShapeId::toString).sorted();
    }
}
