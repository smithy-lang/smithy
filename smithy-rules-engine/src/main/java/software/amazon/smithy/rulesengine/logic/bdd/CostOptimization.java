/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.logic.ConditionCostModel;
import software.amazon.smithy.rulesengine.logic.cfg.Cfg;
import software.amazon.smithy.rulesengine.logic.cfg.ConditionDependencyGraph;
import software.amazon.smithy.rulesengine.traits.EndpointBddTrait;
import software.amazon.smithy.utils.SmithyBuilder;

/**
 * A kind of sifting optimization that refines BDD ordering to minimize expected evaluation cost.
 *
 * <p>Uses greedy hill-climbing: for each hot variable (by reach probability × cost), probes strategic target
 * positions and accepts the best improving move per round.
 */
public final class CostOptimization implements Function<EndpointBddTrait, EndpointBddTrait> {
    private static final Logger LOGGER = Logger.getLogger(CostOptimization.class.getName());

    private final Cfg cfg;
    private final ConditionCostModel costModel;
    private final ToDoubleFunction<Condition> trueProbability;
    private final ConditionDependencyGraph dependencyGraph;
    private final BddBuilder builder = new BddBuilder();
    private final double maxAllowedGrowth;
    private final int maxRounds;
    private final int topK;

    private CostOptimization(Builder builder) {
        this.cfg = SmithyBuilder.requiredState("cfg", builder.cfg);
        this.costModel = builder.costModel != null ? builder.costModel : ConditionCostModel.createDefault();
        this.trueProbability = builder.trueProbability;
        this.maxAllowedGrowth = builder.maxAllowedGrowth;
        this.maxRounds = builder.maxRounds;
        this.topK = builder.topK;
        this.dependencyGraph = new ConditionDependencyGraph(Arrays.asList(cfg.getConditions()));
    }

    /**
     * Creates a new builder.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public EndpointBddTrait apply(EndpointBddTrait trait) {
        State state = new State(trait);
        BddCostEstimator estimator = new BddCostEstimator(trait.getConditions(), costModel, trueProbability);

        LOGGER.info(String.format("Starting cost refinement: %d conditions, initial cost %.0f, max nodes %d " +
                "(%.0f%% delta)", state.order.length, state.initialCost, state.maxNodes, maxAllowedGrowth * 100));

        for (int round = 0; round < maxRounds; round++) {
            int[] bestMove = findBestMove(state, estimator);
            if (bestMove == null) {
                LOGGER.info(String.format("Converged after %d rounds", round + 1));
                break;
            }

            applyMove(state, bestMove[0], bestMove[1], estimator);
            polish(state, estimator);
        }

        logResults(state);
        return buildResult(trait, state);
    }

    /**
     * Finds the best move among hot positions. Returns [from, to] or null if no improvement.
     */
    private int[] findBestMove(State state, BddCostEstimator estimator) {
        int[] hotPositions = getHotPositions(state.bdd, state.order, estimator, topK);
        boolean[] seen = new boolean[state.order.length];
        double bestCost = state.currentCost;
        int bestFrom = -1;
        int bestTo = -1;

        for (int hotPos : hotPositions) {
            List<Integer> targets = buildTargets(hotPos, state.constraints, seen);

            for (int target : targets) {
                if (!state.constraints.canMove(hotPos, target)) {
                    continue;
                }

                Condition[] candidate = state.order.clone();
                BddCompilerSupport.move(candidate, hotPos, target);
                CandidateResult result = evaluateCandidate(candidate, state, estimator);
                if (result != null && result.cost < bestCost) {
                    bestCost = result.cost;
                    bestFrom = hotPos;
                    bestTo = target;
                }
            }
        }

        return bestFrom >= 0 ? new int[] {bestFrom, bestTo} : null;
    }

    /**
     * Compiles a candidate ordering and returns result with cost, or null if it exceeds the node budget.
     */
    private CandidateResult evaluateCandidate(Condition[] candidate, State state, BddCostEstimator estimator) {
        List<Condition> candidateList = Arrays.asList(candidate);
        BddCompilerSupport.BddCompilationResult result =
                BddCompilerSupport.compile(cfg, candidateList, builder);
        state.totalCompiles++;

        int nodes = BddCompilerSupport.nodeCount(result.bdd);
        if (nodes > state.maxNodes) {
            return null;
        }

        double cost = estimator.expectedCost(result.bdd, candidateList);
        return new CandidateResult(result.bdd, cost);
    }

