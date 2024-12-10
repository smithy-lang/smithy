/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * An {@link Appendable} that supports color provided by a {@link ColorFormatter}.
 *
 * <p>A {@code ColorBuffer} is not thread-safe and is meant to write short contiguous output text that will
 * eventually be written to other things like a {@link CliPrinter}.
 *
 * <p>When wrapping a {@link CliPrinter}, ensure you use {@link #close()} to write to the printer. Alternatively,
 * wrap the buffer in a try-with-resources block.
 */
public final class ColorBuffer implements Appendable, AutoCloseable {
    private final ColorFormatter colors;
    private final Appendable buffer;
    private final Consumer<Appendable> closer;

    private ColorBuffer(ColorFormatter colors, Appendable buffer, Consumer<Appendable> closer) {
        this.colors = Objects.requireNonNull(colors);
        this.buffer = Objects.requireNonNull(buffer);
        this.closer = Objects.requireNonNull(closer);
    }

    /**
     * Create a new ColorBuffer that directly writes to the given {@code sink}.
     *
     * <p>No additional buffering is used when buffering over an {@code Appendable}. Each call to write to the
     * buffer will write to the appendable.
     *
     * @param colors ColorFormatter used to provide colors and style.
     * @param sink   Where to write.
     * @return       Returns the created buffer.
     */
    public static ColorBuffer of(ColorFormatter colors, Appendable sink) {
        return new ColorBuffer(colors, sink, s -> {});
    }

    /**
     * Create a new ColorBuffer that stores all output to an internal buffer and only writes to the given
     * {@link CliPrinter} when {@link #close()} is called.
     *
     * @param colors ColorFormatter used to provide colors and style.
     * @param sink   Where to write.
     * @return       Returns the created buffer.
     */
    public static ColorBuffer of(ColorFormatter colors, CliPrinter sink) {
        StringBuilder buffer = new StringBuilder();
        return new ColorBuffer(colors, buffer, s -> sink.append(s.toString()));
    }

    @Override
    public String toString() {
        return buffer.toString();
    }

    @Override
    public ColorBuffer append(CharSequence csq) {
        try {
            buffer.append(csq);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    @Override
    public ColorBuffer append(CharSequence csq, int start, int end) {
        try {
            buffer.append(csq, start, end);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    @Override
    public ColorBuffer append(char c) {
        try {
            buffer.append(c);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    /**
     * Writes styled text to the builder using the CliPrinter's color settings.
     *
     * @param text   Text to write.
     * @param styles Styles to apply to the text.
     * @return Returns self.
     */
    public ColorBuffer print(String text, Style... styles) {
        colors.style(buffer, text, styles);
        return this;
    }

    /**
     * Prints a line of styled text to the buffer.
     *
     * @param text   Text to print.
     * @param styles Styles to apply.
     * @return Returns self.
     */
    public ColorBuffer println(String text, Style... styles) {
        return print(text, styles).append(System.lineSeparator());
    }

    /**
     * Writes a system-dependent new line.
     *
     * @return Returns the buffer.
     */
    public ColorBuffer println() {
        return append(System.lineSeparator());
    }

    public ColorBuffer style(Consumer<ColorBuffer> bufferConsumer, Style... styles) {
        try {
            colors.startStyle(buffer, styles);
            bufferConsumer.accept(this);
            return this;
        } finally {
            if (styles.length > 0) {
                colors.endStyle(buffer);
            }
        }
    }

    @Override
    public void close() {
        closer.accept(buffer);
    }
}
