package software.amazon.smithy.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

public class ColorFormatterTest {
    @Test
    public void writesToPrinterWhenClosed() {
        BufferPrinter printer = new BufferPrinter();
        ColorFormatter formatter = AnsiColorFormatter.FORCE_COLOR;
        String expected ="abc\nHello\n\n\033[31mRed\033[0m\n\n\n0\n";

        try (ColorFormatter.PrinterBuffer buffer = formatter.printerBuffer(printer)) {
            buffer.append('a');
            buffer.append("bc");
            buffer.println();
            buffer.println("Hello");
            buffer.println();
            buffer.println("Red", Style.RED);
            buffer.println();
            buffer.println();
            buffer.append('0');
            // ensure that toString does not add the newline.
            assertThat(buffer.toString(), equalTo(expected.trim()));
        }

        assertThat(printer.toString(), equalTo(expected));
    }

    @Test
    public void appendsNewlineIfNeededInToString() {
        BufferPrinter printer = new BufferPrinter();
        ColorFormatter formatter = AnsiColorFormatter.FORCE_COLOR;
        String expected ="abc\n";

        try (ColorFormatter.PrinterBuffer buffer = formatter.printerBuffer(printer)) {
            buffer.println("abc");
            assertThat(buffer.toString(), equalTo(expected));
        }

        assertThat(printer.toString(), equalTo(expected));
    }
}