    private static final class CandidateResult {
        final Bdd bdd;
        final double cost;

        CandidateResult(Bdd bdd, double cost) {
            this.bdd = bdd;
            this.cost = cost;
        }
    }

    /**
     * Builds deduplicated target positions for a hot position.
     */
    private List<Integer> buildTargets(
            int hotPos,
            ConditionDependencyGraph.OrderConstraints constraints,
            boolean[] seen
    ) {
        int minPos = constraints.getMinValidPosition(hotPos);
        int maxPos = constraints.getMaxValidPosition(hotPos);

        Arrays.fill(seen, false);
        List<Integer> targets = new ArrayList<>();

        // Forward targets (toward front)
        addTarget(minPos, hotPos, seen, targets);
        addTarget(0, hotPos, seen, targets);
        for (int delta : new int[] {1, 2, 4, 8, 16, 32}) {
            addTarget(hotPos - delta, hotPos, seen, targets);
        }

        // Backward targets (toward back)
        addTarget(maxPos, hotPos, seen, targets);
        for (int delta : new int[] {1, 2, 4}) {
            addTarget(hotPos + delta, hotPos, seen, targets);
        }

        return targets;
    }

    /**
     * Applies a move and updates state.
     */
    private void applyMove(State state, int from, int to, BddCostEstimator estimator) {
        BddCompilerSupport.move(state.order, from, to);
        List<Condition> newOrdering = Arrays.asList(state.order);
        BddCompilerSupport.BddCompilationResult result = BddCompilerSupport.compile(cfg, newOrdering, builder);
        state.bdd = result.bdd;
        state.currentCost = estimator.expectedCost(result.bdd, newOrdering);
        state.constraints = dependencyGraph.createOrderConstraints(newOrdering);
    }

    /**
     * Performs 2 sweeps of adjacent-swap hill climbing to polish the current ordering.
     */
    private void polish(State state, BddCostEstimator estimator) {
        for (int sweep = 0; sweep < 2; sweep++) {
            boolean improved = false;
            for (int i = 0; i < state.order.length - 1; i++) {
                // Adjacent swap requires both elements to be able to occupy each other's positions
                if (!state.constraints.canMove(i + 1, i) || !state.constraints.canMove(i, i + 1)) {
                    continue;
                }

                Condition[] candidate = state.order.clone();
                BddCompilerSupport.move(candidate, i + 1, i);
                CandidateResult result = evaluateCandidate(candidate, state, estimator);
                if (result != null && result.cost < state.currentCost) {
                    state.order = candidate;
                    state.bdd = result.bdd;
                    state.currentCost = result.cost;
                    state.constraints = dependencyGraph.createOrderConstraints(Arrays.asList(candidate));
                    improved = true;
                }
            }

            if (!improved) {
                break;
            }
        }
    }

    private void logResults(State state) {
        int finalNodes = BddCompilerSupport.nodeCount(state.bdd);
        double costReduction = (1.0 - state.currentCost / state.initialCost) * 100;
        double nodeGrowth = (finalNodes - state.baselineNodes) * 100.0 / state.baselineNodes;
        LOGGER.info(String.format("Cost refinement complete: cost %.0f -> %.0f (%.1f%% reduction), " +
                "nodes %d -> %d (%.1f%% growth), %d compiles",
                state.initialCost,
                state.currentCost,
                costReduction,
                state.baselineNodes,
                finalNodes,
                nodeGrowth,
                state.totalCompiles));
    }

    private EndpointBddTrait buildResult(EndpointBddTrait trait, State state) {
        List<Condition> finalOrdering = Arrays.asList(state.order);
        BddCompilerSupport.BddCompilationResult finalResult =
                BddCompilerSupport.compile(cfg, finalOrdering, builder);

        return trait.toBuilder()
                .conditions(finalOrdering)
                .results(finalResult.results)
                .bdd(finalResult.bdd)
                .build();
    }

