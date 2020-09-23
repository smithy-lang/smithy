package software.amazon.smithy.cli.commands;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Paths;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.SmithyCli;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

public class SelectCommandTest {
    @Test
    public void hasSelectCommand() throws Exception {
        PrintStream out = System.out;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        System.setOut(printStream);
        SmithyCli.create().run("select", "--help");
        System.setOut(out);
        String help = outputStream.toString("UTF-8");

        assertThat(help, containsString("Queries"));
    }

    @Test
    public void dumpsOutValidationErrorsAndFails() throws Exception {
        PrintStream out = System.out;
        PrintStream err = System.err;

        ByteArrayOutputStream errStream = new ByteArrayOutputStream();
        PrintStream errPrintStream = new PrintStream(errStream);
        System.setErr(errPrintStream);

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        PrintStream outPrintStream = new PrintStream(outStream);
        System.setOut(outPrintStream);

        CliError e = Assertions.assertThrows(CliError.class, () -> {
            String model = Paths.get(getClass().getResource("unknown-trait.smithy").toURI()).toString();
            SmithyCli.create().run("select", "--selector", "string", model);
        });

        System.setOut(out);
        System.setErr(err);

        String outputString = outStream.toString("UTF-8");
        String errorString = errStream.toString("UTF-8");

        // STDERR has the validation events.
        assertThat(errorString, containsString("Unable to resolve trait"));

        // STDOUT has the fatal error message
        assertThat(outputString, containsString("The model is invalid"));
        assertThat(e.getMessage(), containsString("The model is invalid"));
    }

    @Test
    public void printsSuccessfulMatchesToStdout() throws Exception {
        String model = Paths.get(getClass().getResource("valid-model.smithy").toURI()).toString();

        PrintStream out = System.out;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        System.setOut(printStream);
        SmithyCli.create().run("select", "--selector", "string", model);
        System.setOut(out);
        String output = outputStream.toString("UTF-8");

        // This string shape should have matched.
        assertThat(output, containsString("smithy.example#FooId"));
        // Check that other shapes were not included.
        assertThat(output, not(containsString("smithy.example#GetFooOutput")));
    }

    @Test
    public void printsJsonVarsToStdout() throws Exception {
        String model = Paths.get(getClass().getResource("valid-model.smithy").toURI()).toString();

        // Take over stdout.
        PrintStream out = System.out;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        System.setOut(printStream);

        try {
            SmithyCli.create().run("select", "--selector", "string $referenceMe(<)", "--vars", model);
        } finally {
            System.setOut(out);
        }

        validateSelectorOutput(outputStream.toString("UTF-8"));
    }

    @Test
    public void readsSelectorFromStdinToo() throws Exception {
        String model = Paths.get(getClass().getResource("valid-model.smithy").toURI()).toString();

        // Send the selector through input stream.
        InputStream in = System.in;
        System.setIn(new ByteArrayInputStream("string $referenceMe(<)".getBytes()));

        // Take over stdout.
        PrintStream out = System.out;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        System.setOut(printStream);

        try {
            // Note that --selector is omitted.
            SmithyCli.create().run("select", "--vars", model);
        } finally {
            // Restore stdout and stdin.
            System.setIn(in);
            System.setOut(out);
        }

        validateSelectorOutput(outputStream.toString("UTF-8"));
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
