/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.logic.ConditionCostModel;
import software.amazon.smithy.rulesengine.logic.cfg.Cfg;
import software.amazon.smithy.rulesengine.logic.cfg.ConditionDependencyGraph;
import software.amazon.smithy.rulesengine.traits.EndpointBddTrait;
import software.amazon.smithy.utils.SmithyBuilder;

/**
 * BDD optimization using tiered parallel position evaluation with dependency-aware constraints.
 *
 * <p>The optimization runs in three stages with decreasing granularity:
 * <ul>
 *   <li>Coarse: Fast reduction with large steps</li>
 *   <li>Medium: Balanced optimization</li>
 *   <li>Granular: Fine-tuned optimization for maximum reduction</li>
 * </ul>
 */
public final class SiftingOptimization implements Function<EndpointBddTrait, EndpointBddTrait> {
    private static final Logger LOGGER = Logger.getLogger(SiftingOptimization.class.getName());

    private static final int PARALLEL_THRESHOLD = 7;

    // Early termination: number of passes to track for plateau detection
    private static final int PLATEAU_HISTORY_SIZE = 3;
    private static final double PLATEAU_THRESHOLD = 0.5;

    // Thread-local BDD builders to avoid allocation overhead
    private final ThreadLocal<BddBuilder> threadBuilder = ThreadLocal.withInitial(BddBuilder::new);

    private final Cfg cfg;
    private final ConditionDependencyGraph dependencyGraph;
    private final ConditionCostModel costModel = ConditionCostModel.createDefault();;

    // Reusable cost estimator, created once per optimization run
    private BddCostEstimator costEstimator;

    // Tiered optimization settings
    private final int coarseMinNodes;
    private final int coarseMaxPasses;
    private final int mediumMinNodes;
    private final int mediumMaxPasses;
    private final int granularMaxNodes;
    private final int granularMaxPasses;

    // Internal effort levels for the tiered optimization stages.
    private enum OptimizationEffort {
        COARSE(11, 4, 0, 20, 4_000, 6),
        MEDIUM(2, 24, 6, 20, 1_000, 6),
        GRANULAR(1, 50, 12, 20, 8_000, 12);

        final int sampleRate;
        final int maxPositions;
        final int nearbyRadius;
        final int exhaustiveThreshold;
        final int defaultNodeThreshold;
        final int defaultMaxPasses;

        OptimizationEffort(
                int sampleRate,
                int maxPositions,
                int nearbyRadius,
                int exhaustiveThreshold,
                int defaultNodeThreshold,
                int defaultMaxPasses
        ) {
            this.sampleRate = sampleRate;
            this.maxPositions = maxPositions;
            this.nearbyRadius = nearbyRadius;
            this.exhaustiveThreshold = exhaustiveThreshold;
            this.defaultNodeThreshold = defaultNodeThreshold;
            this.defaultMaxPasses = defaultMaxPasses;
        }
    }

    /**
     * Mutable effort tracker that adapts parameters based on observed improvement.
     */
    private static final class AdaptiveEffort {
        static final double HIGH_THRESHOLD = 10.0;
        static final double LOW_THRESHOLD = 2.0;

        final OptimizationEffort base;
        int sampleRate;
        int maxPositions;
        int nearbyRadius;
        int bonusPasses;

        AdaptiveEffort(OptimizationEffort effort) {
            this.base = effort;
            this.sampleRate = effort.sampleRate;
            this.maxPositions = effort.maxPositions;
            this.nearbyRadius = effort.nearbyRadius;
        }

