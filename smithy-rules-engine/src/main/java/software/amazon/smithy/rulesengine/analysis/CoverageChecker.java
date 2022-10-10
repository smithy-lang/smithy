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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.eval.RuleEvaluator;
import software.amazon.smithy.rulesengine.language.eval.Value;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.language.util.PathFinder;
import software.amazon.smithy.rulesengine.language.util.StringUtils;
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
        HashMap<Identifier, Value> map = new HashMap<>();
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

    /**
     * Analyze and provides the coverage results for a specific rule.
     *
     * @return stream of {@link CoverageResult}.
     */
    public Stream<CoverageResult> checkCoverageFromRule(Rule rule) {
        Stream<Condition> conditions = rule.accept(new CollectConditions());
        return coverageForConditions(conditions);

    }

    private Stream<CoverageResult> coverageForConditions(Stream<Condition> conditions) {
        return conditions.distinct().flatMap(condition -> {
            Wrapper<Condition> w = new Wrapper<>(condition);
            ArrayList<BranchResult> conditionResults = checkerCore.conditionResults.getOrDefault(w, new ArrayList<>());
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

    private static class Wrapper<T> {
        private final T inner;

        Wrapper(T inner) {
            this.inner = inner;
        }

        @Override
        public int hashCode() {
            return Objects.hash(inner);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Wrapper<?> wrapper = (Wrapper<?>) o;
            return inner.equals(wrapper.inner);
        }
    }

    public static class BranchResult {
        private final boolean result;
        private final CoverageCheckerCore.Context context;

        public BranchResult(boolean result, CoverageCheckerCore.Context context) {
            this.result = result;
            this.context = context;
        }
    }

    static class CoverageCheckerCore extends RuleEvaluator {
        HashMap<Wrapper<Condition>, ArrayList<BranchResult>> conditionResults = new HashMap<>();
        Context context = null;

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
            assert context != null;
            Value result = super.evaluateCondition(condition);
            Wrapper<Condition> cond = new Wrapper<>(condition);
            ArrayList<BranchResult> list = conditionResults.getOrDefault(cond, new ArrayList<>());
            if (result.isNone() || result.equals(Value.bool(false))) {
                list.add(new BranchResult(false, context));
            } else {
                list.add(new BranchResult(true, context));
            }
            conditionResults.put(cond, list);
            return result;
        }

        static class Context {
            private final Map<Identifier, Value> input;

            Context(Map<Identifier, Value> input) {
                this.input = input;
            }
        }
    }

    static class CollectConditions extends TraversingVisitor<Condition> {
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

        public Condition condition() {
            return condition;
        }

        public boolean result() {
            return result;
        }

        public List<Map<Identifier, Value>> otherUsages() {
            return otherUsages;
        }

        public String pretty() {
            StringBuilder sb = new StringBuilder();
            sb.append("leaf: ").append(pretty(condition));
            return sb.toString();
        }

        private String pretty(Condition condition) {
            return new StringBuilder()
                    .append(condition)
                    .append("(")
                    .append(condition.getSourceLocation().getFilename())
                    .append(":")
                    .append(condition.getSourceLocation().getLine())
                    .append(")")
                    .toString();
        }

        public String prettyWithPath(EndpointRuleSet ruleset) {
            PathFinder.Path path = PathFinder.findPath(ruleset, condition).orElseThrow(NoSuchElementException::new);
            StringBuilder sb = new StringBuilder();
            sb.append(pretty()).append("\n");
            for (List<Condition> cond : path.negated()) {
                sb.append(StringUtils.indent(String.format("!%s", cond.toString()), 2));
            }
            for (Condition cond : path.positive()) {
                sb.append(StringUtils.indent(cond.toString(), 2));
            }
            return sb.toString();
        }
    }
}
