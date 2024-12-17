/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyBuilder;

/**
 * Encapsulates the result of running SmithyBuild.
 */
public final class SmithyBuildResult {
    private final List<ProjectionResult> results;

    private SmithyBuildResult(Builder builder) {
        results = ListUtils.copyOf(builder.results);
    }

    /**
     * Creates a builder used to build SmithyBuildResult.
     *
     * @return Returns the builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns true if any projected models contain error or danger events.
     *
     * @return Returns true if any are broken.
     */
    public boolean anyBroken() {
        for (ProjectionResult result : results) {
            if (result.isBroken()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets all of the artifacts written during the build.
     *
     * @return Returns a stream of paths to artifacts.
     */
    public Stream<Path> allArtifacts() {
        return allManifests().flatMap(manifest -> manifest.getFiles().stream());
    }

    /**
     * Gets all of the manifests that were used in the build.
     *
     * @return Returns a stream of the manifests.
     */
    public Stream<FileManifest> allManifests() {
        return results.stream().flatMap(result -> result.getPluginManifests().values().stream());
    }

    /**
     * Gets a projection result by name.
     *
     * @param projectionName ProjectionConfig name to get.
     * @return Returns the optionally found result.
     */
    public Optional<ProjectionResult> getProjectionResult(String projectionName) {
        for (ProjectionResult result : results) {
            if (result.getProjectionName().equals(projectionName)) {
                return Optional.of(result);
            }
        }
        return Optional.empty();
    }

    /**
     * Gets all of the projection results as an unmodifiable list.
     *
     * @return Returns the projection results.
     */
    public List<ProjectionResult> getProjectionResults() {
        return results;
    }

    /**
     * Gets all of the projection results as a map of projection name to
     * {@link ProjectionResult}.
     *
     * @return Returns the projection results as a map.
     */
    public Map<String, ProjectionResult> getProjectionResultsMap() {
        Map<String, ProjectionResult> resultMap = new HashMap<>();
        for (ProjectionResult result : results) {
            resultMap.put(result.getProjectionName(), result);
        }
        return Collections.unmodifiableMap(resultMap);
    }

    /**
     * Gets the number of projection results in the map.
     *
     * @return Returns the number of results.
     */
    public int size() {
        return results.size();
    }

    /**
     * Checks if the results is empty.
     *
     * @return Returns true if there are no results.
     */
    public boolean isEmpty() {
        return results.isEmpty();
    }

    /**
     * Creates a SmithyBuildResult.
     */
    public static final class Builder implements SmithyBuilder<SmithyBuildResult> {
        private final List<ProjectionResult> results = Collections.synchronizedList(new ArrayList<>());

        private Builder() {}

        @Override
        public SmithyBuildResult build() {
            return new SmithyBuildResult(this);
        }

        /**
         * Adds a projection result to the builder.
         *
         * <p>This method is thread-safe as a synchronized list is updated each
         * time this is called.
         *
         * @param result Result to add.
         * @return Returns the builder.
         */
        public Builder addProjectionResult(ProjectionResult result) {
            results.add(result);
            return this;
        }
    }
}
