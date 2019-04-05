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

import java.nio.file.Paths;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.Colors;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.Parser;
import software.amazon.smithy.cli.SmithyCli;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.LoaderUtils;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

public final class GenerateCommand implements Command {
    @Override
    public String getName() {
        return "generate";
    }

    @Override
    public String getSummary() {
        return "Generates a single Smithy build artifact by name";
    }

    @Override
    public String getHelp() {
        return String.format(
                "Examples:%n%n"
                + "  smithy generate --plugin model --output /tmp/example /path/to/models%n%n"
                + "  smithy generate --plugin MyPlugin --settings /path/to/settings.json --output "
                + "/tmp/example /path/to/models /path/to/more/models%n%n"
                + "  echo \"{\"foo\": \"baz\"} | smithy generate --plugin MyPlugin --settings - "
                + "--output /tmp/example /path/to/models"
        );
    }

    @Override
    public Parser getParser() {
        return Parser.builder()
                .parameter("--plugin", "-p", "The name of the plugin to execute.")
                .parameter("--settings", "-s", "Path to a JSON file that contains plugin settings. Use - for stdin.")
                .parameter("--output", "-o", "Where to write artifacts.")
                .option("--discover", "-d", "Enables model discovery, merging in models found inside of jars")
                .positional("<MODELS>", "Path to Smithy models or directories to generate from.")
                .build();
    }

    @Override
    public void execute(Arguments arguments) {
        var plugin = arguments.parameter("--plugin");
        var settings = arguments.parameter("--settings", null);
        var output = arguments.parameter("--output");
        var models = arguments.positionalArguments();

        if (models.isEmpty()) {
            throw new CliError("No models were provided as positional arguments");
        }

        ObjectNode settingsObject;
        if (settings == null) {
            System.err.println("No plugin settings specified");
            settingsObject = Node.objectNode();
        } else {
            String settingsFileContents;
            if (settings.equals("-")) {
                System.err.println("Loading plugin settings from STD_IN");
                settingsFileContents = LoaderUtils.readInputStream(System.in, "UTF-8");
            } else {
                System.err.println(String.format("Loading plugin settings file: %s", settings));
                settingsFileContents = LoaderUtils.readUtf8File(settings);
            }
            settingsObject = Node.parse(settingsFileContents)
                    .expectObjectNode("--settings must reference a JSON encoded object");
        }

        System.err.println(String.format("Generating '%s' for Smithy models: %s", plugin, String.join(" ", models)));

        var loader = SmithyCli.getConfiguredClassLoader();
        var assembler = Model.assembler(loader);

        if (arguments.has("--discover")) {
            System.err.println("Enabling model discovery");
            assembler.discoverModels(loader);
        }

        models.forEach(assembler::addImport);
        var model = assembler.assemble().unwrap();

        var buildPlugin = loadSmithyBuildPlugin(loader, plugin);
        var manifest = FileManifest.create(Paths.get(output));
        System.err.println(String.format("Output directory set to: %s", output));

        var context = PluginContext.builder()
                .model(model)
                .settings(settingsObject)
                .fileManifest(manifest)
                .build();

        buildPlugin.execute(context);
        Colors.out(Colors.BRIGHT_GREEN, String.format(
                "Smithy '%s' successfully generated the following artifacts: ", plugin));
        manifest.getFiles().stream().sorted().forEach(System.out::println);
    }

    private static SmithyBuildPlugin loadSmithyBuildPlugin(ClassLoader loader, String pluginName) {
        return SmithyBuildPlugin.createServiceFactory(loader).apply(pluginName).orElseThrow(() -> {
            return new RuntimeException(String.format(
                    "Unable to find SmithyBuildPlugin plugin named `%s`.%n%n"
                    + "Plugins are discovered by name using the Java Service Provider Interface.%n"
                    + "Implementations of %s are%n"
                    + "scanned to determine if they implement a plugin by name.%n"
                    + "Plugins can be added to the CLI by placing modular jars on%n"
                    + "the SMITHY_MODULE_PATH.",
                    pluginName, SmithyBuildPlugin.class.getName()));
        });
    }
}
