/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.logging.Logger;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.logic.ConditionInfo;
import software.amazon.smithy.rulesengine.logic.cfg.Cfg;
import software.amazon.smithy.rulesengine.logic.cfg.CfgNode;
import software.amazon.smithy.rulesengine.logic.cfg.ConditionNode;
import software.amazon.smithy.utils.SmithyBuilder;

/**
 * BDD optimization using parallel position evaluation with dependency-aware constraints.
 *
 * <p>This algorithm improves BDD size by finding better variable orderings through:
 * <ul>
 *     <li>Starting with an intelligent initial ordering</li>
 *     <li>Evaluating multiple candidate positions for each variable in parallel</li>
 *     <li>Respecting dependency constraints between variables</li>
 *     <li>Iterating until no improvements are found</li>
 * </ul>
 */
public final class SiftingOptimization implements Function<Bdd, Bdd> {
    private static final Logger LOGGER = Logger.getLogger(SiftingOptimization.class.getName());

    // Maximum number of complete passes through all variables before giving up.
    private static final int DEFAULT_MAX_PASSES = 4;

    // When a variable has fewer than this many valid positions, try them all.
    private static final int EXHAUSTIVE_THRESHOLD = 20;

    // How many positions to check on each side of current position.
    private static final int NEARBY_RADIUS = 5;

    // Maximum number of evenly-spaced samples for large ranges.
    private static final int MAX_SAMPLE_COUNT = 15;

    // Cap on positions to evaluate per variable to prevent pathological cases.
    private static final int MAX_WORK_PER_VAR = 30;

    // Thread-local BDD builders to avoid allocation overhead
    private final ThreadLocal<BddBuilder> threadBuilder = ThreadLocal.withInitial(BddBuilder::new);

    private final Cfg cfg;
    private final int maxPasses;
    private final ConditionDependencyGraph dependencyGraph;
    private final Map<Condition, ConditionInfo> conditionInfos;

    private SiftingOptimization(Builder builder) {
        this.cfg = SmithyBuilder.requiredState("cfg", builder.cfg);
        this.maxPasses = builder.maxPasses;

        // Extract condition infos from CFG
        this.conditionInfos = new LinkedHashMap<>();
        for (CfgNode node : cfg) {
            if (node instanceof ConditionNode) {
                ConditionInfo info = ((ConditionNode) node).getCondition();
                conditionInfos.put(info.getCondition(), info);
            }
        }

        this.dependencyGraph = new ConditionDependencyGraph(conditionInfos);
    }

