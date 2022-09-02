package software.amazon.smithy.cli.dependencies;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ResolvedArtifactTest {
    @Test
    public void loadsFromCoordinates() {
        Path path = Paths.get("/a");
        String coordinates = "com.foo:baz-bam:1.2.0";
        ResolvedArtifact artifact = ResolvedArtifact.fromCoordinates(path, coordinates);

        assertThat(artifact.getPath(), equalTo(path));
        assertThat(artifact.getCoordinates(), equalTo(coordinates));
        assertThat(artifact.getGroupId(), equalTo("com.foo"));
        assertThat(artifact.getArtifactId(), equalTo("baz-bam"));
        assertThat(artifact.getVersion(), equalTo("1.2.0"));
    }

    @Test
    public void createsCoordinatesStringFromParts() {
        Path path = Paths.get("/a");
        ResolvedArtifact artifact = new ResolvedArtifact(path, "com.foo", "baz-bam", "1.2.0");

        assertThat(artifact.getPath(), equalTo(path));
        assertThat(artifact.getCoordinates(), equalTo("com.foo:baz-bam:1.2.0"));
        assertThat(artifact.getGroupId(), equalTo("com.foo"));
        assertThat(artifact.getArtifactId(), equalTo("baz-bam"));
        assertThat(artifact.getVersion(), equalTo("1.2.0"));
    }

    @Test
    public void validatesCoordinatesNotTooManyParts() {
        Path path = Paths.get("/a");
        String coordinates = "com.foo:baz-bam:1.2.0:boo";

        Assertions.assertThrows(DependencyResolverException.class,
                                () -> ResolvedArtifact.fromCoordinates(path, coordinates));
    }

    @Test
    public void validatesCoordinatesEnoughParts() {
        Path path = Paths.get("/a");
        String coordinates = "com.foo:baz-bam";

        Assertions.assertThrows(DependencyResolverException.class,
                                () -> ResolvedArtifact.fromCoordinates(path, coordinates));
    }
}
