package software.amazon.smithy.cli.dependencies;

import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.build.model.MavenRepository;

// This test does some light validation checks. Actual resolution is tested through integ tests.
public class MavenDependencyResolverTest {
    @Test
    public void allowsValidDependenciesAndRepos() {
        DependencyResolver resolver = new MavenDependencyResolver();
        resolver.addRepository(MavenRepository.builder().url("https://example.com").build());
        resolver.addRepository(MavenRepository.builder()
                .url("https://mvn.example.com")
                .httpCredentials("user:pass")
                .build());
        resolver.addDependency("com.foo:baz1:1.0.0");
        resolver.addDependency("com.foo:baz2:[1.0.0]");
        resolver.addDependency("com.foo:baz3:[1.0.0,]");
        resolver.addDependency("smithy.foo:bar:1.25.0-SNAPSHOT");
    }

    @ParameterizedTest
    @MethodSource("invalidDependencies")
    public void validatesDependencies(String value) {
        DependencyResolver resolver = new MavenDependencyResolver();

        DependencyResolverException e = Assertions.assertThrows(DependencyResolverException.class, () -> {
            resolver.addDependency(value);
        });
    }

    public static Stream<Arguments> invalidDependencies() {
        return Stream.of(
            Arguments.of("X"),
            Arguments.of("smithy.foo:bar:RELEASE"),
            Arguments.of("smithy.foo:bar:latest-status"),
            Arguments.of("smithy.foo:bar:LATEST"),
            Arguments.of("smithy.foo:bar:1.25.0+"),
            Arguments.of("a::1.2.0"),
            Arguments.of(":b:1.2.0"),
            Arguments.of("a:b:"),
            Arguments.of("a:b: ")
        );
    }

    @Test
    public void repositoryNeedsValidUrl() {
        DependencyResolver resolver = new MavenDependencyResolver();

        Assertions.assertThrows(DependencyResolverException.class, () -> {
            resolver.addRepository(MavenRepository.builder()
                    .url("!nope://")
                    .build());
        });
    }
}
