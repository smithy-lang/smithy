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

public class NullNodeTest {

    @Test
    public void nullNodeIsEmpty() {
        NullNode node = NullNode.nullNode();

        assertEquals(SourceLocation.none(), node.getSourceLocation());
    }

    @Test
    public void getType() {
        NodeType type = NullNode.nullNode().getType();

        assertEquals(NodeType.NULL, type);
        assertEquals("null", type.toString());
    }

    @Test
    public void isNullNode() {
        NullNode node = NullNode.nullNode();

        assertEquals(false, node.isObjectNode());
        assertEquals(false, node.isArrayNode());
        assertEquals(false, node.isStringNode());
        assertEquals(false, node.isNumberNode());
        assertEquals(false, node.isBooleanNode());
        assertEquals(true, node.isNullNode());
    }

    @Test
    public void expectNullNodeReturnsNullNode() {
        Node node = NullNode.nullNode();

        assertThat(node.expectNullNode(), instanceOf(NullNode.class));
    }

    @Test
    public void expectNullNodeAcceptsErrorMessage() {
        Node node = NullNode.nullNode();

        assertSame(node, node.expectNullNode("does not raise"));
    }

    @Test
    public void getSourceLocation() {
        SourceLocation loc = new SourceLocation("filename", 1, 10);
        NullNode node = new NullNode(loc);

        assertSame(loc, node.getSourceLocation());
    }

    @Test
    public void equalityAndHashCodeTest() {
        NullNode a = Node.nullNode();
        NullNode b = new NullNode(SourceLocation.none());
        BooleanNode c = Node.from(false);

        assertTrue(a.equals(b));
        assertTrue(a.equals(a));
        assertTrue(b.equals(a));
        assertFalse(a.equals(c));
        assertTrue(a.hashCode() == b.hashCode());
        assertFalse(a.hashCode() == c.hashCode());
    }

    @Test
    public void convertsToNullNode() {
        assertTrue(NullNode.nullNode().asNullNode().isPresent());
    }
}
