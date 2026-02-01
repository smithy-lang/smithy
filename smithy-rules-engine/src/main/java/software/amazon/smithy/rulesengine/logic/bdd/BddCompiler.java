/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.NoMatchRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.logic.ConditionReference;
import software.amazon.smithy.rulesengine.logic.cfg.Cfg;
import software.amazon.smithy.rulesengine.logic.cfg.CfgNode;
import software.amazon.smithy.rulesengine.logic.cfg.ConditionNode;
import software.amazon.smithy.rulesengine.logic.cfg.ResultNode;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * BDD compiler that builds a BDD from a CFG.
 */
@SmithyInternalApi
public final class BddCompiler {
    private static final Logger LOGGER = Logger.getLogger(BddCompiler.class.getName());

    private final Cfg cfg;
    private final BddBuilder bddBuilder;
    private final OrderingStrategy orderingStrategy;

    // Condition ordering
    private List<Condition> orderedConditions;
    private Map<Condition, Integer> conditionToIndex;

    // Result indexing
    private final Map<Rule, Integer> ruleToIndex = new HashMap<>();
    private final List<Rule> indexedResults = new ArrayList<>();
    private int nextResultIndex = 0;
    private int noMatchIndex = -1;

    // Simple cache to avoid recomputing identical subgraphs
    private final Map<CfgNode, Integer> nodeCache = new HashMap<>();

    /**
     * @param cfg CFG to convert to a BDD.
     */
    public BddCompiler(Cfg cfg) {
        this(cfg, new BddBuilder());
    }

    BddCompiler(Cfg cfg, BddBuilder bddBuilder) {
        this(cfg, OrderingStrategy.initialOrdering(cfg), bddBuilder);
    }

    BddCompiler(Cfg cfg, OrderingStrategy orderingStrategy, BddBuilder bddBuilder) {
        this.cfg = Objects.requireNonNull(cfg, "CFG cannot be null");
        this.orderingStrategy = Objects.requireNonNull(orderingStrategy, "Ordering strategy cannot be null");
        this.bddBuilder = Objects.requireNonNull(bddBuilder, "BDD builder cannot be null");
    }

    /**
     * Compile the CFG into a BDD.
     *
     * @return the compiled BDD.
     */
    public Bdd compile() {
        long start = System.currentTimeMillis();
        extractAndOrderConditions();

        // Set the condition count in the builder
        bddBuilder.setConditionCount(orderedConditions.size());

        // Create the "no match" terminal
        noMatchIndex = getOrCreateResultIndex(NoMatchRule.INSTANCE);
        int rootRef = convertCfgToBdd(cfg.getRoot());
        rootRef = bddBuilder.reduce(rootRef);
        bddBuilder.setRoot(rootRef);
        bddBuilder.setResultCount(indexedResults.size());
        Bdd bdd = bddBuilder.build();

        long elapsed = System.currentTimeMillis() - start;
        LOGGER.fine(String.format(
                "BDD compilation complete: %d conditions, %d results, %d BDD nodes in %dms",
                orderedConditions.size(),
                indexedResults.size(),
                bdd.getNodeCount(),
                elapsed));

        return bdd;
    }

    /**
     * The ordered result rules after BDD compilation.
     *
     * @return ordered BDD result rules.
     */
    public List<Rule> getIndexedResults() {
        return indexedResults;
    }

    /**
     * Get the ordered conditions referenced in the compiled BDD.
     *
     * @return the ordered BDD conditions.
     */
    public List<Condition> getOrderedConditions() {
        return orderedConditions;
    }

    private int convertCfgToBdd(CfgNode cfgNode) {
        Integer cached = nodeCache.get(cfgNode);
        if (cached != null) {
            return cached;
        }

        int result;
        if (cfgNode == null) {
            result = bddBuilder.makeResult(noMatchIndex);
        } else if (cfgNode instanceof ResultNode) {
            Rule rule = ((ResultNode) cfgNode).getResult();
            result = bddBuilder.makeResult(getOrCreateResultIndex(rule));
        } else {
            ConditionNode cn = (ConditionNode) cfgNode;
            ConditionReference ref = cn.getCondition();
            int varIdx = conditionToIndex.get(ref.getCondition());

            // Recursively build the two branches
            int hi = convertCfgToBdd(cn.getTrueBranch());
            int lo = convertCfgToBdd(cn.getFalseBranch());

            // If the original rule said "not condition", swap branches
            if (ref.isNegated()) {
                int tmp = hi;
                hi = lo;
                lo = tmp;
            }

            // Build the pure boolean test for variable varIdx
            int test = bddBuilder.makeNode(varIdx, bddBuilder.makeTrue(), bddBuilder.makeFalse());

            // Combine with ITE (reduces and merges)
            result = bddBuilder.ite(test, hi, lo);
        }

        nodeCache.put(cfgNode, result);
        return result;
    }

    private int getOrCreateResultIndex(Rule rule) {
        return ruleToIndex.computeIfAbsent(rule, r -> {
            int idx = nextResultIndex++;
            indexedResults.add(r);
            return idx;
        });
    }

    private void extractAndOrderConditions() {
        orderedConditions = orderingStrategy.orderConditions(cfg.getConditions());
        conditionToIndex = new LinkedHashMap<>();
        for (int i = 0; i < orderedConditions.size(); i++) {
            conditionToIndex.put(orderedConditions.get(i), i);
        }
    }
}
