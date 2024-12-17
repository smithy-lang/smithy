/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.node;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.hamcrest.Matchers;
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

        assertThat(thrown.getMessage(),
                containsString(
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

    @Test
    public void failsWhenRequiredMemberMissing() {
        Node value = Node.objectNode();

        Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            value.expectObjectNode().expectMember("foo", Function.identity(), v -> {});
        });
    }

    @Test
    public void failsWhenOptionalStringMemberIsWrongType() {
        ObjectNode value = Node.objectNode().withMember("foo", Node.from(true));

        Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            value.getStringMember("foo", v -> {});
        });
    }

    @Test
    public void failsWhenRequiredBooleanMemberIsWrongType() {
        ObjectNode value = Node.objectNode().withMember("foo", Node.from("hi"));

        Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            value.expectBooleanMember("foo", v -> {});
        });
    }

    @Test
    public void failsWhenOptionalBooleanMemberIsWrongType() {
        ObjectNode value = Node.objectNode().withMember("foo", Node.from("hi"));

        Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            value.getBooleanMember("foo", v -> {});
        });
    }

    @Test
    public void failsWhenOptionalNumberMemberIsWrongType() {
        ObjectNode value = Node.objectNode().withMember("foo", Node.from("hi"));

        Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            value.getNumberMember("foo", v -> {});
        });
    }

    @Test
    public void failsWhenRequiredNumberMemberIsWrongType() {
        ObjectNode value = Node.objectNode().withMember("foo", Node.from("hi"));

        Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            value.expectNumberMember("foo", v -> {});
        });
    }

    @Test
    public void failsWhenExpectedList() {
        ObjectNode value = Node.objectNode().withMember("foo", Node.from("hi"));

        Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            value.expectArrayMember("foo", Function.identity(), v -> {});
        });
    }

    @Test
    public void failsWhenExpectedListOfString() {
        ObjectNode value = Node.objectNode().withMember("foo", Node.arrayNode(Node.from(true)));

        Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            value.expectArrayMember("foo", StringNode::getValue, v -> {});
        });
    }

    @Test
    public void successfullyConsumesTypes() {
        ObjectNode value = Node.objectNodeBuilder()
                .withMember("number1", Node.from(1))
                .withMember("number2", Node.from(2))
                .withMember("string1", Node.from("a"))
                .withMember("string2", Node.from("b"))
                .withMember("boolean1", Node.from(true))
                .withMember("boolean2", Node.from(false))
                .withMember("array1", Node.fromNodes(Node.from("a")))
                .withMember("array2", Node.fromNodes(Node.from("b")))
                .withMember("null1", Node.nullNode())
                .withMember("object1", Node.objectNode())
                .withMember("object2", Node.objectNode())
                .withMember("mapper", Node.objectNode().withMember("a", Node.from("hello")))
                .build();

        Map<String, Object> result = new HashMap<>();
        value.expectObjectNode()
                .getNumberMember("number1", v -> result.put("number1", v))
                .expectNumberMember("number2", v -> result.put("number2", v))
                .getNumberMember("number3", v -> result.put("number3", v))
                .getStringMember("string1", v -> result.put("string1", v))
                .expectStringMember("string2", v -> result.put("string2", v))
                .getStringMember("string3", v -> result.put("string3", v))
                .getBooleanMember("boolean1", v -> result.put("boolean1", v))
                .expectBooleanMember("boolean2", v -> result.put("boolean2", v))
                .getBooleanMember("boolean3", v -> result.put("boolean3", v))
                .getArrayMember("array1", StringNode::getValue, v -> result.put("array1", v))
                .expectArrayMember("array2", StringNode::getValue, v -> result.put("array2", v))
                .getArrayMember("array3", StringNode::getValue, v -> result.put("array3", v))
                .getMember("null1", Node::expectNullNode, v -> result.put("null1", null))
                .getObjectMember("object1", v -> result.put("object1", v))
                .expectObjectMember("object2", v -> result.put("object2", v))
                .expectMember("mapper", Mapper::fromNode, v -> result.put("mapper", v));

        assertThat(result.keySet(), Matchers.equalTo(value.getStringMap().keySet()));
        assertThat(result.get("number1"), Matchers.equalTo(1));
        assertThat(result.get("number2"), Matchers.equalTo(2));
        assertThat(result.get("string1"), Matchers.equalTo("a"));
        assertThat(result.get("string2"), Matchers.equalTo("b"));
        assertThat(result.get("boolean1"), Matchers.equalTo(true));
        assertThat(result.get("boolean2"), Matchers.equalTo(false));
        assertThat(result.get("array1"), Matchers.equalTo(Collections.singletonList("a")));
        assertThat(result.get("array2"), Matchers.equalTo(Collections.singletonList("b")));
        assertThat(result.get("null1"), nullValue());
        assertThat(result.get("object1"), Matchers.equalTo(Node.objectNode()));
        assertThat(result.get("object2"), Matchers.equalTo(Node.objectNode()));
        assertThat(result.get("mapper"), Matchers.instanceOf(Mapper.class));
    }

    private static final class Mapper {
        String a;

        public static Mapper fromNode(Node node) {
            Mapper mapper = new Mapper();
            node.expectObjectNode().expectStringMember("a", value -> mapper.a = value);
            return mapper;
        }
    }
}
