/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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
     * Analyze and provides the coverage results for the rule-set.
     *
     * @return stream of {@link CoverageResult}.
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

    public static class CoverageResult {
        private final Condition condition;
        private final boolean result;

        public CoverageResult(Condition condition, boolean result) {
            this.condition = condition;
            this.result = result;
        }

        public Condition getCondition() {
            return condition;
        }

        public boolean getResult() {
            return result;
        }

        public String pretty() {
            return "leaf: " + condition + "(" + condition.getSourceLocation().getFilename() + ":"
                    + condition.getSourceLocation().getLine() + ")";
        }
    }
}
