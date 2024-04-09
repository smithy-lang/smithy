package software.amazon.smithy.cli.commands;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.cli.CliUtils;

public class AstCommandTest {
    @Test
    public void hasLongHelpCommand() {
        CliUtils.Result result = CliUtils.runSmithy("ast", "--help");

        assertThat(result.code(), equalTo(0));
        assertThat(result.stdout(), containsString("Reads"));
    }

    @Test
    public void hasShortHelpCommand() {
        CliUtils.Result result = CliUtils.runSmithy("ast", "-h");

        assertThat(result.code(), equalTo(0));
        assertThat(result.stdout(), containsString("Reads"));
    }

    @Test
    public void failsOnUnknownTrait() throws Exception {
        String model = Paths.get(getClass().getResource("unknown-trait.smithy").toURI()).toString();
        CliUtils.Result result = CliUtils.runSmithy("ast", model);

        assertThat(result.code(), not(0));
        assertThat(result.stderr(), containsString("ERROR: 1"));
    }

    @Test
    public void allowsUnknownTrait() throws URISyntaxException {
        String model = Paths.get(getClass().getResource("unknown-trait.smithy").toURI()).toString();
        CliUtils.Result result = CliUtils.runSmithy("ast", "--allow-unknown-traits", model);

        assertThat(result.code(), equalTo(0));
    }
}
