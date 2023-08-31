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
    public void loadsFromNode() {
        String coordinates = "com.foo:baz-bam:1.2.0";

        ResolvedArtifact artifact = ResolvedArtifact.fromNode(coordinates, getNodeForPath());

        assertThat(artifact.getPath(), equalTo(Paths.get("/a")));
        assertThat(artifact.getCoordinates(), equalTo(coordinates));
        assertThat(artifact.getGroupId(), equalTo("com.foo"));
        assertThat(artifact.getArtifactId(), equalTo("baz-bam"));
        assertThat(artifact.getVersion(), equalTo("1.2.0"));
        assertThat(artifact.getShaSum(), equalTo("sum"));
    }

    @Test
    public void createsCoordinatesStringFromParts() throws URISyntaxException {
        Path path =  Paths.get(Objects.requireNonNull(getClass().getResource("test.txt")).toURI());

        ResolvedArtifact artifact = new ResolvedArtifact(path, "com.foo", "baz-bam", "1.2.0");

        assertThat(artifact.getPath(), equalTo(path));
        assertThat(artifact.getCoordinates(), equalTo("com.foo:baz-bam:1.2.0"));
        assertThat(artifact.getGroupId(), equalTo("com.foo"));
        assertThat(artifact.getArtifactId(), equalTo("baz-bam"));
        assertThat(artifact.getVersion(), equalTo("1.2.0"));
    }

    @Test
    public void validatesCoordinatesNotTooManyParts() {
        String coordinates = "com.foo:baz-bam:1.2.0:boo";
        Assertions.assertThrows(DependencyResolverException.class,
                                () -> ResolvedArtifact.fromNode(coordinates, getNodeForPath()));
    }

    @Test
    public void validatesCoordinatesEnoughParts() {
        String coordinates = "com.foo:baz-bam";
        Assertions.assertThrows(DependencyResolverException.class,
                                () -> ResolvedArtifact.fromNode(coordinates, getNodeForPath()));
    }

    private static Node getNodeForPath() {
        return Node.objectNodeBuilder()
                .withMember("path", "/a")
                .withMember("shaSum", "sum")
                .build();
    }
}
