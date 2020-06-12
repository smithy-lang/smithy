/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.node;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceLocation;

public class ObjectNodeTest {

    private static final ObjectNode EXPECTATION_NODE = Node.objectNodeBuilder()
            .withMember("array", Node.arrayNode())
            .withMember("boolean", Node.from(true))
            .withMember("number", Node.from(10))
            .withMember("null", Node.nullNode())
            .withMember("string", Node.from("hi"))
            .withMember("object", Node.objectNode())
            .build();

    @Test
    public void emptyNodeIsEmpty() {
        ObjectNode node = Node.objectNode();

        assertTrue(node.isEmpty());
        assertEquals(0, node.size());
        assertEquals(SourceLocation.none(), node.getSourceLocation());
    }

    @Test
    public void getType() {
        ObjectNode node = Node.objectNode();

        assertEquals(NodeType.valueOf("OBJECT"), node.getType());
        assertEquals(NodeType.OBJECT, node.getType());
        assertEquals("object", node.getType().toString());
    }

    @Test
    public void getMembers() {
        HashMap<StringNode, Node> members = new HashMap<>();
        members.put(Node.from("name"), Node.from("John Doe"));
        ObjectNode node = Node.objectNode(members);

        assertEquals(members, node.getMembers());
    }

    @Test
    public void getMember() {
        StringNode strNode = Node.from("John Doe");
        HashMap<StringNode, Node> members = new HashMap<>();
        members.put(Node.from("name"), strNode);
        ObjectNode node = Node.objectNode(members);

        assertEquals(strNode, node.getMember("name").get());
        assertFalse(node.getMember("age").isPresent());
    }

    @Test
    public void containsMember() {
        ObjectNode node = ObjectNode.objectNodeBuilder()
                .withMember("foo", "bar")
                .withMember("baz", true)
                .withMember("bam", false)
                .build();

        assertTrue(node.containsMember("foo"));
        assertTrue(node.containsMember("baz"));
        assertTrue(node.containsMember("bam"));
    }

    @Test
    public void membersAreOrdered() {
        ObjectNode node = ObjectNode.objectNodeBuilder()
                .withMember("foo", "bar")
                .withMember("baz", true)
                .withMember("bam", false)
                .build();

        assertThat(node.getMembers().values(),
                   contains(node.expectMember("foo"), node.expectMember("baz"), node.expectBooleanMember("bam")));
        assertThat(node.getStringMap().keySet(), contains("foo", "baz", "bam"));
    }

    @Test
    public void getMemberByType() {
        ObjectNode node = ObjectNode.objectNodeBuilder()
                .withMember("string", "bar")
                .withMember("boolean", true)
                .withMember("number", 10)
                .build();

        assertThat(node.getStringMemberOrDefault("string", "hi"), equalTo("bar"));
        assertThat(node.getStringMemberOrDefault("not-there", "hi"), equalTo("hi"));

        assertThat(node.getBooleanMemberOrDefault("boolean", false), equalTo(true));
        assertThat(node.getBooleanMemberOrDefault("boolean"), equalTo(true));
        assertThat(node.getBooleanMemberOrDefault("not-there", false), equalTo(false));
        assertThat(node.getBooleanMemberOrDefault("not-there"), equalTo(false));

        assertThat(node.getNumberMemberOrDefault("number", 1), equalTo(10));
        assertThat(node.getNumberMemberOrDefault("not-there", 10), equalTo(10));
    }

    @Test
    public void getMembersByPrefix() {
        ObjectNode node = ObjectNode.objectNodeBuilder()
                .withMember("a.1", 1)
                .withMember("a.2", 2)
                .withMember("bee", 3)
                .build();

        assertThat(node.getMembersByPrefix("a.").keySet(), containsInAnyOrder("a.1", "a.2"));
        assertThat(node.getMembersByPrefix("b").keySet(), containsInAnyOrder("bee"));
        assertThat(node.getMembersByPrefix("bee").keySet(), containsInAnyOrder("bee"));
    }