    /**
     * Creates a new builder for SiftingOptimization.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Bdd apply(Bdd bdd) {
        try {
            return doApply(bdd);
        } finally {
            threadBuilder.remove();
        }
    }

    private Bdd doApply(Bdd bdd) {
        LOGGER.info("Starting BDD sifting optimization");

        // Pre-spin the ForkJoinPool for better first-pass performance
        ForkJoinPool.commonPool().submit(() -> {}).join();

        // Start with an intelligent initial ordering instead of arbitrary CFG order
        List<Condition> initialOrder = DefaultOrderingStrategy.orderConditions(
                bdd.getConditions().toArray(new Condition[0]),
                conditionInfos);

        // Sanity check that ordering didn't lose conditions
        if (initialOrder.size() != bdd.getConditions().size()) {
            throw new IllegalStateException("Initial ordering changed condition count: " +
                    bdd.getConditions().size() + " -> " + initialOrder.size());
        }

        Condition[] order = initialOrder.toArray(new Condition[0]);

        // Create a mutable view that shares the underlying array
        List<Condition> orderView = Arrays.asList(order);

        // Build initial BDD with better ordering
        Bdd currentBest = Bdd.from(cfg, new BddBuilder(), ConditionOrderingStrategy.fixed(orderView));
        int currentSize = currentBest.getNodes().size() - 1; // -1 for terminal
        int initialSize = bdd.getNodes().size() - 1;

        LOGGER.info(String.format("Initial reordering: %d -> %d nodes", initialSize, currentSize));

        // Track the absolute best we've ever seen
        Bdd allTimeBest = currentBest;
        int allTimeSize = currentSize;

        // Main optimization passes
        for (int pass = 1; pass <= maxPasses; pass++) {
            LOGGER.info(String.format("Starting pass %d", pass));
            int passStartSize = currentSize;
            int improvements = 0;

            OrderConstraints constraints = new OrderConstraints(dependencyGraph, orderView);

            for (int varIdx = 0; varIdx < order.length; varIdx++) {
                List<Integer> positions = getPositions(varIdx, constraints);
                if (positions.isEmpty()) {
                    continue;
                } else if (positions.size() > MAX_WORK_PER_VAR) {
                    positions = positions.subList(0, MAX_WORK_PER_VAR);
                }

                // Find best position
                PositionCount best = findBestPosition(positions, order, currentSize, varIdx);

                if (best == null || best.count >= currentSize) {
                    continue;
                }

                // Move to best position and build BDD once
                move(order, varIdx, best.position);
                currentBest = Bdd.from(cfg, new BddBuilder(), ConditionOrderingStrategy.fixed(orderView));
                currentSize = currentBest.getNodes().size() - 1;
                improvements++;

                // Update absolute best
                allTimeSize = currentSize;
                allTimeBest = currentBest;

                // Update constraints after successful move
                constraints = new OrderConstraints(dependencyGraph, orderView);
            }

            LOGGER.info(String.format("Pass %d: %d improvements, size %d -> %d",
                    pass,
                    improvements,
                    passStartSize,
                    currentSize));

            if (improvements == 0) {
                break;
            }
        }

        // Final adjacent swap pass for fine-tuning
        LOGGER.fine("Starting final adjacent swap pass");
        OrderConstraints finalConstraints = new OrderConstraints(dependencyGraph, orderView);
        for (int i = 0; i < order.length - 1; i++) {
            if (finalConstraints.canMove(i, i + 1)) {
                move(order, i, i + 1);
                int swappedSize = countNodes(orderView);
                if (swappedSize < allTimeSize) {
                    allTimeBest = Bdd.from(cfg, new BddBuilder(), ConditionOrderingStrategy.fixed(orderView));
                    allTimeSize = swappedSize;
                    LOGGER.fine(String.format("Adjacent swap improved: %d -> %d", currentSize, allTimeSize));
                } else {
                    // Swap back if no improvement
                    move(order, i + 1, i);
                }
            }
        }

        if (allTimeSize < initialSize) {
            LOGGER.info(String.format("Optimization complete: %d -> %d nodes (%.1f%% reduction)",
                    initialSize,
                    allTimeSize,
                    (1.0 - (double) allTimeSize / initialSize) * 100));
        } else {
            LOGGER.info("No improvements found");
        }

        return allTimeBest;
    }

    private PositionCount findBestPosition(
            List<Integer> positions,
            final Condition[] currentOrder,
            final int currentSize,
            final int varIdx
    ) {
        return positions.parallelStream()
                .map(pos -> {
                    Condition[] threadOrder = currentOrder.clone();
                    move(threadOrder, varIdx, pos);
                    int nodeCount = countNodes(Arrays.asList(threadOrder));
                    return new PositionCount(pos, nodeCount);
                })
                .filter(pc -> pc.count < currentSize)
                .min(Comparator.comparingInt(pc -> pc.count))
                .orElse(null);
    }

    private List<Integer> getPositions(int varIdx, OrderConstraints constraints) {
        int min = constraints.getMinValidPosition(varIdx);
        int max = constraints.getMaxValidPosition(varIdx);
        int range = max - min;

        // Pre-allocate with estimated size
        List<Integer> positions = new ArrayList<>(Math.min(EXHAUSTIVE_THRESHOLD, range));

        if (range <= EXHAUSTIVE_THRESHOLD) {
            // Small range: try all positions
            for (int p = min; p < max; p++) {
                if (p != varIdx && constraints.canMove(varIdx, p)) {
                    positions.add(p);
                }
            }
        } else {
            // Large range: strategic sampling

            // Boundaries (these are most likely to be optimal)
            if (min != varIdx && constraints.canMove(varIdx, min)) {
                positions.add(min);
            }
            if (max - 1 != varIdx && constraints.canMove(varIdx, max - 1)) {
                positions.add(max - 1);
            }

            // Nearby positions
            for (int offset = -NEARBY_RADIUS; offset <= NEARBY_RADIUS; offset++) {
                if (offset != 0) {
                    int p = varIdx + offset;
                    if (p >= min && p < max && !positions.contains(p) && constraints.canMove(varIdx, p)) {
                        positions.add(p);
                    }
                }
            }

            // Adaptive sampling: fewer samples for smaller ranges
            int samples = Math.min(MAX_SAMPLE_COUNT, Math.max(2, range / 4));
            int step = Math.max(1, range / samples);

            for (int p = min + step; p < max - step; p += step) {
                if (!positions.contains(p) && p != varIdx && constraints.canMove(varIdx, p)) {
                    positions.add(p);
                }
            }
        }

        return positions;
    }

    /**
     * Moves an element in an array from one position to another.
     */
    private static void move(Condition[] arr, int from, int to) {
        if (from == to) {
            return;
        }

        Condition moving = arr[from];
        if (from < to) {
            // Moving right: shift elements left
            System.arraycopy(arr, from + 1, arr, from, to - from);
        } else {
            // Moving left: shift elements right
            System.arraycopy(arr, to, arr, to + 1, from - to);
        }
        arr[to] = moving;
    }

    /**
     * Counts nodes for a given ordering without keeping the BDD.
     */
    private int countNodes(List<Condition> ordering) {
        BddBuilder builder = threadBuilder.get().reset();
        return Bdd.from(cfg, builder, ConditionOrderingStrategy.fixed(ordering)).getNodes().size() - 1;
    }

    // Position and its node count.
    private static final class PositionCount {
        final int position;
        final int count;

        PositionCount(int position, int count) {
            this.position = position;
            this.count = count;
        }
    }

    /**
     * Builder for SiftingOptimization.
     */
    public static final class Builder implements SmithyBuilder<SiftingOptimization> {
        private Cfg cfg;
        private int maxPasses = DEFAULT_MAX_PASSES;

        private Builder() {}

        /**
         * Sets the required control flow graph to optimize.
         *
         * @param cfg the control flow graph
         * @return this builder
         */
        public Builder cfg(Cfg cfg) {
            this.cfg = cfg;
            return this;
        }

        /**
         * Sets the maximum number of optimization passes.
         *
         * @param maxPasses the maximum passes (default: 4)
         * @return this builder
         */
        public Builder maxPasses(int maxPasses) {
            this.maxPasses = maxPasses;
            return this;
        }

        @Override
        public SiftingOptimization build() {
            return new SiftingOptimization(this);
        }
    }
}
