/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.node;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceLocation;

public class BooleanNodeTest {

    @Test
    public void trueBooleanIsTrue() {
        BooleanNode node = Node.from(true);

        assertEquals(true, node.getValue());
    }

    @Test
    public void trueBooleanIsEmpty() {
        BooleanNode node = Node.from(true);

        assertEquals(SourceLocation.none(), node.getSourceLocation());
    }

    @Test
    public void getType() {
        NodeType type = Node.from(true).getType();

        assertEquals(NodeType.BOOLEAN, type);
        assertEquals("boolean", type.toString());
    }

    @Test
    public void isBooleanNode() {
        BooleanNode node = Node.from(true);

        assertEquals(false, node.isObjectNode());
        assertEquals(false, node.isArrayNode());
        assertEquals(false, node.isStringNode());
        assertEquals(false, node.isNumberNode());
        assertEquals(true, node.isBooleanNode());
        assertEquals(false, node.isNullNode());
    }

    @Test
    public void expectBooleanNodeReturnsBooleanNode() {
        Node trueNode = Node.from(true);
        Node falseNode = Node.from(false);

        assertThat(trueNode.expectBooleanNode(), instanceOf(BooleanNode.class));
        assertThat(falseNode.expectBooleanNode(), instanceOf(BooleanNode.class));
    }

    @Test
    public void expectBooleanNodeAcceptsString() {
        Node node = Node.from(true);

        assertSame(node, node.expectBooleanNode("does not raise"));
    }

    @Test
    public void getSourceLocation() {
        SourceLocation loc = new SourceLocation("filename", 1, 10);
        BooleanNode node = new BooleanNode(true, loc);

        assertSame(loc, node.getSourceLocation());
    }

    @Test
    public void equalityAndHashCodeTest() {
        BooleanNode a = Node.from(true);
        BooleanNode b = Node.from(true);
        BooleanNode c = Node.from(false);
        StringNode d = Node.from("test");

        assertTrue(a.equals(b));
        assertTrue(a.equals(a));
        assertTrue(b.equals(a));
        assertFalse(a.equals(c));
        assertFalse(a.equals(d));
        assertTrue(a.hashCode() == b.hashCode());
        assertFalse(a.hashCode() == c.hashCode());
    }

    @Test
    public void convertsToBoolean() {
        assertTrue(Node.from(true).asBooleanNode().isPresent());
    }
}
