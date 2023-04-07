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

import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;
import software.amazon.smithy.build.model.MavenConfig;
import software.amazon.smithy.build.model.MavenRepository;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.cli.EnvironmentVariable;
import software.amazon.smithy.cli.dependencies.DependencyResolver;

final class DependencyHelper {

    private static final Logger LOGGER = Logger.getLogger(DependencyHelper.class.getName());
    private static final MavenRepository CENTRAL = MavenRepository.builder()
            .url("https://repo.maven.apache.org/maven2")
            .build();

    private DependencyHelper() { }

    static void addConfiguredMavenRepos(SmithyBuildConfig config, DependencyResolver resolver) {
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
}
