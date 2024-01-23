/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.model.neighbor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import software.amazon.smithy.model.node.Node;

/**
 * Searches {@link Node}s to find matching children. Each search
 * condition is executed on the result of the previous search,
 * and the results are aggregated.
 */
final class NodeQuery {
    private static final Query SELF = (node, result) -> result.add(node);

    private static final Query ANY_MEMBER = (node, result) -> {
        if (node == null || !node.isObjectNode()) {
            return;
        }
        result.addAll(node.expectObjectNode().getMembers().values());
    };

    private static final Query ANY_ELEMENT = (node, result) -> {
        if (node == null || !node.isArrayNode()) {
            return;
        }
        result.addAll(node.expectArrayNode().getElements());
    };

    private static final Query ANY_MEMBER_NAME = (node, result) -> {
        if (node == null || !node.isObjectNode()) {
            return;
        }
        result.addAll(node.expectObjectNode().getMembers().keySet());
    };

    private final List<Query> queries = new ArrayList<>();

    NodeQuery() {
    }

    NodeQuery self() {
        queries.add(SELF);
        return this;
    }

    NodeQuery member(String name) {
        queries.add((node, result) -> {
            if (node == null || !node.isObjectNode()) {
                return;
            }
            node.expectObjectNode().getMember(name).ifPresent(result::add);
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

    Collection<Node> execute(Node node) {
        Queue<Node> previousResult = new ArrayDeque<>();

        if (queries.isEmpty()) {
            return previousResult;
        }

        previousResult.add(node);
        for (Query query : queries) {
            // Each time a query runs, it adds to the queue, but we only want it to
            // run on the nodes added by the previous query.
            for (int i = previousResult.size(); i > 0; i--) {
                query.run(previousResult.poll(), previousResult);
            }
        }

        return previousResult;
    }

    @FunctionalInterface
    interface Query {
        void run(Node node, Queue<Node> result);
    }
}
