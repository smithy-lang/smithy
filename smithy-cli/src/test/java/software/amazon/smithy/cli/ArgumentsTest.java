package software.amazon.smithy.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ArgumentsTest {
    @Test
    public void canShiftArgumentsManually() {
        Arguments arguments = new Arguments(new String[]{"a", "--b", "c"});

        assertThat(arguments.peek(), equalTo("a"));
        assertThat(arguments.hasNext(), is(true));
        assertThat(arguments.shift(), equalTo("a"));
        assertThat(arguments.peek(), equalTo("--b"));
        assertThat(arguments.hasNext(), is(true));
        assertThat(arguments.shift(), equalTo("--b"));
        assertThat(arguments.peek(), equalTo("c"));
        assertThat(arguments.hasNext(), is(true));
        assertThat(arguments.shift(), equalTo("c"));
        assertThat(arguments.hasNext(), is(false));
        assertThat(arguments.shift(), nullValue());
        assertThat(arguments.peek(), nullValue());
    }

    @Test
    public void canShiftArgumentsThatRequireValue() {
        Arguments arguments = new Arguments(new String[]{"--a", "1"});

        assertThat(arguments.shift(), equalTo("--a"));
        assertThat(arguments.shiftFor("--a"), equalTo("1"));
    }

    @Test
    public void throwsWhenExpectedValueNotPresent() {
        Arguments arguments = new Arguments(new String[]{});

        Assertions.assertThrows(CliError.class, () -> arguments.shiftFor("--a"));
    }

    @Test
    public void allArgumentsMustHaveReceiver() {
        Arguments arguments = new Arguments(new String[]{"--a", "--b", "--c"});

        Assertions.assertThrows(CliError.class, arguments::getPositional);
    }

    @Test
    public void evenSingleHyphenArgumentsMustHaveReceiver() {
        Arguments arguments = new Arguments(new String[]{"-h"});

        Assertions.assertThrows(CliError.class, arguments::getPositional);
    }

    @Test
    public void receiversReceiveArguments() {
        Arguments arguments = new Arguments(new String[]{"--a", "1", "--b", "--c", "2", "foo", "bar"});
        Map<String, String> received = new LinkedHashMap<>();

        arguments.addReceiver(new ArgumentReceiver() {
            @Override
            public boolean testOption(String name) {
                if (name.equals("--b")) {
                    received.put("--b", null);
                    return true;
                }
                return false;
            }

            @Override
            public Consumer<String> testParameter(String name) {
                if (name.equals("--a")) {
                    return value -> received.put(name, value);
                }
                if (name.equals("--c")) {
                    return value -> received.put(name, value);
                }
                return null;
            }
        });

        assertThat(arguments.getPositional(), contains("foo", "bar"));
        assertThat(received.keySet(), contains("--a", "--b", "--c"));
        assertThat(received.values(), contains("1", null, "2"));
    }

    @Test
    public void emitsOnCompleteEvents() {
        Arguments arguments = new Arguments(new String[]{"foo", "bar"});
        List<String> received = new ArrayList<>();

        arguments.onComplete((args, positional) -> {
            received.addAll(positional);
        });

        assertThat(arguments.getPositional(), contains("foo", "bar"));
        assertThat(received, contains("foo", "bar"));
    }

    @Test
    public void considersDoubleHyphenBeginningOfPositionalArgs() {
        Arguments arguments = new Arguments(new String[]{"--", "--bar"});

        assertThat(arguments.hasNext(), is(true));
        // Inherently skips "--" because it's handled by Arguments.
        assertThat(arguments.peek(), equalTo("--bar"));
        assertThat(arguments.getPositional(), contains("--bar"));
    }

    @Test
    public void canGetReceiversByClass() {
        StandardOptions options = new StandardOptions();
        Arguments arguments = new Arguments(new String[]{"--help"});
        arguments.addReceiver(options);

        arguments.getPositional();
        assertThat(arguments.getReceiver(StandardOptions.class), sameInstance(options));
        assertThat(arguments.getReceiver(StandardOptions.class).help(), is(true));
    }
}
