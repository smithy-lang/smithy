/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.syntax.bdd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.rulesengine.analysis.HashConsGraph;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.ErrorRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;

/**
 * A reduced order binary decision diagram representation of the rules engine.
 */
public final class RulesBdd implements ToNode {

    private static final int LEAF_OFFSET = 1;

    // Array indices for BDD node structure: [condition, trueTarget, falseTarget]
    private static final int CONDITION_INDEX = 0;
    private static final int TRUE_TARGET = 1;
    private static final int FALSE_TARGET = 2;

    private final List<Condition> conditions;
    private final List<Rule> results;
    private final int[][] nodes;
    private final int root;

    public RulesBdd(List<Condition> conditions, List<Rule> results, int[][] nodes, int root) {
        this.conditions = conditions;
        this.results = results;
        this.nodes = nodes;
        this.root = root;

        if (root < 0) {
            int adjustedRoot = -(root + 1);
            if (adjustedRoot >= results.size()) {
                throw new IllegalArgumentException("Root node references out of bounds result: "
                                                   + adjustedRoot + " vs " + results.size());
            }
        } else if (root >= nodes.length) {
            throw new IllegalArgumentException("Root node references out of bounds node: " + root);
        }
    }

    /**
     * Get the root node of the diagram.
     *
     * <p>The root node may be less than zero, indicating that there are no nodes, and a result is returned
     * directly and unconditionally.
     *
     * @return the root node.
     */
    public int getRootNode() {
        return root;
    }

    /**
     * Get the list of conditions.
     *
     * @return conditions.
     */
    public List<Condition> getConditions() {
        return conditions;
    }

    /**
     * Get the nodes in the BDD.
     *
     * @return BDD nodes.
     */
    public int[][] getNodes() {
        return nodes;
    }

    /**
     * Get a node by index.
     *
     * @param idx Node index.
     * @return the node at this index.
     */
    public int[] getNode(int idx) {
        return nodes[idx];
    }

    /**
     * Get a result by index (an ErrorRule or EndpointRule).
     *
     * @param idx Index of the result.
     * @return the result at this index.
     */
    public Rule getResult(int idx) {
        // Account for how results are encoded as negative numbers in rule triples (e.g., -1 becomes result 0).
        if (idx < 0) {
            idx = decodeLeafReference(idx);
        }
        return results.get(idx);
    }

    /**
     * Get the rule results in the BDD (error and endpoint rules with no conditions).
     *
     * @return BDD rule results.
     */
    public List<Rule> getResults() {
        return results;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("RulesBdd{\n");
        s.append("    conditions:");
        for (int i = 0; i < conditions.size(); i++) {
            s.append("\n        ").append(i).append(": ").append(formatString(conditions.get(i)));
        }
        s.append("\n    results:");
        for (int i = 0; i < results.size(); i++) {
            s.append("\n        ").append(i).append(": ").append(formatString(results.get(i)));
        }
        s.append("\n    root: ").append(root);
        s.append("\n    nodes:");
        for (int i = 0; i < nodes.length; i++) {
            s.append("\n        ").append(i).append(": ").append(Arrays.toString(nodes[i]));
        }
        s.append("\n}");
        return s.toString();
    }

    private static String formatString(Object o) {
        String s = o.toString();
        if (s.contains("\n")) {
            s = s.replace("\n", "\n            ");
        }
        return s.trim();
    }

    /**
     * Create a BDD object from a Node.
     *
     * @param node Node to deserialize.
     * @return the created BDD.
     */
    public static RulesBdd fromNode(Node node) {
        return RulesBddNode.fromNode(node);
    }

    @Override
    public Node toNode() {
        return RulesBddNode.toNode(this);
    }

    // Encodes a leaf index as a negative number for BDD storage
    // Leaf indices are stored as -(index + LEAF_OFFSET) to distinguish from node indices.
    private static int encodeLeafReference(int leafIndex) {
        return -(leafIndex + LEAF_OFFSET);
    }

    private static int decodeLeafReference(int encodedRef) {
        return -encodedRef - LEAF_OFFSET;
    }

