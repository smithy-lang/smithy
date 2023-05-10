package software.amazon.smithy.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.utils.ListUtils;

public class SmithyValidateTest {
    @Test
    public void validatesModelSuccess() {
        IntegUtils.run("simple-config-sources", ListUtils.of("validate"), result -> {
            assertThat(result.getExitCode(), equalTo(0));
            assertThat(result.getOutput(), containsString("SUCCESS: Validated "));
            assertThat(result.hasProjection("source"), is(false));
        });
    }

    @Test
    public void validatesModelSuccessQuiet() {
        IntegUtils.run("simple-config-sources", ListUtils.of("validate", "--quiet"), result -> {
            assertThat(result.getExitCode(), equalTo(0));
            assertThat(result.getOutput().trim(), emptyString());
        });
    }

    @Test
    public void validatesModelFailure() {
        IntegUtils.run("invalid-model", ListUtils.of("validate", "model"), result -> {
            assertThat(result.getExitCode(), equalTo(1));
            assertThat(result.getOutput(), containsString("ERROR"));
            assertThat(result.getOutput(), containsString("smithy.example#MyString"));
            assertThat(result.getOutput(), containsString("TraitTarget"));
            assertThat(result.getOutput(), containsString("invalid.smithy"));
            assertThat(result.getOutput(), containsString("@range(min: 10, max: 100) // not valid for strings!"));
            assertThat(result.getOutput(), containsString("ERROR: 1"));
        });
    }
}
