package software.amazon.smithy.cli.dependencies;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.model.MavenRepository;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.utils.ListUtils;

public class FilterCliVersionResolverTest {
    @Test
    public void doesNothingWhenEmpty() {
        FilterCliVersionResolver filter = new FilterCliVersionResolver("1.26.0", new DependencyResolver() {
            @Override
            public void addRepository(MavenRepository repository) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void addDependency(String coordinates) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<ResolvedArtifact> resolve() {
                return Collections.emptyList();
            }
        });

        assertThat(filter.resolve(), empty());
    }

    @Test
    public void filtersMatchingDependencies() {
        FilterCliVersionResolver filter = new FilterCliVersionResolver("1.26.0", new DependencyResolver() {
            @Override
            public void addRepository(MavenRepository repository) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void addDependency(String coordinates) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<ResolvedArtifact> resolve() {
                return Arrays.asList(
                    ResolvedArtifact.fromCoordinateNode("software.amazon.smithy:smithy-model:1.25.0", getNodeForPath("/a")),
                    ResolvedArtifact.fromCoordinateNode("software.amazon.smithy:smithy-utils:1.25.0", getNodeForPath("/b")),
                    ResolvedArtifact.fromCoordinateNode("software.amazon.smithy:smithy-other:1.25.0", getNodeForPath("/c")),
                    ResolvedArtifact.fromCoordinateNode("software.amazon.foo:foo-other:1.0.0", getNodeForPath("/d"))
                );
            }
        });

        assertThat(filter.resolve(), contains(
            ResolvedArtifact.fromCoordinateNode("software.amazon.smithy:smithy-other:1.25.0", getNodeForPath("/c")),
            ResolvedArtifact.fromCoordinateNode("software.amazon.foo:foo-other:1.0.0", getNodeForPath("/d"))
        ));
    }

    @Test
    public void failsWhenResolvedDependenciesGreaterThanCli() {
        FilterCliVersionResolver filter = new FilterCliVersionResolver("1.26.0", new DependencyResolver() {
            @Override
            public void addRepository(MavenRepository repository) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void addDependency(String coordinates) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<ResolvedArtifact> resolve() {
                return ListUtils.of(ResolvedArtifact.fromCoordinateNode("software.amazon.smithy:smithy-model:1.27.0", getNodeForPath("/a")));
            }
        });

        Assertions.assertThrows(DependencyResolverException.class, filter::resolve);
    }

    private static Node getNodeForPath(String pathStr) {
        return Node.objectNodeBuilder()
                .withMember("path", pathStr)
                .withMember("sha1", "sum")
                .build();
    }
}
