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

import java.util.List;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.dependencies.DependencyResolver;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.validation.Severity;

final class AstCommand extends ClasspathCommand {

    AstCommand(String parentCommandName, DependencyResolver.Factory dependencyResolverFactory) {
        super(parentCommandName, dependencyResolverFactory);
    }

    @Override
    public String getName() {
        return "ast";
    }

    @Override
    public String getSummary() {
        return "Reads Smithy models in and writes out a single JSON AST model.";
    }

    @Override
    protected void configureArgumentReceivers(Arguments arguments) {
        super.configureArgumentReceivers(arguments);

        // The AST command isn't meant for validation. Events are only shown when they fail the command.
        arguments.removeReceiver(SeverityOption.class);
    }

    @Override
    int runWithClassLoader(SmithyBuildConfig config, Arguments arguments, Env env, List<String> models) {
        Model model = new ModelBuilder()
                .config(config)
                .arguments(arguments)
                .env(env)
                .models(models)
                .validationPrinter(env.stderr())
                .validationMode(Validator.Mode.QUIET)
                .severity(Severity.DANGER)
                .build();

        ModelSerializer serializer = ModelSerializer.builder().build();
        env.stdout().println(Node.prettyPrintJson(serializer.serialize(model)));
        return 0;
    }
}
