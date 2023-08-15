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
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
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
import software.amazon.smithy.utils.StringUtils;


final class InitCommand implements Command {
    private static final int LINE_LENGTH = 100;
    private static final String COLUMN_SEPARATOR = "  ";
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

        boolean isLocalRepo = isLocalRepo(options.repositoryUrl);
        Path templateRepoDirPath = getTemplateRepoDirPath(options.repositoryUrl, isLocalRepo);

        // If the cache directory does not exist, create it
        if (!Files.exists(templateRepoDirPath)) {
            try (ProgressTracker t = new ProgressTracker(env,
                    ProgressStyle.dots("cloning template repo", "template repo cloned"),
                    standardOptions.quiet()
            )) {
                Path templateCachePath = CliCache.getTemplateCache().get();
                String relativeTemplateDir = templateCachePath.relativize(templateRepoDirPath).toString();
                // Only clone the latest commit from HEAD. Do not include history
                exec(ListUtils.of("git", "clone", "--depth", "1", "--single-branch",
                        options.repositoryUrl, relativeTemplateDir), templateCachePath);
            }
        }

        validateTemplateDir(templateRepoDirPath, options.repositoryUrl);

        // Check for updates and update repo to latest if applicable
        if (!isLocalRepo) {
            // update remote
            exec(ListUtils.of("git", "fetch", "--depth", "1"), templateRepoDirPath);
            String response = exec(ListUtils.of("git", "rev-list", "origin..HEAD"), templateRepoDirPath);
            // If a change was detected, force the template repo to update
            if (!StringUtils.isEmpty(response)) {
                try (ProgressTracker t = new ProgressTracker(env,
                        ProgressStyle.dots("updating template cache", "template repo updated"),
                        standardOptions.quiet()
                )) {
                    exec(ListUtils.of("git", "reset", "--hard", "origin/main"), templateRepoDirPath);
                    exec(ListUtils.of("git", "clean", "-dfx"), templateRepoDirPath);
                }
            }
        }


        ObjectNode smithyTemplatesNode = readJsonFileAsNode(templateRepoDirPath.resolve(SMITHY_TEMPLATE_JSON))
                .expectObjectNode();
        if (options.listTemplates) {
            this.listTemplates(smithyTemplatesNode, env);
        } else {
            this.cloneTemplate(templateRepoDirPath, smithyTemplatesNode, options, standardOptions, env);
        }

