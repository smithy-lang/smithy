/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.CliUtils;
import software.amazon.smithy.java.aws.credentials.chain.CredentialChain;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeId;

class CallCommandIntegrationTest {
    @TempDir
    Path temporaryDirectory;

    private String previousUserHome;

    @BeforeEach
    void useIsolatedConfigDirectory() {
        previousUserHome = System.getProperty("user.home");
        System.setProperty("user.home", temporaryDirectory.toString());
    }

    @AfterEach
    void restoreUserHome() {
        System.setProperty("user.home", previousUserHome);
        System.clearProperty("aws.profile");
    }

    @Test
    void rejectsRegistrationPathTraversalWithoutDeletingAnything() throws Exception {
        Path victim = temporaryDirectory.resolve("victim");
        Files.createDirectories(victim);
        Files.writeString(victim.resolve("keep.txt"), "keep");

        assertThrows(CliError.class, () -> CallProfiles.remove(victim.toString()));
        assertThrows(CliError.class, () -> CallProfiles.artifactDir("../../victim"));
        assertTrue(Files.isRegularFile(victim.resolve("keep.txt")));
    }

    @Test
    void registersPlansAndRendersStructuredHelp() throws Exception {
        Path model = modelPath();
        CliUtils.Result registered = CliUtils.runSmithy(
                "register",
                "--no-config",
                "--name",
                "agent",
                "--auth",
                "none",
                model.toString());
        assertEquals(0, registered.code(), registered.stdout() + registered.stderr());

        CliUtils.Result commandHelp = CliUtils.runSmithy("call", "--help");
        assertEquals(0, commandHelp.code());
        assertTrue(commandHelp.stdout().contains("smithy call s3" + " ListBuckets"));
        assertFalse(commandHelp.stdout().contains("smithy call s3ListBuckets"));

        CliUtils.Result help = CliUtils.runSmithy("call", "agent", "Echo", "--help", "--json");
        assertEquals(0, help.code(), help.stdout() + help.stderr());
        assertTrue(help.stdout().contains("\"operation\":\"Echo\""));
        assertTrue(help.stdout().contains("\"required\":true"));

        CliUtils.Result plan = CliUtils.runSmithy(
                "call",
                "agent",
                "Echo",
                "--input",
                "{\"message\":\"hello\"}",
                "--url",
                "https://example.com",
                "--plan");
        assertEquals(0, plan.code(), plan.stdout() + plan.stderr());
        assertTrue(plan.stdout().contains("\"operation\":\"Echo\""));
        assertTrue(plan.stdout().contains("\"safety\""));
        assertTrue(plan.stdout().contains("\"@http\""));

        CliUtils.Result extra = CliUtils.runSmithy("call", "agent", "Echo", "ignored");
        assertEquals(1, extra.code());
        assertTrue(extra.stdout().contains("Unexpected positional arguments"));
    }

