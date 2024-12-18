/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.node;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;

public class StringNodeTest {

    @Test
    public void emptyNodeIsEmpty() {
        ArrayNode node = Node.arrayNode();

        assertTrue(node.isEmpty());
        assertEquals(0, node.size());
        assertEquals(SourceLocation.none(), node.getSourceLocation());
    }

    @Test
    public void isStringType() {
        NodeType type = Node.from("").getType();

        assertEquals(NodeType.STRING, type);
        assertEquals("string", type.toString());
    }

    @Test
    public void hasValue() {
        StringNode node = Node.from("foo");

        assertEquals("foo", node.getValue());
    }

    @Test
    public void isStringNode() {
        StringNode node = Node.from("");

        assertEquals(false, node.isObjectNode());
        assertEquals(false, node.isArrayNode());
        assertEquals(true, node.isStringNode());
        assertEquals(false, node.isNumberNode());
        assertEquals(false, node.isBooleanNode());
        assertEquals(false, node.isNullNode());
    }

    @Test
    public void expectStringNodeReturnsStringNode() {
        Node node = Node.from("");

        assertThat(node.expectStringNode(), instanceOf(StringNode.class));
    }

    @Test
    public void expectStringNodeAcceptsErrorMessage() {
        Node node = Node.from("");

        assertSame(node, node.expectStringNode("does not raise"));
    }

    @Test
    public void expectValueReturnsString() {
        StringNode node = Node.from("abc");

        assertEquals("abc", node.expectOneOf("abc"));
    }

    @Test
    public void expectValueAcceptsMultiplePossibleValues() {
        StringNode node = Node.from("xyz");

        assertEquals("xyz", node.expectOneOf("abc", "mno", "xyz"));
    }

    @Test
    public void expectValueAcceptsList() {
        StringNode node = Node.from("xyz");

        assertEquals("xyz", node.expectOneOf(Arrays.asList("abc", "mno", "xyz")));
    }

    @Test
    public void expectValueThrowsOnUnexpectedValue() {
        Throwable thrown = Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            StringNode node = Node.from("hjk");
            node.expectOneOf("abc", "mno", "xyz");
        });

        assertThat(thrown.getMessage(), containsString("Expected one of `abc`, `mno`, `xyz`; got `hjk`."));
    }

    @Test
    public void hasSourceLocation() {
        SourceLocation loc = new SourceLocation("filename", 1, 10);
        StringNode node = new StringNode("foo", loc);
        assertSame(loc, node.getSourceLocation());
    }

    @Test
    public void equalityAndHashCodeTest() {
        StringNode a = Node.from("a");
        StringNode b = Node.from("a");
        StringNode c = Node.from("b");
        BooleanNode d = Node.from(false);

        assertTrue(a.equals(b));
        assertTrue(a.equals(a));
        assertTrue(b.equals(a));
        assertFalse(a.equals(c));
        assertFalse(a.equals(d));
        assertTrue(a.hashCode() == b.hashCode());
        assertFalse(a.hashCode() == c.hashCode());
    }

    @Test
    public void convertsToStringNode() {
        assertTrue(Node.from("foo").asStringNode().isPresent());
    }

    @Test
    public void parsesShapeIds() {
        ShapeId expected = ShapeId.from("foo.baz#Bar");

        assertEquals(expected, Node.from("foo.baz#Bar").expectStringNode().expectShapeId());
        assertEquals(expected, Node.from("foo.baz#Bar").expectStringNode().expectShapeId("notfoo"));
    }
}
