package software.amazon.smithy.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import org.junit.jupiter.api.Test;

public class HelpPrinterTest {
    @Test
    public void worksWithNoArguments() {
        BufferPrinter printer = new BufferPrinter();
        HelpPrinter help = new HelpPrinter("foo");
        help.print(AnsiColorFormatter.NO_COLOR, printer);

        assertThat(printer.toString(), containsString("Usage: foo"));
    }

    @Test
    public void worksWithJustOneOption() {
        BufferPrinter printer = new BufferPrinter();
        HelpPrinter help = new HelpPrinter("foo");
        help.option("--foo", "-f", "The foo value");
        help.print(AnsiColorFormatter.NO_COLOR, printer);

        assertThat(printer.toString(), startsWith(
                "Usage: foo [--foo | -f] \n"
                + "\n"
                + "    --foo, -f\n"
                + "        The foo value"));
    }

    @Test
    public void worksWithMultipleOptions() {
        BufferPrinter printer = new BufferPrinter();
        HelpPrinter help = new HelpPrinter("foo");
        help.option("--foo", "-f", "The foo value");
        help.option("--bar", null, "The bar value");
        help.option(null, "-b", "What is this");
        help.print(AnsiColorFormatter.NO_COLOR, printer);

        assertThat(printer.toString(), startsWith(
                "Usage: foo [--foo | -f] [--bar] [-b] \n"
                + "\n"
                + "    --foo, -f\n"
                + "        The foo value\n"
                + "    --bar\n"
                + "        The bar value\n"
                + "    -b\n"
                + "        What is this"));
    }

    @Test
    public void worksWithMultipleOptionsAndParams() {
        BufferPrinter printer = new BufferPrinter();
        HelpPrinter help = new HelpPrinter("foo");
        help.option("--foo", "-f", "The foo value");
        help.option("--bar", null, "The bar value");
        help.option(null, "-b", "What is this");
        help.param("--baz", "-a", "BAZ", "The baz param");
        help.print(AnsiColorFormatter.NO_COLOR, printer);

        assertThat(printer.toString(), startsWith(
                "Usage: foo [--foo | -f] [--bar] [-b] [--baz | -a BAZ] \n"
                + "\n"
                + "    --foo, -f\n"
                + "        The foo value\n"
                + "    --bar\n"
                + "        The bar value\n"
                + "    -b\n"
                + "        What is this\n"
                + "    --baz, -a BAZ\n"
                + "        The baz param"));
    }

    @Test
    public void includesPositionalArguments() {
        BufferPrinter printer = new BufferPrinter();
        HelpPrinter help = new HelpPrinter("foo");
        help.option("--foo", "-f", "The foo value");
        help.positional("FILE...", "Files to add");
        help.option("--bar", null, "The bar value");
        help.option(null, "-b", "What is this");
        help.param("--baz", "-a", "BAZ", "The baz param");
        help.print(AnsiColorFormatter.NO_COLOR, printer);

        assertThat(printer.toString(), startsWith(
                "Usage: foo [--foo | -f] [--bar] [-b] [--baz | -a BAZ] [FILE...]\n"
                + "\n"
                + "    --foo, -f\n"
                + "        The foo value\n"
                + "    --bar\n"
                + "        The bar value\n"
                + "    -b\n"
                + "        What is this\n"
                + "    --baz, -a BAZ\n"
                + "        The baz param\n"
                + "    FILE...\n"
                + "        Files to add"));
    }

    @Test
    public void wrapsArgumentDocs() {
        BufferPrinter printer = new BufferPrinter();
        HelpPrinter help = new HelpPrinter("foo").maxWidth(100);
        help.option("--foo", "-f", "foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo "
                                   + "foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo "
                                   + "foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo "
                                   + "foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo.");
        help.print(AnsiColorFormatter.NO_COLOR, printer);

        assertThat(printer.toString(), startsWith(
                "Usage: foo [--foo | -f] \n"
                + "\n"
                + "    --foo, -f\n"
                + "        foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo \n"
                + "        foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo \n"
                + "        foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo \n"
                + "        foo foo foo foo foo.\n"));
    }

