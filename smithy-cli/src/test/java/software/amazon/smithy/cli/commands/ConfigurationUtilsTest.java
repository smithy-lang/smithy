package software.amazon.smithy.cli.commands;

import java.util.Set;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.model.MavenConfig;
import software.amazon.smithy.build.model.MavenRepository;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.utils.ListUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConfigurationUtilsTest {
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
        Set<MavenRepository> repositorySet = ConfigurationUtils.getConfiguredMavenRepos(emptyConfig);

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
        Set<MavenRepository> repositorySet = ConfigurationUtils.getConfiguredMavenRepos(config);

        assertEquals(repositorySet.size(), 2);
        assertThat(repositorySet, contains(TEST_REPO_1, TEST_REPO_2));
    }
}