    @Test
    public void expectMemberGetsNodeByString() {
        StringNode keyNode = Node.from("abc");
        StringNode valueNode = Node.from("xyz");
        HashMap<StringNode, Node> members = new HashMap<>();
        members.put(keyNode, valueNode);
        ObjectNode node = Node.objectNode(members);

        assertEquals(valueNode, node.expectMember(keyNode.getValue()));
    }

    @Test
    public void expectMemberThrowsOnMissingKey() {
        Assertions.assertThrows(ExpectationNotMetException.class, () -> Node.objectNode().expectMember("MissingKey"));
    }

    @Test
    public void withMemberAddsMember() {
        SourceLocation sourceLocation = new SourceLocation("filename", 0, 1);
        ObjectNode node = new ObjectNode(new HashMap<>(), sourceLocation);
        node = node.withMember(Node.from("key"), Node.from("value"));

        assertEquals(1, node.size());
        assertTrue(node.getMember("key").isPresent());
        assertSame(sourceLocation, node.getSourceLocation());
    }

    @Test
    public void withoutMemberCanReturnSelf() {
        ObjectNode node1 = new ObjectNode(new HashMap<>(), SourceLocation.none());
        ObjectNode node2 = node1.withoutMember("key-1");

        assertSame(node1, node2);
    }

    @Test
    public void withMemberAsStringAddsMember() {
        ObjectNode node = Node.objectNode().withMember("foo", Node.from("bar"));

        assertEquals(1, node.size());
        assertTrue(node.getMember("foo").isPresent());
    }

    @Test
    public void withoutMemberRemovesMember() {
        SourceLocation sourceLocation = new SourceLocation("filename", 0, 1);
        ObjectNode node = new ObjectNode(new HashMap<>(), sourceLocation);
        node = node
                .withMember(Node.from("key-1"), Node.from("value-1"))
                .withMember(Node.from("key-2"), Node.from("value-2"))
                .withoutMember("key-1");

        assertEquals(1, node.size());
        assertFalse(node.getMember("key-1").isPresent());
        assertTrue(node.getMember("key-2").isPresent());
        assertSame(sourceLocation, node.getSourceLocation());
    }

    @Test
    public void hasSize() {
        StringNode strNode = Node.from("abc");
        HashMap<StringNode, Node> members = new HashMap<>();
        members.put(Node.from("foo"), strNode);
        members.put(Node.from("bar"), strNode);
        members.put(Node.from("baz"), strNode);
        ObjectNode node = Node.objectNode(members);

        assertEquals(3, node.size());
    }

    @Test
    public void isObjectNode() {
        ObjectNode node = Node.objectNode();

        assertEquals(true, node.isObjectNode());
        assertEquals(false, node.isArrayNode());
        assertEquals(false, node.isStringNode());
        assertEquals(false, node.isNumberNode());
        assertEquals(false, node.isBooleanNode());
        assertEquals(false, node.isNullNode());
    }

    @Test
    public void expectObjectNodeReturnsObjectNode() {
        Node node = Node.objectNode();

        assertThat(node.expectObjectNode(), instanceOf(ObjectNode.class));
    }

    @Test
    public void expectObjectNodeAcceptsErrorMessage() {
        Node node = Node.objectNode();

        assertSame(node, node.expectObjectNode("does not raise"));
    }

    @Test
    public void getSourceLocation() {
        SourceLocation loc = new SourceLocation("filename", 1, 10);
        ObjectNode node = new ObjectNode(Collections.emptyMap(), loc);

        assertSame(loc, node.getSourceLocation());
    }

    @Test
    public void checksIfAdditionalPropertiesArePresent() {
        ObjectNode node = new ObjectNode(new HashMap<>(), SourceLocation.none());
        node.withMember(Node.from("a"), Node.from("value-1"))
                .withMember(Node.from("b"), Node.from("value-2"))
                .expectNoAdditionalProperties(Arrays.asList("a", "b", "c"));
    }

