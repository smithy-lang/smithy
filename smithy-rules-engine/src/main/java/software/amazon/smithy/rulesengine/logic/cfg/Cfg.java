/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.cfg;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.RulesVersion;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.ErrorRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.language.syntax.rule.TreeRule;
import software.amazon.smithy.utils.SmithyBuilder;

/**
 * A Control Flow Graph (CFG) representation of endpoint rule decision logic.
 *
 * <p>The CFG transforms the hierarchical decision tree structure into an optimized
 * representation with node deduplication to prevent exponential growth.
 *
 * <p>The CFG consists of:
 * <ul>
 *   <li>A root node representing the entry point of the decision logic</li>
 *   <li>A DAG structure where condition nodes are shared when they have identical subtrees</li>
 * </ul>
 */
public final class Cfg implements Iterable<CfgNode> {

    private final Parameters parameters;
    private final CfgNode root;

    // Lazily computed condition data
    private Condition[] conditions;
    private Map<Condition, Integer> conditionToIndex;
    private final RulesVersion version;

    Cfg(EndpointRuleSet ruleSet, CfgNode root) {
        this(
                ruleSet == null ? Parameters.builder().build() : ruleSet.getParameters(),
                root,
                ruleSet == null ? RulesVersion.V1_1 : ruleSet.getRulesVersion());
    }

    Cfg(Parameters parameters, CfgNode root, RulesVersion version) {
        this.root = SmithyBuilder.requiredState("root", root);
        this.version = version;
        this.parameters = parameters;
    }

    /**
     * Create a CFG from the given ruleset.
     *
     * @param ruleSet Rules to convert to CFG.
     * @return the CFG result.
     */
    public static Cfg from(EndpointRuleSet ruleSet) {
        CfgBuilder builder = new CfgBuilder(ruleSet);
        CfgNode terminal = ResultNode.terminal();
        Map<RuleKey, CfgNode> processedRules = new HashMap<>();
        CfgNode root = convertRulesToChain(builder.ruleSet.getRules(), terminal, builder, processedRules);
        return builder.build(root);
    }

    /**
     * Get the endpoint ruleset version of the CFG.
     *
     * @return endpoint ruleset version.
     */
    public RulesVersion getVersion() {
        return version;
    }

    /**
     * Gets all unique conditions in the CFG, in the order they were discovered.
     *
     * @return array of conditions
     */
    public Condition[] getConditions() {
        ensureConditionsExtracted();
        return conditions;
    }

    /**
     * Gets the index of a condition in the conditions array.
     *
     * @param condition the condition to look up
     * @return the index, or null if not found
     */
    public Integer getConditionIndex(Condition condition) {
        ensureConditionsExtracted();
        return conditionToIndex.get(condition);
    }

    /**
     * Gets the number of unique conditions in the CFG.
     *
     * @return the condition count
     */
    public int getConditionCount() {
        ensureConditionsExtracted();
        return conditions.length;
    }

    private void ensureConditionsExtracted() {
        if (conditions == null) {
            extractConditions();
        }
    }

    private synchronized void extractConditions() {
        if (conditions != null) {
            return;
        }

        List<Condition> conditionList = new ArrayList<>();
        Map<Condition, Integer> indexMap = new LinkedHashMap<>();

        for (CfgNode node : this) {
            if (node instanceof ConditionNode) {
                ConditionNode condNode = (ConditionNode) node;
                Condition condition = condNode.getCondition().getCondition();

                if (!indexMap.containsKey(condition)) {
                    indexMap.put(condition, conditionList.size());
                    conditionList.add(condition);
                }
            }
        }

        this.conditions = conditionList.toArray(new Condition[0]);
        this.conditionToIndex = indexMap;
    }

    public Parameters getParameters() {
        return parameters;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object == null || getClass() != object.getClass()) {
            return false;
        } else {
            Cfg o = (Cfg) object;
            return root.equals(o.root) && version.equals(o.version);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(root, version);
    }

