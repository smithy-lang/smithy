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
    private final List<Query> queries = new ArrayList<>();

    NodeQuery() {
    }

    NodeQuery self() {
        queries.add(Stream::of);
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
        queries.add(node -> {
            if (node == null || !node.isObjectNode()) {
                return Stream.empty();
            }
            return node.expectObjectNode().getMembers().values().stream();
        });
        return this;
    }

    NodeQuery anyElement() {
        queries.add(node -> {
            if (node == null || !node.isArrayNode()) {
                return Stream.empty();
            }
            return node.expectArrayNode().getElements().stream();
        });
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
        Stream<Node> run(Node node);
    }
}
