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

import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.StandardOptions;
import software.amazon.smithy.cli.dependencies.DependencyResolver;
import software.amazon.smithy.cli.dependencies.ResolvedArtifact;
import software.amazon.smithy.utils.IoUtils;

final class FormatCommand implements Command {

    private final String parentCommandName;
    private final DependencyResolver.Factory dependencyResolverFactory;

    FormatCommand(String parentCommandName, DependencyResolver.Factory dependencyResolverFactory) {
        this.parentCommandName = parentCommandName;
        this.dependencyResolverFactory = dependencyResolverFactory;
    }

    @Override
    public String getName() {
        return "format";
    }

    @Override
    public String getSummary() {
        return "Formats Smithy IDL models in place.";
    }

    @Override
    public int execute(Arguments arguments, Env env) {
        arguments.addReceiver(new ConfigOptions());

        CommandAction action = HelpActionWrapper.fromCommand(
                this, parentCommandName, this::runFormatter);

        return action.apply(arguments, env);
    }

    private int runFormatter(Arguments arguments, Env env) {
        StandardOptions standardOptions = arguments.getReceiver(StandardOptions.class);
        ConfigOptions configOptions = arguments.getReceiver(ConfigOptions.class);
        SmithyBuildConfig config = configOptions.createSmithyBuildConfig();
        DependencyResolver resolver = dependencyResolverFactory.create(config, env);

        if (!standardOptions.quiet()) {
            env.stderr().append("Checking for Smithy formatter...").flush();
        }

        DependencyHelper.addConfiguredMavenRepos(config, resolver);
        resolver.addDependency("software.amazon.smithy:smithy-language-server:LATEST");
        List<ResolvedArtifact> resolvedArtifacts = resolver.resolve();
        List<Path> dependencies = new ArrayList<>(resolvedArtifacts.size());
        for (ResolvedArtifact artifact : resolvedArtifacts) {
            dependencies.add(artifact.getPath());
        }

        if (env.colors().isColorEnabled()) {
            env.stderr().append("\r").append(StyleHelper.CLEAR_LINE_ESCAPE).flush();
        }

        List<String> positional = arguments.getPositional();
        if (positional.isEmpty()) {
            throw new CliError("Missing required positional argument pointing to a file to format");
        } else if (positional.size() > 1) {
            throw new CliError("Can only format a single model file");
        }

        String file = positional.get(0);
        String contents = IoUtils.readUtf8File(file);

        new IsolatedRunnable(dependencies, getClass().getClassLoader().getParent(), loader -> {
            try {
                Class<?> main = loader.loadClass("smithyfmt.Formatter");
                Method method = main.getMethod("format", String.class);
                Object response = method.invoke(null, contents);
                Method isSuccess = response.getClass().getMethod("isSuccess");
                if ((boolean) isSuccess.invoke(response)) {
                    Method valueMethod = response.getClass().getMethod("getValue");
                    String formatted = (String) valueMethod.invoke(response);
                    FileWriter fileWriter = new FileWriter(file);
                    PrintWriter printWriter = new PrintWriter(fileWriter);
                    printWriter.print(formatted);
                    printWriter.close();
                } else {
                    Method errorMessage = response.getClass().getMethod("getError");
                    throw new CliError("Error formatting model: " + errorMessage.invoke(response), 1);
                }
            } catch (CliError e) {
                throw e;
            } catch (Exception e) {
                throw new CliError("Error formatting model", 1, e);
            }
        }).run();

        return 0;
    }
}
