package software.amazon.smithy.cli;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import software.amazon.smithy.utils.ListUtils;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.containsString;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

@Isolated
public class MavenResolverMultipleReposTest {

    @Test
    public void multipleRepositoriesOnSameHostName() {
        ClientAndServer mockServer = null;
        try {
            mockServer = startClientAndServer(1234);

            // artifact
            mockArtifactAndSha(
                mockServer,
                "/artifactory-2/com/example/artifact/1.0.0-SNAPSHOT/artifact-1.0.0-20230724.184336-2.jar",
                "FAKE JAR CONTENT"
            );

            mockArtifactAndSha(
                mockServer,
                "/artifactory-2/com/example/artifact/1.0.0-SNAPSHOT/artifact-1.0.0-20230724.184336-1.pom",
                pomFileContents()
            );

            mockArtifactAndSha(
                    mockServer,
                    "/artifactory-2/com/example/artifact/1.0.0-SNAPSHOT/maven-metadata.xml",
                    metadataFileContents()
            );

            // dependency
            mockArtifactAndSha(
                mockServer,
                "/artifactory-1/com/example/dependency/1.0.0/dependency-1.0.0.jar",
                "ANOTHER FAKE JAR"
            );

            mockNotFound(mockServer, "/artifactory-1/.*");
            mockNotFound(mockServer, "/artifactory-2/.*");

            IntegUtils.runWithEmptyCache("maven-multiple-repos", ListUtils.of("validate", "--debug"),
                    Collections.emptyMap(), result -> {
                        assertThat(result.getExitCode(), equalTo(0));
                        assertThat(result.getOutput(), containsString("software.amazon.smithy.cli.dependencies.DependencyResolver - Resolved Maven dependencies: [com.example:artifact:jar:1.0.0-20230724.184336-2"));
                    });
        } finally {
            if (mockServer!=null) {
                mockServer.stop();
            }
        }
    }

    public static void mockArtifactAndSha(ClientAndServer mockServer, String path, String contents) {
        mockSuccess(
            mockServer,
            path,
            contents
        );

        mockSuccess(
            mockServer,
        path + ".sha1",
            DigestUtils.sha1Hex(contents)
        );
    }

    private String metadataFileContents() {
        return
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<metadata modelVersion=\"1.1.0\">\n" +
                "  <groupId>com.example</groupId>\n" +
                "  <artifactId>artifact</artifactId>\n" +
                "  <version>1.0.0-SNAPSHOT</version>\n" +
                "  <versioning>\n" +
                "    <snapshot>\n" +
                "      <timestamp>20230724.184336</timestamp>\n" +
                "      <buildNumber>1</buildNumber>\n" +
                "    </snapshot>\n" +
                "    <lastUpdated>20230724184336</lastUpdated>\n" +
                "    <snapshotVersions>\n" +
                "      <snapshotVersion>\n" +
                "        <extension>jar</extension>\n" +
                "        <value>1.0.0-20230724.184336-2</value>\n" +
                "        <updated>20230724184336</updated>\n" +
                "      </snapshotVersion>\n" +
                "      <snapshotVersion>\n" +
                "        <extension>pom</extension>\n" +
                "        <value>1.0.0-20230724.184336-1</value>\n" +
                "        <updated>20230724184336</updated>\n" +
                "      </snapshotVersion>\n" +
                "    </snapshotVersions>\n" +
                "  </versioning>\n" +
                "</metadata>\n";
    }

    private String pomFileContents() {
        return
            "<?xml version='1.0' encoding='UTF-8'?>\n" +
            "<project xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\">\n" +
            "    <modelVersion>4.0.0</modelVersion>\n" +
            "    <groupId>com.example</groupId>\n" +
            "    <artifactId>artifact</artifactId>\n" +
            "    <packaging>jar</packaging>\n" +
            "    <description>artifact</description>\n" +
            "    <version>1.0.0-SNAPSHOT</version>\n" +
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

    private static void mockNotFound(ClientAndServer mockServer, String path) {
        mockServer.when(
                HttpRequest
                        .request()
                        .withMethod("GET")
                        .withPath(path)
        ).respond(HttpResponse.notFoundResponse());
    }

    private static void mockSuccess(ClientAndServer mockServer, String path, String responseBody) {
        mockServer.when(
                HttpRequest
                        .request()
                        .withMethod("GET")
                        .withPath(path)
        ).respond(
                HttpResponse
                        .response()
                        .withStatusCode(200)
                        .withBody(responseBody)
        );
    }
}
