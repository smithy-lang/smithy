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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import org.eclipse.aether.util.ChecksumUtils;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;

/**
 * An artifact resolved from a repository that provides the path on disk where the artifact
 * was downloaded, and the coordinates of the artifact.
 */
public final class ResolvedArtifact implements ToNode {
    private static final String SHA_SUM_MEMBER_NAME = "sha1";
    private static final String PATH_MEMBER_NAME = "path";
    private static final String CHECKSUM_FILE_EXTENSION = ".sha1";
    private final Path path;
    private final String coordinates;
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String shaSum;

    public ResolvedArtifact(Path path, String groupId, String artifactId, String version) {
        this(path, groupId + ':' + artifactId + ':' + version, groupId, artifactId, version, null);
    }

    private ResolvedArtifact(Path path, String coordinates, String groupId, String artifactId,
                             String version, String shaSum
    ) {
        this.coordinates = Objects.requireNonNull(coordinates);
        this.path = Objects.requireNonNull(path);
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.shaSum = shaSum != null ? shaSum : getOrComputeSha(path);
    }

    private static String getOrComputeSha(Path path) {
        File artifactFile = path.toFile();
        File checksumFile = new File(artifactFile.getParent(), artifactFile.getName() + CHECKSUM_FILE_EXTENSION);
        try {
            if (checksumFile.exists()) {
                return ChecksumUtils.read(checksumFile);
            } else {
                return DependencyUtils.computeSha1(path);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
        String[] parts = parseCoordinates(coordinates);
        return new ResolvedArtifact(location, coordinates, parts[0], parts[1], parts[2], null);
    }

    /**
     * Creates a resolved artifact from a Maven coordinates string and Node.
     *
     * @param coordinates Maven coordinates (e.g., group:artifact:version).
     * @param node Node containing the resolved artifact data.
     * @return Returns the created artifact
     */
    public static ResolvedArtifact fromCoordinateNode(String coordinates, Node node) {
        String[] parts = parseCoordinates(coordinates);
        ObjectNode objectNode = node.expectObjectNode();
        String shaSum = objectNode.expectMember(SHA_SUM_MEMBER_NAME).expectStringNode().getValue();
        Path location = Paths.get(objectNode.expectMember(PATH_MEMBER_NAME).expectStringNode().getValue());
        return new ResolvedArtifact(location, coordinates, parts[0], parts[1], parts[2], shaSum);
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

    /**
     * @return Get the sha1 digest of the artifact.
     */
    public String getShaSum() {
        return shaSum;
    }

    /**
     * @return Get the last time the artifact was modified.
     */
    public long getLastModified() {
        return path.toFile().lastModified();
    }

    @Override
    public String toString() {
        return "{path=" + path + ", coordinates='" + coordinates + ", sha1='" + shaSum + "'}";
    }

    @Override
    public int hashCode() {
        return Objects.hash(coordinates, path, shaSum);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof ResolvedArtifact)) {
            return false;
        }
        ResolvedArtifact artifact = (ResolvedArtifact) o;
        return path.equals(artifact.path)
                && shaSum.equals(artifact.shaSum)
                && coordinates.equals(artifact.coordinates);
    }

    @Override
    public Node toNode() {
        return Node.objectNodeBuilder()
                .withMember(PATH_MEMBER_NAME, path.toString())
                .withMember(SHA_SUM_MEMBER_NAME, shaSum)
                .build();
    }

    private static String[] parseCoordinates(String coordinates) {
        String[] parts = coordinates.split(":");
        if (parts.length != 3) {
            throw new DependencyResolverException("Invalid Maven coordinates: " + coordinates);
        }
        return parts;
    }
}
