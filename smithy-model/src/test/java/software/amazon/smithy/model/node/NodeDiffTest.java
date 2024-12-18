/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.node;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;

import org.junit.jupiter.api.Test;

public class NodeDiffTest {
    @Test
    public void detectsNodeTypeDifference() {
        assertThat(NodeDiff.diff(Node.nullNode(), Node.from(true)),
                contains(String.format(
                        "[]: Expected node of type `boolean` but found node of type `null`.%n%nExpected: true%n%n Found: null")));
    }

    @Test
    public void comparesBooleans() {
        assertThat(NodeDiff.diff(Node.from(true), Node.from(true)), empty());
        assertThat(NodeDiff.diff(Node.from(true), Node.from(false)),
                contains("[]: Expected `false` but found `true`"));
    }

    @Test
    public void comparesNumbers() {
        assertThat(NodeDiff.diff(Node.from(10), Node.from(10)), empty());
        assertThat(NodeDiff.diff(Node.from(2), Node.from(1)),
                contains("[]: Expected `1` but found `2`"));
    }

    @Test
    public void comparesStrings() {
        assertThat(NodeDiff.diff(Node.from("pop"), Node.from("pop")), empty());
        assertThat(NodeDiff.diff(Node.from("foo"), Node.from("bar")),
                contains("[]: Expected `\"bar\"` but found `\"foo\"`"));
    }

    @Test
    public void comparesNulls() {
        assertThat(NodeDiff.diff(Node.nullNode(), Node.nullNode()), empty());
    }

    @Test
    public void detectsExtraListElements() {
        Node actual = Node.fromStrings("snap", "crackle", "pop");
        Node expected = Node.fromStrings("snap", "crackle");

        assertThat(NodeDiff.diff(actual, expected),
                contains("[]: Extra element encountered in list at position 2: \"pop\""));
    }

    @Test
    public void detectsMissingListElements() {
        Node actual = Node.fromStrings("snap", "crackle");
        Node expected = Node.fromStrings("snap", "crackle", "pop");

        assertThat(NodeDiff.diff(actual, expected),
                contains("[]: Expected element (position 2) not encountered in list: \"pop\""));
    }

    @Test
    public void detectsDifferentListElements() {
        Node actual = Node.fromStrings("fizz", "buzz", "pop");
        Node expected = Node.fromStrings("snap", "crackle", "pop");

        assertThat(NodeDiff.diff(actual, expected),
                contains(
                        "[[0]]: Expected `\"snap\"` but found `\"fizz\"`",
                        "[[1]]: Expected `\"crackle\"` but found `\"buzz\"`"));
    }

    @Test
    public void reportsMultipleDifferenceTypesOnLists() {
        Node actual = Node.fromStrings("fizz", "buzz");
        Node expected = Node.fromStrings("snap", "crackle", "pop");

        assertThat(NodeDiff.diff(actual, expected),
                contains("[[0]]: Expected `\"snap\"` but found `\"fizz\"`",
                        "[[1]]: Expected `\"crackle\"` but found `\"buzz\"`",
                        "[]: Expected element (position 2) not encountered in list: \"pop\""));
    }

    @Test
    public void detectsExtraObjectKeys() {
        Node actual = Node.objectNode().withMember("foo", Node.from("bar")).withMember("fizz", Node.from("buzz"));
        Node expected = Node.objectNode().withMember("foo", Node.from("bar"));

        assertThat(NodeDiff.diff(actual, expected),
                contains("[]: Extra key `fizz` encountered with content: \"buzz\""));
    }

    @Test
    public void detectsMissingObjectKeys() {
        Node actual = Node.objectNode().withMember("foo", Node.from("bar"));
        Node expected = Node.objectNode().withMember("foo", Node.from("bar")).withMember("fizz", Node.from("buzz"));

        assertThat(NodeDiff.diff(actual, expected),
                contains("[]: Expected key `fizz` not present."));
    }

    @Test
    public void detectsDifferentObjectKeys() {
        Node actual = Node.objectNode().withMember("foo", Node.from("bar"));
        Node expected = Node.objectNode().withMember("foo", Node.from("baz"));

        assertThat(NodeDiff.diff(actual, expected),
                contains("[/foo]: Expected `\"baz\"` but found `\"bar\"`"));
    }

    @Test
    public void reportsMultipleDifferenceTypesOnObjects() {
        Node actual = Node.objectNode().withMember("foo", Node.from("bar")).withMember("snap", Node.from("crackle"));
        Node expected = Node.objectNode().withMember("foo", Node.from("baz")).withMember("fizz", Node.from("buzz"));

        assertThat(NodeDiff.diff(actual, expected),
                contains(
                        "[/foo]: Expected `\"baz\"` but found `\"bar\"`",
                        "[]: Expected key `fizz` not present.",
                        "[]: Extra key `snap` encountered with content: \"crackle\""));
    }

    @Test
    public void detectsDeeplyNestedDifferences() {
        Node expected = Node.objectNode()
                .withMember("foo",
                        Node.arrayNode()
                                .withValue(Node.from("bar"))
                                .withValue(
                                        Node.objectNode()
                                                .withMember("baz",
                                                        Node.arrayNode()
                                                                .withValue(Node.from("snap"))
                                                                .withValue(Node.objectNode()
                                                                        .withMember("crackle", Node.from("pop"))))));
        Node actual = Node.objectNode()
                .withMember("foo",
                        Node.arrayNode()
                                .withValue(Node.from("bar"))
                                .withValue(Node.objectNode()
                                        .withMember("baz",
                                                Node.arrayNode()
                                                        .withValue(Node.from("snap"))
                                                        .withValue(Node.objectNode()
                                                                .withMember("crackle", Node.from("quux"))))));

        assertThat(NodeDiff.diff(actual, expected),
                contains(
                        "[/foo[1]/baz[1]/crackle]: Expected `\"pop\"` but found `\"quux\"`"));
    }
}
