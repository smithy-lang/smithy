/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.analysis;

import java.util.BitSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.rulesengine.language.evaluation.RuleEvaluator;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.NoMatchRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.logic.ConditionEvaluator;
import software.amazon.smithy.rulesengine.logic.bdd.Bdd;
import software.amazon.smithy.rulesengine.traits.EndpointBddTrait;
import software.amazon.smithy.rulesengine.traits.EndpointTestCase;

/**
 * Analyzes test coverage for BDD-based endpoint rules.
 */
public final class BddCoverageChecker {

    private final Parameters parameters;
    private final Bdd bdd;
    private final List<Condition> conditions;
    private final List<Rule> results;
    private final BitSet visitedConditions;
    private final BitSet visitedResults;

    public BddCoverageChecker(EndpointBddTrait bddTrait) {
        this(bddTrait.getParameters(), bddTrait.getBdd(), bddTrait.getResults(), bddTrait.getConditions());
    }

    BddCoverageChecker(Parameters parameters, Bdd bdd, List<Rule> results, List<Condition> conditions) {
        this.results = results;
        this.parameters = parameters;
        this.conditions = conditions;
        this.bdd = bdd;
        this.visitedConditions = new BitSet(conditions.size());
        this.visitedResults = new BitSet(results.size());
    }

    /**
     * Evaluates a test case and updates coverage information.
     *
     * @param testCase the test case to evaluate
     */
    public void evaluateTestCase(EndpointTestCase testCase) {
        Map<Identifier, Value> input = new LinkedHashMap<>();
        for (Map.Entry<String, Node> entry : testCase.getParams().getStringMap().entrySet()) {
            input.put(Identifier.of(entry.getKey()), Value.fromNode(entry.getValue()));
        }
        evaluateInput(input);
    }

    /**
     * Evaluates with the given inputs and updates coverage.
     *
     * @param input the input parameters to evaluate
     */
    public void evaluateInput(Map<Identifier, Value> input) {
        TestEvaluator evaluator = new TestEvaluator(input);
        int resultIdx = bdd.evaluate(evaluator);
        if (resultIdx >= 0) {
            visitedResults.set(resultIdx);
        }
    }

    /**
     * Returns conditions that were never evaluated during testing.
     *
     * @return set of unevaluated conditions
     */
    public Set<Condition> getUnevaluatedConditions() {
        Set<Condition> unevaluated = new HashSet<>();
        for (int i = 0; i < conditions.size(); i++) {
            if (!visitedConditions.get(i)) {
                unevaluated.add(conditions.get(i));
            }
        }
        return unevaluated;
    }

    /**
     * Returns results that were never reached during testing.
     *
     * @return set of unreached results
     */
    public Set<Rule> getUnevaluatedResults() {
        Set<Rule> unevaluated = new HashSet<>();
        for (int i = 0; i < results.size(); i++) {
            if (!visitedResults.get(i)) {
                Rule result = results.get(i);
                if (!(result instanceof NoMatchRule)) {
                    unevaluated.add(result);
                }
            }
        }
        return unevaluated;
    }

    /**
     * Returns the percentage of conditions that were evaluated at least once.
     *
     * @return condition coverage percentage (0-100)
     */
    public double getConditionCoverage() {
        return conditions.isEmpty() ? 100.0 : (100.0 * visitedConditions.cardinality() / conditions.size());
    }

    /**
     * Returns the percentage of results that were reached at least once.
     *
     * @return result coverage percentage (0-100)
     */
    public double getResultCoverage() {
        // Count only non-NO_MATCH results
        int relevantResults = 0;
        int coveredRelevantResults = 0;

        for (int i = 0; i < results.size(); i++) {
            if (!(results.get(i) instanceof NoMatchRule)) {
                relevantResults++;
                if (visitedResults.get(i)) {
                    coveredRelevantResults++;
                }
            }
        }

        return relevantResults == 0 ? 100.0 : (100.0 * coveredRelevantResults / relevantResults);
    }

    /**
     * Returns conditions that exist in the conditions list but are not referenced by any BDD node.
     *
     * @return set of unreferenced conditions
     */
    public Set<Condition> getUnreferencedConditions() {
        BitSet referencedByBdd = new BitSet(conditions.size());
        for (int i = 0; i < bdd.getNodeCount(); i++) {
            int varIdx = bdd.getVariable(i);
            if (varIdx >= 0 && varIdx < conditions.size()) {
                referencedByBdd.set(varIdx);
            }
        }

        Set<Condition> unreferenced = new HashSet<>();
        for (int i = 0; i < conditions.size(); i++) {
            if (!referencedByBdd.get(i)) {
                unreferenced.add(conditions.get(i));
            }
        }
        return unreferenced;
    }

    // Evaluator that tracks what gets visited during BDD evaluation.
    private final class TestEvaluator implements ConditionEvaluator {
        private final RuleEvaluator ruleEvaluator;

        TestEvaluator(Map<Identifier, Value> input) {
            this.ruleEvaluator = new RuleEvaluator(parameters, input);
        }

        @Override
        public boolean test(int conditionIndex) {
            if (conditionIndex < 0 || conditionIndex >= conditions.size()) {
                return false;
            } else {
                visitedConditions.set(conditionIndex);
                Condition condition = conditions.get(conditionIndex);
                return ruleEvaluator.evaluateCondition(condition).isTruthy();
            }
        }
    }
}
