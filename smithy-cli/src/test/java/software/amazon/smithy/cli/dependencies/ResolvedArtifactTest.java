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
    public void loadsFromCoordinates() {
        Path path = Paths.get("/a");
        String coordinates = "com.foo:baz-bam:1.2.0";
        ResolvedArtifact artifact = ResolvedArtifact.fromNode(coordinates, getNodeForPath());

        assertThat(artifact.getPath(), equalTo(path));
        assertThat(artifact.getCoordinates(), equalTo(coordinates));
        assertThat(artifact.getGroupId(), equalTo("com.foo"));
        assertThat(artifact.getArtifactId(), equalTo("baz-bam"));
        assertThat(artifact.getVersion(), equalTo("1.2.0"));
    }

    @Test
    public void createsCoordinatesStringFromParts() {
        Path path = Paths.get("/a");
        ResolvedArtifact artifact = new ResolvedArtifact(path, "com.foo", "baz-bam", "1.2.0", "sum");

        assertThat(artifact.getPath(), equalTo(path));
        assertThat(artifact.getCoordinates(), equalTo("com.foo:baz-bam:1.2.0"));
        assertThat(artifact.getGroupId(), equalTo("com.foo"));
        assertThat(artifact.getArtifactId(), equalTo("baz-bam"));
        assertThat(artifact.getVersion(), equalTo("1.2.0"));
        assertThat(artifact.getShaSum(), equalTo("sum"));
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

    @Test
    public void generatesShaWhenNoneProvided() throws URISyntaxException {
        Path path =  Paths.get(Objects.requireNonNull(getClass().getResource("test.txt")).toURI());
        ResolvedArtifact artifact = new ResolvedArtifact(path, "com.foo", "baz-bam", "1.2.0", null);

        assertThat(artifact.getPath(), equalTo(path));
        assertThat(artifact.getCoordinates(), equalTo("com.foo:baz-bam:1.2.0"));
        assertThat(artifact.getGroupId(), equalTo("com.foo"));
        assertThat(artifact.getArtifactId(), equalTo("baz-bam"));
        assertThat(artifact.getVersion(), equalTo("1.2.0"));
        assertThat(artifact.getShaSum(), equalTo("f00bf06ec9df73964c0cf09667cf970d33d515c5"));
    }


    private static Node getNodeForPath() {
        return Node.objectNodeBuilder()
                .withMember("path", "/a")
                .withMember("sha1", "sum")
                .build();
    }
}