        return 0;
    }

    private void validateTemplateDir(Path templateRepoDirPath, String templateUrl) {
        if (!Files.isDirectory(templateRepoDirPath)) {
            throw new CliError("Template repository " + templateRepoDirPath + " is not a directory");
        }
        Path templateJsonPath = templateRepoDirPath.resolve(SMITHY_TEMPLATE_JSON);
        if (!Files.exists(templateJsonPath) && Files.isRegularFile(templateJsonPath)) {
            throw new CliError("Template repository " + templateUrl
                    + " does not contain a valid `smithy-templates.json`.");
        }
    }

    private Path getTemplateRepoDirPath(String repoPath, boolean isLocalRepo) {
        if (isLocalRepo) {
            // Just use the local path if the git repo is local
            return Paths.get(repoPath);
        } else {
            return CliCache.getTemplateCache().get().resolve(getCacheDirFromUrl(repoPath));
        }
    }

    // Remove any trailing .git
    // Remove "/" and ".." and ":" characters so a directory can be created with no nesting
    private String getCacheDirFromUrl(final String repositoryUrl) {
        return repositoryUrl.replace(".git", "")
                .replace(":", "_")
                .replace("/", "_")
                .replace(".", "_");
    }

    private void listTemplates(ObjectNode smithyTemplatesNode, Env env) {
        try (ColorBuffer buffer = ColorBuffer.of(env.colors(), env.stdout())) {
            buffer.println(getTemplateList(smithyTemplatesNode, env));
        }
    }

    private boolean isLocalRepo(String repoPath) {
        try  {
            Path localPath = Paths.get(repoPath);
            return Files.exists(localPath);
        } catch (InvalidPathException exc) {
            return false;
        }

    }

    private String getTemplateList(ObjectNode smithyTemplatesNode, Env env) {
        int maxTemplateLength = 0;
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
        }
        int maxDocLength = LINE_LENGTH - maxTemplateLength - COLUMN_SEPARATOR.length();

        ColorBuffer builder = ColorBuffer.of(env.colors(), new StringBuilder());

        writeTemplateBorder(builder, maxTemplateLength, maxDocLength);
        builder.print(pad(NAME.toUpperCase(Locale.US), maxTemplateLength), ColorTheme.NOTE)
                .print(COLUMN_SEPARATOR)
                .print(DOCUMENTATION.toUpperCase(Locale.US), ColorTheme.NOTE)
                .println();
        writeTemplateBorder(builder, maxTemplateLength, maxDocLength);

        int offset = maxTemplateLength + COLUMN_SEPARATOR.length();

        for (Map.Entry<String, String> entry : templates.entrySet()) {
            String template = entry.getKey();
            String doc = entry.getValue();

            builder.print(pad(template, maxTemplateLength), ColorTheme.TEMPLATE_TITLE)
                    .print(COLUMN_SEPARATOR)
                    .print(wrapDocumentation(doc, maxDocLength, offset),
                            ColorTheme.MUTED)
                    .println();
        }

        return builder.toString();
    }


    private static void writeTemplateBorder(ColorBuffer writer, int maxNameLength, int maxDocLength) {
        writer.print(pad("", maxNameLength).replace(" ", "─"), ColorTheme.TEMPLATE_LIST_BORDER)
                .print(COLUMN_SEPARATOR)
                .print(pad("", maxDocLength).replace(" ", "─"), ColorTheme.TEMPLATE_LIST_BORDER)
                .println();
    }

    private static String wrapDocumentation(String doc, int maxLength, int offset) {
        return StringUtils.wrap(doc, maxLength, System.lineSeparator() + pad("", offset), false);
    }

    private void cloneTemplate(Path templateRepoDirPath, ObjectNode smithyTemplatesNode, Options options,
                               StandardOptions standardOptions, Env env) {

        String template = options.template;
        String directory = options.directory;

        if (template == null || template.isEmpty()) {
            throw new IllegalArgumentException("Please specify a template name using `--template` or `-t`");
        }

        // Use templateName if directory is not specified
        if (directory == null) {
            directory = template;
        }
        Path dest = Paths.get(directory);
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

        String templatePath = getTemplatePath(templateNode, template);
        List<String> includedFiles = getIncludedFiles(templateNode);

        Path stagingPath = createStagingRepo(templateRepoDirPath);

        // Specify the subdirectory to check out
        exec(ListUtils.of("git", "sparse-checkout", "set", "--no-cone", templatePath), stagingPath);
        // add any additional files that should be included
        for (String includedFile : includedFiles) {
            exec(ListUtils.of("git", "sparse-checkout", "add", "--no-cone", includedFile), stagingPath);
        }
        exec(ListUtils.of("git", "checkout"), stagingPath);

        if (!Files.exists(stagingPath.resolve(templatePath))) {
            throw new CliError(String.format("Template path `%s` for template \"%s\" is invalid.",
                    templatePath, template));
        }
        IoUtils.copyDir(Paths.get(stagingPath.toString(), templatePath), dest);
        copyIncludedFiles(stagingPath.toString(), dest.toString(), includedFiles, template, env);

        if (!standardOptions.quiet()) {
            try (ColorBuffer buffer = ColorBuffer.of(env.colors(), env.stdout())) {
                buffer.println(String.format("Smithy project created in directory: %s", directory), ColorTheme.SUCCESS);
            }
        }
    }

    private static Path createStagingRepo(Path repoPath) {
        Path temp;
        try {
            temp = Files.createTempDirectory("temp");
        } catch (IOException exc) {
            throw new CliError("Unable to create staging directory for template.");
        }
        exec(ListUtils.of("git", "clone", "--no-checkout", "--depth", "1", "--sparse",
                "file://" + repoPath.toString(), temp.toString()), Paths.get("."));

        return temp;
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
                                          String templateName, Env env) {
        for (String included : includedFiles) {
            Path includedPath = Paths.get(temp, included);
            if (!Files.exists(includedPath)) {
                throw new CliError(String.format(
                        "File or directory `%s` is marked for inclusion in template \"%s\", but was not found",
                        included, templateName));
            }

            Path target = Paths.get(dest, Objects.requireNonNull(includedPath.getFileName()).toString());
            if (Files.isDirectory(includedPath)) {
                IoUtils.copyDir(includedPath, target);
            } else if (Files.isRegularFile(includedPath)) {
                try {
                    Files.copy(includedPath, target);
                } catch (IOException e) {
                    throw new CliError("Unable to copy included file: " + includedPath
                            + "to destination directory: " + target);
                }
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
