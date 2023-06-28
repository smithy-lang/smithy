package software.amazon.smithy.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;

public class MavenResolverTest {

    private static final String TEST_VERSION = "1.26.0";

    @Test
    public void resolvesDependenciesFromMavenCentralDefault() {
        IntegUtils.run("aws-model", ListUtils.of("validate", "model"), result -> {
            assertThat(result.getExitCode(), equalTo(0));
        });
    }

    @Test
    public void resolveDependenciesWithDebugInfo() {
        IntegUtils.run("aws-model", ListUtils.of("validate", "--debug", "model"), result -> {
            assertThat(result.getExitCode(), equalTo(0));
            assertThat(result.getOutput(), containsString("Resolving Maven dependencies for Smithy CLI"));
            assertThat(result.getOutput(), containsString("Dependency resolution time in ms"));
        });
    }

    @Test
    public void lowerSmithyVersionsAreUpgradedToNewerVersions() {
        IntegUtils.run("lower-smithy-version", ListUtils.of("validate", "--logging", "FINE"), result -> {
            assertThat(result.getExitCode(), equalTo(0));
            assertThat(result.getOutput(), containsString("Replaced software.amazon.smithy:smithy-model:1.25.2"));
        });
    }

    @Test
    public void lowerSmithyVersionsAreUpgradedToNewerVersionsQuiet() {
        IntegUtils.run("lower-smithy-version", ListUtils.of("validate", "--quiet"), result -> {
            assertThat(result.getExitCode(), equalTo(0));
            assertThat(result.getOutput(), not(containsString("Replacing software.amazon.smithy:smithy-model:jar:1.0.0")));
        });
    }

    @Test
    public void failsWhenBadVersionRequested() {
        IntegUtils.run("bad-smithy-version", ListUtils.of("validate"), result -> {
            assertThat(result.getExitCode(), equalTo(1));
            assertThat(result.getOutput(), containsString("software.amazon.smithy:smithy-model:jar:[999.999.999]"));
        });
    }

    @Test
    public void usesCustomRepoWithAuth() {
        ClientAndServer mockServer = null;
        try {
            mockServer = startClientAndServer(1234);
            mockServer.when(
                    HttpRequest
                            .request()
                            .withMethod("GET")
                            .withHeader("Authorization", "Basic eHh4Onl5eQ==")
                            .withPath("/maven/not/there/software/amazon/smithy/smithy-aws-iam-traits/.*\\.jar")
            ).respond(
                    HttpResponse
                            .response()
                            .withStatusCode(200)
                            .withBody("FAKE JAR CONTENT")
            );
            mockServer.when(
                    HttpRequest
                            .request()
                            .withMethod("GET")
                            .withPath("/maven/not/there/software/amazon/smithy/smithy-aws-iam-traits/.*")
            ).respond(
                    HttpResponse
                            .response()
                            .withStatusCode(401)
                            .withHeader("WWW-Authenticate", "Basic realm=\"Artifactory Realm\"")
            );

            IntegUtils.runWithEmptyCache("maven-auth", ListUtils.of("validate", "--debug"),
                    Collections.emptyMap(), result -> {
                        assertThat(result.getExitCode(), equalTo(1));
                        assertThat(result.getOutput(), containsString("HttpAuthenticator - Selected authentication options: [BASIC [complete=true]]"));
                        assertThat(result.getOutput(), containsString("HttpAuthenticator - Authentication succeeded"));
                    });
        } finally {
            if(mockServer!=null) {
                mockServer.stop();
            }
        }
    }

