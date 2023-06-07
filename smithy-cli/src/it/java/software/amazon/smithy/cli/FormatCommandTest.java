package software.amazon.smithy.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.utils.ListUtils;

public class FormatCommandTest {
    @Test
    public void failsWhenNoModelsGiven() {
        IntegUtils.run("bad-formatting", ListUtils.of("format"), result -> {
            assertThat(result.getExitCode(), equalTo(1));
            assertThat(result.getOutput(),
                       containsString("No .smithy model or directory was provided as a positional argument"));
        });
    }

    @Test
    public void failsWhenBadFileGiven() {
        IntegUtils.run("bad-formatting", ListUtils.of("format", "THIS_FILE_DOES_NOT_EXIST_1234"), result -> {
            assertThat(result.getExitCode(), equalTo(1));

            assertThat(result.getOutput(),
                       containsString("`THIS_FILE_DOES_NOT_EXIST_1234` is not a valid file or directory"));
        });
    }

    @Test
    public void formatsSingleFile() {
        IntegUtils.run("bad-formatting", ListUtils.of("format", "model/other.smithy"), result -> {
            assertThat(result.getExitCode(), equalTo(0));

            String model = result.getFile("model/other.smithy").replace("\r\n", "\n");
            assertThat(model, equalTo("$version: \"2.0\"\n"
                                      + "\n"
                                      + "namespace smithy.example\n"
                                      + "\n"
                                      + "string MyString\n"
                                      + "\n"
                                      + "string MyString2\n"));
        });
    }

    @Test
    public void formatsDirectory() {
        IntegUtils.run("bad-formatting", ListUtils.of("format", "model"), result -> {
            assertThat(result.getExitCode(), equalTo(0));

            String main = result.getFile("model/main.smithy").replace("\r\n", "\n");
            assertThat(main, equalTo("$version: \"2.0\"\n"
                                     + "\n"
                                     + "metadata this_is_a_long_string = {\n"
                                     + "    this_is_a_long_string1: \"a\"\n"
                                     + "    this_is_a_long_string2: \"b\"\n"
                                     + "    this_is_a_long_string3: \"c\"\n"
                                     + "    this_is_a_long_string4: \"d\"\n"
                                     + "}\n"));

            String other = result.getFile("model/other.smithy").replace("\r\n", "\n");
            assertThat(other, equalTo("$version: \"2.0\"\n"
                                      + "\n"
                                      + "namespace smithy.example\n"
                                      + "\n"
                                      + "string MyString\n"
                                      + "\n"
                                      + "string MyString2\n"));
        });
    }
}