    private int[] getHotPositions(Bdd bdd, Condition[] order, BddCostEstimator estimator, int limit) {
        double[] reachProbs = estimator.reachProbabilities(bdd, Arrays.asList(order));
        double[] hotness = new double[order.length];
        for (int i = 0; i < order.length; i++) {
            hotness[i] = reachProbs[i] * estimator.cost(order[i]);
        }

        return IntStream.range(0, order.length)
                .boxed()
                .sorted((a, b) -> Double.compare(hotness[b], hotness[a]))
                .limit(limit)
                .mapToInt(Integer::intValue)
                .toArray();
    }

    private static void addTarget(int target, int hotPos, boolean[] seen, List<Integer> targets) {
        if (target >= 0 && target < seen.length && target != hotPos && !seen[target]) {
            seen[target] = true;
            targets.add(target);
        }
    }

    /**
     * Mutable state for the optimization loop.
     */
    private final class State {
        Condition[] order;
        Bdd bdd;
        ConditionDependencyGraph.OrderConstraints constraints;
        final int baselineNodes;
        final int maxNodes;
        final double initialCost;
        double currentCost;
        int totalCompiles;

        State(EndpointBddTrait trait) {
            List<Condition> conditions = trait.getConditions();
            this.order = conditions.toArray(new Condition[0]);
            this.bdd = trait.getBdd();
            this.baselineNodes = BddCompilerSupport.nodeCount(bdd);
            this.maxNodes = baselineNodes + (int) (baselineNodes * maxAllowedGrowth);
            this.constraints = dependencyGraph.createOrderConstraints(conditions);
            BddCostEstimator estimator = new BddCostEstimator(conditions, costModel, trueProbability);
            this.initialCost = estimator.expectedCost(bdd, conditions);
            this.currentCost = initialCost;
            this.totalCompiles = 0;
        }
    }

    /**
     * Builder for {@link CostOptimization}.
     */
    public static final class Builder implements SmithyBuilder<CostOptimization> {
        private Cfg cfg;
        private ConditionCostModel costModel;
        private ToDoubleFunction<Condition> trueProbability;
        private double maxAllowedGrowth = 0.1;
        private int maxRounds = 30;
        private int topK = 50;

        private Builder() {}

        /**
         * Sets the control flow graph (required).
         *
         * @param cfg the CFG
         * @return the builder
         */
        public Builder cfg(Cfg cfg) {
            this.cfg = cfg;
            return this;
        }

        /**
         * Sets the cost model for conditions.
         *
         * @param costModel the cost model
         * @return the builder
         */
        public Builder costModel(ConditionCostModel costModel) {
            this.costModel = costModel;
            return this;
        }

        /**
         * Sets the function that returns the probability a condition evaluates to true.
         *
         * @param trueProbability function returning probability in [0.0, 1.0] (null for uniform 0.5)
         * @return the builder
         */
        public Builder trueProbability(ToDoubleFunction<Condition> trueProbability) {
            this.trueProbability = trueProbability;
            return this;
        }

        /**
         * Sets the maximum allowed node growth as a fraction (default 0.1 or 10%).
         *
         * @param maxAllowedGrowth maximum growth (0.0 = no growth, 0.1 = 10% growth)
         * @return the builder
         */
        public Builder maxAllowedGrowth(double maxAllowedGrowth) {
            this.maxAllowedGrowth = maxAllowedGrowth;
            return this;
        }

        /**
         * Sets the maximum number of optimization rounds (default 30).
         *
         * @param maxRounds maximum rounds
         * @return the builder
         */
        public Builder maxRounds(int maxRounds) {
            this.maxRounds = maxRounds;
            return this;
        }

        /**
         * Sets the number of hot positions to probe per round (default 50).
         *
         * <p>"Hot" positions are conditions ranked by {@code reachProbability × cost}.
         * These are conditions that are both frequently evaluated and expensive,
         * making them the best candidates for reordering to reduce expected cost.
         *
         * @param topK number of hot positions to consider moving each round
         * @return the builder
         */
        public Builder topK(int topK) {
            this.topK = topK;
            return this;
        }

        @Override
        public CostOptimization build() {
            return new CostOptimization(this);
        }
    }
}