    @Test
    public void wrapsArgumentDocsWithDefaultWrapping() {
        BufferPrinter printer = new BufferPrinter();
        HelpPrinter help = new HelpPrinter("foo");
        help.option("--foo", "-f", "foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo "
                                   + "foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo "
                                   + "foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo "
                                   + "foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo.");
        help.print(AnsiColorFormatter.NO_COLOR, printer);

        assertThat(printer.toString(), startsWith(
                "Usage: foo [--foo | -f] \n"
                + "\n"
                + "    --foo, -f\n"
                + "        foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo \n"
                + "        foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo \n"
                + "        foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo \n"
                + "        foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo \n"
                + "        foo foo."));
    }

    @Test
    public void wrappingHandlesIndentingOnNewlinesToo() {
        BufferPrinter printer = new BufferPrinter();
        HelpPrinter help = new HelpPrinter("foo").maxWidth(100);
        help.option("--foo", "-f", "foo foo foo foo foo foo foo foo foo foo\nfoo foo foo foo foo foo foo foo foo foo "
                                   + "foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo "
                                   + "foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo "
                                   + "foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo.");
        help.print(AnsiColorFormatter.NO_COLOR, printer);

        assertThat(printer.toString(), startsWith(
                "Usage: foo [--foo | -f] \n"
                + "\n"
                + "    --foo, -f\n"
                + "        foo foo foo foo foo foo foo foo foo foo\n"
                + "        foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo \n"
                + "        foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo \n"
                + "        foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo foo."));
    }

    @Test
    public void includesWrappedSummaryAndDocumentation() {
        BufferPrinter printer = new BufferPrinter();
        HelpPrinter help = new HelpPrinter("foo").maxWidth(100);;
        help.option("--foo", "-f", "The foo value");
        help.summary("Hello1 Hello2 Hello3 Hello4 Hello5 Hello6 Hello7 Hello8 Hello9 Hello10 Hello11 Hello12 Hello13 "
                     + "Hello14 Hello15 Hello16 Hello17 Hello18 Hello19 Hello20 Hello21 Hello22 Hello23 Hello24 "
                     + "Hello25 Hello26 Hello27 Hello28.");
        help.documentation("Goodbye1 Goodbye2 Goodbye3 Goodbye4 Goodbye5 Goodbye6 Goodbye7 Goodbye8 Goodbye9 "
                           + "Goodbye10 Goodbye11 Goodbye12 Goodbye13 Goodbye14 Goodbye15 Goodbye16 Goodbye17 "
                           + "Goodbye18 Goodbye19 Goodbye20 Goodbye21.");
        help.print(AnsiColorFormatter.NO_COLOR, printer);

        assertThat(printer.toString().trim(), equalTo(
                "Usage: foo [--foo | -f] \n"
                + "\n"
                + "Hello1 Hello2 Hello3 Hello4 Hello5 Hello6 Hello7 Hello8 Hello9 Hello10 Hello11 Hello12 Hello13 \n"
                + "Hello14 Hello15 Hello16 Hello17 Hello18 Hello19 Hello20 Hello21 Hello22 Hello23 Hello24 Hello25 \n"
                + "Hello26 Hello27 Hello28.\n"
                + "\n"
                + "    --foo, -f\n"
                + "        The foo value\n"
                + "    \n"
                + "Goodbye1 Goodbye2 Goodbye3 Goodbye4 Goodbye5 Goodbye6 Goodbye7 Goodbye8 Goodbye9 Goodbye10 Goodbye11\n"
                + "Goodbye12 Goodbye13 Goodbye14 Goodbye15 Goodbye16 Goodbye17 Goodbye18 Goodbye19 Goodbye20 Goodbye21."));
    }

    @Test
    public void handlesCarriageReturns() {
        BufferPrinter printer = new BufferPrinter();
        HelpPrinter help = new HelpPrinter("foo");
        help.option("--foo", "-f", "The foo value");
        help.summary("Hello1\r\nHello2\r\rHello3\rHello4.\r");
        help.print(AnsiColorFormatter.NO_COLOR, printer);

        assertThat(printer.toString(), startsWith(
                "Usage: foo [--foo | -f] \n"
                + "\n"
                + "Hello1\nHello2\n\nHello3\nHello4.\n\n"
                + "\n"
                + "    --foo, -f\n"
                + "        The foo value\n"));
    }
}
