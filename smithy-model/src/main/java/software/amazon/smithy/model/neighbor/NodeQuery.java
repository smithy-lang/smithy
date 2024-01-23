/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.model.neighbor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.utils.ListUtils;

/**
 * Searches {@link Node}s to find matching children. Each search
 * condition is executed on the result of the previous search,
 * and the results are aggregated.
 */
final class NodeQuery {
    private static final Query SELF = Stream::of;

    private static final Query ANY_MEMBER = (node) -> {
        if (node == null || !node.isObjectNode()) {
            return Stream.empty();
        }
        return node.expectObjectNode().getMembers().values().stream();
    };

    private static final Query ANY_ELEMENT = (node) -> {
        if (node == null || !node.isArrayNode()) {
            return Stream.empty();
        }
        return node.expectArrayNode().getElements().stream();
    };

    private static final Query ANY_MEMBER_NAME = (node) -> {
        if (node == null || !node.isObjectNode()) {
            return Stream.empty();
        }
        return node.expectObjectNode().getMembers().keySet().stream();
    };

    private final List<Query> queries = new ArrayList<>();

    NodeQuery() {
    }

    NodeQuery self() {
        queries.add(SELF);
        return this;
    }

    NodeQuery member(String name) {
        queries.add(node -> {
            if (node == null || !node.isObjectNode()) {
                return Stream.empty();
            }
            return node.expectObjectNode().getMember(name).map(Stream::of).orElse(Stream.empty());
        });
        return this;
    }

    NodeQuery anyMember() {
        queries.add(ANY_MEMBER);
        return this;
    }

    NodeQuery anyElement() {
        queries.add(ANY_ELEMENT);
        return this;
    }

    NodeQuery anyMemberName() {
        queries.add(ANY_MEMBER_NAME);
        return this;
    }

    List<Node> execute(Node node) {
        if (queries.isEmpty()) {
            return ListUtils.of();
        }

        Stream<Node> previousResult = Stream.of(node);
        for (Query query : queries) {
            previousResult = previousResult.flatMap(query::run);
        }
        return previousResult.collect(Collectors.toList());
    }

    @FunctionalInterface
    interface Query {
        Stream<? extends Node> run(Node node);
    }
}