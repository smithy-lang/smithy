/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.logging.Logger;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.logic.cfg.Cfg;
import software.amazon.smithy.utils.SmithyBuilder;

/**
 * BDD optimization using tiered parallel position evaluation with dependency-aware constraints.
 *
 * <p>This algorithm improves BDD size through a multi-stage approach:
 * <ul>
 *     <li>Coarse optimization for large BDDs (fast reduction)</li>
 *     <li>Medium optimization for moderate BDDs (balanced approach)</li>
 *     <li>Granular optimization for small BDDs (maximum quality)</li>
 * </ul>
 *
 * <p>Each stage runs until reaching its target size or maximum passes.
 */
public final class SiftingOptimization implements Function<BddTrait, BddTrait> {
    private static final Logger LOGGER = Logger.getLogger(SiftingOptimization.class.getName());

    // Default thresholds and passes for each optimization level
    private static final int DEFAULT_COARSE_MIN_NODES = 50_000;
    private static final int DEFAULT_COARSE_MAX_PASSES = 5;
    private static final int DEFAULT_MEDIUM_MIN_NODES = 10_000;
    private static final int DEFAULT_MEDIUM_MAX_PASSES = 5;
    private static final int DEFAULT_GRANULAR_MAX_NODES = 10_000;
    private static final int DEFAULT_GRANULAR_MAX_PASSES = 8;

    // When a variable has fewer than this many valid positions, try them all.
    private static final int EXHAUSTIVE_THRESHOLD = 20;

    // Thread-local BDD builders to avoid allocation overhead
    private final ThreadLocal<BddBuilder> threadBuilder = ThreadLocal.withInitial(BddBuilder::new);

    private final Cfg cfg;
    private final ConditionDependencyGraph dependencyGraph;

    // Tiered optimization settings
    private final int coarseMinNodes;
    private final int coarseMaxPasses;
    private final int mediumMinNodes;
    private final int mediumMaxPasses;
    private final int granularMaxNodes;
    private final int granularMaxPasses;

    // Internal effort levels for the tiered optimization stages.
    private enum OptimizationEffort {
        COARSE(12, 4, 0),
        MEDIUM(2, 18, 5),
        GRANULAR(1, 30, 6);

        final int sampleRate;
        final int maxPositions;
        final int nearbyRadius;

        OptimizationEffort(int sampleRate, int maxPositions, int nearbyRadius) {
            this.sampleRate = sampleRate;
            this.maxPositions = maxPositions;
            this.nearbyRadius = nearbyRadius;
        }
    }

