package software.amazon.smithy.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AnsiColorFormatterTest {
    @Test
    public void detectsIfColorIsEnabled() {
        assertThat(AnsiColorFormatter.NO_COLOR.isColorEnabled(), is(false));
        assertThat(AnsiColorFormatter.FORCE_COLOR.isColorEnabled(), is(true));
    }

    @Test
    public void wrapsConsumerWithColor() {
        ColorFormatter formatter = AnsiColorFormatter.FORCE_COLOR;
        StringBuilder builder = new StringBuilder();

        formatter.style(builder, b -> {
            b.append("Hello");
        }, Style.RED);

        String result = builder.toString();

        assertThat(result, equalTo("\033[31mHello\033[0m"));
    }

    @Test
    public void wrapsConsumerWithColorAndClosesColorIfThrows() {
        ColorFormatter formatter = AnsiColorFormatter.FORCE_COLOR;
        StringBuilder builder = new StringBuilder();

        Assertions.assertThrows(RuntimeException.class, () -> {
            formatter.style(builder, b -> {
                b.append("Hello");
                throw new RuntimeException("A");
            }, Style.RED);
        });

        String result = builder.toString();

        assertThat(result, equalTo("\033[31mHello\033[0m"));
    }
}
