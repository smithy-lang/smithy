/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.dependencies.ResolvedArtifact;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.utils.ListUtils;

public class LockFileTest {
    @Test
    public void hasNoDefaultsBuiltInToThePojo() {
        LockFile lock = LockFile.builder().build();

        assertThat(lock.getDependencyCoordinateSet(), empty());
        assertThat(lock.getRepositories(), empty());
    }

    @Test
    public void loadsEmptyLockfile() {
        LockFile lock = LockFile.fromNode(Node.objectNode());

        assertThat(lock.getDependencyCoordinateSet(), empty());
        assertThat(lock.getRepositories(), empty());
    }

    @Test
    public void loadsFromNode() {
        LockFile lock = LockFile.fromNode(getNode());

        assertThat(lock.getVersion(), equalTo("1.0"));
        assertThat(lock.getConfigHash(), equalTo(-1856284556));
        assertThat(lock.getRepositories(), hasSize(1));
        assertThat(lock.getRepositories(), contains("repo"));
        assertThat(lock.getDependencyCoordinateSet(), hasSize(1));
        assertThat(lock.getDependencyCoordinateSet(), contains("software.amazon.smithy:smithy-aws-traits:1.37.0"));
    }

    @Test
    public void validatesResolved() {
        LockFile lock = LockFile.fromNode(getNode());
        List<ResolvedArtifact> artifactList = ListUtils.of(
                ResolvedArtifact.fromCoordinateNode("software.amazon.smithy:smithy-aws-traits:1.37.0",
                        Node.objectNodeBuilder()
                                .withMember("path", "/a")
                                .withMember("sha1", "sum")
                                .build()));
        lock.validateArtifacts(artifactList);
    }

    @Test
    public void throwsErrorOnIncompatibleDependencies() {
        LockFile lock = LockFile.fromNode(getNode());
        List<ResolvedArtifact> artifactList = ListUtils.of(
                ResolvedArtifact.fromCoordinateNode("software.amazon.smithy:smithy-aws-traits:1.37.0",
                        Node.objectNodeBuilder()
                                .withMember("path", "/a")
                                .withMember("sha1", "badSum")
                                .build()));
        Assertions.assertThrows(CliError.class, () -> lock.validateArtifacts(artifactList));
    }

    @Test
    public void loadsSmithyLockfile() throws URISyntaxException {
        File lockfileResource = new File(
                Objects.requireNonNull(getClass().getResource("smithy-lock-test.json")).toURI());
        Optional<LockFile> lockFileOptional = LockFile.load(lockfileResource.toPath());

        assertTrue(lockFileOptional.isPresent());
        LockFile lockFile = lockFileOptional.get();
        assertEquals(lockFile.getConfigHash(), -1856284556);
        assertEquals(lockFile.getVersion(), "1.0");
        assertThat(lockFile.getDependencyCoordinateSet(), contains("software.amazon.smithy:smithy-aws-traits:1.37.0"));
        assertThat(lockFile.getRepositories(), contains("https://repo.maven.apache.org/maven2"));
    }

    @Test
    public void returnsEmptyWhenNoLockfile() {
        Optional<LockFile> lockFileOptional = LockFile.load();
        assertFalse(lockFileOptional.isPresent());
    }

    private Node getNode() {
        return Node.objectNodeBuilder()
                .withMember("version", "1.0")
                .withMember("configHash", -1856284556)
                .withMember("repositories", ArrayNode.fromNodes(Node.from("repo")))
                .withMember("artifacts",
                        Node.objectNode()
                                .withMember("software.amazon.smithy:smithy-aws-traits:1.37.0",
                                        Node.objectNode().withMember("sha1", "sum")))
                .build();
    }
}
