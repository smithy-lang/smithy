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

import java.nio.file.Path;
import java.util.Objects;

/**
 * An artifact resolved from a repository that provides the path on disk where the artifact
 * was downloaded, and the coordinates of the artifact.
 */
public final class ResolvedArtifact {
    private final Path path;
    private final String coordinates;
    private final String groupId;
    private final String artifactId;
    private final String version;

    public ResolvedArtifact(Path path, String groupId, String artifactId, String version) {
        this(path, groupId + ':' + artifactId + ':' + version, groupId, artifactId, version);
    }

    private ResolvedArtifact(Path path, String coordinates, String groupId, String artifactId, String version) {
        this.coordinates = Objects.requireNonNull(coordinates);
        this.path = Objects.requireNonNull(path);
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    /**
     * Creates a resolved artifact from a file path and Maven coordinates string.
     *
     * @param location    Location of the artifact.
     * @param coordinates Maven coordinates (e.g., group:artifact:version).
     * @return Returns the created artifact.
     * @throws DependencyResolverException if the provided coordinates are invalid.
     */
    public static ResolvedArtifact fromCoordinates(Path location, String coordinates) {
        String[] parts = coordinates.split(":");
        if (parts.length != 3) {
            throw new DependencyResolverException("Invalid Maven coordinates: " + coordinates);
        }
        return new ResolvedArtifact(location, coordinates, parts[0], parts[1], parts[2]);
    }

    /**
     * Get the path to the artifact on disk.
     *
     * @return Returns the location of the downloaded artifact.
     */
    public Path getPath() {
        return path;
    }

    /**
     * Get the resolved coordinates (e.g., group:artifact:version).
     *
     * @return Returns the resolved coordinates.
     */
    public String getCoordinates() {
        return coordinates;
    }

    /**
     * @return Get the group ID of the artifact.
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * @return Get the artifact ID of the artifact.
     */
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * @return Get the version of the artifact.
     */
    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "{path=" + path + ", coordinates='" + coordinates + "'}";
    }

    @Override
    public int hashCode() {
        return Objects.hash(coordinates, path);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof ResolvedArtifact)) {
            return false;
        }
        ResolvedArtifact artifact = (ResolvedArtifact) o;
        return path.equals(artifact.path) && coordinates.equals(artifact.coordinates);
    }
}
