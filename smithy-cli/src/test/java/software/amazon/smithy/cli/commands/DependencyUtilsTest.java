package software.amazon.smithy.cli.commands;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.model.MavenConfig;
import software.amazon.smithy.build.model.MavenRepository;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.utils.ListUtils;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DependencyUtilsTest {
    private static final MavenRepository CENTRAL = MavenRepository.builder()
            .url("https://repo.maven.apache.org/maven2")
            .build();

    private static final MavenRepository TEST_REPO_1 = MavenRepository.builder()
            .url("fake_fake_fake")
            .build();
    private static final MavenRepository TEST_REPO_2 = MavenRepository.builder()
            .url("not_real")
            .build();

    @Test
    public void returnsDefaultRepository() {
        SmithyBuildConfig emptyConfig = SmithyBuildConfig.builder().version("1.0").build();
        Set<MavenRepository> repositorySet = DependencyUtils.getConfiguredMavenRepos(emptyConfig);

        assertEquals(repositorySet.size(), 1);
        assertThat(repositorySet, contains(CENTRAL));
    }

    @Test
    public void returnsCorrectRepositoriesInConfig() {
        SmithyBuildConfig config = SmithyBuildConfig.builder()
                .version("1.0")
                .maven(MavenConfig.builder()
                        .repositories(ListUtils.of(TEST_REPO_1, TEST_REPO_2))
                        .build())
                .build();
        Set<MavenRepository> repositorySet = DependencyUtils.getConfiguredMavenRepos(config);

        assertEquals(repositorySet.size(), 2);
        assertThat(repositorySet, contains(TEST_REPO_1, TEST_REPO_2));
    }

    @Test
    public void loadsSmithyLockfile() throws URISyntaxException {
        File lockfileResource = new File(
                Objects.requireNonNull(getClass().getResource("smithy-lock-test.json")).toURI());
        Optional<LockFile> lockFileOptional = DependencyUtils.loadLockfile(lockfileResource.toPath());

        assertTrue(lockFileOptional.isPresent());
        LockFile lockFile = lockFileOptional.get();
        assertEquals(lockFile.getConfigHash(), -1856284556);
        assertEquals(lockFile.getVersion(), "1.0");
        assertThat(lockFile.getDependencyCoordinateSet(), contains("software.amazon.smithy:smithy-aws-traits:1.37.0"));
        assertThat(lockFile.getRepositories(), contains("https://repo.maven.apache.org/maven2"));
    }

    @Test
    public void returnsEmptyWhenNoLockfile() {
        Optional<LockFile> lockFileOptional = DependencyUtils.loadLockfile();
        assertFalse(lockFileOptional.isPresent());
    }
}