        /** Adapts effort based on improvement. Returns true if effort increased. */
        boolean adapt(double reductionPercent) {
            if (reductionPercent >= HIGH_THRESHOLD) {
                sampleRate = Math.max(1, sampleRate - 1);
                maxPositions = Math.min(base.maxPositions * 2, maxPositions + 5);
                nearbyRadius = Math.min(base.nearbyRadius + 6, nearbyRadius + 2);
                bonusPasses = Math.min(bonusPasses + 2, 6);
                return true;
            } else if (reductionPercent < LOW_THRESHOLD) {
                sampleRate = Math.min(base.sampleRate * 2, sampleRate + 2);
                maxPositions = Math.max(base.maxPositions / 2, maxPositions - 3);
                nearbyRadius = Math.max(0, nearbyRadius - 2);
                bonusPasses = Math.max(0, bonusPasses - 2);
            }
            return false;
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

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public EndpointBddTrait apply(EndpointBddTrait trait) {
        try {
            return doApply(trait);
        } finally {
            threadBuilder.remove();
        }
    }

    private EndpointBddTrait doApply(EndpointBddTrait trait) {
        LOGGER.info("Starting BDD sifting optimization");
        long startTime = System.currentTimeMillis();
        State state = initializeOptimization(trait);
        LOGGER.info(String.format("Initial size: %d nodes", state.initialSize));

        // Create cost estimator once for the entire optimization run
        this.costEstimator = new BddCostEstimator(state.orderView, costModel, null);

        runOptimizationStage("Coarse", state, OptimizationEffort.COARSE, coarseMinNodes, coarseMaxPasses, 4.0);
        runOptimizationStage("Medium", state, OptimizationEffort.MEDIUM, mediumMinNodes, mediumMaxPasses, 1.5);
        if (state.currentSize <= granularMaxNodes) {
            runOptimizationStage("Granular", state, OptimizationEffort.GRANULAR, 0, granularMaxPasses, 0.0);
        }
        runBlockMoves(state);
        runAdjacentSwaps(state);

        double totalTimeInSeconds = (System.currentTimeMillis() - startTime) / 1000.0;

        if (state.currentSize >= state.initialSize) {
            LOGGER.info(String.format("No improvements found in %fs", totalTimeInSeconds));
            return trait;
        }

        LOGGER.info(String.format("Optimization complete: %d -> %d nodes (%.1f%% total reduction) in %fs",
                state.initialSize,
                state.currentSize,
                (1.0 - (double) state.currentSize / state.initialSize) * 100,
                totalTimeInSeconds));

        return trait.toBuilder().conditions(state.orderView).results(state.results).bdd(state.bestBdd).build();
    }

    private State initializeOptimization(EndpointBddTrait trait) {
        List<Condition> initialOrder = new ArrayList<>(trait.getConditions());
        Condition[] order = initialOrder.toArray(new Condition[0]);
        List<Condition> orderView = Arrays.asList(order);
        Bdd bdd = trait.getBdd();
        int initialSize = bdd.getNodeCount() - 1;
        return new State(order, orderView, bdd, initialSize, trait.getResults());
    }

    private void runOptimizationStage(
            String stageName,
            State state,
            OptimizationEffort effort,
            int targetNodes,
            int maxPasses,
            double minReduction
    ) {
        if (targetNodes > 0 && state.currentSize <= targetNodes) {
            return;
        }

        LOGGER.info(String.format("Stage: %s (%d nodes)", stageName, state.currentSize));

        AdaptiveEffort ae = new AdaptiveEffort(effort);
        double[] history = new double[PLATEAU_HISTORY_SIZE];
        int historyIdx = 0, consecutiveLow = 0;

        for (int pass = 1; pass <= maxPasses + ae.bonusPasses; pass++) {
            if (targetNodes > 0 && state.currentSize <= targetNodes) {
                break;
            }

            int startSize = state.currentSize;
            PassContext result = runPass(state, ae);
            if (result.improvements == 0) {
                break;
            }

            state.update(result.bestBdd, result.bestSize, result.bestResults);
            double reduction = (1.0 - (double) result.bestSize / startSize) * 100;

            history[historyIdx++ % PLATEAU_HISTORY_SIZE] = reduction;
            if (historyIdx >= PLATEAU_HISTORY_SIZE) {
                boolean plateau = true;
                for (double r : history) {
                    if (r >= PLATEAU_THRESHOLD) {
                        plateau = false;
                        break;
                    }
                }
                if (plateau) {
                    break;
                }
            }

            consecutiveLow = ae.adapt(reduction) ? 0 : (reduction < 2.0 ? consecutiveLow + 1 : 0);
            if (consecutiveLow >= 2 || (minReduction > 0 && reduction < minReduction)) {
                break;
            }
        }
    }

    private void runBlockMoves(State state) {
        if (state.currentSize > granularMaxNodes) {
            return;
        }
        LOGGER.info("Running block moves");

        List<List<Integer>> blocks = new ArrayList<>();
        for (List<Integer> b : findDependencyBlocks(state.orderView)) {
            if (b.size() >= 2 && b.size() <= 5) {
                blocks.add(b);
            }
        }

        for (List<Integer> block : blocks) {
            PassContext ctx = new PassContext(state, dependencyGraph);
            Result r = tryBlockMove(block, ctx);
            if (r != null && r.size < ctx.bestSize) {
                state.update(r.bdd, r.size, r.results);
            }
        }
    }

    private List<List<Integer>> findDependencyBlocks(List<Condition> ordering) {
        List<List<Integer>> blocks = new ArrayList<>();
        if (ordering.isEmpty()) {
            return blocks;
        }

        List<Integer> curr = new ArrayList<>();
        curr.add(0);
        for (int i = 1; i < ordering.size(); i++) {
            if (dependencyGraph.getDependencies(ordering.get(i)).contains(ordering.get(i - 1))) {
                curr.add(i);
            } else {
                if (curr.size() >= 2) {
                    blocks.add(curr);
                }
                curr = new ArrayList<>();
                curr.add(i);
            }
        }

        if (curr.size() >= 2) {
            blocks.add(curr);
        }

        return blocks;
    }

    private Result tryBlockMove(List<Integer> block, PassContext ctx) {
        int blockStart = block.get(0), blockEnd = block.get(block.size() - 1), blockSize = block.size();

        // Compute valid range considering all block members' constraints
        int minPos = 0, maxPos = ctx.order.length - blockSize;
        for (int idx : block) {
            int offset = idx - blockStart;
            minPos = Math.max(minPos, ctx.constraints.getMinValidPosition(idx) - offset);
            maxPos = Math.min(maxPos, ctx.constraints.getMaxValidPosition(idx) - offset);
        }

        if (minPos >= maxPos) {
            return null;
        }

        // Try a few strategic positions: min, max, mid
        int[] targets = {minPos, maxPos, minPos + (maxPos - minPos) / 2};
        Result best = null;

        for (int target : targets) {
            if (target == blockStart) {
                continue;
            }

            Condition[] candidate = ctx.order.clone();
            moveBlock(candidate, blockStart, blockEnd, target);
            List<Condition> candidateList = Arrays.asList(candidate);

            // Validate constraints
            ConditionDependencyGraph.OrderConstraints nc = dependencyGraph.createOrderConstraints(candidateList);
            boolean valid = true;
            for (int j = 0; j < candidate.length; j++) {
                if (nc.getMinValidPosition(j) > j || nc.getMaxValidPosition(j) < j) {
                    valid = false;
                    break;
                }
            }

            if (!valid) {
                continue;
            }

            BddCompilerSupport.BddCompilationResult cr =
                    BddCompilerSupport.compile(cfg, candidateList, threadBuilder.get());
            int size = cr.bdd.getNodeCount() - 1;
            double cost = computeCost(cr.bdd, candidateList);
            if (best == null || size < best.size || (size == best.size && cost < best.cost)) {
                best = new Result(target, size, cost, cr.bdd, cr.results);
            }
        }
        return best;
    }

    /**
     * Moves a contiguous block of elements from [start, end] to begin at targetStart.
     */
    private static void moveBlock(Condition[] order, int start, int end, int targetStart) {
        if (targetStart == start) {
            return;
        }

        int blockSize = end - start + 1;
        Condition[] block = new Condition[blockSize];
        System.arraycopy(order, start, block, 0, blockSize);

        if (targetStart < start) {
            // Move block earlier: shift elements [targetStart, start) to the right
            System.arraycopy(order, targetStart, order, targetStart + blockSize, start - targetStart);
            System.arraycopy(block, 0, order, targetStart, blockSize);
        } else {
            // Move block later: shift elements (end, targetStart + blockSize) to the left
            int shiftStart = end + 1;
            int shiftEnd = targetStart + blockSize;
            if (shiftEnd > order.length) {
                shiftEnd = order.length;
            }
            System.arraycopy(order, shiftStart, order, start, shiftEnd - shiftStart);
            System.arraycopy(block, 0, order, targetStart, blockSize);
        }
    }

    private void runAdjacentSwaps(State state) {
        if (state.currentSize > granularMaxNodes) {
            return;
        }

        for (int sweep = 0; sweep < 3; sweep++) {
            PassContext ctx = new PassContext(state, dependencyGraph);
            for (int i = 0; i < state.order.length - 1; i++) {
                // Adjacent swap requires both elements to be able to occupy each other's positions
                if (ctx.constraints.canMove(i, i + 1) && ctx.constraints.canMove(i + 1, i)) {
                    BddCompilerSupport.move(state.order, i, i + 1);
                    BddCompilerSupport.BddCompilationResult cr = BddCompilerSupport.compile(
                            cfg,
                            state.orderView,
                            threadBuilder.get());
                    int size = cr.bdd.getNodeCount() - 1;
                    if (size < ctx.bestSize) {
                        ctx.recordImprovement(new Result(i + 1, size, cr.bdd, cr.results, null));
                    } else {
                        BddCompilerSupport.move(state.order, i + 1, i);
                    }
                }
            }
            if (ctx.improvements == 0) {
                break;
            }
            state.update(ctx.bestBdd, ctx.bestSize, ctx.bestResults);
        }
    }

    private PassContext runPass(State state, AdaptiveEffort effort) {
        PassContext ctx = new PassContext(state, dependencyGraph);
        int[] nodeCounts = computeNodeCountsPerVariable(state.bestBdd);
        int[] selectedIndices = selectConditionsByPriority(state.orderView.size(), nodeCounts, effort.sampleRate);

        for (int varIdx : selectedIndices) {
            List<Integer> positions = getStrategicPositions(varIdx, ctx.constraints, effort, state.orderView.size());
            if (positions.isEmpty()) {
                continue;
            }
            Result best = findBestPosition(positions, ctx, varIdx);
            if (best != null && best.size <= ctx.bestSize) {
                BddCompilerSupport.move(ctx.order, varIdx, best.position);
                ctx.recordImprovement(best);
            }
        }
        return ctx;
    }

    /**
     * Computes the number of BDD nodes testing each variable.
     */
    private static int[] computeNodeCountsPerVariable(Bdd bdd) {
        int[] counts = new int[bdd.getConditionCount()];
        for (int i = 0; i < bdd.getNodeCount(); i++) {
            int v = bdd.getVariable(i);
            if (v >= 0 && v < counts.length) {
                counts[v]++;
            }
        }
        return counts;
    }

    private static int[] selectConditionsByPriority(int n, int[] nodeCounts, int sampleRate) {
        int[] indices = IntStream.range(0, n)
                .boxed()
                .sorted((a, b) -> Integer.compare(nodeCounts[b], nodeCounts[a]))
                .mapToInt(i -> i)
                .toArray();
        return sampleRate <= 1 ? indices : Arrays.copyOf(indices, Math.max(1, n / sampleRate));
    }

    /** Two-pass position finder: compile candidates, then cost-break ties among min-size. */
    private Result findBestPosition(List<Integer> positions, PassContext ctx, int varIdx) {
        // First pass: compile all candidates
        List<Result> candidates = (positions.size() > PARALLEL_THRESHOLD
                ? positions.parallelStream()
                : positions.stream())
                .map(pos -> {
                    Condition[] order = ctx.order.clone();
                    BddCompilerSupport.move(order, varIdx, pos);
                    List<Condition> orderList = Arrays.asList(order);
                    BddCompilerSupport.BddCompilationResult cr =
                            BddCompilerSupport.compile(cfg, orderList, threadBuilder.get());
                    return new Result(pos, cr.bdd.getNodeCount() - 1, cr.bdd, cr.results, orderList);
                })
                .filter(c -> c.size <= ctx.bestSize)
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            return null;
        }

        // Second pass: among min-size candidates, pick lowest cost
        int minSize = Integer.MAX_VALUE;
        for (Result c : candidates) {
            if (c.size < minSize) {
                minSize = c.size;
            }
        }

        Result best = null;
        for (Result c : candidates) {
            if (c.size == minSize) {
                double cost = computeCost(c.bdd, c.orderList);
                if (best == null || cost < best.cost || (cost == best.cost && c.position < best.position)) {
                    best = new Result(c.position, c.size, cost, c.bdd, c.results);
                }
            }
        }
        return best;
    }

