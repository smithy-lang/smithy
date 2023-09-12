/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.cli.commands;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import software.amazon.smithy.build.model.MavenConfig;
import software.amazon.smithy.build.model.MavenRepository;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.SmithyCli;
import software.amazon.smithy.cli.dependencies.DependencyResolver;
import software.amazon.smithy.cli.dependencies.FilterCliVersionResolver;
import software.amazon.smithy.cli.dependencies.ResolvedArtifact;


final class LockCommand implements Command {
    private static final Logger LOGGER = Logger.getLogger(LockCommand.class.getName());

    private final String parentCommandName;
    private final DependencyResolver.Factory dependencyResolverFactory;

    LockCommand(String parentCommandName, DependencyResolver.Factory dependencyResolverFactory) {
        this.parentCommandName = parentCommandName;
        this.dependencyResolverFactory = dependencyResolverFactory;
    }

    @Override
    public String getName() {
        return "lock";
    }

    @Override
    public String getSummary() {
        return "Resolves dependencies and generates a lockfile to pin to the resolved dependency versions.";
    }

    @Override
    public int execute(Arguments arguments, Env env) {
        arguments.addReceiver(new ConfigOptions());

        CommandAction action = HelpActionWrapper.fromCommand(this, parentCommandName, this::run);

        return action.apply(arguments, env);
    }

    private int run(Arguments arguments, Env env) {
        ConfigOptions configOptions = arguments.getReceiver(ConfigOptions.class);
        SmithyBuildConfig smithyBuildConfig = configOptions.createSmithyBuildConfig();

        DependencyResolver baseResolver = dependencyResolverFactory.create(smithyBuildConfig, env);
        DependencyResolver resolver = new FilterCliVersionResolver(SmithyCli.getVersion(), baseResolver);

        Set<MavenRepository> repositories = DependencyUtils.getConfiguredMavenRepos(smithyBuildConfig);
        Set<String> dependencies = smithyBuildConfig.getMaven()
                .map(MavenConfig::getDependencies)
                .orElse(Collections.emptySet());
        for (String dep: dependencies) {
            resolver.addDependency(dep);
        }
        for (MavenRepository repository: repositories) {
            resolver.addRepository(repository);
        }

        List<ResolvedArtifact> resolvedArtifacts = resolver.resolve();
        LOGGER.fine(() -> "Resolved artifacts with Maven: " + resolvedArtifacts);
        LockFile lock = LockFile.builder()
                .configHash(DependencyUtils.configHash(dependencies, repositories))
                .artifacts(resolvedArtifacts)
                .repositories(repositories)
                .build();
        LOGGER.fine(() -> "Saving resolved artifacts to lockfile.");
        DependencyUtils.saveLockFile(lock);

        return 0;
    }
}
