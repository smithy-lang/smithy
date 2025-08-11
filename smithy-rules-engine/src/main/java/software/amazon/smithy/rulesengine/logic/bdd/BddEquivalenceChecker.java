/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.NoMatchRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.logic.ConditionEvaluator;
import software.amazon.smithy.rulesengine.logic.cfg.Cfg;
import software.amazon.smithy.rulesengine.logic.cfg.CfgNode;
import software.amazon.smithy.rulesengine.logic.cfg.ConditionNode;
import software.amazon.smithy.rulesengine.logic.cfg.ResultNode;

/**
 * Verifies functional equivalence between a CFG and its BDD representation.
 *
 * <p>This verifier uses structural equivalence checking to ensure that both representations produce the same result.
 * When the BDD has fewer than 20 conditions, the checking is exhaustive. When there are more, random samples are
 * checked up to the earlier of max samples being reached or the max duration being reached.
 */
public final class BddEquivalenceChecker {

    private static final Logger LOGGER = Logger.getLogger(BddEquivalenceChecker.class.getName());

    private static final int EXHAUSTIVE_THRESHOLD = 20;
    private static final int DEFAULT_MAX_SAMPLES = 1_000_000;
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(1);

    private final Cfg cfg;
    private final Bdd bdd;
    private final List<Condition> conditions;
    private final List<Rule> results;
    private final List<Parameter> parameters;
    private final Map<Condition, Integer> conditionToIndex = new HashMap<>();

    private int maxSamples = DEFAULT_MAX_SAMPLES;
    private Duration timeout = DEFAULT_TIMEOUT;

    private int testsRun = 0;
    private long startTime;

    public static BddEquivalenceChecker of(Cfg cfg, Bdd bdd, List<Condition> conditions, List<Rule> results) {
        return new BddEquivalenceChecker(cfg, bdd, conditions, results);
    }

    private BddEquivalenceChecker(Cfg cfg, Bdd bdd, List<Condition> conditions, List<Rule> results) {
        this.cfg = cfg;
        this.bdd = bdd;
        this.conditions = conditions;
        this.results = results;
        this.parameters = new ArrayList<>(cfg.getRuleSet().getParameters().toList());

        for (int i = 0; i < conditions.size(); i++) {
            conditionToIndex.put(conditions.get(i), i);
        }
    }

    /**
     * Sets the maximum number of samples to test for large condition sets.
     *
     * <p>Defaults to a max of 1M samples. Set to {@code <= 0} to disable the max.
     *
     * @param maxSamples the maximum number of samples
     * @return this verifier for method chaining
     */
    public BddEquivalenceChecker setMaxSamples(int maxSamples) {
        if (maxSamples < 1) {
            maxSamples = Integer.MAX_VALUE;
        }
        this.maxSamples = maxSamples;
        return this;
    }

