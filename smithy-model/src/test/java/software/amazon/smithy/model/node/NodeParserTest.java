/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.node;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.loader.ModelSyntaxException;

public class NodeParserTest {
    @Test
    public void parsesFalseNodes() {
        Node result = Node.parse("true");

        assertThat(result.isBooleanNode(), is(true));
        assertThat(result.expectBooleanNode().getValue(), is(true));
        assertThat(result.getSourceLocation().getLine(), is(1));
        assertThat(result.getSourceLocation().getColumn(), is(1));
    }

    @Test
    public void parsesTrueNodes() {
        Node result = Node.parse("false");

        assertThat(result.isBooleanNode(), is(true));
        assertThat(result.expectBooleanNode().getValue(), is(false));
        assertThat(result.getSourceLocation().getLine(), is(1));
        assertThat(result.getSourceLocation().getColumn(), is(1));
    }

    @Test
    public void parsesNullNodes() {
        Node result = Node.parse("null");

        assertThat(result.isNullNode(), is(true));
        assertThat(result.getSourceLocation().getLine(), is(1));
        assertThat(result.getSourceLocation().getColumn(), is(1));
    }

    @Test
    public void parsesStringNode() {
        Node result = Node.parse("\"foo\"");

        assertThat(result.isStringNode(), is(true));
        assertThat(result.expectStringNode().getValue(), equalTo("foo"));
        assertThat(result.getSourceLocation().getLine(), is(1));
        assertThat(result.getSourceLocation().getColumn(), is(1));
    }

    @Test
    public void parsesIntegerNode() {
        Node result = Node.parse("10");

        assertThat(result.isNumberNode(), is(true));
        assertThat(result.expectNumberNode().getValue(), equalTo(10L));
        assertThat(result.getSourceLocation().getLine(), is(1));
        assertThat(result.getSourceLocation().getColumn(), is(1));
    }

    @Test
    public void parsesFloatNode() {
        Node result = Node.parse("1.5");

        assertThat(result.isNumberNode(), is(true));
        assertThat(result.expectNumberNode().getValue(), equalTo(1.5));
        assertThat(result.getSourceLocation().getLine(), is(1));
        assertThat(result.getSourceLocation().getColumn(), is(1));
    }

    @Test
    public void parsesEmptyArray() {
        Node result = Node.parse("[]");

        assertThat(result.isArrayNode(), is(true));
        assertThat(result.expectArrayNode().isEmpty(), is(true));
        assertThat(result.getSourceLocation().getLine(), is(1));
        assertThat(result.getSourceLocation().getColumn(), is(1));
    }

    @Test
    public void parsesArrayOneValue() {
        Node result = Node.parse("[true]");

        assertThat(result.isArrayNode(), is(true));
        assertThat(result.expectArrayNode().isEmpty(), is(false));
        assertThat(result.expectArrayNode().get(0).get().isBooleanNode(), is(true));
        assertThat(result.expectArrayNode().get(0).get().expectBooleanNode().getValue(), is(true));
        assertThat(result.getSourceLocation().getLine(), is(1));
        assertThat(result.getSourceLocation().getColumn(), is(1));
        assertThat(result.expectArrayNode().get(0).get().getSourceLocation().getLine(), is(1));
        assertThat(result.expectArrayNode().get(0).get().getSourceLocation().getColumn(), is(2));
    }

    @Test
    public void parsesArrayMultipleValues() {
        Node result = Node.parse("[true, false, \"foo\"]");

        assertThat(result.isArrayNode(), is(true));
        assertThat(result.expectArrayNode().isEmpty(), is(false));
        assertThat(result.expectArrayNode().get(0).get().isBooleanNode(), is(true));
        assertThat(result.expectArrayNode().get(0).get().expectBooleanNode().getValue(), is(true));
        assertThat(result.expectArrayNode().get(1).get().isBooleanNode(), is(true));
        assertThat(result.expectArrayNode().get(1).get().expectBooleanNode().getValue(), is(false));
        assertThat(result.expectArrayNode().get(2).get().isStringNode(), is(true));
        assertThat(result.expectArrayNode().get(2).get().expectStringNode().getValue(), equalTo("foo"));
        assertThat(result.getSourceLocation().getLine(), is(1));
        assertThat(result.getSourceLocation().getColumn(), is(1));

        assertThat(result.expectArrayNode().get(1).get().getSourceLocation().getLine(), is(1));
        assertThat(result.expectArrayNode().get(1).get().getSourceLocation().getColumn(), is(8));
        assertThat(result.expectArrayNode().get(2).get().getSourceLocation().getColumn(), is(15));
    }

