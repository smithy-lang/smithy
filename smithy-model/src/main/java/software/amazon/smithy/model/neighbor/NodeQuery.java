/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.model.neighbor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
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
        queries.add(ListUtils::of);
        return this;
    }

    NodeQuery member(String name) {
        queries.add(node -> {
            if (node == null || !node.isObjectNode()) {
                return ListUtils.of();
            }
            return node.expectObjectNode().getMember(name).map(ListUtils::of).orElse(ListUtils.of());
        });
        return this;
    }

    NodeQuery anyMember() {
        queries.add(node -> {
            if (node == null || !node.isObjectNode()) {
                return ListUtils.of();
            }
            return ListUtils.copyOf(node.expectObjectNode().getMembers().values());
        });
        return this;
    }

    NodeQuery anyElement() {
        queries.add(node -> {
            if (node == null || !node.isArrayNode()) {
                return ListUtils.of();
            }
            return node.expectArrayNode().getElements();
        });
        return this;
    }

    List<Node> execute(Node node) {
        if (queries.isEmpty()) {
            return ListUtils.of();
        }

        List<Node> previousResult = ListUtils.of(node);
        for (Query query : queries) {
            previousResult = previousResult.stream().flatMap(n -> query.run(n).stream()).collect(Collectors.toList());
        }
        return previousResult;
    }

    @FunctionalInterface
    interface Query {
        List<Node> run(Node node);
    }
}
