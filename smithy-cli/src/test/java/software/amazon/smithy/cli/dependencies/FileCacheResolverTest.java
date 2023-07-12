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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
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
    public void invalidatesCacheWhenArtifactDeleted() throws IOException {
        // Delete the "JAR" to invalidate the cache.
        validateCacheScenario(File::delete);
    }

    @Test
    public void invalidatesCacheWhenArtifactIsNewerThanCache() throws IOException {
        // Set the last modified time of the "JAR" to the future to ensure the cache is invalidated.
        validateCacheScenario(jar -> jar.setLastModified(new Date().getTime() + Duration.parse("P1D").toMillis()));
    }

    private void validateCacheScenario(Consumer<File> jarFileMutation) throws IOException {
        File cache = File.createTempFile("classpath", ".json");
        File jar = File.createTempFile("foo", ".jar");
        Files.write(jar.toPath(), "{}".getBytes(StandardCharsets.UTF_8));

        ResolvedArtifact artifact = ResolvedArtifact.fromCoordinates(jar.toPath(), "com.foo:bar:1.0.0");
        List<ResolvedArtifact> result = new ArrayList<>();
        result.add(artifact);

        Mock mock = new Mock(result);
        DependencyResolver cachingResolver = new FileCacheResolver(cache, jar.lastModified(), mock);
        List<ResolvedArtifact> resolved = cachingResolver.resolve();

        // Make sure artifacts were cached as expected.
        assertThat(resolved, contains(artifact));
        assertThat(IoUtils.readUtf8File(cache.toPath()), containsString("com.foo:bar:1.0.0"));

        // Remove the canned entry from the mock so that when the cache is invalidated, we get a different result.
        result.clear();

        // Calling it again will load from the cached file and not from the delegate mock that's now empty.
        assertThat(cachingResolver.resolve(), contains(artifact));

        // The cache should still be there.
        assertThat(IoUtils.readUtf8File(cache.toPath()), containsString("com.foo:bar:1.0.0"));

        // Mutate the JAR using the provided method. This method should invalidate the cache.
        jarFileMutation.accept(jar);

        // Resolving here skips the cache (which contains artifacts) and calls the delegate (which is now empty).
        assertThat(cachingResolver.resolve(), empty());

        // The caching resolver should now write an empty cache file.
        assertThat(IoUtils.readUtf8File(cache.toPath()), containsString("{}"));
    }

    @Test
    public void invalidatesCacheWhenConfigIsNewerThanCache() throws IOException {
        File cache = File.createTempFile("classpath", ".json");
        File jar = File.createTempFile("foo", ".jar");
        Files.write(jar.toPath(), "{}".getBytes(StandardCharsets.UTF_8));

        ResolvedArtifact artifact = ResolvedArtifact.fromCoordinates(jar.toPath(), "com.foo:bar:1.0.0");
        List<ResolvedArtifact> result = new ArrayList<>();
        result.add(artifact);

        Mock mock = new Mock(result);
        // Set the "config" last modified to a future date to ensure it's newer than the "JAR" file.
        DependencyResolver cachingResolver = new FileCacheResolver(
            cache,
            jar.lastModified() + Duration.parse("P1D").toMillis(),
            mock
        );
        List<ResolvedArtifact> resolved = cachingResolver.resolve();

        // Make sure artifacts were cached as expected.
        assertThat(resolved, contains(artifact));
        assertThat(IoUtils.readUtf8File(cache.toPath()), containsString("com.foo:bar:1.0.0"));

        // Remove the canned entry from the mock so that when the cache is invalidated, we get a different result.
        result.clear();

        // The cache will be invalidated here and reloaded from source, resulting in an empty result.
        assertThat(cachingResolver.resolve(), empty());
    }

    @Test
    public void invalidatesCacheWhenCacheExceedsTTL() throws IOException {
        long tenDaysAgo = new Date().getTime() - Duration.parse("P10D").toMillis();
        File cache = File.createTempFile("classpath", ".json");
        File jar = File.createTempFile("foo", ".jar");
        Files.write(jar.toPath(), "{}".getBytes(StandardCharsets.UTF_8));

        ResolvedArtifact artifact = ResolvedArtifact.fromCoordinates(jar.toPath(), "com.foo:bar:1.0.0");
        List<ResolvedArtifact> result = new ArrayList<>();
        result.add(artifact);

        Mock mock = new Mock(result);
        // Make sure the config is set to 10 days ago too, so that config date checking doesn't invalidate.
        DependencyResolver cachingResolver = new FileCacheResolver(cache, tenDaysAgo, mock);
        List<ResolvedArtifact> resolved = cachingResolver.resolve();

        // Make sure artifacts were cached as expected.
        assertThat(resolved, contains(artifact));
        assertThat(IoUtils.readUtf8File(cache.toPath()), containsString("com.foo:bar:1.0.0"));

        // Remove the canned entry from the mock so that when the cache is invalidated, we get a different result.
        result.clear();

        // Change the last modified of the cache to a date in the distant past to invalidate the cache.
        assertThat(cache.setLastModified(tenDaysAgo), is(true));

        // The cache will be invalidated here and reloaded from source, resulting in an empty result.
        assertThat(cachingResolver.resolve(), empty());
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
