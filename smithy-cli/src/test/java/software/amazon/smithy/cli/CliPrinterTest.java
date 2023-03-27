package software.amazon.smithy.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

public class CliPrinterTest {
    @Test
    public void printsWithNewlineByDefault() {
        StringBuilder builder = new StringBuilder();
        CliPrinter printer = builder::append;
        printer.println("Hi");

        assertThat(builder.toString(), equalTo("Hi" + System.lineSeparator()));
    }
}
