/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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
                new ClasspathAction(dependencyResolverFactory, this::runWithClassLoader));

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
