package software.amazon.smithy.cli.commands;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.SmithyCli;

public class AstCommandTest {
    @Test
    public void hasAstCommand() throws Exception {
        PrintStream out = System.out;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        System.setOut(printStream);
        SmithyCli.create().run("ast", "--help");
        System.setOut(out);
        String help = outputStream.toString("UTF-8");

        assertThat(help, containsString("Reads"));
    }

    @Test
    public void usesModelDiscoveryWithCustomValidClasspath() throws Exception {
        PrintStream out = System.out;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        System.setOut(printStream);

        String dir = Paths.get(getClass().getResource("valid.jar").toURI()).toString();
        SmithyCli.create().run("ast", "--debug", "--discover-classpath", dir);
        System.setOut(out);

        String output = outputStream.toString("UTF-8");
        assertThat(output, containsString("{"));
    }

    @Test
    public void usesModelDiscoveryWithCustomInvalidClasspath() {
        CliError e = Assertions.assertThrows(CliError.class, () -> {
            String dir = Paths.get(getClass().getResource("invalid.jar").toURI()).toString();
            SmithyCli.create().run("ast", "--debug", "--discover-classpath", dir);
        });

        assertThat(e.getMessage(), containsString("1 ERROR(s)"));
    }

    @Test
    public void failsOnUnknownTrait() {
        CliError e = Assertions.assertThrows(CliError.class, () -> {
            String model = Paths.get(getClass().getResource("unknown-trait.smithy").toURI()).toString();
            SmithyCli.create().run("ast", model);
        });

        assertThat(e.getMessage(), containsString("1 ERROR(s)"));
    }

    @Test
    public void allowsUnknownTrait() throws URISyntaxException {
        String model = Paths.get(getClass().getResource("unknown-trait.smithy").toURI()).toString();
        SmithyCli.create().run("ast", "--allow-unknown-traits", model);
    }
}
