/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.cfg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.syntax.parameters.ParameterType;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.ErrorRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.language.syntax.rule.TreeRule;
import software.amazon.smithy.rulesengine.logic.ConditionReference;
import software.amazon.smithy.rulesengine.logic.TestHelpers;

class CfgTest {

    @Test
    void gettersReturnConstructorValues() {
        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(Parameters.builder().build())
                .build();
        CfgNode root = ResultNode.terminal();

        Cfg cfg = new Cfg(ruleSet, root);

        assertSame(ruleSet, cfg.getRuleSet());
        assertSame(root, cfg.getRoot());
    }

    @Test
    void fromCreatesSimpleCfg() {
        EndpointRule rule = EndpointRule.builder()
                .endpoint(TestHelpers.endpoint("https://example.com"));

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(Parameters.builder().build())
                .addRule(rule)
                .build();

        Cfg cfg = Cfg.from(ruleSet);

        assertNotNull(cfg);
        assertNotNull(cfg.getRoot());
        assertEquals(ruleSet, cfg.getRuleSet());

        // Root should be a result node for a simple endpoint rule
        assertInstanceOf(ResultNode.class, cfg.getRoot());
        ResultNode resultNode = (ResultNode) cfg.getRoot();
        assertEquals(rule.withConditions(Collections.emptyList()), resultNode.getResult());
    }

    @Test
    void fromCreatesConditionNode() {
        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder().name("region").type(ParameterType.STRING).build())
                .build();

        EndpointRule rule = EndpointRule.builder()
                .condition(Condition.builder().fn(TestHelpers.isSet("region")).build())
                .endpoint(TestHelpers.endpoint("https://example.com"));

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(params)
                .addRule(rule)
                .build();

        Cfg cfg = Cfg.from(ruleSet);

        // Root should be a condition node
        assertInstanceOf(ConditionNode.class, cfg.getRoot());
        ConditionNode condNode = (ConditionNode) cfg.getRoot();
        assertEquals("isSet(region)", condNode.getCondition().getCondition().toString());
    }

    @Test
    void fromHandlesMultipleRules() {
        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder().name("region").type(ParameterType.STRING).build())
                .build();

        // TreeRule with isSet check followed by stringEquals
        Rule treeRule = TreeRule.builder()
                .condition(Condition.builder().fn(TestHelpers.isSet("region")).build())
                .treeRule(
                        EndpointRule.builder()
                                .condition(
                                        Condition.builder().fn(TestHelpers.stringEquals("region", "us-east-1")).build())
                                .endpoint(TestHelpers.endpoint("https://us-east-1.example.com")),
                        EndpointRule.builder()
                                .condition(
                                        Condition.builder().fn(TestHelpers.stringEquals("region", "eu-west-1")).build())
                                .endpoint(TestHelpers.endpoint("https://eu-west-1.example.com")));

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(params)
                .addRule(treeRule)
                .addRule(ErrorRule.builder().error("Unknown region"))
                .build();

        Cfg cfg = Cfg.from(ruleSet);

        assertInstanceOf(ConditionNode.class, cfg.getRoot());
    }

    @Test
    void iteratorVisitsAllNodes() {
        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder().name("region").type(ParameterType.STRING).build())
                .build();

        Rule rule1 = EndpointRule.builder()
                .condition(Condition.builder().fn(TestHelpers.isSet("region")).build())
                .endpoint(TestHelpers.endpoint("https://with-region.com"));

        Rule rule2 = EndpointRule.builder()
                .endpoint(TestHelpers.endpoint("https://no-region.com"));

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(params)
                .addRule(rule1)
                .addRule(rule2)
                .build();

        Cfg cfg = Cfg.from(ruleSet);

        Set<CfgNode> visited = new HashSet<>();
        for (CfgNode node : cfg) {
            visited.add(node);
        }

        // Should have at least 3 nodes: condition node and 2 result nodes
        assertTrue(visited.size() >= 3);
    }

    @Test
    void iteratorHandlesEmptyCfg() {
        CfgNode root = ResultNode.terminal();
        Cfg cfg = new Cfg(null, root);

        List<CfgNode> nodes = new ArrayList<>();
        for (CfgNode node : cfg) {
            nodes.add(node);
        }

        assertEquals(1, nodes.size());
        assertSame(root, nodes.get(0));
    }

    @Test
    void iteratorThrowsNoSuchElementException() {
        CfgNode root = ResultNode.terminal();
        Cfg cfg = new Cfg(null, root);

        Iterator<CfgNode> iterator = cfg.iterator();
        assertTrue(iterator.hasNext());
        iterator.next();
        assertFalse(iterator.hasNext());

        assertThrows(NoSuchElementException.class, iterator::next);
    }

    @Test
    void iteratorDoesNotVisitNodesTwice() {
        // Create a diamond-shaped CFG where multiple paths lead to the same node
        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder().name("a").type(ParameterType.STRING).build())
                .addParameter(Parameter.builder().name("b").type(ParameterType.STRING).build())
                .build();

        CfgBuilder builder = new CfgBuilder(EndpointRuleSet.builder()
                .parameters(params)
                .build());

        CfgNode sharedResult = builder.createResult(
                EndpointRule.builder().endpoint(TestHelpers.endpoint("https://shared.com")));

        Condition cond1 = Condition.builder().fn(TestHelpers.isSet("a")).build();
        Condition cond2 = Condition.builder().fn(TestHelpers.isSet("b")).build();

        ConditionReference ref1 = builder.createConditionReference(cond1);
        ConditionReference ref2 = builder.createConditionReference(cond2);

        // Both conditions can lead to the same result
        CfgNode branch1 = builder.createCondition(ref2, sharedResult, sharedResult);
        CfgNode root = builder.createCondition(ref1, branch1, sharedResult);

        Cfg cfg = builder.build(root);

        List<CfgNode> visitedNodes = new ArrayList<>();
        for (CfgNode node : cfg) {
            visitedNodes.add(node);
        }

        // Count occurrences of sharedResult
        long sharedResultCount = visitedNodes.stream()
                .filter(node -> node == sharedResult)
                .count();

        assertEquals(1, sharedResultCount, "Shared node should only be visited once");
    }

    @Test
    void equalsAndHashCodeBasedOnRoot() {
        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(Parameters.builder().build())
                .build();
        CfgNode root1 = ResultNode.terminal();
        CfgNode root2 = new ResultNode(
                EndpointRule.builder().endpoint(TestHelpers.endpoint("https://example.com")));

        Cfg cfg1a = new Cfg(ruleSet, root1);
        Cfg cfg1b = new Cfg(ruleSet, root1);
        Cfg cfg2 = new Cfg(ruleSet, root2);

        // Same root
        assertEquals(cfg1a, cfg1b);
        assertEquals(cfg1a.hashCode(), cfg1b.hashCode());

        // Different root
        assertNotEquals(cfg1a, cfg2);
    }
}
