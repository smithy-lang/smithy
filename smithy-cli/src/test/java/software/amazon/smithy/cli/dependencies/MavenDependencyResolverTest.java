/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.dependencies;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import javax.net.ssl.SSLHandshakeException;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.connector.transport.http.HttpTransporterException;
import org.eclipse.aether.spi.connector.transport.http.RFC9457.HttpRFC9457Exception;
import org.eclipse.aether.spi.connector.transport.http.RFC9457.RFC9457Payload;
import org.eclipse.aether.transfer.ArtifactNotFoundException;
import org.eclipse.aether.transfer.ArtifactTransferException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.smithy.build.model.MavenRepository;
import software.amazon.smithy.cli.EnvironmentVariable;

// This test does some light validation checks. Actual resolution is tested through integ tests.
public class MavenDependencyResolverTest {
    private static final DefaultArtifact ARTIFACT = new DefaultArtifact("com.example:missing:1.0.0");
    private static final RemoteRepository REPOSITORY = new RemoteRepository.Builder(
            "test",
            "default",
            "https://example.com/").build();

    @AfterEach
    public void clearMaxAttempts() {
        EnvironmentVariable.SMITHY_MAVEN_MAX_ATTEMPTS.clear();
    }

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
                Arguments.of("a:b: "));
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

    @Test
    public void retriesRefusedConnections(@TempDir Path cache) {
        List<Long> delays = resolveAndFail(cache, "https://localhost:1/");

        Assertions.assertEquals(2, delays.size());
        assertDelayWithin(delays.get(0), 1000);
        assertDelayWithin(delays.get(1), 2000);
    }

    @Test
    public void retriesDnsFailures(@TempDir Path cache) {
        List<Long> delays = resolveAndFail(cache, "https://smithy-cli-test.invalid/");

        Assertions.assertEquals(2, delays.size());
    }

    @Test
    public void retriesTlsFailures(@TempDir Path cache) throws Exception {
        // Accepts each connection and closes it before the TLS handshake completes.
        AtomicInteger connections = new AtomicInteger();
        try (ServerSocket server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
            Thread acceptor = new Thread(() -> {
                while (!server.isClosed()) {
                    try (Socket ignored = server.accept()) {
                        connections.incrementAndGet();
                    } catch (IOException e) {
                        return;
                    }
                }
            });
            acceptor.setDaemon(true);
            acceptor.start();

            String url = "https://127.0.0.1:" + server.getLocalPort() + "/";
            EnvironmentVariable.SMITHY_MAVEN_MAX_ATTEMPTS.set("1");
            resolveAndFail(cache, url);
            int connectionsPerAttempt = connections.getAndSet(0);
            EnvironmentVariable.SMITHY_MAVEN_MAX_ATTEMPTS.clear();
            List<Long> delays = resolveAndFail(cache, url);

            Assertions.assertEquals(2, delays.size());
            // Each retry makes the same requests as the first attempt.
            Assertions.assertEquals(3 * connectionsPerAttempt, connections.get());
        }
    }

    @Test
    public void doesNotRetryMissingArtifacts(@TempDir Path cache, @TempDir Path repo) {
        List<Long> delays = resolveAndFail(cache, repo.toUri().toString());

        Assertions.assertEquals(0, delays.size());
    }

    @Test
    public void usesMaxAttemptsFromEnvironment(@TempDir Path cache) {
        EnvironmentVariable.SMITHY_MAVEN_MAX_ATTEMPTS.set("1");
        List<Long> delays = resolveAndFail(cache, "https://localhost:1/");

        Assertions.assertEquals(0, delays.size());

        EnvironmentVariable.SMITHY_MAVEN_MAX_ATTEMPTS.set("5");
        delays = resolveAndFail(cache, "https://localhost:1/");

        Assertions.assertEquals(4, delays.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  "})
    public void usesDefaultMaxAttemptsWhenEmpty(String value, @TempDir Path cache) {
        EnvironmentVariable.SMITHY_MAVEN_MAX_ATTEMPTS.set(value);
        List<Long> delays = resolveAndFail(cache, "https://localhost:1/");

        Assertions.assertEquals(2, delays.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc"})
    public void validatesMaxAttemptsWhenResolving(String value, @TempDir Path cache) {
        EnvironmentVariable.SMITHY_MAVEN_MAX_ATTEMPTS.set(value);
        DependencyResolver resolver = new MavenDependencyResolver(cache.toString());
        resolver.addRepository(MavenRepository.builder().url("https://localhost:1/").build());
        resolver.addDependency("com.example:missing:1.0.0");

        DependencyResolverException e = Assertions.assertThrows(DependencyResolverException.class,
                resolver::resolve);
        Assertions.assertTrue(e.getMessage().contains("SMITHY_MAVEN_MAX_ATTEMPTS"), e.getMessage());
    }

    @ParameterizedTest
    @MethodSource("retryableFailures")
    public void retriesIoFailures(Exception cause) {
        Assertions.assertSame(cause, MavenDependencyResolver.findRetryableFailure(transferFailure(cause)));
    }

    public static Stream<Arguments> retryableFailures() {
        return Stream.of(
                Arguments.of(new ConnectException("Connection refused")),
                Arguments.of(new UnknownHostException("example.invalid")),
                Arguments.of(new SSLHandshakeException("Remote host terminated the handshake")),
                Arguments.of(new SocketTimeoutException("Read timed out")));
    }

    @ParameterizedTest
    @MethodSource("nonRetryableFailures")
    public void doesNotRetryOtherFailures(Exception e) {
        Assertions.assertNull(MavenDependencyResolver.findRetryableFailure(e));
    }

    public static Stream<Arguments> nonRetryableFailures() {
        return Stream.of(
                Arguments.of(new ArtifactNotFoundException(ARTIFACT, REPOSITORY)),
                Arguments.of(transferFailure(new HttpTransporterException(401))),
                Arguments.of(transferFailure(new HttpRFC9457Exception(403, "Forbidden", RFC9457Payload.INSTANCE))));
    }

    // DependencyCollectionException uses only the first failure as its cause, which here is a missing artifact.
    @Test
    public void retriesWhenAnotherDependencyHasIoFailure() {
        DefaultArtifact missing = new DefaultArtifact("com.example:missing:1.0.0");
        DefaultArtifact unreachable = new DefaultArtifact("com.example:unreachable:1.0.0");
        ConnectException connectionFailure = new ConnectException("Connection refused");
        CollectResult collectResult = new CollectResult(new CollectRequest())
                .addException(descriptorFailure(missing, new ArtifactNotFoundException(missing, REPOSITORY)))
                .addException(descriptorFailure(unreachable,
                        new ArtifactTransferException(unreachable, REPOSITORY, connectionFailure)));
        DependencyResolutionException e = new DependencyResolutionException(
                new DependencyResult(new DependencyRequest()),
                new DependencyCollectionException(collectResult));

        Assertions.assertInstanceOf(ArtifactDescriptorException.class, e.getCause().getCause());
        Assertions.assertInstanceOf(ArtifactNotFoundException.class, e.getCause().getCause().getCause());
        Assertions.assertSame(connectionFailure, MavenDependencyResolver.findRetryableFailure(e));
    }

    private static ArtifactTransferException transferFailure(Exception cause) {
        return new ArtifactTransferException(ARTIFACT, REPOSITORY, cause);
    }

    private static ArtifactDescriptorException descriptorFailure(DefaultArtifact artifact, Exception cause) {
        ArtifactDescriptorResult result = new ArtifactDescriptorResult(
                new ArtifactDescriptorRequest(artifact, null, null));
        return new ArtifactDescriptorException(result, "Failed to read artifact descriptor for " + artifact, cause);
    }

    private static List<Long> resolveAndFail(Path cache, String repositoryUrl) {
        List<Long> delays = new ArrayList<>();
        DependencyResolver resolver = new MavenDependencyResolver(cache.toString(), delays::add);
        resolver.addRepository(MavenRepository.builder().url(repositoryUrl).build());
        resolver.addDependency("com.example:missing:1.0.0");

        Assertions.assertThrows(DependencyResolverException.class, resolver::resolve);
        return delays;
    }

    private static void assertDelayWithin(long delay, long ceiling) {
        Assertions.assertTrue(delay >= 0 && delay < ceiling, "Unexpected delay: " + delay);
    }
}
