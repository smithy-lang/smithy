/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.IsSet;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.logic.cfg.Cfg;
import software.amazon.smithy.rulesengine.logic.cfg.ConditionDependencyGraph;

/**
 * Orders conditions by following the natural structure of the CFG.
 *
 * <p>This strategy has proven to be the most effective for BDD construction because it preserves the locality that
 * rule authors built into their decision trees. Conditions that are evaluated together in the original rules stay
 * together in the BDD, enabling better node sharing. This ordering implementation flattens the tree structure while
 * respecting data dependencies.
 */
final class InitialOrdering implements OrderingStrategy {
    private static final Logger LOGGER = Logger.getLogger(InitialOrdering.class.getName());

    /** How many distinct consumers make an isSet() a "gate". */
    private static final int GATE_SUCCESSOR_THRESHOLD = 2;

    private final Cfg cfg;

    InitialOrdering(Cfg cfg) {
        this.cfg = cfg;
    }

    @Override
    public List<Condition> orderConditions(Condition[] conditions) {
        long startTime = System.currentTimeMillis();

        ConditionDependencyGraph deps = new ConditionDependencyGraph(Arrays.asList(conditions));
        Map<Condition, Integer> conditionToIndex = deps.getConditionToIndex();
        CfgConeAnalysis cones = new CfgConeAnalysis(cfg, conditions, conditionToIndex);
        List<Integer> order = buildCfgOrder(conditions, deps, cones);

        List<Condition> result = new ArrayList<>();
        for (int id : order) {
            result.add(conditions[id]);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        LOGGER.info(() -> String.format("Initial ordering: %d conditions in %dms", conditions.length, elapsed));
        return result;
    }

    // Builds an ordering using a topological sort that prefers conditions based on their position in the CFG.
    private List<Integer> buildCfgOrder(Condition[] conditions, ConditionDependencyGraph deps, CfgConeAnalysis cones) {
        List<Integer> result = new ArrayList<>();
        BitSet placed = new BitSet();
        BitSet ready = new BitSet();

        // Start with conditions that have no dependencies
        for (int i = 0; i < conditions.length; i++) {
            if (deps.getPredecessorCount(i) == 0) {
                ready.set(i);
            }
        }

        while (!ready.isEmpty()) {
            int chosen = getNext(ready, cones, conditions, deps);
            result.add(chosen);
            placed.set(chosen);
            ready.clear(chosen);

            // Make successors ready if all their dependencies are satisfied
            for (int succ : deps.getSuccessors(chosen)) {
                if (!placed.get(succ) && allPredecessorsPlaced(succ, deps, placed)) {
                    ready.set(succ);
                }
            }
        }

        if (result.size() != conditions.length) {
            throw new IllegalStateException("Topological ordering incomplete (possible cyclic deps). Placed="
                    + result.size() + " of " + conditions.length);
        }

        return result;
    }

    /**
     * Selects the next condition to place based on CFG structure.
     *
     * <p>1. Pick conditions closest to the CFG root (minimum depth)
     * 2. Prefer "gate" conditions that guard many branches
     * 3. Break ties with cone size (bigger is more discriminating)
     * 4. Tie-break with ID for determinism
     */
    private int getNext(BitSet ready, CfgConeAnalysis cones, Condition[] conditions, ConditionDependencyGraph deps) {
        int best = -1;
        int bestDepth = Integer.MAX_VALUE;
        int bestCone = -1;
        boolean bestIsGate = false;

        for (int i = ready.nextSetBit(0); i >= 0; i = ready.nextSetBit(i + 1)) {
            int depth = cones.dominatorDepth(i);
            if (depth > bestDepth) {
                continue; // Skip if worse
            }

            int cone = cones.coneSize(i);
            boolean isGate = isIsSet(conditions[i]) && deps.getSuccessorCount(i) > GATE_SUCCESSOR_THRESHOLD;

            if (depth < bestDepth) {
                // New best if shallower
                best = i;
                bestDepth = depth;
                bestCone = cone;
                bestIsGate = isGate;
            } else if (!bestIsGate && isGate) {
                // Gates win
                best = i;
                bestCone = cone;
                bestIsGate = true;
            } else if (bestIsGate == isGate && (cone > bestCone || (cone == bestCone && i < best))) {
                // Same gate status, so pick larger cone or lower ID for stability
                best = i;
                bestCone = cone;
            }
        }

        return best;
    }

    private boolean allPredecessorsPlaced(int id, ConditionDependencyGraph deps, BitSet placed) {
        for (int pred : deps.getPredecessors(id)) {
            if (!placed.get(pred)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isIsSet(Condition c) {
        return c.getFunction().getFunctionDefinition() == IsSet.getDefinition();
    }
}