    private SiftingOptimization(Builder builder) {
        this.cfg = SmithyBuilder.requiredState("cfg", builder.cfg);
        this.coarseMinNodes = builder.coarseMinNodes;
        this.coarseMaxPasses = builder.coarseMaxPasses;
        this.mediumMinNodes = builder.mediumMinNodes;
        this.mediumMaxPasses = builder.mediumMaxPasses;
        this.granularMaxNodes = builder.granularMaxNodes;
        this.granularMaxPasses = builder.granularMaxPasses;
        this.dependencyGraph = new ConditionDependencyGraph(Arrays.asList(cfg.getConditions()));
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
    public BddTrait apply(BddTrait trait) {
        try {
            return doApply(trait);
        } finally {
            threadBuilder.remove();
        }
    }

    private BddTrait doApply(BddTrait trait) {
        LOGGER.info("Starting BDD sifting optimization");
        long startTime = System.currentTimeMillis();

        // Pre-spin the ForkJoinPool for better first-pass performance
        ForkJoinPool.commonPool().submit(() -> {}).join();

        OptimizationState state = initializeOptimization(trait);
        LOGGER.info(String.format("Initial size: %d nodes", state.initialSize));

        state = runCoarseStage(state);
        state = runMediumStage(state);
        state = runGranularStage(state);

        double totalTimeInSeconds = (System.currentTimeMillis() - startTime) / 1000.0;

        // Only rebuild if we found an improvement
        if (state.bestSize >= state.initialSize) {
            LOGGER.info(String.format("No improvements found in %fs", totalTimeInSeconds));
            return trait;
        }

        LOGGER.info(String.format("Optimization complete: %d -> %d nodes (%.1f%% total reduction) in %fs",
                state.initialSize,
                state.bestSize,
                (1.0 - (double) state.bestSize / state.initialSize) * 100,
                totalTimeInSeconds));

        // Rebuild the BddTrait with the optimized ordering and BDD
        return trait.toBuilder()
                .conditions(state.orderView)
                .results(state.results)
                .bdd(state.bestBdd)
                .build();
    }

    private OptimizationState initializeOptimization(BddTrait trait) {
        // Use the trait's existing ordering as the starting point
        List<Condition> initialOrder = new ArrayList<>(trait.getConditions());

        Condition[] order = initialOrder.toArray(new Condition[0]);
        List<Condition> orderView = Arrays.asList(order);

        // Get the initial size from the input BDD
        Bdd bdd = trait.getBdd();
        int initialSize = bdd.getNodeCount() - 1; // -1 for terminal

        // No need to recompile just for results—use the trait's own results
        return new OptimizationState(order,
                orderView,
                bdd,
                initialSize,
                initialSize,
                trait.getResults());
    }

    private OptimizationState runCoarseStage(OptimizationState state) {
        if (state.currentSize <= coarseMinNodes) {
            return state;
        }
        return runOptimizationStage(state, "Coarse", OptimizationEffort.COARSE, coarseMinNodes, coarseMaxPasses, 4.0);
    }

    private OptimizationState runMediumStage(OptimizationState state) {
        if (state.currentSize <= mediumMinNodes) {
            return state;
        }
        return runOptimizationStage(state, "Medium", OptimizationEffort.MEDIUM, mediumMinNodes, mediumMaxPasses, 1.5);
    }

    private OptimizationState runGranularStage(OptimizationState state) {
        if (state.currentSize > granularMaxNodes) {
            LOGGER.info(String.format("Skipping granular stage - BDD too large (%d nodes > %d threshold)",
                    state.currentSize,
                    granularMaxNodes));
            return state;
        }

        // Run with no minimums
        state = runOptimizationStage(state, "Granular", OptimizationEffort.GRANULAR, 0, granularMaxPasses, 0.0);

        // Also perform adjacent swaps in granular stage
        OptimizationResult swapResult = performAdjacentSwaps(state.order, state.orderView, state.currentSize);
        if (swapResult.improved) {
            LOGGER.info(String.format("Adjacent swaps: %d -> %d nodes", state.currentSize, swapResult.size));
            return state.withResult(swapResult.bdd, swapResult.size, swapResult.results);
        }

        return state;
    }

    private OptimizationState runOptimizationStage(
            OptimizationState state,
            String stageName,
            OptimizationEffort effort,
            int targetNodeCount,
            int maxPasses,
            double minReductionPercent
    ) {

        LOGGER.info(String.format("Stage: %s optimization (%d nodes%s)",
                stageName,
                state.currentSize,
                targetNodeCount > 0 ? String.format(", target < %d", targetNodeCount) : ""));

        OptimizationState currentState = state;

        for (int pass = 1; pass <= maxPasses; pass++) {
            // Stop if we've reached the target
            if (targetNodeCount > 0 && currentState.currentSize <= targetNodeCount) {
                break;
            }

            int passStartSize = currentState.currentSize;
            OptimizationResult result = runOptimizationPass(
                    currentState.order,
                    currentState.orderView,
                    currentState.currentSize,
                    effort);

            if (!result.improved) {
                LOGGER.fine(String.format("%s pass %d found no improvements", stageName, pass));
                break;
            } else {
                currentState = currentState.withResult(result.bdd, result.size, result.results);
                double reduction = (1.0 - (double) result.size / passStartSize) * 100;
                LOGGER.fine(String.format("%s pass %d: %d -> %d nodes (%.1f%% reduction)",
                        stageName,
                        pass,
                        passStartSize,
                        result.size,
                        reduction));
                // Check for diminishing returns
                if (minReductionPercent > 0 && reduction < minReductionPercent) {
                    LOGGER.fine(String.format("%s optimization yielding diminishing returns", stageName));
                    break;
                }
            }
        }

        return currentState;
    }

    private OptimizationResult runOptimizationPass(
            Condition[] order,
            List<Condition> orderView,
            int currentSize,
            OptimizationEffort effort
    ) {

        int improvements = 0;
        OrderConstraints constraints = new OrderConstraints(dependencyGraph, orderView);
        Bdd bestBdd = null;
        int bestSize = currentSize;
        List<Rule> bestResults = null;

        // Sample variables based on effort level
        for (int varIdx = 0; varIdx < order.length; varIdx += effort.sampleRate) {
            List<Integer> positions = getPositions(varIdx, constraints, effort);
            if (positions.isEmpty()) {
                continue;
            } else if (positions.size() > effort.maxPositions) {
                positions = positions.subList(0, effort.maxPositions);
            }

            // Find best position
            PositionCount best = findBestPosition(positions, order, bestSize, varIdx);

            if (best == null || best.count >= bestSize) {
                continue;
            }

            // Move to best position and build BDD once
            move(order, varIdx, best.position);
            BddCompilationResult compilationResult = compileBddWithResults(orderView);
            Bdd newBdd = compilationResult.bdd;
            int newSize = newBdd.getNodeCount() - 1;

            if (newSize < bestSize) {
                bestBdd = newBdd;
                bestSize = newSize;
                bestResults = compilationResult.results;
                improvements++;

                // Update constraints after successful move
                constraints = new OrderConstraints(dependencyGraph, orderView);
            }
        }

        return new OptimizationResult(bestBdd, bestSize, improvements > 0, bestResults);
    }

    private OptimizationResult performAdjacentSwaps(Condition[] order, List<Condition> orderView, int currentSize) {
        OrderConstraints constraints = new OrderConstraints(dependencyGraph, orderView);
        Bdd bestBdd = null;
        int bestSize = currentSize;
        List<Rule> bestResults = null;
        boolean improved = false;

        for (int i = 0; i < order.length - 1; i++) {
            if (constraints.canMove(i, i + 1)) {
                move(order, i, i + 1);
                int swappedSize = countNodes(orderView);
                if (swappedSize < bestSize) {
                    BddCompilationResult compilationResult = compileBddWithResults(orderView);
                    bestBdd = compilationResult.bdd;
                    bestSize = swappedSize;
                    bestResults = compilationResult.results;
                    improved = true;
                } else {
                    // Swap back if no improvement
                    move(order, i + 1, i);
                }
            }
        }

        return new OptimizationResult(bestBdd, bestSize, improved, bestResults);
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

    private List<Integer> getPositions(int varIdx, OrderConstraints constraints, OptimizationEffort effort) {
        int min = constraints.getMinValidPosition(varIdx);
        int max = constraints.getMaxValidPosition(varIdx);
        int range = max - min;
        return range <= EXHAUSTIVE_THRESHOLD
                ? getExhaustivePositions(varIdx, min, max, constraints)
                : getStrategicPositions(varIdx, min, max, range, constraints, effort);
    }

    private List<Integer> getExhaustivePositions(int varIdx, int min, int max, OrderConstraints constraints) {
        List<Integer> positions = new ArrayList<>(max - min);
        for (int p = min; p < max; p++) {
            if (p != varIdx && constraints.canMove(varIdx, p)) {
                positions.add(p);
            }
        }
        return positions;
    }

    private List<Integer> getStrategicPositions(
            int varIdx,
            int min,
            int max,
            int range,
            OrderConstraints constraints,
            OptimizationEffort effort
    ) {
        List<Integer> positions = new ArrayList<>(effort.maxPositions);

        // Boundaries (these are most likely to be optimal)
        if (min != varIdx && constraints.canMove(varIdx, min)) {
            positions.add(min);
        }
        if (max - 1 != varIdx && constraints.canMove(varIdx, max - 1)) {
            positions.add(max - 1);
        }

        // Nearby positions (only if effort includes nearbyRadius)
        if (effort.nearbyRadius > 0) {
            for (int offset = -effort.nearbyRadius; offset <= effort.nearbyRadius; offset++) {
                if (offset != 0) {
                    int p = varIdx + offset;
                    if (p >= min && p < max && !positions.contains(p) && constraints.canMove(varIdx, p)) {
                        positions.add(p);
                    }
                }
            }
        }

        // Adaptive sampling: fewer samples for smaller ranges
        int maxSamples = Math.min(15, effort.maxPositions / 2);
        int samples = Math.min(maxSamples, Math.max(2, range / 4));
        int step = Math.max(1, range / samples);

        for (int p = min + step; p < max - step; p += step) {
            if (p != varIdx && !positions.contains(p) && constraints.canMove(varIdx, p)) {
                positions.add(p);
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
     * Compiles a BDD with the given condition ordering and returns both BDD and results.
     */
    private BddCompilationResult compileBddWithResults(List<Condition> ordering) {
        BddBuilder builder = threadBuilder.get().reset();
        BddCompiler compiler = new BddCompiler(cfg, ConditionOrderingStrategy.fixed(ordering), builder);
        Bdd bdd = compiler.compile();
        return new BddCompilationResult(bdd, compiler.getIndexedResults());
    }

    /**
     * Counts nodes for a given ordering without keeping the BDD.
     */
    private int countNodes(List<Condition> ordering) {
        Bdd bdd = compileBddWithResults(ordering).bdd;
        return bdd.getNodeCount() - 1; // -1 for terminal
    }

    // Container for BDD compilation results
    private static final class BddCompilationResult {
        final Bdd bdd;
        final List<Rule> results;

        BddCompilationResult(Bdd bdd, List<Rule> results) {
            this.bdd = bdd;
            this.results = results;
        }
    }

    // Position and its node count
    private static final class PositionCount {
        final int position;
        final int count;

        PositionCount(int position, int count) {
            this.position = position;
            this.count = count;
        }
    }

    // Result of an optimization pass
    private static final class OptimizationResult {
        final Bdd bdd;
        final int size;
        final boolean improved;
        final List<Rule> results;

        OptimizationResult(Bdd bdd, int size, boolean improved, List<Rule> results) {
            this.bdd = bdd;
            this.size = size;
            this.improved = improved;
            this.results = results;
        }
    }

    // State tracking during optimization
    private static final class OptimizationState {
        final Condition[] order;
        final List<Condition> orderView;
        final Bdd bestBdd;
        final int currentSize;
        final int bestSize;
        final int initialSize;
        final List<Rule> results;

        OptimizationState(
                Condition[] order,
                List<Condition> orderView,
                Bdd bestBdd,
                int currentSize,
                int initialSize,
                List<Rule> results
        ) {
            this.order = order;
            this.orderView = orderView;
            this.bestBdd = bestBdd;
            this.currentSize = currentSize;
            this.bestSize = currentSize;
            this.initialSize = initialSize;
            this.results = results;
        }

        OptimizationState withResult(Bdd newBdd, int newSize, List<Rule> newResults) {
            return new OptimizationState(order, orderView, newBdd, newSize, initialSize, newResults);
        }
    }

    /**
     * Builder for SiftingOptimization.
     */
    public static final class Builder implements SmithyBuilder<SiftingOptimization> {
        private Cfg cfg;
        private int coarseMinNodes = DEFAULT_COARSE_MIN_NODES;
        private int coarseMaxPasses = DEFAULT_COARSE_MAX_PASSES;
        private int mediumMinNodes = DEFAULT_MEDIUM_MIN_NODES;
        private int mediumMaxPasses = DEFAULT_MEDIUM_MAX_PASSES;
        private int granularMaxNodes = DEFAULT_GRANULAR_MAX_NODES;
        private int granularMaxPasses = DEFAULT_GRANULAR_MAX_PASSES;

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
         * Sets the coarse optimization parameters.
         *
         * <p>Coarse optimization runs until the BDD has fewer than minNodeCount nodes
         * or maxPasses have been completed.
         *
         * @param minNodeCount the target size to stop coarse optimization (default: 50,000)
         * @param maxPasses the maximum number of coarse passes (default: 3)
         * @return this builder
         */
        public Builder coarseEffort(int minNodeCount, int maxPasses) {
            this.coarseMinNodes = minNodeCount;
            this.coarseMaxPasses = maxPasses;
            return this;
        }

        /**
         * Sets the medium optimization parameters.
         *
         * <p>Medium optimization runs until the BDD has fewer than minNodeCount nodes
         * or maxPasses have been completed.
         *
         * @param minNodeCount the target size to stop medium optimization (default: 10,000)
         * @param maxPasses the maximum number of medium passes (default: 4)
         * @return this builder
         */
        public Builder mediumEffort(int minNodeCount, int maxPasses) {
            this.mediumMinNodes = minNodeCount;
            this.mediumMaxPasses = maxPasses;
            return this;
        }

        /**
         * Sets the granular optimization parameters.
         *
         * <p>Granular optimization only runs if the BDD has fewer than maxNodeCount nodes,
         * and runs for at most maxPasses.
         *
         * @param maxNodeCount the maximum size to attempt granular optimization (default: 3,000)
         * @param maxPasses the maximum number of granular passes (default: 2)
         * @return this builder
         */
        public Builder granularEffort(int maxNodeCount, int maxPasses) {
            this.granularMaxNodes = maxNodeCount;
            this.granularMaxPasses = maxPasses;
            return this;
        }

        @Override
        public SiftingOptimization build() {
            return new SiftingOptimization(this);
        }
    }
}