    /**
     * Sets the maximum amount of time to take for the verification (runs until timeout or max samples met).
     *
     * <p>Defaults to a 1-minute timeout if not overridden.
     *
     * @param timeout the timeout duration
     * @return this verifier for method chaining
     */
    public BddEquivalenceChecker setMaxDuration(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    /**
     * Verifies that the BDD produces identical results to the CFG.
     *
     * @throws VerificationException if any discrepancy is found
     */
    public void verify() {
        startTime = System.currentTimeMillis();
        verifyResults();
        testsRun = 0;

        LOGGER.info(() -> String.format("Verifying BDD with %d conditions (max samples: %d, timeout: %s)",
                bdd.getConditionCount(),
                maxSamples,
                timeout));

        if (bdd.getConditionCount() <= EXHAUSTIVE_THRESHOLD) {
            verifyExhaustive();
        } else {
            verifyWithLimits();
        }

        LOGGER.info(String.format("BDD verification passed: %d tests in %s", testsRun, getElapsedDuration()));
    }

    private void verifyResults() {
        Set<Rule> cfgResults = new HashSet<>();
        for (CfgNode node : cfg) {
            if (node instanceof ResultNode) {
                Rule result = ((ResultNode) node).getResult();
                if (result != null) {
                    cfgResults.add(result);
                }
            }
        }

        // Remove the NoMatchRule that's added by default. It's not in the CFG.
        Set<Rule> bddResults = new HashSet<>(results);
        bddResults.removeIf(v -> v == NoMatchRule.INSTANCE);

        if (!cfgResults.equals(bddResults)) {
            Set<Rule> inCfgOnly = new HashSet<>(cfgResults);
            inCfgOnly.removeAll(bddResults);
            Set<Rule> inBddOnly = new HashSet<>(bddResults);
            inBddOnly.removeAll(cfgResults);
            throw new IllegalStateException(String.format(
                    "Result mismatch: CFG has %d results, BDD has %d results (excluding NoMatchRule).%n" +
                            "In CFG only: %s%n" +
                            "In BDD only: %s",
                    cfgResults.size(),
                    bddResults.size(),
                    inCfgOnly,
                    inBddOnly));
        }
    }

    /**
     * Exhaustively tests all possible condition combinations.
     */
    private void verifyExhaustive() {
        long totalCombinations = 1L << bdd.getConditionCount();
        LOGGER.info(() -> "Running exhaustive verification with " + totalCombinations + " combinations");
        for (long mask = 0; mask < totalCombinations; mask++) {
            verifyCase(mask);
            if (hasEitherLimitBeenExceeded()) {
                LOGGER.info(String.format("Exhaustive verification stopped after %d tests "
                        + "(limit: %d samples or %s timeout)", testsRun, maxSamples, timeout));
                break;
            }
        }
    }

    /**
     * Verifies with configured limits (samples and timeout).
     * Continues until EITHER limit is reached: maxSamples reached OR timeout exceeded.
     */
    private void verifyWithLimits() {
        LOGGER.info(() -> String.format("Running limited verification (will stop at %d samples OR %s timeout)",
                maxSamples,
                timeout));
        verifyCriticalCases();

        while (!hasEitherLimitBeenExceeded()) {
            long mask = randomMask();
            verifyCase(mask);
            if (testsRun % 10000 == 0 && testsRun > 0) {
                LOGGER.fine(() -> String.format("Progress: %d tests run, %s elapsed", testsRun, getElapsedDuration()));
            }
        }

        LOGGER.info(() -> String.format("Verification complete: %d tests run in %s", testsRun, getElapsedDuration()));
    }

    /**
     * Tests critical edge cases that are likely to expose bugs.
     */
    private void verifyCriticalCases() {
        LOGGER.fine("Testing critical edge cases");

        // All conditions false
        verifyCase(0);

        // All conditions true
        verifyCase((1L << bdd.getConditionCount()) - 1);

        // Each condition true individually
        for (int i = 0; i < bdd.getConditionCount() && !hasEitherLimitBeenExceeded(); i++) {
            verifyCase(1L << i);
        }

        // Each condition false individually (all others true)
        long allTrue = (1L << bdd.getConditionCount()) - 1;
        for (int i = 0; i < bdd.getConditionCount() && !hasEitherLimitBeenExceeded(); i++) {
            verifyCase(allTrue ^ (1L << i));
        }

        // Alternating patterns: 0101... (even conditions false, odd true)
        if (!hasEitherLimitBeenExceeded()) {
            verifyCase(0x5555555555555555L & ((1L << bdd.getConditionCount()) - 1));
        }

        // Pattern: 1010... (even conditions true, odd false)
        if (!hasEitherLimitBeenExceeded()) {
            verifyCase(0xAAAAAAAAAAAAAAAAL & ((1L << bdd.getConditionCount()) - 1));
        }
    }

    private boolean hasEitherLimitBeenExceeded() {
        return testsRun >= maxSamples || isTimedOut();
    }

    private boolean isTimedOut() {
        return getElapsedDuration().compareTo(timeout) >= 0;
    }

    private Duration getElapsedDuration() {
        return Duration.ofMillis(System.currentTimeMillis() - startTime);
    }

    private void verifyCase(long mask) {
        testsRun++;

        // Create evaluators that will return fixed values for conditions
        FixedMaskEvaluator maskEvaluator = new FixedMaskEvaluator(mask);
        Rule cfgResult = evaluateCfgWithMask(maskEvaluator);
        Rule bddResult = evaluateBdd(mask);

        if (!resultsEqual(cfgResult, bddResult)) {
            StringBuilder errorMsg = new StringBuilder();
            errorMsg.append("BDD verification mismatch found!\n");
            errorMsg.append("Test case #").append(testsRun).append("\n");
            errorMsg.append("Condition mask: ").append(Long.toBinaryString(mask)).append("\n");
            errorMsg.append("\nCondition details:\n");
            for (int i = 0; i < conditions.size(); i++) {
                Condition condition = conditions.get(i);
                boolean value = (mask & (1L << i)) != 0;
                errorMsg.append("  Condition ")
                        .append(i)
                        .append(" [")
                        .append(value)
                        .append("]: ")
                        .append(condition)
                        .append("\n");
            }
            errorMsg.append("\nResults:\n");
            errorMsg.append("  CFG result: ").append(describeResult(cfgResult)).append("\n");
            errorMsg.append("  BDD result: ").append(describeResult(bddResult));
            throw new VerificationException(errorMsg.toString());
        }
    }

    private Rule evaluateCfgWithMask(ConditionEvaluator maskEvaluator) {
        Map<Condition, Integer> cfgConditionToIndex = new HashMap<>();
        Condition[] cfgConditions = cfg.getConditions();
        for (int i = 0; i < cfgConditions.length; i++) {
            cfgConditionToIndex.put(cfgConditions[i], i);
        }

        CfgNode result = evaluateCfgNode(cfg.getRoot(), cfgConditionToIndex, maskEvaluator);
        if (result instanceof ResultNode) {
            return ((ResultNode) result).getResult();
        }

        return null;
    }

    // Recursively evaluates a CFG node.
    private CfgNode evaluateCfgNode(
            CfgNode node,
            Map<Condition, Integer> conditionToIndex,
            ConditionEvaluator maskEvaluator
    ) {
        if (node instanceof ResultNode) {
            return node;
        }

        if (node instanceof ConditionNode) {
            ConditionNode condNode = (ConditionNode) node;
            Condition condition = condNode.getCondition().getCondition();

            Integer index = conditionToIndex.get(condition);
            if (index == null) {
                throw new IllegalStateException("Condition not found in CFG: " + condition);
            }

            boolean conditionResult = maskEvaluator.test(index);

            // Handle negation if the condition reference is negated
            if (condNode.getCondition().isNegated()) {
                conditionResult = !conditionResult;
            }

            // Follow the appropriate branch
            if (conditionResult) {
                return evaluateCfgNode(condNode.getTrueBranch(), conditionToIndex, maskEvaluator);
            } else {
                return evaluateCfgNode(condNode.getFalseBranch(), conditionToIndex, maskEvaluator);
            }
        }

        throw new IllegalStateException("Unknown CFG node type: " + node);
    }

    private Rule evaluateBdd(long mask) {
        FixedMaskEvaluator evaluator = new FixedMaskEvaluator(mask);
        int resultIndex = bdd.evaluate(evaluator);
        return resultIndex < 0 ? null : results.get(resultIndex);
    }

    private boolean resultsEqual(Rule r1, Rule r2) {
        if (r1 == r2) {
            return true;
        } else if (r1 == null || r2 == null) {
            return false;
        } else {
            return r1.withConditions(Collections.emptyList()).equals(r2.withConditions(Collections.emptyList()));
        }
    }

    // Generates a random bit mask for sampling.
    private long randomMask() {
        long mask = 0;
        for (int i = 0; i < bdd.getConditionCount(); i++) {
            if (Math.random() < 0.5) {
                mask |= (1L << i);
            }
        }
        return mask;
    }

    private String describeResult(Rule rule) {
        return rule == null ? "null (no match)" : rule.toString();
    }

    // A condition evaluator that returns values based on a fixed bit mask.
    private static class FixedMaskEvaluator implements ConditionEvaluator {
        private final long mask;

        FixedMaskEvaluator(long mask) {
            this.mask = mask;
        }

        @Override
        public boolean test(int conditionIndex) {
            return (mask & (1L << conditionIndex)) != 0;
        }
    }

    /**
     * Exception thrown when verification fails.
     */
    public static class VerificationException extends RuntimeException {
        public VerificationException(String message) {
            super(message);
        }
    }
}