    @Test
    public void ignoresEmptyCacheFiles() {
        IntegUtils.withProject("aws-model", path -> {
            try {
                Files.createDirectories(path.resolve("build").resolve("smithy"));
                Files.write(path.resolve("build").resolve("smithy").resolve("classpath.json"),
                            "".getBytes(StandardCharsets.UTF_8));
                RunResult result = IntegUtils.run(path, ListUtils.of("validate", "--debug", "model"));

                assertThat(result.getExitCode(), is(0));
                assertThat(result.getOutput(), containsString("Invalidating dependency cache"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void deletesStaleCacheFiles() {
        IntegUtils.withProject("aws-model", path -> {
            // Do the first run and expect it to resolve, cache, and work.
            RunResult result = IntegUtils.run(path, ListUtils.of("validate", "--debug", "model"));
            assertThat(result.getExitCode(), is(0));
            assertThat(result.hasFile("build", "smithy", "classpath.json"), is(true));

            // Update the config file lastModified time to force the cache to be invalidated.
            assertThat(result.resolve(result.getRoot(), "smithy-build.json").toFile()
                               .setLastModified(System.currentTimeMillis()), is(true));

            // Do the next run and expect it to resolve, invalidate the cache, and work.
            result = IntegUtils.run(path, ListUtils.of("validate", "--debug", "model"));
            assertThat(result.getExitCode(), is(0));
            assertThat(result.hasFile("build", "smithy", "classpath.json"), is(true));
            assertThat(result.getOutput(), containsString("Invalidating dependency cache"));
        });
    }

    // If a dependency changes, its POM could have changed too, so invalidate the cache.
    @Test
    public void invalidatesCacheWhenDependencyChanges() {
        IntegUtils.withProject("aws-model", path -> {
            // Do the first run and expect it to resolve, cache, and work.
            RunResult result = IntegUtils.run(path, ListUtils.of("validate", "--debug", "model"));
            assertThat(result.getExitCode(), is(0));
            String cacheContents = result.getFile("build", "smithy", "classpath.json");

            ObjectNode node = Node.parse(cacheContents).expectObjectNode();
            String location = node.expectStringMember("software.amazon.smithy:smithy-aws-traits:"
                                                      + TEST_VERSION).getValue();

            // Set the lastModified of the JAR to the current time, which is > than the time of the config file,
            // so the cache is invalided.
            assertThat(new File(location).setLastModified(System.currentTimeMillis()), is(true));

            // Do the next run and expect it to resolve, invalidate the cache, and work.
            result = IntegUtils.run(path, ListUtils.of("validate", "--debug", "model"));
            assertThat(result.getExitCode(), is(0));
            assertThat(result.hasFile("build", "smithy", "classpath.json"), is(true));
            assertThat(result.getOutput(), containsString("Invalidating dependency cache"));
        });
    }

    @Test
    public void canIgnoreDependencyResolution() {
        IntegUtils.run("aws-model", ListUtils.of("validate", "model"),
                       MapUtils.of(EnvironmentVariable.SMITHY_DEPENDENCY_MODE.toString(), "ignore"),
                       result -> {
            assertThat(result.getExitCode(), equalTo(1));
        });
    }

    @Test
    public void canForbidDependencyResolution() {
        IntegUtils.run("aws-model", ListUtils.of("validate", "model"),
                       MapUtils.of(EnvironmentVariable.SMITHY_DEPENDENCY_MODE.toString(), "forbid"),
                       result -> {
            assertThat(result.getExitCode(), equalTo(1));
            assertThat(result.getOutput(), containsString("set to 'forbid'"));
        });
    }

    @Test
    public void validatesDependencyResolution() {
        IntegUtils.run("aws-model",
                       ListUtils.of("validate", "model"),
                       MapUtils.of(EnvironmentVariable.SMITHY_DEPENDENCY_MODE.toString(), "Beeblebrox"),
                       result -> {
            assertThat(result.getExitCode(), equalTo(1));
            assertThat(result.getOutput(), containsString("Beeblebrox"));
        });
    }

    @Test
    public void canDisableMavenLocalDefaultWithEnvSetting() {
        // Note that running with an empty cache means it can't find the packages at all. Running with a cache
        // means it could potentially find the packages.
        IntegUtils.runWithEmptyCache("aws-model",
                                     ListUtils.of("validate", "--debug", "model"),
                                     MapUtils.of(EnvironmentVariable.SMITHY_MAVEN_REPOS.toString(), ""),
                                     result -> {
            assertThat(result.getExitCode(), equalTo(1));
        });
    }

    @Test
    public void canSetMavenReposUsingEnvironmentVariable() {
        IntegUtils.runWithEmptyCache("aws-model",
                                     ListUtils.of("validate", "--debug", "model"),
                                     MapUtils.of(EnvironmentVariable.SMITHY_MAVEN_REPOS.toString(),
                                                 "https://repo.maven.apache.org/maven2"),
                                     result -> {
            assertThat(result.getExitCode(), equalTo(0));
        });
    }

    @Test
    public void setSetMavenRepoWithEnvUsingAuth() {
        String repo = "https://xxx:yyy@localhost:1234/maven/not/there";
        IntegUtils.runWithEmptyCache("aws-model",
                                     ListUtils.of("validate", "--debug", "model"),
                                     MapUtils.of(EnvironmentVariable.SMITHY_MAVEN_REPOS.toString(), repo),
                                     result -> {
            assertThat(result.getOutput(), containsString("username=xxx, password=***"));
            assertThat(result.getExitCode(), equalTo(1));
        });
    }
}
