/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.bdd.RulesBdd;
import software.amazon.smithy.rulesengine.language.syntax.bdd.RulesBddCondition;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.ErrorRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.language.syntax.rule.TreeRule;

/**
 * Converts a {@link EndpointRuleSet} into a list of unique paths, a tree of conditions and leaves, and a BDD.
 */
public final class HashConsGraph {

    // Endpoint ruleset to optimize.
    private final EndpointRuleSet ruleSet;

    // Provides a hash of endpoints/errors to their index.
    private final Map<Rule, Integer> resultHashCons = new HashMap<>();

    // Provides a hash of conditions to their index.
    private final Map<Condition, Integer> conditionHashCons = new HashMap<>();

    // Provides a mapping of originally defined conditions to their canonicalized conditions.
    // (e.g., moving variables before literals in commutative functions).
    private final Map<Condition, Condition> canonicalizedConditions = new HashMap<>();

    // A flattened list of unique leaves.
    private final List<Rule> results = new ArrayList<>();

    // A flattened list of unique conditions
    private final List<RulesBddCondition> conditions = new ArrayList<>();

    // A flattened set of unique condition paths to leaves, sorted based on desired complexity order.
    private final Set<BddPath> paths = new LinkedHashSet<>();

    public HashConsGraph(EndpointRuleSet ruleSet) {
        this.ruleSet = ruleSet;
        hashConsConditions();

        // Now build up paths and refer to the hash-consed conditions.
        for (Rule rule : ruleSet.getRules()) {
            crawlRules(rule, new LinkedHashSet<>());
        }
    }

    // First create a global ordering of conditions. The ordering of conditions is the primary way to influence
    // the resulting node tables of a BDD.
    // 1. Simplest conditions come first (e.g., isset, booleanEquals, etc.). We build this up by gathering all
    //    the stateless conditions and sorting them by complexity order so that simplest checks happen earlier.
    // 2. Stateful conditions come after, and they must appear in a dependency ordering (i.e., if a condition
    //    depends on a previous condition to bind a variable, then it must come after its dependency). This is
    //    done by iterating over paths and add stateful conditions, in path order, to a LinkedHashSet of
    //    conditions, giving us a hash-consed but ordered set of all stateful conditions across all paths.
    private void hashConsConditions() {
        Set<RulesBddCondition> statelessCondition = new LinkedHashSet<>();
        Set<RulesBddCondition> statefulConditions = new LinkedHashSet<>();
        for (Rule rule : ruleSet.getRules()) {
            crawlConditions(rule, statelessCondition, statefulConditions);
        }

        // Sort the stateless conditions by complexity order, maintaining insertion order when equal.
        List<RulesBddCondition> sortedStatelessConditions = new ArrayList<>(statelessCondition);
        sortedStatelessConditions.sort(Comparator.comparingInt(RulesBddCondition::getComplexity));

        // Now build up the hash-consed map of conditions to their integer position in a sorted array of RuleCondition.
        hashConsCollectedConditions(sortedStatelessConditions);
        hashConsCollectedConditions(statefulConditions);
    }

    private void hashConsCollectedConditions(Collection<RulesBddCondition> ruleConditions) {
        for (RulesBddCondition ruleCondition : ruleConditions) {
            conditionHashCons.put(ruleCondition.getCondition(), conditions.size());
            conditions.add(ruleCondition);
        }
    }

    public List<BddPath> getPaths() {
        return new ArrayList<>(paths);
    }

    public List<RulesBddCondition> getConditions() {
        return new ArrayList<>(conditions);
    }

    public List<Rule> getResults() {
        return new ArrayList<>(results);
    }

    public EndpointRuleSet getRuleSet() {
        return ruleSet;
    }

    public RulesBdd getBdd() {
        return RulesBdd.from(this);
    }

    // Crawl rules to build up the global total ordering of variables.
    private void crawlConditions(
            Rule rule,
            Set<RulesBddCondition> statelessConditions,
            Set<RulesBddCondition> statefulConditions
    ) {
        for (Condition condition : rule.getConditions()) {
            if (!canonicalizedConditions.containsKey(condition)) {
                // Create the RuleCondition and also canonicalize the underlying condition.
                RulesBddCondition ruleCondition = RulesBddCondition.from(condition, ruleSet);
                // Add a mapping between the original condition and the canonicalized condition.
                canonicalizedConditions.put(condition, ruleCondition.getCondition());
                if (ruleCondition.isStateful()) {
                    statefulConditions.add(ruleCondition);
                } else {
                    statelessConditions.add(ruleCondition);
                }
            }
        }

        if (rule instanceof TreeRule) {
            TreeRule treeRule = (TreeRule) rule;
            for (Rule subRule : treeRule.getRules()) {
                crawlConditions(subRule, statelessConditions, statefulConditions);
            }
        }
    }

