package software.amazon.smithy.cli.commands;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
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

        String dir = getClass().getResource("valid.jar").getPath();
        SmithyCli.create().run("ast", "--debug", "--discover-classpath", dir);
        System.setOut(out);

        String output = outputStream.toString("UTF-8");
        assertThat(output, containsString("{"));
    }

    @Test
    public void usesModelDiscoveryWithCustomInvalidClasspath() {
        CliError e = Assertions.assertThrows(CliError.class, () -> {
            String dir = getClass().getResource("invalid.jar").getPath();
            SmithyCli.create().run("ast", "--debug", "--discover-classpath", dir);
        });

        assertThat(e.getMessage(), containsString("1 ERROR(s)"));
    }

    @Test
    public void failsOnUnknownTrait() {
        CliError e = Assertions.assertThrows(CliError.class, () -> {
            String model = getClass().getResource("unknown-trait.smithy").getPath();
            SmithyCli.create().run("ast", model);
        });

        assertThat(e.getMessage(), containsString("1 ERROR(s)"));
    }

    @Test
    public void allowsUnknownTrait() {
        String model = getClass().getResource("unknown-trait.smithy").getPath();
        SmithyCli.create().run("ast", "--allow-unknown-traits", model);
    }
}
