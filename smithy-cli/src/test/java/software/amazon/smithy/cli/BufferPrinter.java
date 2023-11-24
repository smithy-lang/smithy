package software.amazon.smithy.cli;

final class BufferPrinter implements CliPrinter {

    private final StringBuilder builder = new StringBuilder();

    @Override
    public CliPrinter append(char c) {
        builder.append(c);
        return this;
    }

    @Override
    public BufferPrinter println(String text) {
        return append(text + "\n");
    }

    @Override
    public CliPrinter append(CharSequence csq, int start, int end) {
        builder.append(csq, start, end);
        return this;
    }

    @Override
    public BufferPrinter append(CharSequence text) {
        synchronized (this) {
            builder.append(text);
            return this;
        }
    }

    @Override
    public String toString() {
        // normalize line endings for tests.
        return builder.toString().replace("\r\n", "\n").replace("\r", "\n");
    }
}