    private void crawlRules(Rule rule, Set<Integer> conditionIndices) {
        for (Condition condition : rule.getConditions()) {
            Condition c = Objects.requireNonNull(canonicalizedConditions.get(condition), "Condition not found");
            Integer idx = Objects.requireNonNull(conditionHashCons.get(c), "Condition not hashed");
            conditionIndices.add(idx);
        }

        Rule leaf = null;
        if (rule instanceof TreeRule) {
            TreeRule treeRule = (TreeRule) rule;
            for (Rule subRule : treeRule.getRules()) {
                crawlRules(subRule, new LinkedHashSet<>(conditionIndices));
            }
        } else if (!rule.getConditions().isEmpty()) {
            leaf = createStandaloneResult(rule);
        } else {
            leaf = rule;
        }

        if (leaf != null) {
            int position = resultHashCons.computeIfAbsent(leaf, l -> {
                results.add(l);
                return results.size() - 1;
            });
            paths.add(createPath(position, conditionIndices));
        }
    }

    // Create a rule that strips off conditions and is just left with docs + the error or endpoint.
    private static Rule createStandaloneResult(Rule rule) {
        if (rule instanceof ErrorRule) {
            ErrorRule e = (ErrorRule) rule;
            return new ErrorRule(
                    ErrorRule.builder().description(e.getDocumentation().orElse(null)),
                    e.getError());
        } else if (rule instanceof EndpointRule) {
            EndpointRule e = (EndpointRule) rule;
            return new EndpointRule(
                    EndpointRule.builder().description(e.getDocumentation().orElse(null)),
                    e.getEndpoint());
        } else {
            throw new UnsupportedOperationException("Unsupported result node: " + rule);
        }
    }

    private BddPath createPath(int leafIdx, Set<Integer> conditionIndices) {
        Set<Integer> statefulConditions = new LinkedHashSet<>();
        Set<Integer> statelessConditions = new TreeSet<>((a, b) -> {
            int conditionComparison = ruleComparator(conditions.get(a), conditions.get(b));
            // fall back to index comparison to ensure uniqueness
            return conditionComparison != 0 ? conditionComparison : Integer.compare(a, b);
        });

        for (Integer conditionIdx : conditionIndices) {
            RulesBddCondition node = conditions.get(conditionIdx);
            if (!node.isStateful()) {
                statelessConditions.add(conditionIdx);
            } else {
                statefulConditions.add(conditionIdx);
            }
        }

        return new BddPath(leafIdx, statelessConditions, statefulConditions);
    }

    private int ruleComparator(RulesBddCondition a, RulesBddCondition b) {
        return Integer.compare(a.getComplexity(), b.getComplexity());
    }

    /**
     * Represents a path through rule conditions to reach a specific result.
     *
     * <p>Contains both stateless conditions (sorted by complexity) and stateful conditions (ordered by dependency)
     * that must be evaluated to reach the target leaf (endpoint or error).
     */
    public static final class BddPath {

        // The endpoint or error index.
        private final int leafIndex;

        // Conditions that create or use stateful bound variables and must be maintained in order.
        private final Set<Integer> statefulConditions;

        // Sort conditions based on complexity scores.
        private final Set<Integer> statelessConditions;

        private int hash;

        BddPath(int leafIndex, Set<Integer> statelessConditions, Set<Integer> statefulConditions) {
            this.leafIndex = leafIndex;
            this.statelessConditions = Collections.unmodifiableSet(statelessConditions);
            this.statefulConditions = Collections.unmodifiableSet(statefulConditions);
        }

        public Set<Integer> getStatefulConditions() {
            return statefulConditions;
        }

        public Set<Integer> getStatelessConditions() {
            return statelessConditions;
        }

        public int getLeafIndex() {
            return leafIndex;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            } else if (object == null || getClass() != object.getClass()) {
                return false;
            }
            BddPath path = (BddPath) object;
            return leafIndex == path.leafIndex
                   && statefulConditions.equals(path.statefulConditions)
                   && statelessConditions.equals(path.statelessConditions);
        }

        @Override
        public int hashCode() {
            int result = hash;
            if (result == 0) {
                result = Objects.hash(leafIndex, statefulConditions, statelessConditions);
                hash = result;
            }
            return result;
        }

        @Override
        public String toString() {
            return "Path{statelessConditions=" + statelessConditions + ", statefulConditions=" + statefulConditions
                   + ", leafIndex=" + leafIndex + '}';
        }
    }
}
