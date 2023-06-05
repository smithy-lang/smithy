/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import software.amazon.smithy.cli.ArgumentReceiver;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.ColorBuffer;
import software.amazon.smithy.cli.ColorTheme;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.HelpPrinter;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.ListUtils;

final class InitCommand implements Command {
    private static final String SMITHY_TEMPLATE_JSON = "smithy-templates.json";
    private static final String DEFAULT_REPOSITORY_URL = "https://github.com/smithy-lang/smithy-examples.git";

    private final String parentCommandName;

    InitCommand(String parentCommandName) {
        this.parentCommandName = parentCommandName;
    }

    @Override
    public String getName() {
        return "init";
    }

    @Override
    public String getSummary() {
        return "Initialize a smithy project using a template";
    }

    @Override
    public int execute(Arguments arguments, Env env) {
        arguments.addReceiver(new Options());
        CommandAction action = HelpActionWrapper.fromCommand(this, parentCommandName, this::run);
        return action.apply(arguments, env);
    }

    private int run(Arguments arguments, Env env) {
        Options options = arguments.getReceiver(Options.class);
        try {
            this.cloneTemplate(options.repositoryUrl, options.template, options.directory, env);
        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw new RuntimeException(e);
        }

        return 0;
    }

    private void cloneTemplate(String repositoryUrl, String template, String directory, Env env)
            throws IOException, InterruptedException, URISyntaxException {

        if (template == null || template.isEmpty()) {
            throw new IllegalArgumentException("Please specify a template using `--template` or `-t`");
        }

        final Path root = Paths.get(".");
        final Path temp = Files.createTempDirectory("temp");

        // Use templateName if directory is not specified
        if (directory == null) {
            directory = template;
        }

        // Check Git is installed
        exec(ListUtils.of("git", "clone", "--filter=blob:none", "--no-checkout", "--depth", "1", "--sparse",
            repositoryUrl, temp.toString()), root);

        // Download template json file
        exec(ListUtils.of("git", "sparse-checkout", "set", "--no-cone", SMITHY_TEMPLATE_JSON), temp);

        exec(ListUtils.of("git", "checkout"), temp);

        // Retrieve template path from smithy-templates.json
        String templatePath = readJsonFileAsNode(Paths.get(temp.toString(), SMITHY_TEMPLATE_JSON))
            .expectObjectNode()
            .expectMember("templates", String.format(
                    "Missing expected member `templates` from %s", SMITHY_TEMPLATE_JSON))
            .expectObjectNode()
            .expectMember(template, String.format("Missing expected member `%s` from `templates` object", template))
            .expectObjectNode()
            .expectMember("path", String.format("Missing expected member `path` from `%s` object", template))
            .expectStringNode()
            .getValue();

        // Specify the subdirectory to download
        exec(ListUtils.of("git", "sparse-checkout", "set", "--no-cone", templatePath), temp);

        exec(ListUtils.of("git", "checkout"), temp);

        IoUtils.copyDir(Paths.get(temp.toString(), templatePath), Paths.get(directory));

        try (ColorBuffer buffer = ColorBuffer.of(env.colors(), env.stderr())) {
            buffer.println(String.format("Smithy project created in directory: %s", directory), ColorTheme.SUCCESS);
        }
    }

    private static String exec(List<String> args, Path directory) {
        StringBuilder output = new StringBuilder();
        int code = IoUtils.runCommand(args, directory, output, Collections.emptyMap());
        if (code != 0) {
            String errorPrefix = "Unable to run `" + String.join(" ", args) + "`";
            throw new CliError(errorPrefix + ": " + output);
        }
        return output.toString();
    }

    private Node readJsonFileAsNode(Path jsonFilePath) {
        return Node.parse(IoUtils.readUtf8File(jsonFilePath));
    }

    private static final class Options implements ArgumentReceiver {
        private String template;
        private String directory;
        private String repositoryUrl = DEFAULT_REPOSITORY_URL;

        @Override
        public Consumer<String> testParameter(String name) {
            switch (name) {
                case "--template":
                case "-t":
                    return value -> template = value;
                case "--url":
                case "-u":
                    return value -> repositoryUrl = value;
                case "--output":
                case "-o":
                    return value -> directory = value;
                default:
                    return null;
            }
        }

        @Override
        public void registerHelp(HelpPrinter printer) {
            printer.param("--template", "-t", "quickstart-cli",
                    "Specify the template to be used in the Smithy project");
            printer.param("--url", null,
                    "https://github.com/smithy-lang/smithy-examples.git",
                    "Smithy templates repository url");
            printer.param("--output", "-o", "new-smithy-project",
                    "Smithy project directory");
        }
    }
}
