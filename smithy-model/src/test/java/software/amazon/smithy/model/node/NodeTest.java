/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.node;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Comparator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class NodeTest {
    @Test
    public void ExpectationExpceptionProvidesAccessToNodeSource() {
        Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            Node node = NullNode.nullNode();
            try {
                node.expectObjectNode();
            } catch (ExpectationNotMetException error) {
                assertSame(node.getSourceLocation(), error.getSourceLocation());
                throw error;
            }
        });
    }

    @Test
    public void expectObjectNodeThrowsOnWrongNodeType() {
        Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            NullNode.nullNode().expectObjectNode();
        });
    }

    @Test
    public void expectObjectNodeAcceptsAnErrorMessage() {
        Throwable thrown = Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            NullNode.nullNode().expectObjectNode("foo {type}");
        });

        assertThat(thrown.getMessage(), containsString("foo null"));
    }

    @Test
    public void expectArrayNodeThrowsOnWrongNodeType() {
        Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            NullNode.nullNode().expectArrayNode();
        });
    }

    @Test
    public void expectArrayNodeAcceptsAnErrorMessage() {
        Throwable thrown = Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            Node.objectNode().expectArrayNode("foo {type}");
        });

        assertThat(thrown.getMessage(), containsString("foo object"));
    }

    @Test
    public void expectStringNodeThrowsOnWrongNodeType() {
        Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            NullNode.nullNode().expectStringNode();
        });
    }

    @Test
    public void expectStringNodeAcceptsAnErrorMessage() {
        Throwable thrown = Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            Node.arrayNode().expectStringNode("foo {type}");
        });

        assertThat(thrown.getMessage(), containsString("foo array"));
    }

    @Test
    public void expectNumberNodeThrowsOnWrongNodeType() {
        Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            NullNode.nullNode().expectNumberNode();
        });
    }

    @Test
    public void expectNumberNodeAcceptsAnErrorMessage() {
        Throwable thrown = Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            Node.from("").expectNumberNode("foo {type}");
        });

        assertThat(thrown.getMessage(), containsString("foo string"));
    }

    @Test
    public void expectBooleanNodeThrowsOnWrongNodeType() {
        Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            NullNode.nullNode().expectBooleanNode();
        });
    }

    @Test
    public void expectBooleanNodeAcceptsAnErrorMessage() {
        Throwable thrown = Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            Node.from(0).expectBooleanNode("foo {type}");
        });

        assertThat(thrown.getMessage(), containsString("foo number"));
    }

    @Test
    public void expectNullNodeThrowsOnWrongNodeType() {
        Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            Node.from("").expectNullNode();
        });
    }

    @Test
    public void expectNullNodeAcceptsAnErrorMessage() {
        Throwable thrown = Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            Node.from(true).expectNullNode("foo {type}");
        });

        assertThat(thrown.getMessage(), containsString("foo boolean"));
    }

    @Test
    public void returnsEmptyOptionalWhenConverting() {
        Node objectNode = Node.objectNode();
        assertFalse(objectNode.asBooleanNode().isPresent());
        assertFalse(objectNode.asArrayNode().isPresent());
        assertFalse(objectNode.asNumberNode().isPresent());
        assertFalse(objectNode.asNullNode().isPresent());
        assertFalse(objectNode.asStringNode().isPresent());
        Node stringNode = Node.from("foo");
        assertFalse(stringNode.asObjectNode().isPresent());
    }

    @Test
    public void deepSortsNodes() {
        ObjectNode node = Node.parse("{\"foo\": [1, {\"baz\": 1, \"a\": 2}, true], \"bar\": false}")
                .withDeepSortedKeys()
                .expectObjectNode();

        assertThat(node.getMembers().keySet(), contains(Node.from("bar"), Node.from("foo")));
        assertThat(node.getMember("bar").get(), instanceOf(BooleanNode.class));
        assertThat(node.getMember("foo").get().expectArrayNode().get(0).get(), instanceOf(NumberNode.class));
        assertThat(node.getMember("foo").get().expectArrayNode().get(1).get().expectObjectNode().getMembers().keySet(),
                contains(Node.from("a"), Node.from("baz")));
        assertThat(node.getMember("foo").get().expectArrayNode().get(2).get(), instanceOf(BooleanNode.class));
    }

    @Test
    public void deepSortsNodesWithComparator() {
        ObjectNode node = Node.parse("{\"foo\": [1, {\"baz\": 1, \"a\": 2}, true], \"bar\": false}")
                .withDeepSortedKeys(Comparator.comparing(StringNode::getValue).reversed())
                .expectObjectNode();

        assertThat(node.getMembers().keySet(), contains(Node.from("foo"), Node.from("bar")));
        assertThat(node.getMember("foo").get().expectArrayNode().get(1).get().expectObjectNode().getMembers().keySet(),
                contains(Node.from("baz"), Node.from("a")));
    }

    @Test
    public void prettyPrintsJson() {
        assertThat(Node.prettyPrintJson(Node.parse("{\"foo\": true}")),
                equalTo(String.format("{%n    \"foo\": true%n}")));
    }

    @Test
    public void parsesJsonWithComments() {
        Node result = Node.parseJsonWithComments("//Hello!\n{}");

        assertThat(result.getType(), is(NodeType.OBJECT));
    }

    @Test
    public void ensuresNodesAreEqual() {
        Node node = Node.from(true);

        Node.assertEquals(node, node);
    }

    @Test
    public void throwsWhenNodesArentEqual() {
        ObjectNode a = Node.objectNodeBuilder().withMember("foo", "bar").withMember("baz", true).build();
        ObjectNode b = Node.objectNodeBuilder().withMember("foo", "bar").withMember("baz", false).build();

        Assertions.assertThrows(ExpectationNotMetException.class, () -> Node.assertEquals(a, b));
    }
}