    private double computeCost(Bdd bdd, List<Condition> ordering) {
        return costEstimator.expectedCost(bdd, ordering);
    }

    private static List<Integer> getStrategicPositions(
            int varIdx,
            ConditionDependencyGraph.OrderConstraints c,
            AdaptiveEffort ae,
            int orderSize
    ) {
        int min = c.getMinValidPosition(varIdx);
        int max = c.getMaxValidPosition(varIdx);
        int range = max - min;

        // Exhaustive for small ranges
        if (range <= ae.base.exhaustiveThreshold) {
            List<Integer> pos = new ArrayList<>(range);
            for (int p = min; p < max; p++) {
                if (p != varIdx && c.canMove(varIdx, p)) {
                    pos.add(p);
                }
            }
            return pos;
        }

        List<Integer> pos = new ArrayList<>(ae.maxPositions);
        boolean[] seen = new boolean[orderSize];

        // Extremes
        if (min != varIdx && c.canMove(varIdx, min)) {
            pos.add(min);
            seen[min] = true;
        }

        if (max - 1 != varIdx && c.canMove(varIdx, max - 1)) {
            pos.add(max - 1);
            seen[max - 1] = true;
        }

        // Global sampling
        int step = Math.max(1, range / Math.min(15, ae.maxPositions / 2));
        for (int p = min + step; p < max - step && pos.size() < ae.maxPositions; p += step) {
            if (p != varIdx && !seen[p] && c.canMove(varIdx, p)) {
                pos.add(p);
                seen[p] = true;
            }
        }

        // Local neighborhood
        for (int off = -ae.nearbyRadius; off <= ae.nearbyRadius && pos.size() < ae.maxPositions; off++) {
            int p = varIdx + off;
            if (off != 0 && p >= min && p < max && !seen[p] && c.canMove(varIdx, p)) {
                pos.add(p);
                seen[p] = true;
            }
        }
        return pos;
    }