    @Test
    public void parsesEmptyObject() {
        Node result = Node.parse("{}");

        assertThat(result.isObjectNode(), is(true));
        assertThat(result.expectObjectNode().isEmpty(), is(true));
        assertThat(result.getSourceLocation().getLine(), is(1));
        assertThat(result.getSourceLocation().getColumn(), is(1));
    }

    @Test
    public void parsesObjectWithOneEntry() {
        Node result = Node.parse("{\"foo\": \"bar\"}");

        assertThat(result.isObjectNode(), is(true));
        assertThat(result.expectObjectNode().isEmpty(), is(false));
        assertThat(result.expectObjectNode().getMember("foo").isPresent(), is(true));
        assertThat(result.expectObjectNode().expectMember("foo").isStringNode(), is(true));
        assertThat(result.expectObjectNode().expectMember("foo").expectStringNode().getValue(), equalTo("bar"));

        assertThat(result.getSourceLocation().getLine(), is(1));
        assertThat(result.getSourceLocation().getColumn(), is(1));

        assertThat(result.expectObjectNode().expectMember("foo").getSourceLocation().getLine(), is(1));
        assertThat(result.expectObjectNode().expectMember("foo").getSourceLocation().getColumn(), is(9));
    }

    @Test
    public void parsesObjectWithNestedObjects() {
        Node result = Node.parse("{\"foo\": {\"bar\": {\"baz\": [1, true]}}}");

        assertThat(result.isObjectNode(), is(true));
        assertThat(result.expectObjectNode().isEmpty(), is(false));
        assertThat(result.getSourceLocation().getLine(), is(1));
        assertThat(result.getSourceLocation().getColumn(), is(1));

        assertThat(result.expectObjectNode()
                .expectMember("foo")
                .expectObjectNode()
                .expectMember("bar")
                .getSourceLocation()
                .getLine(), is(1));
        assertThat(result.expectObjectNode()
                .expectMember("foo")
                .expectObjectNode()
                .expectMember("bar")
                .getSourceLocation()
                .getColumn(), is(17));
    }

    @Test
    public void parsesJsonWithComments() {
        ObjectNode result = Node.parseJsonWithComments(
                "// Skip leading comments...\n"
                        + " { // Hello!\n"
                        + "// Foo\n"
                        + "\"foo\"// baz bar //\n"
                        + ": // bam\n"
                        + "true // hi\n"
                        + "} // there\n"
                        + "// some more?\n"
                        + "     // even more\n",
                "/path/to/file.json").expectObjectNode();

        assertThat(Node.printJson(result), equalTo("{\"foo\":true}"));
        assertThat(result.getSourceLocation().getFilename(), equalTo("/path/to/file.json"));
        assertThat(result.getSourceLocation().getLine(), equalTo(2));
        assertThat(result.getSourceLocation().getColumn(), equalTo(2));

        Map.Entry<StringNode, Node> entry = result.getMembers().entrySet().iterator().next();
        assertThat(entry.getKey().getSourceLocation().getLine(), equalTo(4));
        assertThat(entry.getKey().getSourceLocation().getColumn(), equalTo(1));
        assertThat(entry.getValue().getSourceLocation().getLine(), equalTo(6));
        assertThat(entry.getValue().getSourceLocation().getColumn(), equalTo(1));
    }

    @Test
    public void parsesCommentsToEof() {
        Node result = Node.parseJsonWithComments("{\"foo\": true}\n"
                + "// EOF");

        assertThat(Node.printJson(result), equalTo("{\"foo\":true}"));
    }

    @Test
    public void requiresNonEmptyString() {
        Assertions.assertThrows(ModelSyntaxException.class, () -> Node.parse(""));
    }

    @Test
    public void usesCorrectErrorMessage() {
        ModelSyntaxException e = Assertions.assertThrows(ModelSyntaxException.class, () -> Node.parse("{"));

        assertThat(e.getMessage(), startsWith("Error parsing JSON: "));
    }
}
