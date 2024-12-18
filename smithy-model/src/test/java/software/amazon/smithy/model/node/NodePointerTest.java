/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.node;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

// See https://github.com/json-patch/json-patch-tests/blob/master/tests.json
public class NodePointerTest {
    @Test
    public void mustStartWithSlash() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> NodePointer.parse("foo"));
    }

    @Test
    public void persistsOriginalValue() {
        NodePointer pointer = NodePointer.parse("/~1/foo");

        assertThat(pointer.toString(), equalTo("/~1/foo"));
        assertThat(pointer.hashCode(), equalTo("/~1/foo".hashCode()));
        assertThat(pointer, equalTo(NodePointer.parse("/~1/foo")));
    }

    @Test
    public void persistsOriginalValueWithPound() {
        NodePointer pointer = NodePointer.parse("#/~1/foo");

        assertThat(pointer.toString(), equalTo("#/~1/foo"));
        assertThat(pointer.hashCode(), equalTo("#/~1/foo".hashCode()));
        assertThat(pointer, equalTo(NodePointer.parse("#/~1/foo")));
    }

    @Test
    public void unescapesTildes() {
        NodePointer pointer = NodePointer.parse("/~0/~~11");

        assertThat(pointer.toString(), equalTo("/~0/~~11"));
        assertThat(pointer.getParts(), contains("~", "~/1"));
    }

    @Test
    public void getsNestedPointerValue() {
        NodePointer pointer = NodePointer.parse("/a/b/1/c");
        Node node = Node.parse("{\"a\":{\"b\":[0, {\"c\":true}]}}");

        assertThat(pointer.getValue(node), equalTo(Node.from(true)));
    }

    @Test
    public void getLastArrayElement() {
        NodePointer pointer = NodePointer.parse("/-");
        Node node = Node.parse("[0, 1]");

        assertThat(pointer.getValue(node), equalTo(Node.from(1)));
    }

    @Test
    public void getsRoot() {
        NodePointer pointer = NodePointer.parse("");
        Node node = Node.parse("[0, 1]");

        assertThat(pointer.getValue(node), equalTo(node));
    }

    @Test
    public void getsRootWithPound() {
        NodePointer pointer = NodePointer.parse("#");
        Node node = Node.parse("[0, 1]");

        assertThat(pointer.getValue(node), equalTo(node));
    }

    @Test
    public void slashIsNotRoot() {
        NodePointer pointer = NodePointer.parse("/");
        Node node = Node.parse("{\"\":true}");

        assertThat(pointer.getValue(node), equalTo(Node.from(true)));
    }

    @Test
    public void slashWithPoundIsNotRoot() {
        NodePointer pointer = NodePointer.parse("#/");
        Node node = Node.parse("{\"\":true}");

        assertThat(pointer.getValue(node), equalTo(Node.from(true)));
    }

    @Test
    public void getNullNodeForInvalidGetType() {
        NodePointer pointer = NodePointer.parse("/a");
        Node node = Node.parse("[1]");

        assertThat(pointer.getValue(node), equalTo(Node.nullNode()));
    }

    @Test
    public void getNullNodeForInvalidArrayIndex() {
        NodePointer pointer = NodePointer.parse("/1");
        Node node = Node.parse("[1]");

        assertThat(pointer.getValue(node), equalTo(Node.nullNode()));
    }

    @Test
    public void getNullNodeForInvalidObjectKey() {
        NodePointer pointer = NodePointer.parse("/a");
        Node node = Node.parse("{\"b\":true}");

        assertThat(pointer.getValue(node), equalTo(Node.nullNode()));
    }

    @Test
    public void getNullNodeForInvalidType() {
        NodePointer pointer = NodePointer.parse("/a");
        Node node = Node.from(true);

        assertThat(pointer.getValue(node), equalTo(Node.nullNode()));
    }

    @Test
    public void addsNestedValue() {
        NodePointer pointer = NodePointer.parse("/a/b/1/c");
        Node node = Node.parse("{\"a\":{\"b\":[0, {\"c\":true}]}}");
        Node result = pointer.addValue(node, Node.from(false));

        assertThat(result, equalTo(Node.parse("{\"a\":{\"b\":[0, {\"c\":false}]}}")));
    }

    @Test
    public void addReplacesAnExistingField() {
        NodePointer pointer = NodePointer.parse("/a");
        Node node = Node.objectNode().withMember("a", true);
        Node result = pointer.addValue(node, Node.from(false));

        assertThat(result, equalTo(Node.objectNode().withMember("a", false)));
    }

    @Test
    public void addsTo0InEmptyArray() {
        NodePointer pointer = NodePointer.parse("/0");
        Node node = Node.parse("[]");
        Node result = pointer.addValue(node, Node.from(true));

        assertThat(result, equalTo(Node.parse("[true]")));
    }

    @Test
    public void addsToEmptyObject() {
        NodePointer pointer = NodePointer.parse("/foo");
        Node node = Node.objectNode();
        Node result = pointer.addValue(node, Node.from(true));

        assertThat(result, equalTo(Node.objectNode().withMember("foo", true)));
    }

    @Test
    public void addReplacesObjectDocumentWithArrayDocument() {
        NodePointer pointer = NodePointer.parse("");
        Node node = Node.arrayNode();
        Node result = pointer.addValue(node, Node.objectNode());

        assertThat(result, equalTo(Node.objectNode()));
    }

    @Test
    public void addReplacesArrayDocumentWithObjectDocument() {
        NodePointer pointer = NodePointer.parse("");
        Node node = Node.objectNode();
        Node result = pointer.addValue(node, Node.arrayNode());

        assertThat(result, equalTo(Node.arrayNode()));
    }

    @Test
    public void addAppendsToEmptyRootArray() {
        NodePointer pointer = NodePointer.parse("/-");
        Node node = Node.arrayNode();
        Node result = pointer.addValue(node, Node.from(true));

        assertThat(result, equalTo(Node.arrayNode(Node.from(true))));
    }

    @Test
    public void addAppendsToExistingArray() {
        NodePointer pointer = NodePointer.parse("/-");
        Node node = Node.arrayNode().withValue(Node.from(true));
        Node result = pointer.addValue(node, Node.from(false));

        assertThat(result, equalTo(Node.arrayNode(Node.from(true), Node.from(false))));
    }

    @Test
    public void addAppendsToExistingArrayByIndex() {
        NodePointer pointer = NodePointer.parse("/1");
        Node node = Node.arrayNode(Node.from(1), Node.from(2));
        Node result = pointer.addValue(node, Node.from(3));

        assertThat(result, equalTo(Node.arrayNode(Node.from(1), Node.from(3), Node.from(2))));
    }

    @Test
    public void addAppendsToEndOfExistingArrayByIndexPlusOne() {
        NodePointer pointer = NodePointer.parse("/2");
        Node node = Node.arrayNode(Node.from(1), Node.from(2));
        Node result = pointer.addValue(node, Node.from(3));

        assertThat(result, equalTo(Node.arrayNode(Node.from(1), Node.from(2), Node.from(3))));
    }

    @Test
    public void addIgnoresAppendingToIndexPlus2orMore() {
        NodePointer pointer = NodePointer.parse("/3");
        Node node = Node.arrayNode(Node.from(1), Node.from(2));
        Node result = pointer.addValue(node, Node.from(3));

        assertThat(result, equalTo(node));
    }

    @Test
    public void addIgnoresAppendingToNegativeIndex() {
        NodePointer pointer = NodePointer.parse("/-1");
        Node node = Node.arrayNode(Node.from(1));
        Node result = pointer.addValue(node, Node.from(2));

        assertThat(result, equalTo(node));
    }

    @Test
    public void addIgnoresAppendingThroughMissingIndex() {
        NodePointer pointer = NodePointer.parse("/2/-1");
        Node node = Node.arrayNode(Node.from(1));
        Node result = pointer.addValue(node, Node.from(2));

        assertThat(result, equalTo(node));
    }

    @Test
    public void addIgnoresAppendingToInvalidType() {
        NodePointer pointer = NodePointer.parse("/foo");
        Node node = Node.from(true);
        Node result = pointer.addValue(node, Node.from("hi"));

        assertThat(result, equalTo(node));
    }

    @Test
    public void addIgnoresAppendingToMissingObject() {
        NodePointer pointer = NodePointer.parse("/foo/bar/baz");
        Node node = Node.parse("{\"foo\": {}}");
        Node result = pointer.addValue(node, Node.from(true));

        assertThat(result, equalTo(node));
    }

    @Test
    public void addAppendsToNestedArray() {
        NodePointer pointer = NodePointer.parse("/2/1/-");
        Node node = Node.parse("[ 1, 2, [ 3, [ 4, 5 ] ] ]");
        Node result = pointer.addValue(node, Node.parse("{ \"foo\": [ \"bar\", \"baz\" ] }"));

        assertThat(result, equalTo(Node.parse("[ 1, 2, [ 3, [ 4, 5, { \"foo\": [ \"bar\", \"baz\" ] } ] ] ]")));
    }

    @Test
    public void addCanCreateEntryForSlashTarget() {
        NodePointer pointer = NodePointer.parse("/");
        Node node = Node.objectNode();
        Node result = pointer.addValue(node, Node.from(true));

        assertThat(result, equalTo(Node.objectNode().withMember("", true)));
    }

    @Test
    public void addFooWithTrailingSlash() {
        NodePointer pointer = NodePointer.parse("/foo/");
        Node node = Node.objectNode().withMember("foo", Node.objectNode());
        Node result = pointer.addValue(node, Node.from(true));

        assertThat(result, equalTo(Node.parse("{\"foo\": {\"\": true}}")));
    }

    @Test
    public void addsCompositeValueAtTopLevel() {
        NodePointer pointer = NodePointer.parse("/bar");
        Node node = Node.objectNode().withMember("foo", 1);
        Node result = pointer.addValue(node, Node.arrayNode(Node.from(1), Node.from(2)));

        assertThat(result, equalTo(Node.parse("{\"foo\": 1, \"bar\": [1, 2]}")));
    }

    @Test
    public void addObjectOperationOnArrayTargetReturnsValueAsIs() {
        NodePointer pointer = NodePointer.parse("/foo");
        Node node = Node.arrayNode().withValue(Node.from(true));
        Node result = pointer.addValue(node, Node.from(false));

        assertThat(result, equalTo(node));
    }

    @Test
    public void addCanUseZeroAsObjectMember() {
        NodePointer pointer = NodePointer.parse("/0");
        Node node = Node.objectNode();
        Node result = pointer.addValue(node, Node.from(true));

        assertThat(result, equalTo(Node.objectNode().withMember("0", true)));
    }

    @Test
    public void addsIntermediateValues() {
        NodePointer pointer = NodePointer.parse("/a/b/c");
        Node node = Node.objectNode();
        Node result = pointer.addWithIntermediateValues(node, Node.from(true));

        assertThat(result, equalTo(Node.parse("{\"a\": {\"b\": {\"c\": true } } }")));
    }
}
