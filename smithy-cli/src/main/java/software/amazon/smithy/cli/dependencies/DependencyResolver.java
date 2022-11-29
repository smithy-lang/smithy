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

package software.amazon.smithy.cli.dependencies;

import java.util.List;
import software.amazon.smithy.build.model.MavenRepository;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.cli.Command;

/**
 * Resolves Maven dependencies for the Smithy CLI.
 */
public interface DependencyResolver {
    /**
     * Add a Maven repository.
     *
     * @param repository Repository to add.
     * @throws DependencyResolverException When the repository is invalid.
     */
    void addRepository(MavenRepository repository);

    /**
     * Add a dependency.
     *
     * <p>Coordinates must be given a group ID, artifact ID, and version in the form
     * of "groupId:artifactId:version". Coordinates support Maven dependency ranges.
     * Coordinates do not support LATEST, SNAPSHOT, latest-release, latest.*, or
     * Gradle style "+" syntax.
     *
     * @param coordinates Dependency coordinates to add.
     * @throws DependencyResolverException When the dependency is invalid.
     */
    void addDependency(String coordinates);

    /**
     * Resolves artifacts for the configured dependencies.
     *
     * @return Returns the resolved artifacts, including file on disk and coordinates.
     * @throws DependencyResolverException If dependency resolution fails.
     */
    List<ResolvedArtifact> resolve();

    /**
     * Responsible for creating a {@link DependencyResolver} for the CLI,
     * optionally based on configuration.
     */
    @FunctionalInterface
    interface Factory {
        /**
         * Creates a {@link DependencyResolver}.
         *
         * @param config smithy-build.json configuration that can be used to configure the resolver.
         * @param env Command environment, including stderr and stdout printers.
         * @return Returns the created resolver.
         */
        DependencyResolver create(SmithyBuildConfig config, Command.Env env);
    }
}
