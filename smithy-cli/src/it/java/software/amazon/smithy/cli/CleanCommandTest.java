package software.amazon.smithy.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasLength;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.utils.ListUtils;

public class CleanCommandTest {
    @Test
    public void exitNormallyIfBuildDirMissing() {
        IntegUtils.run("simple-config-sources", ListUtils.of("clean"), result -> {
            assertThat(result.getExitCode(), equalTo(0));
            assertThat(result.getOutput(), hasLength(0));
        });
    }

    @Test
    public void deletesContentsOfBuildDir() {
        IntegUtils.withProject("simple-config-sources", root -> {
            try {
                Path created = Files.createDirectories(root.resolve("build").resolve("smithy").resolve("foo"));
                assertThat(Files.exists(created), is(true));
                RunResult result = IntegUtils.run(root, ListUtils.of("clean"));
                assertThat(Files.exists(created), is(false));
                assertThat(result.getExitCode(), is(0));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }
}
