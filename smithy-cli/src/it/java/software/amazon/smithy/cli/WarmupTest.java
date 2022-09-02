package software.amazon.smithy.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.utils.ListUtils;

public class WarmupTest {
    @Test
    public void providingNoInputPrintsHelpExits0() {
        IntegUtils.run("simple-config-sources", ListUtils.of("--help"), result -> {
            assertThat(result.getOutput(), not(containsString("warmup")));
        });
    }

    @Test
    public void warmupDoesNotWorkWithoutEnvvar() {
        IntegUtils.run("simple-config-sources", ListUtils.of("warmup"), result -> {
            assertThat(result.getExitCode(), equalTo(1));
        });
    }
}
