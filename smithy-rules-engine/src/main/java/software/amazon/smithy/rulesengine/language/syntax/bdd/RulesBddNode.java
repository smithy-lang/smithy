/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.syntax.bdd;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;

/**
 * Handles converting BDD to and from Nodes.
 */
final class RulesBddNode {

    private RulesBddNode() {}

    static Node toNode(RulesBdd bdd) {
        ObjectNode.Builder builder = Node.objectNodeBuilder();
        builder.withMember("root", bdd.getRootNode());

        ArrayNode.Builder conditionBuilder = ArrayNode.builder();
        for (Condition condition : bdd.getConditions()) {
            conditionBuilder.withValue(condition.toNode());
        }
        builder.withMember("conditions", conditionBuilder.build());

        ArrayNode.Builder resultBuilder = ArrayNode.builder();
        for (Rule result : bdd.getResults()) {
            resultBuilder.withValue(result.toNode());
        }
        builder.withMember("results", resultBuilder.build());

        if (bdd.getNodes().length > 0) {
            ArrayNode.Builder nodeBuilder = ArrayNode.builder();
            builder.withMember("nodes", nodeBuilder.build());
        }

        return Node.objectNode();
    }

    static RulesBdd fromNode(Node node) {
        ObjectNode o = node.expectObjectNode();
        int root = o.expectNumberMember("root").getValue().intValue();

        ArrayNode conditionsArray = o.expectArrayMember("conditions").expectArrayNode();
        List<Condition> conditions = new ArrayList<>(conditionsArray.size());
        for (Node value : conditionsArray.getElements()) {
            conditions.add(Condition.fromNode(value));
        }

        ArrayNode resultsArray = o.expectArrayMember("results").expectArrayNode();
        List<Rule> results = new ArrayList<>(resultsArray.size());
        for (Node value : resultsArray.getElements()) {
            results.add(Rule.fromNode(value));
        }

        ArrayNode nodesArray = o.expectArrayMember("nodes").expectArrayNode();
        int[][] nodes = new int[nodesArray.size()][];
        int row = 0;
        for (Node value : nodesArray.getElements()) {
            ArrayNode nodeArray = value.expectArrayNode();
            if (nodeArray.size() != 3) {
                throw new ExpectationNotMetException("Each node array must have three numbers", nodeArray);
            }
            int[] nodeRow = new int[3];
            for (int i = 0; i < 3; i++) {
                nodeRow[i] = nodeArray.get(i).get().expectNumberNode().getValue().intValue();
            }
            nodes[row++] = nodeRow;
        }

        return new RulesBdd(conditions, results, nodes, root);
    }
}