    @Test
    void streamsResponsePayloadToFile() throws Exception {
        byte[] payload = new byte[2 * 1024 * 1024];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) (i % 251);
        }
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/download", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
            exchange.sendResponseHeaders(200, payload.length);
            exchange.getResponseBody().write(payload);
            exchange.close();
        });
        server.start();
        try {
            CliUtils.Result registered = CliUtils.runSmithy(
                    "register",
                    "--no-config",
                    "--name",
                    "stream",
                    "--auth",
                    "none",
                    modelPath().toString());
            assertEquals(0, registered.code(), registered.stdout() + registered.stderr());

            Path output = temporaryDirectory.resolve("download.bin");
            CliUtils.Result called = CliUtils.runSmithy(
                    "call",
                    "stream",
                    "Download",
                    "--url",
                    "http://127.0.0.1:" + server.getAddress().getPort(),
                    "--output-payload",
                    output.toString());
            assertEquals(0, called.code(), called.stdout() + called.stderr());
            assertArrayEquals(payload, Files.readAllBytes(output));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void unmodeledErrorsOnlyIncludeWireDataWhenExplicitlyRequested() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/echo", exchange -> {
            byte[] error = "server failed".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("X-Private-Diagnostic", "sensitive");
            exchange.sendResponseHeaders(500, error.length);
            exchange.getResponseBody().write(error);
            exchange.close();
        });
        server.start();
        try {
            CliUtils.Result registered = CliUtils.runSmithy(
                    "register",
                    "--no-config",
                    "--name",
                    "errors",
                    "--auth",
                    "none",
                    modelPath().toString());
            assertEquals(0, registered.code(), registered.stdout() + registered.stderr());
            String endpoint = "http://127.0.0.1:" + server.getAddress().getPort();

            CliUtils.Result implicit = CliUtils.runSmithy(
                    "call",
                    "errors",
                    "Echo",
                    "--url",
                    endpoint,
                    "--input",
                    "{\"message\":\"hi\"}");
            assertEquals(1, implicit.code());
            assertFalse(implicit.stdout().contains("\"@http\""));
            assertFalse(implicit.stdout().contains("X-Private-Diagnostic"));

            CliUtils.Result explicit = CliUtils.runSmithy(
                    "call",
                    "errors",
                    "Echo",
                    "--url",
                    endpoint,
                    "--input",
                    "{\"message\":\"hi\"}",
                    "--wire",
                    "headers");
            assertEquals(1, explicit.code());
            assertTrue(explicit.stdout().contains("\"@http\""));
            assertTrue(explicit.stdout().toLowerCase(java.util.Locale.ROOT).contains("x-private-diagnostic"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void concurrentProfileUpdatesDoNotLoseEntriesOrProduceInvalidJson() throws Exception {
        int count = 12;
        var executor = Executors.newFixedThreadPool(4);
        CountDownLatch start = new CountDownLatch(1);
        List<java.util.concurrent.Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int index = i;
            futures.add(executor.submit(() -> {
                start.await();
                CallProfiles.save("service-" + index, profile());
                return null;
            }));
        }
        start.countDown();
        for (var future : futures) {
            future.get();
        }
        executor.shutdownNow();

        var profiles = CallProfiles.loadAllProfiles().orElseThrow();
        assertEquals(count, profiles.size());
        assertFalse(Files.readString(CallProfiles.configPath()).isBlank());
    }

    @Test
    void paginationRoundTripsInputRegionAndOperation() {
        Model model = Model.assembler().addUnparsedModel("pagination.smithy", PAGINATED_MODEL).assemble().unwrap();
        OperationShape operation =
                model.expectShape(ShapeId.from("smithy.pagination#ListThings"), OperationShape.class);
        Document input = Document.of(java.util.Map.of("filter", Document.of("active")));
        Document result = Document.of(java.util.Map.of("next", Document.of("token-2")));

        String token = CallPagination.nextToken(operation,
                model,
                "things",
                "smithy.pagination#Pagination",
                "ListThings",
                input,
                result,
                "us-west-2",
                "https://example.com",
                "rest-json");
        CallPagination.Decoded decoded = CallPagination.decode(
                token,
                "things",
                "smithy.pagination#Pagination",
                "ListThings");

        assertEquals("us-west-2", decoded.region);
        assertEquals("active", decoded.input.getMember("filter").asString());
        assertEquals("token-2", decoded.input.getMember("nextToken").asString());
        assertEquals("https://example.com", decoded.url);
        assertEquals("rest-json", decoded.protocol);
        assertThrows(CliError.class,
                () -> CallPagination.decode(
                        token,
                        "things",
                        "smithy.pagination#Pagination",
                        "OtherOperation"));
        assertThrows(CliError.class,
                () -> CallPagination.decode(
                        token,
                        "other",
                        "smithy.pagination#Pagination",
                        "ListThings"));
    }

    @Test
    void realCredentialsUseTheStandardProviderChain() {
        assertInstanceOf(CredentialChain.class, CallCredentials.resolve(false));
    }

    @Test
    void restoresAwsProfileSystemPropertyAfterInvocation() throws Exception {
        System.setProperty("aws.profile", "before-call");
        CliUtils.Result registered = CliUtils.runSmithy(
                "register",
                "--no-config",
                "--name",
                "profile-scope",
                "--auth",
                "none",
                modelPath().toString());
        assertEquals(0, registered.code(), registered.stdout() + registered.stderr());

        CliUtils.Result plan = CliUtils.runSmithy(
                "call",
                "profile-scope",
                "Echo",
                "--aws-profile",
                "during-call",
                "--input",
                "{\"message\":\"hello\"}",
                "--url",
                "https://example.com",
                "--plan");

        assertEquals(0, plan.code(), plan.stdout() + plan.stderr());
        assertEquals("before-call", System.getProperty("aws.profile"));
    }

    private Path modelPath() throws Exception {
        return Path.of(getClass().getResource("call-model.smithy").toURI());
    }

    private static CallProfiles.Profile profile() {
        return new CallProfiles.Profile(List.of(),
                List.of(),
                "smithy.example#Service",
                List.of(),
                false,
                null,
                null,
                "none",
                0);
    }

    private static final String PAGINATED_MODEL = """
            $version: "2"
            namespace smithy.pagination
            @paginated(inputToken: "nextToken", outputToken: "next")
            service Pagination { version: "1", operations: [ListThings] }
            operation ListThings { input := { nextToken: String, filter: String }, output := { next: String } }
            """;
}
