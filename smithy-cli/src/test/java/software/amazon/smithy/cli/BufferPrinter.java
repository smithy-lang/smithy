package software.amazon.smithy.cli;

final class BufferPrinter implements CliPrinter {

    private final StringBuilder builder = new StringBuilder();

    @Override
    public void println(String text) {
        print(text + "\n");
    }

    @Override
    public void print(String text) {
        synchronized (this) {
            builder.append(text);
        }
    }

    @Override
    public String toString() {
        // normalize line endings for tests.
        return builder.toString().replace("\r\n", "\n").replace("\r", "\n");
    }
}
