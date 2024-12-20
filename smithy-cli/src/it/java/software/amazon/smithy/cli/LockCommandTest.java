/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static software.amazon.smithy.cli.MavenResolverMultipleReposTest.mockArtifactAndSha;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockserver.integration.ClientAndServer;
import software.amazon.smithy.utils.ListUtils;

@Isolated
public class LockCommandTest {

    @Test
    public void writesLockfile() throws IOException {
        ClientAndServer mockServer = null;
        try {
            mockServer = startClientAndServer(6789);

            // artifact
            mockArtifactAndSha(
                    mockServer,
                    "/artifactory-2/com/example/artifact/1.0.0/artifact-1.0.0.jar",
                    "FAKE JAR CONTENT");

            mockArtifactAndSha(
                    mockServer,
                    "/artifactory-2/com/example/artifact/maven-metadata.xml",
                    getMetadataForVersions(ListUtils.of("1.0.0")));

            mockArtifactAndSha(
                    mockServer,
                    "/artifactory-2/com/example/artifact/1.0.0/artifact-1.0.0.pom",
                    pomFileContents());

            // Dependency
            mockArtifactAndSha(
                    mockServer,
                    "/artifactory-1/com/example/dependency/1.0.0/dependency-1.0.0.jar",
                    "ANOTHER FAKE JAR");

            // Use an empty cache
            String cacheDir = Files.createTempDirectory("foo").toString();
            Map<String, String> env = new HashMap<>();
            env.put(EnvironmentVariable.SMITHY_MAVEN_CACHE.toString(), cacheDir);

            IntegUtils.withProject("lockfile-written", root -> {
                // Create initial lock file
                RunResult lockResult = IntegUtils.run(root, ListUtils.of("lock", "--debug"), env);
                assertThat(lockResult.getExitCode(), equalTo(0));
                assertThat(lockResult.getOutput(),
                        containsString(
                                "software.amazon.smithy.cli.dependencies.DependencyResolver - Resolved Maven dependencies: [com.example:artifact:jar:1.0.0"));
                assertThat(lockResult.getOutput(), containsString("Saving resolved artifacts to lockfile."));

                // Confirm lockfile detected in path and used
                RunResult validateResult = IntegUtils.run(root, ListUtils.of("validate", "--debug"), env);
                assertThat(validateResult.getExitCode(), equalTo(0));
                assertThat(validateResult.getOutput(),
                        containsString("`smithy-lock.json` found. Using locked dependencies: "));
            });
        } finally {
            if (mockServer != null) {
                mockServer.stop();
            }
        }
    }

    @Test
    public void lockedDependenciesUsed() {
        ClientAndServer mockServer = null;
        try {
            mockServer = startClientAndServer(5678);

            // artifacts
            mockArtifactAndSha(
                    mockServer,
                    "/artifactory-2/com/example/artifact/1.0.0/artifact-1.0.0.jar",
                    "FAKE JAR CONTENT");

            mockArtifactAndSha(
                    mockServer,
                    "/artifactory-2/com/example/artifact/1.1.0/artifact-1.1.0.jar",
                    "FAKE JAR CONTENT");

            mockArtifactAndSha(
                    mockServer,
                    "/artifactory-2/com/example/artifact/1.2.0/artifact-1.2.0.jar",
                    "FAKE JAR CONTENT");

            mockArtifactAndSha(
                    mockServer,
                    "/artifactory-2/com/example/artifact/maven-metadata.xml",
                    getMetadataForVersions(ListUtils.of("1.2.0", "1.1.0", "1.0.0")));

            IntegUtils.runWithEmptyCache("lockfile-used",
                    ListUtils.of("validate", "--debug"),
                    Collections.emptyMap(),
                    result -> {
                        assertThat(result.getExitCode(), equalTo(0));
                        assertThat(result.getOutput(),
                                containsString(
                                        "software.amazon.smithy.cli.dependencies.DependencyResolver - Resolved Maven dependencies: [com.example:artifact:jar:1.1.0"));
                    });
        } finally {
            if (mockServer != null) {
                mockServer.stop();
            }
        }
    }

    @Test
    public void clashingLockAndConfigThrowsException() {
        IntegUtils.runWithEmptyCache("clashing-lockfile",
                ListUtils.of("validate", "--debug"),
                Collections.emptyMap(),
                result -> {
                    assertThat(result.getExitCode(), equalTo(1));
                    assertThat(result.getOutput(),
                            containsString("`smithy-lock.json` does not match configured dependencies."));
                });
    }

    private String getMetadataForVersions(List<String> versions) {
        String versionSection =
                versions.stream().map(v -> "<version>" + v + "</version>\n").collect(Collectors.joining());
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<metadata modelVersion=\"1.1.0\">\n" +
                "  <groupId>com.example</groupId>\n" +
                "  <artifactId>artifact</artifactId>\n" +
                "  <version>" + versions.get(0) + "</version>\n" +
                "  <versioning>\n" +
                "    <latest>" + versions.get(0) + "</latest>\n" +
                "    <release>" + versions.get(0) + "</release>\n" +
                "    <versions>\n" + versionSection +
                "    </versions>\n" +
                "    <lastUpdated>20230724184336</lastUpdated>\n" +
                "  </versioning>\n" +
                "</metadata>\n";
    }

    private String pomFileContents() {
        return "<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<project xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\">\n"
                +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "    <groupId>com.example</groupId>\n" +
                "    <artifactId>artifact</artifactId>\n" +
                "    <packaging>jar</packaging>\n" +
                "    <description>artifact</description>\n" +
                "    <version>1.0.0</version>\n" +
                "    <name>artifact</name>\n" +
                "    <organization>\n" +
                "        <name>com.example</name>\n" +
                "    </organization>\n" +
                "    <properties>\n" +
                "        <info.versionScheme>early-semver</info.versionScheme>\n" +
                "    </properties>\n" +
                "    <dependencies>\n" +
                "        <dependency>\n" +
                "            <groupId>com.example</groupId>\n" +
                "            <artifactId>dependency</artifactId>\n" +
                "            <version>1.0.0</version>\n" +
                "        </dependency>\n" +
                "    </dependencies>\n" +
                "</project>";
    }
}
