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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionScheme;
import software.amazon.smithy.build.model.MavenRepository;
import software.amazon.smithy.utils.SetUtils;

/**
 * Removes Smithy CLI dependencies that conflict with the JARs used by the CLI.
 *
 * <p>This makes creating a dedicated ClassLoader simpler because Smithy dependencies are provided by the parent
 * class loader when running the CLI.
 */
public final class FilterCliVersionResolver implements DependencyResolver {

    private static final Logger LOGGER = Logger.getLogger(FilterCliVersionResolver.class.getName());
    private static final String SMITHY_GROUP = "software.amazon.smithy";
    private static final Set<String> CLI_ARTIFACTS = SetUtils.of(
            "smithy-utils", "smithy-model", "smithy-build", "smithy-cli", "smithy-diff", "smithy-syntax");

    private final String version;
    private final DependencyResolver delegate;

    /**
     * @param version Version of the Smithy CLI.
     * @param delegate Resolver to resolve dependencies and filter.
     */
    public FilterCliVersionResolver(String version, DependencyResolver delegate) {
        this.version = version;
        this.delegate = delegate;
    }

    @Override
    public void addRepository(MavenRepository repository) {
        delegate.addRepository(repository);
    }

    @Override
    public void addDependency(String coordinates) {
        delegate.addDependency(coordinates);
    }

    @Override
    public List<ResolvedArtifact> resolve() {
        List<ResolvedArtifact> artifacts = delegate.resolve();

        // Don't cache an empty file.
        if (artifacts.isEmpty()) {
            return artifacts;
        }

        VersionScheme versionScheme = new GenericVersionScheme();
        Version parsedSmithyVersion = getMavenVersion(version, versionScheme);
        List<String> replacements = new ArrayList<>();
        List<ResolvedArtifact> filtered = new ArrayList<>();

        for (ResolvedArtifact artifact : artifacts) {
            if (artifact.getGroupId().equals(SMITHY_GROUP) && CLI_ARTIFACTS.contains(artifact.getArtifactId())) {
                // The resolved artifact version does not match the version used by the CLI. In this case,
                // the resolved version must not be newer than that used by the CLI.
                Version artifactVersion = getMavenVersion(artifact.getVersion(), versionScheme);
                int compare = artifactVersion.compareTo(parsedSmithyVersion);
                if (compare > 0) {
                    throw new DependencyResolverException(
                            "The Smithy CLI is at version " + parsedSmithyVersion + ", but dependencies resolved to "
                            + "use a newer, incompatible version of " + artifact.getCoordinates() + ". Please "
                            + "update the Smithy CLI.");
                } else if (compare < 0) {
                    replacements.add("- Replaced " + artifact.getCoordinates());
                }
            } else {
                filtered.add(artifact);
            }
        }

        if (!replacements.isEmpty()) {
            String contents = String.join(System.lineSeparator(), replacements);
            LOGGER.info("Resolved dependencies were replaced with dependencies used by the Smithy CLI ("
                        + version + "). If the CLI fails due to issues like unknown classes, methods, missing "
                        + "traits, etc, then consider upgrading your dependencies to match the version of the CLI "
                        + "or modifying your declared dependencies."
                        + System.lineSeparator() + contents);
        }

        return filtered;
    }

    private static Version getMavenVersion(String input, VersionScheme versionScheme) {
        try {
            return versionScheme.parseVersion(input);
        } catch (InvalidVersionSpecificationException e) {
            throw new DependencyResolverException("Unable to parse dependency version: " + input, e);
        }
    }
}