    @Test
    public void throwsWhenAdditionalPropertiesAreFound() {
        Throwable thrown = Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            ObjectNode node = new ObjectNode(new HashMap<>(), SourceLocation.none());
            node.withMember(Node.from("a"), Node.from("value-1"))
                    .withMember(Node.from("b"), Node.from("value-2"))
                    .expectNoAdditionalProperties(Arrays.asList("foo", "baz"));
        });

        assertThat(thrown.getMessage(), containsString(
                "Expected an object with possible properties of `baz`, `foo`, but found "
                + "additional properties: `a`, `b`"));
    }

    @Test
    public void doesNotThrowForAdditionalPropertiesWarning() {
        ObjectNode node = new ObjectNode(new HashMap<>(), SourceLocation.none());
        node.withMember(Node.from("a"), Node.from("value-1"))
                .withMember(Node.from("b"), Node.from("value-2"))
                .warnIfAdditionalProperties(Arrays.asList("foo", "baz"));
    }

    @Test
    public void equalityAndHashCodeTest() {
        StringNode a = Node.from("a");
        StringNode b = Node.from("a");
        StringNode c = Node.from("b");
        BooleanNode d = Node.from(false);
        ObjectNode o1 = Node.objectNode().withMember("a", a).withMember("b", b);
        ObjectNode o2 = Node.objectNode().withMember("a", a).withMember("b", b);
        ObjectNode o3 = Node.objectNode().withMember("not_a", a).withMember("b", b);

        assertTrue(o1.equals(o2));
        assertTrue(o1.equals(o1));
        assertFalse(o1.equals(o3));
        assertFalse(o1.equals(a));
        assertTrue(o1.hashCode() == o2.hashCode());
        assertFalse(o1.hashCode() == o3.hashCode());
    }

    @Test
    public void convertsToObjectNode() {
        assertTrue(Node.objectNode().asObjectNode().isPresent());
    }

    @Test
    public void mergesTwoObjects() {
        ObjectNode a = Node.objectNode().withMember("a", Node.from("a")).withMember("b", Node.from("b"));
        ObjectNode b = Node.objectNode().withMember("c", Node.from("c")).withMember("d", Node.from("d"));
        ObjectNode result = a.merge(b);

        assertThat(result.size(), is(4));
        assertThat(result.getMember("a"), equalTo(Optional.of(Node.from("a"))));
        assertThat(result.getMember("b"), equalTo(Optional.of(Node.from("b"))));
        assertThat(result.getMember("c"), equalTo(Optional.of(Node.from("c"))));
        assertThat(result.getMember("d"), equalTo(Optional.of(Node.from("d"))));
    }

    @Test
    public void collectsIntoObjectNode() {
        List<String> values = Arrays.asList("a", "b", "c");
        ObjectNode result = values.stream().collect(ObjectNode.collectStringKeys(Function.identity(), Node::from));

        assertThat(result.size(), is(3));
        assertThat(result.getMember("a"), equalTo(Optional.of(Node.from("a"))));
        assertThat(result.getMember("b"), equalTo(Optional.of(Node.from("b"))));
        assertThat(result.getMember("c"), equalTo(Optional.of(Node.from("c"))));
    }

    @Test
    public void expectsArrayMember() {
        EXPECTATION_NODE.expectArrayMember("array");
        Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            EXPECTATION_NODE.expectArrayMember("object");
        });
    }

    @Test
    public void expectsBooleanMember() {
        EXPECTATION_NODE.expectBooleanMember("boolean");
        Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            EXPECTATION_NODE.expectBooleanMember("object");
        });
    }

    @Test
    public void expectsNumberMember() {
        EXPECTATION_NODE.expectNumberMember("number");
        Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            EXPECTATION_NODE.expectNumberMember("object");
        });
    }

    @Test
    public void expectsNullMember() {
        EXPECTATION_NODE.expectNullMember("null");
        Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            EXPECTATION_NODE.expectNullMember("object");
        });
    }

    @Test
    public void expectsObjectMember() {
        EXPECTATION_NODE.expectObjectMember("object");
        Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            EXPECTATION_NODE.expectObjectMember("string");
        });
    }

    @Test
    public void expectsStringMember() {
        EXPECTATION_NODE.expectStringMember("string");
        Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            EXPECTATION_NODE.expectStringMember("object");
        });
    }
}
