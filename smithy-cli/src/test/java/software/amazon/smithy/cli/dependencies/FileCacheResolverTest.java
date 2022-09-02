package software.amazon.smithy.cli.dependencies;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.model.MavenRepository;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.ListUtils;

public class FileCacheResolverTest {
    @Test
    public void proxiesCallsToDelegate() throws IOException {
        File cache = File.createTempFile("classpath", ".json");
        Mock mock = new Mock(ListUtils.of());
        DependencyResolver resolver = new FileCacheResolver(cache, System.currentTimeMillis(), mock);
        MavenRepository repo = MavenRepository.builder().url("https://example.com").build();
        resolver.addDependency("com.foo:baz-bar:1.0.0");
        resolver.addRepository(repo);

        assertThat(mock.repositories, contains(repo));
        assertThat(mock.coordinates, contains("com.foo:baz-bar:1.0.0"));
    }

    @Test
    public void ignoresAndDeletesEmptyCacheFiles() throws IOException {
        File cache = File.createTempFile("classpath", ".json");
        File jar = File.createTempFile("foo", ".json");

        List<ResolvedArtifact> result = ListUtils.of(
                ResolvedArtifact.fromCoordinates(jar.toPath(), "com.foo:bar:1.0.0"));
        Mock mock = new Mock(result);
        DependencyResolver resolver = new FileCacheResolver(cache, System.currentTimeMillis(), mock);

        // Delete the cache before resolving to ensure missing files are ignored by the cache.
        assertThat(cache.delete(), is(true));
        assertThat(resolver.resolve(), equalTo(result));
    }

    @Test
    public void loadsCacheFromDelegateWhenCacheMissingAndSaves() throws IOException {
        File cache = File.createTempFile("classpath", ".json");
        File jar = File.createTempFile("foo", ".json");
        Files.write(jar.toPath(), "{}".getBytes(StandardCharsets.UTF_8));

        ResolvedArtifact artifact = ResolvedArtifact.fromCoordinates(jar.toPath(), "com.foo:bar:1.0.0");
        List<ResolvedArtifact> result = new ArrayList<>();
        result.add(artifact);

        Mock mock = new Mock(result);
        DependencyResolver resolver = new FileCacheResolver(cache, jar.lastModified(), mock);
        List<ResolvedArtifact> resolved = resolver.resolve();

        assertThat(resolved, contains(artifact));
        assertThat(IoUtils.readUtf8File(cache.toPath()), containsString("com.foo:bar:1.0.0"));

        // Remove the canned entry from the mock to ensure the cache is working before delegating.
        result.clear();

        // Calling it again will load from the cached file.
        assertThat(resolver.resolve(), contains(artifact));

        // The cache should still be there.
        assertThat(IoUtils.readUtf8File(cache.toPath()), containsString("com.foo:bar:1.0.0"));

        // Removing the cache artifact invalidates the cache.
        assertThat(jar.delete(), is(true));

        assertThat(resolver.resolve(), empty());
        assertThat(IoUtils.readUtf8File(cache.toPath()), containsString("{}"));
    }

    private static final class Mock implements DependencyResolver {
        final List<ResolvedArtifact> artifacts;
        final List<MavenRepository> repositories = new ArrayList<>();
        final List<String> coordinates = new ArrayList<>();

        Mock(List<ResolvedArtifact> artifacts) {
            this.artifacts = artifacts;
        }

        @Override
        public void addRepository(MavenRepository repository) {
            repositories.add(repository);
        }

        @Override
        public void addDependency(String coordinates) {
            this.coordinates.add(coordinates);
        }

        @Override
        public List<ResolvedArtifact> resolve() {
            return artifacts;
        }
    }
}
