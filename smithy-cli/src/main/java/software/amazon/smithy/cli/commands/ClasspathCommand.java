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
import software.amazon.smithy.cli.ArgumentReceiver;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.ConfigOptions;
import software.amazon.smithy.cli.EnvironmentVariable;
import software.amazon.smithy.cli.SmithyCli;
import software.amazon.smithy.cli.dependencies.DependencyResolver;
import software.amazon.smithy.cli.dependencies.DependencyResolverException;
import software.amazon.smithy.cli.dependencies.FileCacheResolver;
import software.amazon.smithy.cli.dependencies.FilterCliVersionResolver;
import software.amazon.smithy.cli.dependencies.ResolvedArtifact;

abstract class ClasspathCommand extends SimpleCommand {

    /**
     * The minimum Smithy version range allowed for dependencies to declare so
     * that they are compatible with this version of the Smithy CLI.
     *
     * <p>This version should generally not need to change unless some major new
     * feature or change is made to Smithy in the current major version range,
     * or a major version bump is done on Smithy itself.
     */
    private static final String MINIMUM_ALLOWED_SMITHY_VERSION = "1.25.2";

    private static final Logger LOGGER = Logger.getLogger(ClasspathCommand.class.getName());
    private static final MavenRepository CENTRAL = MavenRepository.builder()
            .url("https://repo.maven.apache.org/maven2")
            .build();
    private final DependencyResolver.Factory dependencyResolverFactory;

    ClasspathCommand(String parentCommandName, DependencyResolver.Factory dependencyResolverFactory) {
        super(parentCommandName);
        this.dependencyResolverFactory = dependencyResolverFactory;
    }

    @Override
    protected final List<ArgumentReceiver> createArgumentReceivers() {
        List<ArgumentReceiver> receivers = new ArrayList<>();
        receivers.add(new ConfigOptions());
        receivers.add(new BuildOptions());
        addAdditionalArgumentReceivers(receivers);
        return receivers;
    }

    @Override
    protected final int run(Arguments arguments, Env env, List<String> positional) {
        BuildOptions buildOptions = arguments.getReceiver(BuildOptions.class);
        ThreadResult threadResult = new ThreadResult();
        ConfigOptions configOptions = arguments.getReceiver(ConfigOptions.class);
        SmithyBuildConfig config = configOptions.createSmithyBuildConfig();

        runTaskWithClasspath(buildOptions, config, env, classLoader -> {
            Env updatedEnv = env.withClassLoader(classLoader);
            threadResult.returnCode = runWithClassLoader(config, arguments, updatedEnv, positional);
        });

        return threadResult.returnCode;
    }

    private static final class ThreadResult {
        int returnCode;
    }

    protected void addAdditionalArgumentReceivers(List<ArgumentReceiver> receivers) {
    }

    abstract int runWithClassLoader(SmithyBuildConfig config, Arguments arguments, Env env, List<String> positional);

    private void runTaskWithClasspath(
            BuildOptions buildOptions,
            SmithyBuildConfig smithyBuildConfig,
            Env env,
            Consumer<ClassLoader> consumer
    ) {
        BuildOptions.DependencyMode mode = buildOptions.dependencyMode();
        Set<String> dependencies = smithyBuildConfig.getMaven()
                .map(MavenConfig::getDependencies)
                .orElse(Collections.emptySet());
        boolean useIsolation = mode == BuildOptions.DependencyMode.STANDARD && !dependencies.isEmpty();

        if (mode == BuildOptions.DependencyMode.FORBID && !dependencies.isEmpty()) {
            throw new DependencyResolverException(String.format(
                    "%s is set to 'forbid', but the following Maven dependencies are defined in smithy-build.json: "
                    + "%s. Dependencies are forbidden in this configuration.",
                    BuildOptions.DEPENDENCY_MODE, dependencies));
        } else if (mode == BuildOptions.DependencyMode.IGNORE && !dependencies.isEmpty()) {
            LOGGER.warning(() -> String.format(
                    "%s is set to 'ignore', and the following Maven dependencies are defined in smithy-build.json: "
                    + "%s. If the build fails, then you may need to manually configure the classpath.",
                    BuildOptions.DEPENDENCY_MODE, dependencies));
        }

        if (useIsolation) {
            long start = System.nanoTime();
            List<Path> files = resolveDependencies(buildOptions, smithyBuildConfig, env,
                                                   smithyBuildConfig.getMaven().get());
            long end = System.nanoTime();
            LOGGER.fine(() -> "Dependency resolution time in ms: " + ((end - start) / 1000000));
            new IsolatedRunnable(files, getClass().getClassLoader(), consumer).run();
            LOGGER.fine(() -> "Command time in ms: " + ((System.nanoTime() - end) / 1000000));
        } else {
            consumer.accept(getClass().getClassLoader());
        }
    }

    private List<Path> resolveDependencies(
            BuildOptions buildOptions,
            SmithyBuildConfig smithyBuildConfig,
            Env env,
            MavenConfig maven
    ) {
        DependencyResolver baseResolver = dependencyResolverFactory.create(smithyBuildConfig, env);
        long lastModified = smithyBuildConfig.getLastModifiedInMillis();
        DependencyResolver delegate = new FilterCliVersionResolver(SmithyCli.getVersion(), baseResolver);
        DependencyResolver resolver = new FileCacheResolver(getCacheFile(buildOptions), lastModified, delegate);
        addDefaultConfiguration(resolver);
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

    private static void addDefaultConfiguration(DependencyResolver resolver) {
        // Add provided Smithy CLI dependencies, allowing for a range of compatible versions up to, but not
        // exceeding the current version of the CLI.
        String version = String.format("[%s,%s]", MINIMUM_ALLOWED_SMITHY_VERSION, SmithyCli.getVersion());
        resolver.addDependency("software.amazon.smithy:smithy-model:" + version);
        resolver.addDependency("software.amazon.smithy:smithy-utils:" + version);
        resolver.addDependency("software.amazon.smithy:smithy-build:" + version);
        resolver.addDependency("software.amazon.smithy:smithy-diff:" + version);
    }

    private static void addConfiguredMavenRepos(SmithyBuildConfig config, DependencyResolver resolver) {
        // Environment variables take precedence over config files.
        String envRepos = EnvironmentVariable.SMITHY_MAVEN_REPOS.getValue();
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
