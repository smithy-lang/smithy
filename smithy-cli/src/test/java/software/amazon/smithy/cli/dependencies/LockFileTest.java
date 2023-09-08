package software.amazon.smithy.cli.dependencies;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.utils.ListUtils;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

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
                                .withMember("shaSum", "sum")
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
                        .withMember("shaSum", "badSum")
                        .build()));
        Assertions.assertThrows(CliError.class, () -> lock.validateArtifacts(artifactList));
    }


    private Node getNode() {
        return Node.objectNodeBuilder()
                .withMember("version", "1.0")
                .withMember("config_hash", -1856284556)
                .withMember("repositories", ArrayNode.fromNodes(Node.from("repo")))
                .withMember("artifacts", Node.objectNode()
                        .withMember("software.amazon.smithy:smithy-aws-traits:1.37.0",
                                        Node.objectNode().withMember("shaSum", "sum")))
                .build();
    }
}
