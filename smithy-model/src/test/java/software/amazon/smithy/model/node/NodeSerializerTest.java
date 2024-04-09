package software.amazon.smithy.model.node;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

public class NodeSerializerTest {
    @Test
    public void serializesNodes() {
        Node node = Node.parse("{\"foo\": \"bar\", \"baz\": true, \"bam\": false, \"boo\": 10}");

        assertThat(Node.printJson(node), equalTo("{\"foo\":\"bar\",\"baz\":true,\"bam\":false,\"boo\":10}"));
        assertThat(Node.prettyPrintJson(node), equalTo(String.format("{%n"
                                                       + "    \"foo\": \"bar\",%n"
                                                       + "    \"baz\": true,%n"
                                                       + "    \"bam\": false,%n"
                                                       + "    \"boo\": 10%n"
                                                       + "}")));
        assertThat(Node.prettyPrintJson(node, "\t"), equalTo(String.format("{%n"
                                                             + "\t\"foo\": \"bar\",%n"
                                                             + "\t\"baz\": true,%n"
                                                             + "\t\"bam\": false,%n"
                                                             + "\t\"boo\": 10%n"
                                                             + "}")));
    }

    @Test
    public void serializesComplexNodes() {
        Node node = Node.parse(
                "[\n"
                + "{\"foo\": \"\uD83D\uDCA9\", \"baz\": true, \"bam\": false, \"boo\": 10},\n"
                + "10,\n"
                + "true,\n"
                + "false,\n"
                + "{},\n"
                + "[],\n"
                + "\"\",\n"
                + "\" \",\n"
                + "null,\n"
                + "-1,\n"
                + "-1.0\n"
                + "]");

        assertThat(Node.printJson(node), equalTo(
                "[{\"foo\":\"\uD83D\uDCA9\",\"baz\":true,\"bam\":false,"
                + "\"boo\":10},10,true,false,{},[],\"\",\" \",null,-1,-1.0]"));
        assertThat(Node.prettyPrintJson(node), equalTo(String.format(
                "[%n"
                + "    {%n"
                + "        \"foo\": \"\uD83D\uDCA9\",%n"
                + "        \"baz\": true,%n"
                + "        \"bam\": false,%n"
                + "        \"boo\": 10%n"
                + "    },%n"
                + "    10,%n"
                + "    true,%n"
                + "    false,%n"
                + "    {},%n" // optimized empty object
                + "    [],%n" // optimized empty array
                + "    \"\",%n"
                + "    \" \",%n"
                + "    null,%n"
                + "    -1,%n"
                + "    -1.0%n"
                + "]")));
    }
}