    /**
     * Create a BDD from a processed graph of tree-based rules.
     *
     * @param graph Graph to process.
     * @return the BDD result.
     */
    public static RulesBdd from(HashConsGraph graph) {
        Objects.requireNonNull(graph, "RuleGraph is null");
        List<int[]> nodesList = new ArrayList<>();
        List<Rule> results = new ArrayList<>(graph.getResults());
        int fallbackLeafIndex = getFallbackLeafIndex(graph, results);
        int root = buildBDDNode(graph.getPaths(), graph.getConditions(), 0, nodesList, fallbackLeafIndex);

        // Extract the list of conditions from wrapped RuleConditions since that's all we need in the BDD class.
        List<Condition> justConditions = new ArrayList<>(graph.getConditions().size());
        for (RulesBddCondition condition : graph.getConditions()) {
            justConditions.add(condition.getCondition());
        }

        if (root < 0) {
            // No nodes created? Return BDD with empty node array that points to a result.
            return new RulesBdd(justConditions, results, new int[0][], root);
        } else {
            // There are nodes (the norm), so reverse the node order.
            int[][] nodes = reverseNodeOrder(nodesList);
            int adjustedRoot = nodesList.size() - 1 - root;
            return new RulesBdd(justConditions, results, nodes, adjustedRoot);
        }
    }

    // Find the path, if any, that has no conditions and leads to the top-most fallback error condition.
    private static int getFallbackLeafIndex(HashConsGraph graph, List<Rule> results) {
        int fallbackLeafIndex = -1;

        for (HashConsGraph.BddPath path : graph.getPaths()) {
            if (path.getStatelessConditions().isEmpty() && path.getStatefulConditions().isEmpty()) {
                if (fallbackLeafIndex != -1) {
                    throw new IllegalStateException("Multiple paths with no conditions");
                }
                fallbackLeafIndex = path.getLeafIndex();
            }
        }

        // Create a fallback leaf if one wasn't found.
        if (fallbackLeafIndex == -1) {
            results.add(ErrorRule.builder().error("Unable to resolve an endpoint"));
            fallbackLeafIndex = results.size() - 1;
        }

        return fallbackLeafIndex;
    }

    /**
     * Recursively constructs BDD nodes by partitioning rule paths based on condition evaluation.
     *
     * @return Node index (non-negative) or encoded leaf reference (negative).
     */
    private static int buildBDDNode(
            List<HashConsGraph.BddPath> paths,
            List<RulesBddCondition> conditions,
            int conditionIndex,
            List<int[]> nodesList,
            int fallbackLeafIndex
    ) {
        // no paths left, use fallback
        if (paths.isEmpty()) {
            return encodeLeafReference(fallbackLeafIndex);
        }

        // Skip conditions that no remaining path cares about, unless they're stateful.
        conditionIndex = findNextRelevantCondition(paths, conditions, conditionIndex);

        // No more conditions to check: go to fallback
        if (conditionIndex >= conditions.size()) {
            return encodeLeafReference(paths.get(0).getLeafIndex());
        }

        // Short-circuit here if all paths lead to the same leaf and none of the conditions are stateful.
        if (canShortCircuit(paths, conditions, conditionIndex)) {
            return encodeLeafReference(paths.get(0).getLeafIndex());
        }

        return createDecisionNode(paths, conditions, conditionIndex, nodesList, fallbackLeafIndex);
    }

    private static int findNextRelevantCondition(
            List<HashConsGraph.BddPath> paths,
            List<RulesBddCondition> conditions,
            int conditionIndex
    ) {
        while (conditionIndex < conditions.size()) {
            boolean anyPathCaresAboutCondition = false;
            for (HashConsGraph.BddPath path : paths) {
                if (path.getStatelessConditions().contains(conditionIndex)
                        || path.getStatefulConditions().contains(conditionIndex)) {
                    anyPathCaresAboutCondition = true;
                    break;
                }
            }

            boolean isStateful = conditions.get(conditionIndex).isStateful();

            if (!anyPathCaresAboutCondition && !isStateful) {
                // Skip this condition - no remaining path cares and it's not stateful
                conditionIndex++;
            } else {
                break; // Found a relevant condition
            }
        }
        return conditionIndex;
    }

    private static boolean canShortCircuit(List<HashConsGraph.BddPath> paths, List<RulesBddCondition> conditions, int idx) {
        // Short circuit if all paths lead to same leaf and no stateful condition needs processing.
        return !hasStatefulConditionAtIndex(paths, conditions, idx) && allSameResultLeaf(paths);
    }

    // Detects if the given list of paths all resolve to the same result.
    private static boolean allSameResultLeaf(List<HashConsGraph.BddPath> paths) {
        if (!paths.isEmpty()) {
            int firstLeaf = paths.get(0).getLeafIndex();
            for (int pos = 1; pos < paths.size(); pos++) {
                if (paths.get(pos).getLeafIndex() != firstLeaf) {
                    return false;
                }
            }
        }

        return true; // Empty list trivially has all same result
    }

