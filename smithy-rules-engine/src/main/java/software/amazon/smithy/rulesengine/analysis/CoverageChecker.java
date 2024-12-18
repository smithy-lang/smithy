/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.analysis;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.TraversingVisitor;
import software.amazon.smithy.rulesengine.language.evaluation.RuleEvaluator;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.traits.EndpointTestCase;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Analyzer for determining coverage of a rule-set.
 */
@SmithyUnstableApi
public final class CoverageChecker {
    private final CoverageCheckerCore checkerCore;
    private final EndpointRuleSet ruleSet;

    public CoverageChecker(EndpointRuleSet ruleSet) {
        this.ruleSet = ruleSet;
        checkerCore = new CoverageCheckerCore();
    }

    /**
     * Evaluates the rule-set with the given inputs to determine rule coverage.
     *
     * @param input the map parameters and inputs to test coverage.
     */
    public void evaluateInput(Map<Identifier, Value> input) {
        checkerCore.evaluateRuleSet(ruleSet, input);
    }

    /**
     * Evaluate the rule-set using the given test case to determine rule coverage.
     *
     * @param testCase the test case to evaluate.
     */
    public void evaluateTestCase(EndpointTestCase testCase) {
        Map<Identifier, Value> map = new LinkedHashMap<>();
        for (Map.Entry<String, Node> entry : testCase.getParams().getStringMap().entrySet()) {
            map.put(Identifier.of(entry.getKey()), Value.fromNode(entry.getValue()));
        }
        checkerCore.evaluateRuleSet(ruleSet, map);
    }

    /**
     * Analyze coverage for the rule-set, providing results when coverage is found.
     *
     * @return returns a stream of {@link CoverageResult}.
     */
    public Stream<CoverageResult> checkCoverage() {
        return new CollectConditions().visitRuleset(ruleSet).distinct().flatMap(this::getConditionCoverage);
    }

    private Stream<CoverageResult> getConditionCoverage(Condition condition) {
        Set<Boolean> conditionResults = checkerCore.getResult(condition);
        if (conditionResults.size() == 1) {
            return Stream.of(new CoverageResult(condition, !conditionResults.iterator().next()));
        }
        if (conditionResults.size() == 0) {
            return Stream.of(new CoverageResult(condition, false), new CoverageResult(condition, true));
        }
        return Stream.empty();
    }

    private static class CoverageCheckerCore extends RuleEvaluator {
        // Sets are used to contain results, as coverage is only concerned with both
        // having coverage or no happening at all for the condition in question.
        private final Map<Condition, Set<Boolean>> results = new LinkedHashMap<>();

        private Set<Boolean> getResult(Condition condition) {
            return results.getOrDefault(condition, SetUtils.of());
        }

        @Override
        public Value evaluateCondition(Condition condition) {
            // evaluateRuleSet needs to be called first. This class is inner private
            // and ensures this, so we don't need to worry about that at this point.
            Value conditionResult = super.evaluateCondition(condition);
            boolean result = !(conditionResult.isEmpty() || conditionResult.equals(Value.booleanValue(false)));

            Set<Boolean> resultSet = results.getOrDefault(condition, new HashSet<>());
            resultSet.add(result);
            results.put(condition, resultSet);

            return conditionResult;
        }
    }

    private static class CollectConditions extends TraversingVisitor<Condition> {
        @Override
        public Stream<Condition> visitConditions(List<Condition> conditions) {
            return conditions.stream();
        }
    }

    /**
     * A container for a specific condition's coverage result.
     */
    public static class CoverageResult {
        private final Condition condition;
        private final boolean result;

        /**
         * Constructs a new coverage result container for the given condition and result.
         *
         * @param condition the condition being covered.
         * @param result if the condition is covered by test cases or not.
         */
        public CoverageResult(Condition condition, boolean result) {
            this.condition = condition;
            this.result = result;
        }

        /**
         * Gets the condition that is covered.
         *
         * @return the condition being covered.
         */
        public Condition getCondition() {
            return condition;
        }

        /**
         * Gets if the condition is covered or not.
         *
         * @return returns true if the condition is covered, false otherwise.
         */
        public boolean getResult() {
            return result;
        }

        /**
         * Pretty prints this CoverageResult.
         *
         * @return A pretty representation of the condition and result.
         */
        public String pretty() {
            return "leaf: " + condition + "(" + condition.getSourceLocation().getFilename() + ":"
                    + condition.getSourceLocation().getLine() + ")";
        }
    }
}
