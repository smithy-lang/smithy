/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.dependencies;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.smithy.build.model.MavenRepository;

// This test does some light validation checks. Actual resolution is tested through integ tests.
public class MavenDependencyResolverTest {
    private static final DefaultArtifact ARTIFACT = new DefaultArtifact("com.example:missing:1.0.0");
    private static final RemoteRepository REPOSITORY = new RemoteRepository.Builder(
            "test",
            "default",
            "https://example.com/").build();

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
        // Accepts each connection and closes it before the TLS handshake completes. The whole ClientHello record is
        // read first, because closing a socket with unread data sends a reset, which Apache HttpClient retries on
        // its own.
        AtomicInteger connections = new AtomicInteger();
        try (ServerSocket server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
            Thread acceptor = new Thread(() -> {
                while (!server.isClosed()) {
                    try (Socket socket = server.accept()) {
                        connections.incrementAndGet();
                        readTlsRecord(socket);
                    } catch (IOException e) {
                        return;
                    }
                }
            });
            acceptor.setDaemon(true);
            acceptor.start();

            String url = "https://127.0.0.1:" + server.getLocalPort() + "/";
            resolveAndFail(cache, url, 1);
            int connectionsPerAttempt = connections.getAndSet(0);
            List<Long> delays = resolveAndFail(cache, url);

            Assertions.assertEquals(2, delays.size());
            // Each retry makes the same requests as the first attempt.
            Assertions.assertEquals(3 * connectionsPerAttempt, connections.get());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 502, 504})
    public void retriesServerErrors(int status, @TempDir Path cache) throws IOException {
        String pomPath = "/com/example/a/1.0.0/a-1.0.0.pom";
        String jarPath = "/com/example/a/1.0.0/a-1.0.0.jar";
        Map<String, byte[]> files = new ConcurrentHashMap<>();
        files.put(pomPath,
                ("<project><modelVersion>4.0.0</modelVersion><groupId>com.example</groupId>"
                        + "<artifactId>a</artifactId><version>1.0.0</version></project>")
                        .getBytes(StandardCharsets.UTF_8));
        files.put(jarPath, emptyJar());
        Map<String, Integer> requests = new ConcurrentHashMap<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            int count = requests.merge(path, 1, Integer::sum);
            if (path.equals(pomPath) && count == 1) {
                // Retry-After keeps the transport from waiting its default 5 seconds before retrying.
                exchange.getResponseHeaders().set("Retry-After", "0");
                respond(exchange, status, new byte[0]);
            } else if (files.containsKey(path)) {
                respond(exchange, 200, files.get(path));
            } else {
                respond(exchange, 404, new byte[0]);
            }
        });
        server.start();
        try {
            List<Long> delays = new ArrayList<>();
            DependencyResolver resolver = new MavenDependencyResolver(cache.toString(), 3, delays::add);
            resolver.addRepository(MavenRepository.builder()
                    .url("http://127.0.0.1:" + server.getAddress().getPort() + "/")
                    .build());
            resolver.addDependency("com.example:a:1.0.0");

            List<ResolvedArtifact> artifacts = resolver.resolve();

            Assertions.assertEquals(1, artifacts.size());
            Assertions.assertEquals(2, requests.get(pomPath));
            // The HTTP transport retried the response, so resolution didn't need its own retry.
            Assertions.assertEquals(0, delays.size());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void doesNotRetryMissingArtifacts(@TempDir Path cache, @TempDir Path repo) {
        List<Long> delays = resolveAndFail(cache, repo.toUri().toString());

        Assertions.assertEquals(0, delays.size());
    }

    @Test
    public void doesNotRetryLocalFileSystemErrors(@TempDir Path cache) throws IOException {
        Assumptions.assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));
        Files.setPosixFilePermissions(cache, PosixFilePermissions.fromString("r-xr-xr-x"));
        try {
            // Permissions are not enforced for some users, such as root.
            Assumptions.assumeFalse(Files.isWritable(cache));
            List<Long> delays = resolveAndFail(cache, "https://localhost:1/");

            Assertions.assertEquals(0, delays.size());
        } finally {
            Files.setPosixFilePermissions(cache, PosixFilePermissions.fromString("rwxr-xr-x"));
        }
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
                Arguments.of(transferFailure(new HttpRFC9457Exception(403, "Forbidden", RFC9457Payload.INSTANCE))),
                Arguments.of(transferFailure(new AccessDeniedException("/repo/com/example/missing.pom"))),
                Arguments.of(transferFailure(new FileSystemException("/repo/com/example/missing.pom"))));
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

    // Reads one TLS record: a 5-byte header whose last two bytes are the length of the body, then the body.
    // Errors are ignored so that the server keeps accepting connections.
    private static void readTlsRecord(Socket socket) {
        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            byte[] header = new byte[5];
            in.readFully(header);
            in.readFully(new byte[((header[3] & 0xFF) << 8) | (header[4] & 0xFF)]);
        } catch (IOException ignored) {
            // The client closed the connection before sending a full record.
        }
    }

    private static void respond(HttpExchange exchange, int status, byte[] body) throws IOException {
        exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
        if (body.length > 0) {
            exchange.getResponseBody().write(body);
        }
        exchange.close();
    }

    private static byte[] emptyJar() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
            zip.write("Manifest-Version: 1.0\n".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }

    private static List<Long> resolveAndFail(Path cache, String repositoryUrl) {
        return resolveAndFail(cache, repositoryUrl, 3);
    }

    private static List<Long> resolveAndFail(Path cache, String repositoryUrl, int maxAttempts) {
        List<Long> delays = new ArrayList<>();
        DependencyResolver resolver = new MavenDependencyResolver(cache.toString(), maxAttempts, delays::add);
        resolver.addRepository(MavenRepository.builder().url(repositoryUrl).build());
        resolver.addDependency("com.example:missing:1.0.0");

        Assertions.assertThrows(DependencyResolverException.class, resolver::resolve);
        return delays;
    }

    private static void assertDelayWithin(long delay, long ceiling) {
        Assertions.assertTrue(delay >= 0 && delay < ceiling, "Unexpected delay: " + delay);
    }
}