    // Check if we need to process a stateful condition even if all paths lead to same leaf
    private static boolean hasStatefulConditionAtIndex(
            List<HashConsGraph.BddPath> paths,
            List<RulesBddCondition> conditions,
            int idx
    ) {
        if (idx >= conditions.size()) {
            return false;
        } else if (!conditions.get(idx).isStateful()) {
            return false;
        }

        // Check if any path contains this stateful condition
        for (HashConsGraph.BddPath path : paths) {
            if (path.getStatefulConditions().contains(idx)) {
                return true;
            }
        }

        return false;
    }

    private static int createDecisionNode(
            List<HashConsGraph.BddPath> paths,
            List<RulesBddCondition> conditions,
            int conditionIndex,
            List<int[]> nodesList,
            int fallbackLeafIndex
    ) {
        // Split paths based on current condition
        List<HashConsGraph.BddPath> truePaths = new ArrayList<>();
        List<HashConsGraph.BddPath> falsePaths = new ArrayList<>();
        for (HashConsGraph.BddPath path : paths) {
            if (isTruePath(path, conditionIndex)) {
                truePaths.add(path);
            } else {
                falsePaths.add(path);
            }
        }

        // Special handling for stateful conditions that might be elided due to short-circuiting.
        // BDDs optimize by eliminating redundant nodes (where true/false branches are identical), and by
        // short-circuiting when all remaining paths lead to the same outcome. However, stateful conditions in our
        // rules engine must always execute for side effects and dependency ordering.
        //
        // When truePaths.isEmpty(), no remaining paths require this condition to be true, meaning a standard BDD
        // would skip it entirely. We create a pass-through node to ensure execution while maintaining the BDD
        // decision structure.
        if (truePaths.isEmpty() && conditions.get(conditionIndex).isStateful()) {
            return createPassThroughNode(paths, conditions, conditionIndex, nodesList, fallbackLeafIndex);
        }

        // Recursively build subtrees
        int trueTarget = buildBDDNode(truePaths, conditions, conditionIndex + 1, nodesList, fallbackLeafIndex);
        int falseTarget = buildBDDNode(falsePaths, conditions, conditionIndex + 1, nodesList, fallbackLeafIndex);

        // Create this node
        int currentNodeIndex = nodesList.size();
        int[] node = {conditionIndex, trueTarget, falseTarget};
        nodesList.add(node);

        return currentNodeIndex;
    }

    private static boolean isTruePath(HashConsGraph.BddPath path, int conditionIndex) {
        return path.getStatelessConditions().contains(conditionIndex) ||
               path.getStatefulConditions().contains(conditionIndex);
    }

    // Creates a pass-through decision node for stateful conditions that must execute even when no paths require
    // them to be true.
    private static int createPassThroughNode(
            List<HashConsGraph.BddPath> paths,
            List<RulesBddCondition> conditions,
            int conditionIndex,
            List<int[]> nodesList,
            int fallbackLeafIndex
    ) {
        int target = buildBDDNode(paths, conditions, conditionIndex + 1, nodesList, fallbackLeafIndex);
        int currentNodeIndex = nodesList.size();
        int[] node = {conditionIndex, target, target}; // Both true and false go to same target
        nodesList.add(node);
        return currentNodeIndex;
    }

    // --------- Node reversal methods ---------

    // Reverses the order of nodes in the BDD to make it easier to read, and more cache friendly.
    // The root node becomes index 0, and all node references are updated accordingly.
    private static int[][] reverseNodeOrder(List<int[]> originalNodes) {
        int nodeCount = originalNodes.size();
        int[][] reversedNodes = new int[nodeCount][];

        // Create mapping from old index to new index.
        // - Root node (originally last) becomes index 0
        // - Last node (originally first) becomes index `nodeCount - 1`
        int[] indexMapping = new int[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            indexMapping[i] = nodeCount - 1 - i;
        }

        // Copy nodes in reverse order and update their references
        for (int oldIndex = 0; oldIndex < nodeCount; oldIndex++) {
            int newIndex = indexMapping[oldIndex];
            int[] originalNode = originalNodes.get(oldIndex);

            int condition = originalNode[CONDITION_INDEX];
            int originalTrueTarget = originalNode[TRUE_TARGET];
            int originalFalseTarget = originalNode[FALSE_TARGET];

            // Update node references
            int newTrueTarget = updateNodeReference(originalTrueTarget, indexMapping);
            int newFalseTarget = updateNodeReference(originalFalseTarget, indexMapping);

            reversedNodes[newIndex] = new int[]{condition, newTrueTarget, newFalseTarget};
        }

        return reversedNodes;
    }

    // Updates a node reference when reordering nodes. Leaves are negative and aren't changed.
    private static int updateNodeReference(int originalRef, int[] indexMapping) {
        return originalRef < 0 ? originalRef : indexMapping[originalRef];
    }
}
