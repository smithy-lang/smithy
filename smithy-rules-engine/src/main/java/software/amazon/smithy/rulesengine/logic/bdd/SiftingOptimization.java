/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.logic.cfg.Cfg;
import software.amazon.smithy.rulesengine.logic.cfg.ConditionDependencyGraph;
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

    // When to use a parallel stream
    private static final int PARALLEL_THRESHOLD = 7;

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
        COARSE(11, 4, 0, 20, 4_000, 6),
        MEDIUM(2, 20, 6, 20, 1_000, 6),
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
        OptimizationState state = initializeOptimization(trait);
        LOGGER.info(String.format("Initial size: %d nodes", state.initialSize));

        state = runOptimizationStage("Coarse", state, OptimizationEffort.COARSE, coarseMinNodes, coarseMaxPasses, 4.0);
        state = runOptimizationStage("Medium", state, OptimizationEffort.MEDIUM, mediumMinNodes, mediumMaxPasses, 1.5);
        if (state.currentSize <= granularMaxNodes) {
            state = runOptimizationStage("Granular", state, OptimizationEffort.GRANULAR, 0, granularMaxPasses, 0.0);
        } else {
            LOGGER.info("Skipping granular stage - too large");
        }
        state = runAdjacentSwaps(state);

        double totalTimeInSeconds = (System.currentTimeMillis() - startTime) / 1000.0;

        if (state.bestSize >= state.initialSize) {
            LOGGER.info(String.format("No improvements found in %fs", totalTimeInSeconds));
            return trait;
        }

        LOGGER.info(String.format("Optimization complete: %d -> %d nodes (%.1f%% total reduction) in %fs",
                state.initialSize,
                state.bestSize,
                (1.0 - (double) state.bestSize / state.initialSize) * 100,
                totalTimeInSeconds));

        return trait.toBuilder().conditions(state.orderView).results(state.results).bdd(state.bestBdd).build();
    }

    private OptimizationState initializeOptimization(EndpointBddTrait trait) {
        // Use the trait's existing ordering as the starting point
        List<Condition> initialOrder = new ArrayList<>(trait.getConditions());
        Condition[] order = initialOrder.toArray(new Condition[0]);
        List<Condition> orderView = Arrays.asList(order);
        Bdd bdd = trait.getBdd();
        int initialSize = bdd.getNodeCount() - 1;
        return new OptimizationState(order, orderView, bdd, initialSize, initialSize, trait.getResults());
    }

    private OptimizationState runOptimizationStage(
            String stageName,
            OptimizationState state,
            OptimizationEffort effort,
            int targetNodeCount,
            int maxPasses,
            double minReductionPercent
    ) {
        if (targetNodeCount > 0 && state.currentSize <= targetNodeCount) {
            return state;
        }

        LOGGER.info(String.format("Stage: %s optimization (%d nodes%s)",
                stageName,
                state.currentSize,
                targetNodeCount > 0 ? String.format(", target < %d", targetNodeCount) : ""));

        OptimizationState currentState = state;
        for (int pass = 1; pass <= maxPasses; pass++) {
            if (targetNodeCount > 0 && currentState.currentSize <= targetNodeCount) {
                break;
            }

            int passStartSize = currentState.currentSize;
            OptimizationResult result = runPass(currentState, effort);
            if (result.improved) {
                currentState = currentState.withResult(result.bdd, result.size, result.results);
                double reduction = (1.0 - (double) result.size / passStartSize) * 100;
                LOGGER.fine(String.format("%s pass %d: %d -> %d nodes (%.1f%% reduction)",
                        stageName,
                        pass,
                        passStartSize,
                        result.size,
                        reduction));
                if (minReductionPercent > 0 && reduction < minReductionPercent) {
                    LOGGER.fine(String.format("%s optimization yielding diminishing returns", stageName));
                    break;
                }
            } else {
                LOGGER.fine(String.format("%s pass %d found no improvements", stageName, pass));
                break;
            }
        }

        return currentState;
    }

    private OptimizationState runAdjacentSwaps(OptimizationState state) {
        if (state.currentSize > granularMaxNodes) {
            return state;
        }

        LOGGER.info("Running adjacent swaps optimization");
        OptimizationState currentState = state;

        // Run multiple sweeps until no improvement
        for (int sweep = 1; sweep <= 3; sweep++) {
            OptimizationContext context = new OptimizationContext(currentState, dependencyGraph);
            int startSize = currentState.currentSize;

            for (int i = 0; i < currentState.order.length - 1; i++) {
                if (context.constraints.canMove(i, i + 1)) {
                    move(currentState.order, i, i + 1);
                    BddCompilationResult compilationResult = compileBddWithResults(currentState.orderView);
                    int swappedSize = compilationResult.bdd.getNodeCount() - 1;
                    if (swappedSize < context.bestSize) {
                        context = context.withImprovement(
                                new PositionResult(i + 1,
                                        swappedSize,
                                        compilationResult.bdd,
                                        compilationResult.results));
                    } else {
                        move(currentState.order, i + 1, i); // Swap back
                    }
                }
            }

            if (context.improvements > 0) {
                currentState = currentState.withResult(context.bestBdd, context.bestSize, context.bestResults);
                LOGGER.fine(String.format("Adjacent swaps sweep %d: %d -> %d nodes",
                        sweep,
                        startSize,
                        context.bestSize));
            } else {
                break;
            }
        }

        return currentState;
    }

    private OptimizationResult runPass(OptimizationState state, OptimizationEffort effort) {
        OptimizationContext context = new OptimizationContext(state, dependencyGraph);

        List<Condition> selectedConditions = IntStream.range(0, state.orderView.size())
                .filter(i -> i % effort.sampleRate == 0)
                .mapToObj(state.orderView::get)
                .collect(Collectors.toList());

        for (Condition condition : selectedConditions) {
            Integer varIdx = context.liveIndex.get(condition);
            if (varIdx == null) {
                continue;
            }

            List<Integer> positions = getStrategicPositions(varIdx, context.constraints, effort);
            if (positions.isEmpty()) {
                continue;
            }

            context = tryImprovePosition(context, varIdx, positions);
        }

        return context.toResult();
    }

    private OptimizationContext tryImprovePosition(OptimizationContext context, int varIdx, List<Integer> positions) {
        PositionResult best = findBestPosition(positions, context, varIdx);
        if (best != null && best.count <= context.bestSize) { // Accept ties
            move(context.order, varIdx, best.position);
            return context.withImprovement(best);
        }

        return context;
    }

    private PositionResult findBestPosition(List<Integer> positions, OptimizationContext ctx, int varIdx) {
        return (positions.size() > PARALLEL_THRESHOLD ? positions.parallelStream() : positions.stream())
                .map(pos -> {
                    Condition[] order = ctx.order.clone();
                    move(order, varIdx, pos);
                    BddCompilationResult cr = compileBddWithResults(Arrays.asList(order));
                    return new PositionResult(pos, cr.bdd.getNodeCount() - 1, cr.bdd, cr.results);
                })
                .filter(pr -> pr.count <= ctx.bestSize)
                .min(Comparator.comparingInt((PositionResult pr) -> pr.count).thenComparingInt(pr -> pr.position))
                .orElse(null);
    }

    private static List<Integer> getStrategicPositions(
            int varIdx,
            ConditionDependencyGraph.OrderConstraints constraints,
            OptimizationEffort effort
    ) {
        int min = constraints.getMinValidPosition(varIdx);
        int max = constraints.getMaxValidPosition(varIdx);
        int range = max - min;

        if (range <= effort.exhaustiveThreshold) {
            List<Integer> positions = new ArrayList<>(range);
            for (int p = min; p < max; p++) {
                if (p != varIdx && constraints.canMove(varIdx, p)) {
                    positions.add(p);
                }
            }
            return positions;
        }

        List<Integer> positions = new ArrayList<>(effort.maxPositions);

        // Test extremes first since they often yield the best improvements
        if (min != varIdx && constraints.canMove(varIdx, min)) {
            positions.add(min);
        }
        if (positions.size() >= effort.maxPositions) {
            return positions;
        }

        if (max - 1 != varIdx && constraints.canMove(varIdx, max - 1)) {
            positions.add(max - 1);
        }
        if (positions.size() >= effort.maxPositions) {
            return positions;
        }

        // Test local moves that preserve relative ordering with neighbors
        for (int offset = -effort.nearbyRadius; offset <= effort.nearbyRadius; offset++) {
            if (offset != 0) {
                if (positions.size() >= effort.maxPositions) {
                    return positions;
                }
                int p = varIdx + offset;
                if (p >= min && p < max && !positions.contains(p) && constraints.canMove(varIdx, p)) {
                    positions.add(p);
                }
            }
        }

        // Sample intermediate positions to find global improvements
        if (positions.size() >= effort.maxPositions) {
            return positions;
        }

        int maxSamples = Math.min(15, effort.maxPositions / 2);
        int samples = Math.min(maxSamples, Math.max(2, range / 4));
        int step = Math.max(1, range / samples);

        for (int p = min + step; p < max - step && positions.size() < effort.maxPositions; p += step) {
            if (p != varIdx && !positions.contains(p) && constraints.canMove(varIdx, p)) {
                positions.add(p);
            }
        }
        return positions;
    }

    private static void move(Condition[] arr, int from, int to) {
        if (from == to) {
            return;
        }

        Condition moving = arr[from];
        if (from < to) {
            System.arraycopy(arr, from + 1, arr, from, to - from);
        } else {
            System.arraycopy(arr, to, arr, to + 1, from - to);
        }
        arr[to] = moving;
    }

    private static Map<Condition, Integer> rebuildIndex(List<Condition> orderView) {
        Map<Condition, Integer> index = new IdentityHashMap<>();
        for (int i = 0; i < orderView.size(); i++) {
            index.put(orderView.get(i), i);
        }
        return index;
    }

    private BddCompilationResult compileBddWithResults(List<Condition> ordering) {
        BddBuilder builder = threadBuilder.get().reset();
        BddCompiler compiler = new BddCompiler(cfg, OrderingStrategy.fixed(ordering), builder);
        Bdd bdd = compiler.compile();
        return new BddCompilationResult(bdd, compiler.getIndexedResults());
    }

    // Helper class to track optimization context within a pass
    private static final class OptimizationContext {
        final Condition[] order;
        final List<Condition> orderView;
        final ConditionDependencyGraph dependencyGraph;
        final ConditionDependencyGraph.OrderConstraints constraints;
        final Map<Condition, Integer> liveIndex;
        final Bdd bestBdd;
        final int bestSize;
        final List<Rule> bestResults;
        final int improvements;

        OptimizationContext(OptimizationState state, ConditionDependencyGraph dependencyGraph) {
            this.order = state.order;
            this.orderView = state.orderView;
            this.dependencyGraph = dependencyGraph;
            this.constraints = dependencyGraph.createOrderConstraints(orderView);
            this.liveIndex = rebuildIndex(orderView);
            this.bestBdd = null;
            this.bestSize = state.currentSize;
            this.bestResults = null;
            this.improvements = 0;
        }

        private OptimizationContext(
                Condition[] order,
                List<Condition> orderView,
                ConditionDependencyGraph dependencyGraph,
                ConditionDependencyGraph.OrderConstraints constraints,
                Map<Condition, Integer> liveIndex,
                Bdd bestBdd,
                int bestSize,
                List<Rule> bestResults,
                int improvements
        ) {
            this.order = order;
            this.orderView = orderView;
            this.dependencyGraph = dependencyGraph;
            this.constraints = constraints;
            this.liveIndex = liveIndex;
            this.bestBdd = bestBdd;
            this.bestSize = bestSize;
            this.bestResults = bestResults;
            this.improvements = improvements;
        }

        OptimizationContext withImprovement(PositionResult result) {
            ConditionDependencyGraph.OrderConstraints newConstraints =
                    dependencyGraph.createOrderConstraints(orderView);
            Map<Condition, Integer> newIndex = rebuildIndex(orderView);
            return new OptimizationContext(order,
                    orderView,
                    dependencyGraph,
                    newConstraints,
                    newIndex,
                    result.bdd,
                    result.count,
                    result.results,
                    improvements + 1);
        }

        OptimizationResult toResult() {
            return new OptimizationResult(bestBdd, bestSize, improvements > 0, bestResults);
        }
    }

    private static final class BddCompilationResult {
        final Bdd bdd;
        final List<Rule> results;

        BddCompilationResult(Bdd bdd, List<Rule> results) {
            this.bdd = bdd;
            this.results = results;
        }
    }

    private static final class PositionResult {
        final int position;
        final int count;
        final Bdd bdd;
        final List<Rule> results;

        PositionResult(int position, int count, Bdd bdd, List<Rule> results) {
            this.position = position;
            this.count = count;
            this.bdd = bdd;
            this.results = results;
        }
    }

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