    /**
     * Returns the root node of the control flow graph.
     *
     * @return the root node
     */
    public CfgNode getRoot() {
        return root;
    }

    @Override
    public Iterator<CfgNode> iterator() {
        return new Iterator<CfgNode>() {
            private final Deque<CfgNode> stack = new ArrayDeque<>();
            private final Set<CfgNode> visited = new HashSet<>();
            private CfgNode next;

            {
                if (root != null) {
                    stack.push(root);
                }
                advance();
            }

            private void advance() {
                next = null;
                while (!stack.isEmpty()) {
                    CfgNode node = stack.pop();
                    if (visited.add(node)) {
                        // Push children before returning this node
                        if (node instanceof ConditionNode) {
                            ConditionNode cond = (ConditionNode) node;
                            stack.push(cond.getFalseBranch());
                            stack.push(cond.getTrueBranch());
                        }
                        next = node;
                        return;
                    }
                }
            }

            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public CfgNode next() {
                if (next == null) {
                    throw new NoSuchElementException();
                }
                CfgNode result = next;
                advance();
                return result;
            }
        };
    }

    // Converts a list of rules into a conditional chain. Each rule's false branch goes to the next rule.
    private static CfgNode convertRulesToChain(
            List<Rule> rules,
            CfgNode fallthrough,
            CfgBuilder builder,
            Map<RuleKey, CfgNode> processedRules
    ) {
        // Make a reversed view of the rules list
        List<Rule> reversed = new ArrayList<>(rules);
        Collections.reverse(reversed);
        CfgNode next = fallthrough;
        for (Rule rule : reversed) {
            next = convertRule(rule, next, builder, processedRules);
        }
        return next;
    }

    /**
     * Converts a single rule to CFG nodes.
     *
     * @param rule the rule to convert
     * @param fallthrough what to do if this rule doesn't match
     * @param builder the CFG builder
     * @param processedRules cache for processed rules
     * @return the entry point for this rule
     */
    private static CfgNode convertRule(
            Rule rule,
            CfgNode fallthrough,
            CfgBuilder builder,
            Map<RuleKey, CfgNode> processedRules
    ) {
        RuleKey key = new RuleKey(rule, fallthrough);
        CfgNode existing = processedRules.get(key);
        if (existing != null) {
            return existing;
        }

        CfgNode body;
        if (rule instanceof EndpointRule || rule instanceof ErrorRule) {
            body = builder.createResult(rule);
        } else if (rule instanceof TreeRule) {
            TreeRule treeRule = (TreeRule) rule;
            // Recursively convert nested rules with same fallthrough
            body = convertRulesToChain(treeRule.getRules(), fallthrough, builder, processedRules);
        } else {
            throw new IllegalArgumentException("Unknown rule type: " + rule.getClass());
        }

        // Build conditions from last to first
        CfgNode current = body;
        for (int i = rule.getConditions().size() - 1; i >= 0; i--) {
            Condition cond = rule.getConditions().get(i);
            // For chained conditions (AND semantics), if one fails, we go to the fallthrough
            current = builder.createCondition(cond, current, fallthrough);
        }

        // Cache the result for this (rule, fallthrough) combination
        processedRules.put(key, current);

        return current;
    }

    private static final class RuleKey {
        private final Rule rule;
        private final CfgNode fallthrough;
        private final int hashCode;

        RuleKey(Rule rule, CfgNode fallthrough) {
            this.rule = rule;
            this.fallthrough = fallthrough;
            // Use identity hash for fallthrough since it's a node reference
            this.hashCode = System.identityHashCode(rule) * 31 + System.identityHashCode(fallthrough);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (!(o instanceof RuleKey)) {
                return false;
            }
            RuleKey that = (RuleKey) o;
            return rule == that.rule && fallthrough == that.fallthrough;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }
}
