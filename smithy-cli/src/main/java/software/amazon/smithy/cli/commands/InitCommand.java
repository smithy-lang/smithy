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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;
import software.amazon.smithy.cli.ArgumentReceiver;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.ColorBuffer;
import software.amazon.smithy.cli.ColorTheme;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.HelpPrinter;
import software.amazon.smithy.cli.StandardOptions;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.ListUtils;

final class InitCommand implements Command {
    private static final String SMITHY_TEMPLATE_JSON = "smithy-templates.json";
    private static final String DEFAULT_REPOSITORY_URL = "https://github.com/smithy-lang/smithy-examples.git";
    private static final String DEFAULT_TEMPLATE_NAME = "quickstart-cli";
    private static final String DOCUMENTATION = "documentation";

    private static final String NAME = "name";
    private static final String TEMPLATES = "templates";
    private static final String PATH = "path";
    private static final String INCLUDED = "include";

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
        CommandAction action = HelpActionWrapper.fromCommand(this, parentCommandName, c -> {
            ColorBuffer buffer = ColorBuffer.of(c, new StringBuilder());
            buffer.println("Examples:");
            buffer.println("   smithy init --list", ColorTheme.LITERAL);
            buffer.println("   smithy init -o /tmp/quickstart-gradle -t quickstart-gradle", ColorTheme.LITERAL);
            return buffer.toString();
        }, this::run);
        return action.apply(arguments, env);
    }

    private int run(Arguments arguments, Env env) {
        Options options = arguments.getReceiver(Options.class);
        StandardOptions standardOptions = arguments.getReceiver(StandardOptions.class);

        try {
            final Path root = Paths.get(".");
            final Path temp = Files.createTempDirectory("temp");

            loadSmithyTemplateJsonFile(options.repositoryUrl, root, temp);

            final ObjectNode smithyTemplatesNode = getSmithyTemplatesNode(temp);

            if (options.listTemplates) {
                this.listTemplates(smithyTemplatesNode, env);
            } else {
                this.cloneTemplate(temp, smithyTemplatesNode, options, standardOptions, env);
            }
        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw new RuntimeException(e);
        }

        return 0;
    }

    private void listTemplates(ObjectNode smithyTemplatesNode, Env env) throws IOException {
        try (ColorBuffer buffer = ColorBuffer.of(env.colors(), env.stderr())) {
            buffer.println(getTemplateList(smithyTemplatesNode, env));
        }
    }

    private String getTemplateList(ObjectNode smithyTemplatesNode, Env env) {
        int maxTemplateLength = 0;
        int maxDocumentationLength = 0;
        Map<String, String> templates = new TreeMap<>();

        for (Map.Entry<StringNode, Node> entry : getTemplatesNode(smithyTemplatesNode).getMembers().entrySet()) {
            String template = entry.getKey().getValue();
            String documentation = entry.getValue()
                .expectObjectNode()
                .expectMember(DOCUMENTATION, String.format(
                        "Missing expected member `%s` from `%s` object", DOCUMENTATION, template))
                .expectStringNode()
                .getValue();

            templates.put(template, documentation);

            maxTemplateLength = Math.max(maxTemplateLength, template.length());
            maxDocumentationLength = Math.max(maxDocumentationLength, documentation.length());
        }

        final String space = "   ";

        ColorBuffer builder = ColorBuffer.of(env.colors(), new StringBuilder())
                .print(pad(NAME.toUpperCase(Locale.US), maxTemplateLength), ColorTheme.LITERAL)
                .print(space)
                .print(DOCUMENTATION.toUpperCase(Locale.US), ColorTheme.LITERAL)
                .println()
                .print(pad("", maxTemplateLength).replace(' ', '-'), ColorTheme.MUTED)
                .print(space)
                .print(pad("", maxDocumentationLength).replace(' ', '-'), ColorTheme.MUTED)
                .println();

        for (Map.Entry<String, String> entry : templates.entrySet()) {
            String template = entry.getKey();
            String doc = entry.getValue();
            builder.print(pad(template, maxTemplateLength))
                    .print(space)
                    .print(pad(doc, maxDocumentationLength))
                    .println();
        }

        return builder.toString();
    }

    private void cloneTemplate(Path temp, ObjectNode smithyTemplatesNode, Options options,
                               StandardOptions standardOptions, Env env)
            throws IOException, InterruptedException, URISyntaxException {

        String template = options.template;
        String directory = options.directory;

        if (template == null || template.isEmpty()) {
            throw new IllegalArgumentException("Please specify a template name using `--template` or `-t`");
        }

        // Use templateName if directory is not specified
        if (directory == null) {
            directory = template;
        }
        final Path dest = Paths.get(directory);
        if (Files.exists(dest)) {
            throw new CliError("Output directory `" + directory + "` already exists.");
        }

        ObjectNode templatesNode = getTemplatesNode(smithyTemplatesNode);
        if (!templatesNode.containsMember(template)) {
            throw new IllegalArgumentException(String.format(
                    "Invalid template `%s`. `%s` provides the following templates:%n%n%s",
                    template, getTemplatesName(smithyTemplatesNode), getTemplateList(smithyTemplatesNode, env)));
        }

        ObjectNode templateNode = templatesNode.expectObjectMember(template).expectObjectNode();

        final String templatePath = getTemplatePath(templateNode, template);
        List<String> includedFiles = getIncludedFiles(templateNode);

        try (ProgressTracker t = new ProgressTracker(env,
                ProgressStyle.dots("cloning template", "template cloned"),
                standardOptions.quiet()
        )) {
            // Specify the subdirectory to download
            exec(ListUtils.of("git", "sparse-checkout", "set", "--no-cone", templatePath), temp);
            // add any additional files that should be included
            for (String includedFile : includedFiles) {
                exec(ListUtils.of("git", "sparse-checkout", "add", "--no-cone", includedFile), temp);
            }
            exec(ListUtils.of("git", "checkout"), temp);
        }



        IoUtils.copyDir(Paths.get(temp.toString(), templatePath), dest);
        copyIncludedFiles(temp.toString(), dest.toString(), includedFiles, template, env);

        if (!standardOptions.quiet()) {
            try (ColorBuffer buffer = ColorBuffer.of(env.colors(), env.stderr())) {
                buffer.println(String.format("Smithy project created in directory: %s", directory), ColorTheme.SUCCESS);
            }
        }
    }

    private static void loadSmithyTemplateJsonFile(String repositoryUrl, Path root, Path temp) {
        exec(ListUtils.of("git", "clone", "--filter=blob:none", "--no-checkout", "--depth", "1", "--sparse",
                repositoryUrl, temp.toString()), root);

        exec(ListUtils.of("git", "sparse-checkout", "set", "--no-cone", SMITHY_TEMPLATE_JSON), temp);

        exec(ListUtils.of("git", "checkout"), temp);
    }

    private static ObjectNode getSmithyTemplatesNode(Path jsonFilePath) {
        return readJsonFileAsNode(Paths.get(jsonFilePath.toString(), SMITHY_TEMPLATE_JSON)).expectObjectNode();
    }

    private static ObjectNode getTemplatesNode(ObjectNode smithyTemplatesNode) {
        return smithyTemplatesNode
                .expectMember(TEMPLATES, String.format(
                        "Missing expected member `%s` from %s", TEMPLATES, SMITHY_TEMPLATE_JSON))
                .expectObjectNode();
    }

    private static String getTemplatesName(ObjectNode smithyTemplatesNode) {
        return smithyTemplatesNode
                .expectMember(NAME, String.format(
                        "Missing expected member `%s` from %s", NAME, SMITHY_TEMPLATE_JSON))
                .expectStringNode()
                .getValue();
    }

    private static String getTemplatePath(ObjectNode templateNode, String templateName) {
        return templateNode
                .expectMember(PATH, String.format("Missing expected member `%s` from `%s` object", PATH, templateName))
                .expectStringNode()
                .getValue();
    }

    private static List<String> getIncludedFiles(ObjectNode templateNode) {
        List<String> includedPaths = new ArrayList<>();
        templateNode.getArrayMember(INCLUDED, StringNode::getValue, includedPaths::addAll);
        return includedPaths;
    }

    private static void copyIncludedFiles(String temp, String dest, List<String> includedFiles,
                                          String templateName, Env env) throws IOException {
        for (String included : includedFiles) {
            final Path includedPath = Paths.get(temp, included);
            if (!Files.exists(includedPath)) {
                try (ColorBuffer buffer = ColorBuffer.of(env.colors(), env.stderr())) {
                    buffer.println(String.format(
                            "File or directory %s is marked for inclusion in template %s but was not found",
                            included, templateName), ColorTheme.WARNING);
                }
            }

            Path target = Paths.get(dest, Objects.requireNonNull(includedPath.getFileName()).toString());
            if (Files.isDirectory(includedPath)) {
                IoUtils.copyDir(includedPath, target);
            } else if (Files.isRegularFile(includedPath)) {
                Files.copy(includedPath, target);
            }
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

    private static Node readJsonFileAsNode(Path jsonFilePath) {
        return Node.parse(IoUtils.readUtf8File(jsonFilePath));
    }

    private static String pad(String s, int n) {
        return String.format("%-" + n + "s", s);
    }

    private static final class Options implements ArgumentReceiver {
        private String template = DEFAULT_TEMPLATE_NAME;
        private String directory;
        private Boolean listTemplates = false;
        private String repositoryUrl = DEFAULT_REPOSITORY_URL;

        @Override
        public boolean testOption(String name) {
            switch (name) {
                case "--list":
                case "-l":
                    listTemplates = true;
                    return true;
                default:
                    return false;
            }
        }

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
            printer.param("--list", "-l", null,
                    "List available templates");
        }
    }
}
