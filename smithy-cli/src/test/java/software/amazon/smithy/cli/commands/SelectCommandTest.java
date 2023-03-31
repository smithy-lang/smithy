package software.amazon.smithy.cli.commands;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.cli.CliUtils;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

public class SelectCommandTest {
    @Test
    public void hasLongHelpCommand() {
        CliUtils.Result result = CliUtils.runSmithy("select", "--help");

        assertThat(result.code(), equalTo(0));
        assertThat(result.stdout(), containsString("Queries"));
    }

    @Test
    public void hasShortHelpCommand() {
        CliUtils.Result result = CliUtils.runSmithy("select", "-h");

        assertThat(result.code(), equalTo(0));
        assertThat(result.stdout(), containsString("Queries"));
    }

    @Test
    public void dumpsOutValidationErrorsAndFails() throws Exception {
        String model = Paths.get(getClass().getResource("unknown-trait.smithy").toURI()).toString();
        CliUtils.Result result = CliUtils.runSmithy("select", "--selector", "string", model);

        assertThat(result.code(), not(0));
        assertThat(result.stderr(), containsString("Unable to resolve trait"));
        assertThat(result.stderr(), containsString("FAILURE"));
    }

    @Test
    public void printsSuccessfulMatchesToStdout() throws Exception {
        String model = Paths.get(getClass().getResource("valid-model.smithy").toURI()).toString();
        CliUtils.Result result = CliUtils.runSmithy("select", "--selector", "string", model);

        assertThat(result.code(), equalTo(0));
        // This string shape should have matched.
        assertThat(result.stdout(), containsString("smithy.example#FooId"));
        // Check that other shapes were not included.
        assertThat(result.stdout(), not(containsString("smithy.example#GetFooOutput")));
    }

    @Test
    public void printsJsonVarsToStdout() throws Exception {
        String model = Paths.get(getClass().getResource("valid-model.smithy").toURI()).toString();
        CliUtils.Result result = CliUtils.runSmithy("select", "--selector", "string $referenceMe(<)",
                                                    "--show-vars", model);

        assertThat(result.code(), equalTo(0));
        validateSelectorOutput(result.stdout());
    }

    @Test
    public void printsJsonVarsToStdoutWithDeprecatedVars() throws Exception {
        String model = Paths.get(getClass().getResource("valid-model.smithy").toURI()).toString();
        CliUtils.Result result = CliUtils.runSmithy("select", "--selector", "string $referenceMe(<)", "--vars", model);

        assertThat(result.code(), equalTo(0));
        assertThat(result.stderr(), containsString("--vars is deprecated"));
        validateSelectorOutput(result.stdout());
    }

    @Test
    public void readsSelectorFromStdinToo() throws Exception {
        String model = Paths.get(getClass().getResource("valid-model.smithy").toURI()).toString();
        InputStream in = System.in;

        try {
            // Send the selector through input stream.
            System.setIn(new ByteArrayInputStream("string $referenceMe(<)".getBytes()));
            CliUtils.Result result = CliUtils.runSmithy("select", "--show-vars", model);

            assertThat(result.code(), equalTo(0));
            validateSelectorOutput(result.stdout());
        } finally {
            // Restore stdout and stdin.
            System.setIn(in);
        }
    }

    private void validateSelectorOutput(String output) {
        // The output must be valid JSON.
        Node node = Node.parse(output);

        // Validate the contents.
        ArrayNode array = node.expectArrayNode();
        for (Node element : array.getElements()) {
            ObjectNode object = element.expectObjectNode();
            object.expectStringMember("shape");
            ObjectNode vars = object.expectObjectMember("vars");
            // Each variable is an array of shape IDs.
            for (Node reference : vars.expectArrayMember("referenceMe").getElements()) {
                reference.expectStringNode().expectShapeId();
            }
        }
    }
}