    /** Mutable context for tracking optimization progress within a pass. */
    private static final class PassContext {
        final Condition[] order;
        final List<Condition> orderView;
        final ConditionDependencyGraph dependencyGraph;
        ConditionDependencyGraph.OrderConstraints constraints;
        Bdd bestBdd;
        int bestSize;
        List<Rule> bestResults;
        int improvements;

        PassContext(State state, ConditionDependencyGraph dependencyGraph) {
            this.order = state.order;
            this.orderView = state.orderView;
            this.bestSize = state.currentSize;
            this.dependencyGraph = dependencyGraph;
            this.constraints = dependencyGraph.createOrderConstraints(orderView);
        }

        void recordImprovement(Result result) {
            this.bestBdd = result.bdd;
            this.bestSize = result.size;
            this.bestResults = result.results;
            this.constraints = dependencyGraph.createOrderConstraints(orderView);
            this.improvements++;
        }
    }

    /** Result holder for BDD compilation with optional position/cost metadata. */
    private static final class Result {
        final int position;
        final int size;
        final double cost;
        final Bdd bdd;
        final List<Rule> results;
        final List<Condition> orderList; // For deferred cost computation

        Result(int position, int size, Bdd bdd, List<Rule> results, List<Condition> orderList) {
            this(position, size, Double.MAX_VALUE, bdd, results, orderList);
        }

