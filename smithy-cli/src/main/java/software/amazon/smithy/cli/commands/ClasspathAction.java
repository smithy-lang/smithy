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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;
import software.amazon.smithy.build.SmithyBuild;
import software.amazon.smithy.build.model.MavenConfig;
import software.amazon.smithy.build.model.MavenRepository;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.EnvironmentVariable;
import software.amazon.smithy.cli.SmithyCli;
import software.amazon.smithy.cli.dependencies.DependencyResolver;
import software.amazon.smithy.cli.dependencies.DependencyResolverException;
import software.amazon.smithy.cli.dependencies.FileCacheResolver;
import software.amazon.smithy.cli.dependencies.FilterCliVersionResolver;
import software.amazon.smithy.cli.dependencies.ResolvedArtifact;

/**
 * A {@code CommandAction} that runs a wrapped action within a custom classpath.
 */
class ClasspathAction implements CommandAction {

    private static final Logger LOGGER = Logger.getLogger(ClasspathAction.class.getName());
    private static final MavenRepository CENTRAL = MavenRepository.builder()
            .url("https://repo.maven.apache.org/maven2")
            .build();
    private final DependencyResolver.Factory dependencyResolverFactory;
    private final CommandActionWithConfig action;

    ClasspathAction(DependencyResolver.Factory dependencyResolverFactory, CommandActionWithConfig action) {
        this.dependencyResolverFactory = dependencyResolverFactory;
        this.action = action;
    }

    @Override
    public int apply(Arguments arguments, Command.Env env) {
        BuildOptions buildOptions = arguments.getReceiver(BuildOptions.class);
        ThreadResult threadResult = new ThreadResult();
        ConfigOptions configOptions = arguments.getReceiver(ConfigOptions.class);
        SmithyBuildConfig config = configOptions.createSmithyBuildConfig();

        runTaskWithClasspath(buildOptions, config, env, classLoader -> {
            Command.Env updatedEnv = env.withClassLoader(classLoader);
            threadResult.returnCode = action.apply(config, arguments, updatedEnv);
        });

        return threadResult.returnCode;
    }

    static final class ThreadResult {
        int returnCode;
    }

    private void runTaskWithClasspath(
            BuildOptions buildOptions,
            SmithyBuildConfig smithyBuildConfig,
            Command.Env env,
            Consumer<ClassLoader> consumer
    ) {
        Set<String> dependencies = smithyBuildConfig.getMaven()
                .map(MavenConfig::getDependencies)
                .orElse(Collections.emptySet());

        String dependencyMode = EnvironmentVariable.SMITHY_DEPENDENCY_MODE.get();
        boolean useIsolation = false;
        switch (dependencyMode) {
            case "forbid":
                if (!dependencies.isEmpty()) {
                    throw new DependencyResolverException(String.format(
                            "%s is set to 'forbid', but the following Maven dependencies are defined in "
                            + "smithy-build.json: %s. Dependencies are forbidden in this configuration.",
                            EnvironmentVariable.SMITHY_DEPENDENCY_MODE, dependencies));
                }
                break;
            case "ignore":
                if (!dependencies.isEmpty()) {
                    LOGGER.warning(() -> String.format(
                            "%s is set to 'ignore', and the following Maven dependencies are defined in "
                            + "smithy-build.json: %s. If the build fails, then you may need to manually configure "
                            + "the classpath.", EnvironmentVariable.SMITHY_DEPENDENCY_MODE, dependencies));
                }
                break;
            case "standard":
                useIsolation = !dependencies.isEmpty();
                break;
            default:
                throw new CliError(String.format("Unknown %s setting: '%s'",
                                                 EnvironmentVariable.SMITHY_DEPENDENCY_MODE, dependencyMode));
        }

        if (useIsolation) {
            long start = System.nanoTime();
            List<Path> files = resolveDependencies(buildOptions, smithyBuildConfig, env,
                                                   smithyBuildConfig.getMaven().get());
            long end = System.nanoTime();
            LOGGER.fine(() -> "Dependency resolution time in ms: " + ((end - start) / 1000000));
            new IsolatedRunnable(files, env.classLoader(), consumer).run();
            LOGGER.fine(() -> "Command time in ms: " + ((System.nanoTime() - end) / 1000000));
        } else {
            consumer.accept(env.classLoader());
        }
    }

    private List<Path> resolveDependencies(
            BuildOptions buildOptions,
            SmithyBuildConfig smithyBuildConfig,
            Command.Env env,
            MavenConfig maven
    ) {
        DependencyResolver baseResolver = dependencyResolverFactory.create(smithyBuildConfig, env);
        long lastModified = smithyBuildConfig.getLastModifiedInMillis();
        DependencyResolver delegate = new FilterCliVersionResolver(SmithyCli.getVersion(), baseResolver);
        DependencyResolver resolver = new FileCacheResolver(getCacheFile(buildOptions), lastModified, delegate);
        addConfiguredMavenRepos(smithyBuildConfig, resolver);
        maven.getDependencies().forEach(resolver::addDependency);
        List<ResolvedArtifact> artifacts = resolver.resolve();
        LOGGER.fine(() -> "Classpath resolved with Maven: " + artifacts);

        List<Path> result = new ArrayList<>(artifacts.size());
        for (ResolvedArtifact artifact : artifacts) {
            result.add(artifact.getPath());
        }

        return result;
    }

    private static void addConfiguredMavenRepos(SmithyBuildConfig config, DependencyResolver resolver) {
        // Environment variables take precedence over config files.
        String envRepos = EnvironmentVariable.SMITHY_MAVEN_REPOS.get();
        if (envRepos != null) {
            for (String repo : envRepos.split("\\|")) {
                resolver.addRepository(MavenRepository.builder().url(repo.trim()).build());
            }
        }

        Set<MavenRepository> configuredRepos = config.getMaven()
                .map(MavenConfig::getRepositories)
                .orElse(Collections.emptySet());

        if (!configuredRepos.isEmpty()) {
            configuredRepos.forEach(resolver::addRepository);
        } else if (envRepos == null) {
            LOGGER.finest(() -> String.format("maven.repositories is not defined in smithy-build.json and the %s "
                                              + "environment variable is not set. Defaulting to Maven Central.",
                                              EnvironmentVariable.SMITHY_MAVEN_REPOS));
            resolver.addRepository(CENTRAL);
        }
    }

    private File getCacheFile(BuildOptions buildOptions) {
        String output = buildOptions.output();
        Path buildPath = output == null ? SmithyBuild.getDefaultOutputDirectory() : Paths.get(output);
        return buildPath.resolve("classpath.json").toFile();
    }
}
