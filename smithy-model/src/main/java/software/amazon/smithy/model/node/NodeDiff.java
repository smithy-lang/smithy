/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.node;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Creates a line-by-line diff of two Node values.
 */
final class NodeDiff {

    NodeDiff() {}

    static List<String> diff(ToNode actual, ToNode expected) {
        return new NodeDiff().findDifferences(actual.toNode(), expected.toNode(), "")
                .sorted()
                .collect(Collectors.toList());
    }

    private Stream<String> findDifferences(Node actual, Node expected, String prefix) {
        if (actual.equals(expected)) {
            return Stream.empty();
        }

        if (!actual.getType().equals(expected.getType())) {
            return Stream.of(String.format(
                    "[%s]: Expected node of type `%s` but found node of type `%s`.%n%nExpected: %s%n%n Found: %s",
                    prefix,
                    expected.getType(),
                    actual.getType(),
                    nodeToJson(expected, true),
                    nodeToJson(actual, true)));
        }

        switch (actual.getType()) {
            case OBJECT:
                return findDifferences(actual.expectObjectNode(), expected.expectObjectNode(), prefix);
            case ARRAY:
                return findDifferences(actual.expectArrayNode(), expected.expectArrayNode(), prefix);
            default:
                return Stream.of(String.format(
                        "[%s]: Expected `%s` but found `%s`",
                        prefix,
                        nodeToJson(expected, false),
                        nodeToJson(actual, false)));
        }
    }

    private Stream<String> findDifferences(ObjectNode actual, ObjectNode expected, String prefix) {
        List<String> differences = new ArrayList<>();
        Set<String> actualKeys = actual.getMembers()
                .keySet()
                .stream()
                .map(StringNode::getValue)
                .collect(Collectors.toSet());
        Set<String> expectedKeys = expected.getMembers()
                .keySet()
                .stream()
                .map(StringNode::getValue)
                .collect(Collectors.toSet());

        Set<String> extraKeys = new HashSet<>(actualKeys);
        extraKeys.removeAll(expectedKeys);
        for (String extraKey : extraKeys) {
            differences.add(String.format(
                    "[%s]: Extra key `%s` encountered with content: %s",
                    prefix,
                    extraKey,
                    nodeToJson(actual.expectMember(extraKey), true)));
        }

        Set<String> missingKeys = new HashSet<>(expectedKeys);
        missingKeys.removeAll(actualKeys);
        for (String missingKey : missingKeys) {
            differences.add(String.format("[%s]: Expected key `%s` not present.", prefix, missingKey));
        }

        Set<String> sharedKeys = new HashSet<>(actualKeys);
        sharedKeys.retainAll(expectedKeys);

        return Stream.concat(differences.stream(),
                sharedKeys.stream()
                        .flatMap(key -> findDifferences(
                                actual.expectMember(key),
                                expected.expectMember(key),
                                String.format("%s/%s", prefix, key.replace("^", "^^").replace("/", "^/")))));
    }

    private Stream<String> findDifferences(ArrayNode actual, ArrayNode expected, String prefix) {
        List<String> differences = new ArrayList<>();
        List<Node> actualElements = actual.getElements();
        List<Node> expectedElements = expected.getElements();

        for (int i = expectedElements.size(); i < actualElements.size(); i++) {
            differences.add(String.format(
                    "[%s]: Extra element encountered in list at position %d: %s",
                    prefix,
                    i,
                    nodeToJson(actualElements.get(i), true)));
        }

        for (int i = actualElements.size(); i < expectedElements.size(); i++) {
            differences.add(String.format(
                    "[%s]: Expected element (position %d) not encountered in list: %s",
                    prefix,
                    i,
                    nodeToJson(expectedElements.get(i), true)));
        }

        return Stream.concat(
                differences.stream(),
                IntStream.range(0, Math.min(actualElements.size(), expectedElements.size()))
                        .boxed()
                        .flatMap(i -> findDifferences(
                                actualElements.get(i),
                                expectedElements.get(i),
                                String.format("%s[%d]", prefix, i))));
    }

    private static String nodeToJson(Node node, boolean prettyPrint) {
        return prettyPrint ? Node.prettyPrintJson(node) : Node.printJson(node);
    }
}
