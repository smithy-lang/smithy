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

import java.util.logging.Logger;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.dependencies.DependencyResolver;

final class ValidateCommand implements Command {

    private static final Logger LOGGER = Logger.getLogger(ValidateCommand.class.getName());
    private final String parentCommandName;
    private final DependencyResolver.Factory dependencyResolverFactory;

    ValidateCommand(String parentCommandName, DependencyResolver.Factory dependencyResolverFactory) {
        this.parentCommandName = parentCommandName;
        this.dependencyResolverFactory = dependencyResolverFactory;
    }

    @Override
    public String getName() {
        return "validate";
    }

    @Override
    public String getSummary() {
        return "Validates Smithy models.";
    }

    @Override
    public int execute(Arguments arguments, Env env) {
        arguments.addReceiver(new ConfigOptions());
        arguments.addReceiver(new DiscoveryOptions());
        arguments.addReceiver(new ValidatorOptions());
        arguments.addReceiver(new BuildOptions());
        arguments.addReceiver(new ValidationEventFormatOptions());

        CommandAction action = HelpActionWrapper.fromCommand(
            this,
            parentCommandName,
            new ClasspathAction(dependencyResolverFactory, this::runWithClassLoader)
        );

        return action.apply(arguments, env);
    }

    private int runWithClassLoader(SmithyBuildConfig config, Arguments arguments, Env env) {
        new ModelBuilder()
                .config(config)
                .arguments(arguments)
                .env(env)
                .models(arguments.getPositional())
                .validationPrinter(env.stdout())
                .build();
        LOGGER.info("Smithy validation complete");
        return 0;
    }
}
