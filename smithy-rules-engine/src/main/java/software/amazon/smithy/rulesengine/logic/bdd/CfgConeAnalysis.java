/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.logic.cfg.Cfg;
import software.amazon.smithy.rulesengine.logic.cfg.CfgNode;
import software.amazon.smithy.rulesengine.logic.cfg.ConditionNode;
import software.amazon.smithy.rulesengine.logic.cfg.ResultNode;

/**
 * Analyzes a CFG to compute cone information for each condition.
 *
 * <p>A "cone" is the subgraph of the CFG that is reachable from a given condition node. Think of it as the
 * "downstream impact" of a condition. A condition with a large cone controls many downstream decisions (high impact).
 * A condition with many reachable result nodes in its cone affects many endpoints. Conditions that appear early in the
 * CFG (low dominator depth) are "gates" that control access to large portions of the decision tree.
 */
final class CfgConeAnalysis {
    /**
     * Dominator depth for each condition, or how many edges from CFG root to first occurrence.
     * Initialized to MAX_VALUE, then updated to minimum depth encountered during traversal.
     */
    private final int[] dominatorDepth;

    /** Number of result nodes (endpoints/errors) reachable from each condition's cone. */
    private final int[] reachableResults;

    /** Cache of computed cone information for each CFG node to avoid redundant traversals. */
    private final Map<CfgNode, ConeInfo> coneCache = new HashMap<>();

    /** Maps conditions to their indices for quick lookups. */
    private final Map<Condition, Integer> conditionToIndex;

    /**
     * Creates a new cone analysis for the given CFG and conditions.
     *
     * @param cfg the control flow graph to analyze
     * @param conditions array of conditions in the rule set
     * @param conditionToIndex mapping from conditions to their indices
     */
    public CfgConeAnalysis(Cfg cfg, Condition[] conditions, Map<Condition, Integer> conditionToIndex) {
        this.conditionToIndex = conditionToIndex;
        int n = conditions.length;
        this.dominatorDepth = new int[n];
        this.reachableResults = new int[n];
        Arrays.fill(dominatorDepth, Integer.MAX_VALUE);
        analyzeCfgNode(cfg.getRoot(), 0);
    }

    /**
     * Recursively analyzes a CFG node and its subtree, computing cone information.
     *
     * @param node the current CFG node being analyzed
     * @param depth the current depth in the CFG traversal (edges from root)
     * @return cone information for this subtree
     */
    private ConeInfo analyzeCfgNode(CfgNode node, int depth) {
        if (node == null) {
            return ConeInfo.empty();
        }

        ConeInfo cached = coneCache.get(node);
        if (cached != null) {
            if (cached.inProgress) {
                throw new IllegalStateException("Cycle detected in CFG during cone analysis: " + node);
            }
            return cached;
        }

        // Cycle guard: if a transform accidentally introduced a cycle, fail fast.
        coneCache.put(node, ConeInfo.IN_PROGRESS);
        ConeInfo info;

        if (node instanceof ResultNode) {
            info = ConeInfo.singleResult();
        } else if (node instanceof ConditionNode) {
            ConditionNode condNode = (ConditionNode) node;
            Condition condition = condNode.getCondition().getCondition();
            Integer conditionIdx = conditionToIndex.get(condition);
            if (conditionIdx == null) {
                throw new IllegalStateException("Condition not indexed in CFG: " + condition);
            }

            // Handle conditions that appear multiple times by updating dominator depth.
            // Keep the minimum depth where this condition appears.
            dominatorDepth[conditionIdx] = Math.min(dominatorDepth[conditionIdx], depth);

            ConeInfo trueBranchCone = analyzeCfgNode(condNode.getTrueBranch(), depth + 1);
            ConeInfo falseBranchCone = analyzeCfgNode(condNode.getFalseBranch(), depth + 1);
            info = ConeInfo.combine(trueBranchCone, falseBranchCone);

            // Update the maximum result count this condition can influence
            reachableResults[conditionIdx] = Math.max(reachableResults[conditionIdx], info.resultNodes);
        } else {
            throw new UnsupportedOperationException("Unknown node type: " + node);
        }

        coneCache.put(node, info);
        return info;
    }

    /**
     * Gets the dominator depth of a condition, the minimum depth at which this condition appears in the CFG.
     *
     * <p>Lower values indicate conditions that appear earlier in the decision tree and have more influence over
     * the overall control flow.
     *
     * @param conditionIdx the index of the condition
     * @return the minimum depth, or Integer.MAX_VALUE if never encountered
     */
    public int dominatorDepth(int conditionIdx) {
        return dominatorDepth[conditionIdx];
    }

    /**
     * Gets the cone size as the number of reachable result nodes for a condition, representing how many different
     * endpoints/errors can be reached downstream of the condition.
     *
     * <p>Larger values indicate conditions that have broader impact on the final outcome.
     *
     * @param conditionIdx the index of the condition
     * @return the number of result nodes in this condition's cone
     */
    public int coneSize(int conditionIdx) {
        return reachableResults[conditionIdx];
    }

    private static final class ConeInfo {
        private static final ConeInfo IN_PROGRESS = new ConeInfo(0, true);

        final int resultNodes;
        final boolean inProgress;

        private ConeInfo(int resultNodes, boolean inProgress) {
            this.resultNodes = resultNodes;
            this.inProgress = inProgress;
        }

        private static ConeInfo empty() {
            return new ConeInfo(0, false);
        }

        private static ConeInfo singleResult() {
            return new ConeInfo(1, false);
        }

        private static ConeInfo combine(ConeInfo trueBranch, ConeInfo falseBranch) {
            if (trueBranch.inProgress || falseBranch.inProgress) {
                throw new IllegalStateException("Cycle detected in CFG during cone analysis (branch in-progress)");
            }
            return new ConeInfo(trueBranch.resultNodes + falseBranch.resultNodes, false);
        }
    }
}
