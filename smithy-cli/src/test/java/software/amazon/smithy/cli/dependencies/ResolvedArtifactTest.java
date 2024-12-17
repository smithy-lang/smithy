/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.dependencies;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;

public class ResolvedArtifactTest {
    @Test
    public void loadsFromCoordinates() throws URISyntaxException {
        Path path = Paths.get(Objects.requireNonNull(getClass().getResource("test.txt")).toURI());
        String coordinates = "com.foo:baz-bam:1.2.0";
        ResolvedArtifact artifact = ResolvedArtifact.fromCoordinates(path, coordinates);

        assertThat(artifact.getPath(), equalTo(path));
        assertThat(artifact.getCoordinates(), equalTo(coordinates));
        assertThat(artifact.getGroupId(), equalTo("com.foo"));
        assertThat(artifact.getArtifactId(), equalTo("baz-bam"));
        assertThat(artifact.getVersion(), equalTo("1.2.0"));
        assertThat(artifact.getShaSum(), equalTo("bbe99d58c301c90f0ea9354275468f3c84de710e"));
    }

    @Test
    public void createsCoordinatesStringFromParts() throws URISyntaxException {
        Path path = Paths.get(Objects.requireNonNull(getClass().getResource("test.txt")).toURI());
        ResolvedArtifact artifact = new ResolvedArtifact(path, "com.foo", "baz-bam", "1.2.0");

        assertThat(artifact.getPath(), equalTo(path));
        assertThat(artifact.getCoordinates(), equalTo("com.foo:baz-bam:1.2.0"));
        assertThat(artifact.getGroupId(), equalTo("com.foo"));
        assertThat(artifact.getArtifactId(), equalTo("baz-bam"));
        assertThat(artifact.getVersion(), equalTo("1.2.0"));
        assertThat(artifact.getShaSum(), equalTo("bbe99d58c301c90f0ea9354275468f3c84de710e"));
    }

    @Test
    public void validatesCoordinatesNotTooManyParts() {
        String coordinates = "com.foo:baz-bam:1.2.0:boo";

        Assertions.assertThrows(DependencyResolverException.class,
                () -> ResolvedArtifact.fromCoordinateNode(coordinates, getNode()));
    }

    @Test
    public void validatesCoordinatesEnoughParts() {
        String coordinates = "com.foo:baz-bam";

        Assertions.assertThrows(DependencyResolverException.class,
                () -> ResolvedArtifact.fromCoordinateNode(coordinates, getNode()));
    }

    @Test
    public void createsArtifactFromNode() {
        Path path = Paths.get("/a");
        ResolvedArtifact artifact = ResolvedArtifact.fromCoordinateNode("com.foo:baz-bam:1.2.0",
                getNode());

        assertThat(artifact.getPath(), equalTo(path));
        assertThat(artifact.getCoordinates(), equalTo("com.foo:baz-bam:1.2.0"));
        assertThat(artifact.getGroupId(), equalTo("com.foo"));
        assertThat(artifact.getArtifactId(), equalTo("baz-bam"));
        assertThat(artifact.getVersion(), equalTo("1.2.0"));
        assertThat(artifact.getShaSum(), equalTo("sum"));
    }

    public Node getNode() {
        return Node.objectNodeBuilder()
                .withMember("path", "/a")
                .withMember("sha1", "sum")
                .build();
    }
}
