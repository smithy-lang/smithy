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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.eval.RuleEvaluator;
import software.amazon.smithy.rulesengine.language.eval.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.visit.TraversingVisitor;
import software.amazon.smithy.rulesengine.traits.EndpointTestCase;
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
        this.checkerCore = new CoverageCheckerCore();
    }

    /**
     * Evaluates the rule-set with the given inputs to determine rule coverage.
     *
     * @param input the map parameters and inputs to test coverage.
     */
    public void evaluateInput(Map<Identifier, Value> input) {
        this.checkerCore.evaluateRuleSet(ruleSet, input);
    }

    /**
     * Evaluate the rule-set using the given test case to determine rule coverage.
     *
     * @param testCase the test case to evaluate.
     */
    public void evaluateTestCase(EndpointTestCase testCase) {
        Map<Identifier, Value> map = new LinkedHashMap<>();
        testCase.getParams().getStringMap().forEach((s, node) -> map.put(Identifier.of(s), Value.fromNode(node)));
        this.checkerCore.evaluateRuleSet(ruleSet, map);
    }

    /**
     * Analyze and provides the coverage results for the rule-set.
     *
     * @return stream of {@link CoverageResult}.
     */
    public Stream<CoverageResult> checkCoverage() {
        Stream<Condition> conditions = new CollectConditions().visitRuleset(ruleSet);
        return coverageForConditions(conditions);
    }

    private Stream<CoverageResult> coverageForConditions(Stream<Condition> conditions) {
        return conditions.distinct().flatMap(condition -> {
            List<BranchResult> conditionResults = checkerCore.conditionResults.getOrDefault(condition,
                    new ArrayList<>());
            List<Boolean> branches = conditionResults.stream()
                    .map(c -> c.result)
                    .distinct()
                    .collect(Collectors.toList());
            if (branches.size() == 1) {
                return Stream.of(new CoverageResult(condition, !branches.get(0), conditionResults.stream()
                        .map(c -> c.context.input)
                        .collect(Collectors.toList())));
            } else if (branches.size() == 0) {
                return Stream.of(new CoverageResult(condition, false, Collections.emptyList()),
                        new CoverageResult(condition, true, Collections.emptyList()));
            } else {
                return Stream.empty();
            }
        });
    }

    private static class BranchResult {
        private final boolean result;
        private final CoverageCheckerCore.Context context;

        BranchResult(boolean result, CoverageCheckerCore.Context context) {
            this.result = result;
            this.context = context;
        }
    }

    private static class CoverageCheckerCore extends RuleEvaluator {
        private final Map<Condition, List<BranchResult>> conditionResults = new LinkedHashMap<>();
        private Context context = null;

        @Override
        public Value evaluateRuleSet(EndpointRuleSet ruleset, Map<Identifier, Value> parameterArguments) {
            try {
                context = new Context(parameterArguments);
                return super.evaluateRuleSet(ruleset, parameterArguments);
            } finally {
                context = null;
            }
        }

        @Override
        public Value evaluateCondition(Condition condition) {
            if (context == null) {
                throw new RuntimeException("Must call `evaluateRuleSet` before calling `evaluateCondition`");
            }

            Value result = super.evaluateCondition(condition);
            List<BranchResult> list = conditionResults.getOrDefault(condition, new ArrayList<>());
            if (result.isEmpty() || result.equals(Value.booleanValue(false))) {
                list.add(new BranchResult(false, context));
            } else {
                list.add(new BranchResult(true, context));
            }
            conditionResults.put(condition, list);
            return result;
        }

        private static class Context {
            private final Map<Identifier, Value> input;

            Context(Map<Identifier, Value> input) {
                this.input = input;
            }
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
        private final List<Map<Identifier, Value>> otherUsages;

        public CoverageResult(Condition condition, boolean result, List<Map<Identifier, Value>> otherUsages) {
            this.condition = condition;
            this.result = result;
            this.otherUsages = otherUsages;
        }

        public Condition getCondition() {
            return condition;
        }

        public boolean getResult() {
            return result;
        }

        public List<Map<Identifier, Value>> getOtherUsages() {
            return otherUsages;
        }

        public String pretty() {
            StringBuilder sb = new StringBuilder();
            sb.append("leaf: ").append(pretty(condition));
            return sb.toString();
        }

        private String pretty(Condition condition) {
            return condition + "(" + condition.getSourceLocation().getFilename() + ":"
                    + condition.getSourceLocation().getLine() + ")";
        }
    }
}