        Result(int position, int size, double cost, Bdd bdd, List<Rule> results) {
            this(position, size, cost, bdd, results, null);
        }

        Result(int position, int size, double cost, Bdd bdd, List<Rule> results, List<Condition> orderList) {
            this.position = position;
            this.size = size;
            this.cost = cost;
            this.bdd = bdd;
            this.results = results;
            this.orderList = orderList;
        }
    }

    /** Tracks overall optimization state across stages. */
    private static final class State {
        final Condition[] order;
        final List<Condition> orderView;
        final int initialSize;
        Bdd bestBdd;
        int currentSize;
        List<Rule> results;

        State(Condition[] order, List<Condition> orderView, Bdd bdd, int size, List<Rule> results) {
            this.order = order;
            this.orderView = orderView;
            this.bestBdd = bdd;
            this.currentSize = size;
            this.initialSize = size;
            this.results = results;
        }

        void update(Bdd bdd, int size, List<Rule> results) {
            this.bestBdd = bdd;
            this.currentSize = size;
            this.results = results;
        }
    }

    public static final class Builder implements SmithyBuilder<SiftingOptimization> {
        private Cfg cfg;
        private int coarseMinNodes = OptimizationEffort.COARSE.defaultNodeThreshold;
        private int coarseMaxPasses = OptimizationEffort.COARSE.defaultMaxPasses;
        private int mediumMinNodes = OptimizationEffort.MEDIUM.defaultNodeThreshold;
        private int mediumMaxPasses = OptimizationEffort.MEDIUM.defaultMaxPasses;
        private int granularMaxNodes = OptimizationEffort.GRANULAR.defaultNodeThreshold;
        private int granularMaxPasses = OptimizationEffort.GRANULAR.defaultMaxPasses;

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
         * @param minNodeCount the target size to stop coarse optimization (default: 4,000)
         * @param maxPasses the maximum number of coarse passes (default: 6)
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
         * @param minNodeCount the target size to stop medium optimization (default: 1,000)
         * @param maxPasses the maximum number of medium passes (default: 6)
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
         * @param maxNodeCount the maximum size to attempt granular optimization (default: 8,000)
         * @param maxPasses the maximum number of granular passes (default: 12)
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
