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
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;
import software.amazon.smithy.build.model.MavenRepository;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.EnvironmentVariable;
import software.amazon.smithy.cli.dependencies.DependencyResolver;
import software.amazon.smithy.cli.dependencies.MavenDependencyResolver;

final class WarmupCommand extends ClasspathCommand {

    private static final Logger LOGGER = Logger.getLogger(WarmupCommand.class.getName());

    WarmupCommand(String parentCommandName, DependencyResolver.Factory dependencyResolverFactory) {
        super(parentCommandName, dependencyResolverFactory);
    }

    @Override
    public String getName() {
        return "warmup";
    }

    @Override
    public String getSummary() {
        return "Creates caches for faster subsequent executions";
    }

    @Override
    public boolean isHidden() {
        return true;
    }

    @Override
    int runWithClassLoader(SmithyBuildConfig config, Arguments arguments, Env env, List<String> models) {
        if (EnvironmentVariable.getByName("SMITHY_WARMUP_INTERNAL_ONLY") == null) {
            throw new UnsupportedOperationException("The warmup command is for internal use only and may "
                                                    + "be removed in the future");
        }

        LOGGER.info(() -> "Warming up Smithy CLI");

        try {
            Path tempDirWithPrefix = Files.createTempDirectory("smithy-warmup");
            DependencyResolver resolver = new MavenDependencyResolver(tempDirWithPrefix.toString());

            resolve(resolver);
            // Resolve again, but find it in the cache.
            resolve(resolver);

            // Create and load SmithyBuild files.
            File buildFile = tempDirWithPrefix.resolve("smithy-build.json").toFile();
            try (FileWriter writer = new FileWriter(buildFile)) {
                writer.write("{\n"
                             + "  \"version\": \"1.0\",\n"
                             + "  \"maven\": {\"dependencies\": [\"software.amazon.smithy:smithy-model:1.23.1\"]}\n"
                             + "}");
            }

            SmithyBuildConfig.builder().load(buildFile.toPath()).build();

            new ValidateCommand("a", (c, e) -> resolver).execute(arguments, env);
            new BuildCommand("a", (c, e) -> resolver).execute(arguments, env);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return 0;
    }

    private void resolve(DependencyResolver resolver) {
        resolver.addRepository(MavenRepository.builder().url("https://repo.maven.apache.org/maven2").build());
        resolver.addDependency("software.amazon.smithy:smithy-model:1.23.0");
        resolver.resolve();
    }
}
