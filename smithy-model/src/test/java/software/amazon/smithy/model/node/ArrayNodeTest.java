/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.node;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceLocation;

public class ArrayNodeTest {

    @Test
    public void emptyNodeIsEmpty() {
        ArrayNode node = Node.arrayNode();

        assertEquals(true, node.isEmpty());
        assertEquals(0, node.size());
        assertEquals(SourceLocation.none(), node.getSourceLocation());
    }

    @Test
    public void getType() {
        NodeType type = Node.arrayNode().getType();

        assertEquals(NodeType.ARRAY, type);
        assertEquals("array", type.toString());
    }

    @Test
    public void hasElements() {
        List<Node> elements = Arrays.asList(Node.from("abc"), Node.from("xyz"));
        ArrayNode node = new ArrayNode(elements, SourceLocation.none());

        assertEquals(elements, node.getElements());
    }

    @Test
    public void hasGet() {
        StringNode strNode = Node.from("abc");
        ArrayNode node = new ArrayNode(Arrays.asList(strNode), SourceLocation.none());

        assertTrue(node.get(0).isPresent());
        assertEquals(strNode, node.get(0).get());
        assertFalse(node.get(1).isPresent());
    }

    @Test
    public void isArrayNode() {
        ArrayNode node = Node.arrayNode();

        assertEquals(false, node.isObjectNode());
        assertEquals(true, node.isArrayNode());
        assertEquals(false, node.isStringNode());
        assertEquals(false, node.isNumberNode());
        assertEquals(false, node.isBooleanNode());
        assertEquals(false, node.isNullNode());
    }

    @Test
    public void withValue() {
        StringNode strNode1 = Node.from("abc");
        StringNode strNode2 = Node.from("abc");
        ArrayNode node = Node.arrayNode()
                .withValue(strNode1)
                .withValue(strNode2);

        assertThat(node.size(), is(2));
        assertSame(strNode1, node.get(0).get());
        assertSame(strNode2, node.get(1).get());
    }

    @Test
    public void expectArrayNodeReturnsArrayNode() {
        Node node = Node.arrayNode();

        assertThat(node.expectArrayNode(), instanceOf(ArrayNode.class));
    }

    @Test
    public void expectNullNodeAcceptsErrorMessage() {
        Node node = Node.arrayNode();

        assertSame(node, node.expectArrayNode("does not raise"));
    }

    @Test
    public void getSourceLocation() {
        SourceLocation sourceLocation = new SourceLocation("filename", 1, 10);
        ArrayNode node = new ArrayNode(Collections.emptyList(), sourceLocation);

        assertEquals(sourceLocation, node.getSourceLocation());
    }

    @Test
    public void createFromVariadic() {
        ArrayNode node = Node.fromStrings("a", "b");

        assertThat(node.getElements(), hasSize(2));
    }

    @Test
    public void equalityAndHashCodeTest() {
        ArrayNode a = Node.fromStrings("a", "b");
        ArrayNode b = Node.fromStrings("a", "b");
        ArrayNode c = Node.fromNodes(Node.from(false));
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
    public void convertsToArray() {
        assertTrue(Node.arrayNode().asArrayNode().isPresent());
    }

    @Test
    public void mergesNodes() {
        SourceLocation sloc = new SourceLocation("file");
        ArrayNode a = Node.fromStrings("a", "b");
        ArrayNode b = new ArrayNode(Arrays.asList(Node.from("c"), Node.from("d")), sloc);
        ArrayNode result = a.merge(b);

        assertThat(result.getSourceLocation(), equalTo(sloc));
        assertThat(result.getElements()
                .stream()
                .map(Node::expectStringNode)
                .map(StringNode::getValue)
                .collect(Collectors.toList()),
                contains("a", "b", "c", "d"));
    }

    @Test
    public void collectsValuesIntoArrayNode() {
        List<String> values = Arrays.asList("a", "b", "c");
        ArrayNode result = values.stream().map(Node::from).collect(ArrayNode.collect());

        assertThat(result.getElements(), contains(Node.from("a"), Node.from("b"), Node.from("c")));
    }

    @Test
    public void getsValuesAsClass() {
        ArrayNode array = Node.fromStrings("a", "b", "c");
        List<StringNode> list = array.getElementsAs(StringNode.class);

        assertThat(array.getElements(), equalTo(list));
    }

    @Test
    public void getsValuesUsingMapper() {
        ArrayNode array = Node.fromStrings("a", "b", "c");
        List<String> list = array.getElementsAs(StringNode::getValue);

        assertThat(array.getElements()
                .stream()
                .map(Node::expectStringNode)
                .map(StringNode::getValue)
                .collect(Collectors.toList()),
                equalTo(list));
    }

    @Test
    public void failsToGetValuesAsClass() {
        Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            Node.fromStrings("a", "b", "c").getElementsAs(NumberNode.class);
        });
    }

    @Test
    public void failsToGetValuesUsingMapper() {
        Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            Node.fromStrings("a", "b", "c").getElementsAs(BooleanNode::getValue);
        });
    }

    @Test
    public void checksBoundaries() {
        ArrayNode array = Node.arrayNode();

        assertFalse(array.get(-1).isPresent());
        assertFalse(array.get(10).isPresent());
    }
}
