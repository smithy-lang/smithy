/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.syntax.parameters.ParameterType;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.ErrorRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.logic.TestHelpers;
import software.amazon.smithy.rulesengine.logic.cfg.Cfg;
import software.amazon.smithy.utils.ListUtils;

class BddTest {

    @Test
    void testConstructorValidation() {
        Parameters params = Parameters.builder().build();
        int[][] nodes = new int[][] {{-1, 1, -1}};

        // Should reject complemented root (except -1 which is FALSE terminal)
        assertThrows(IllegalArgumentException.class, () -> new Bdd(params, ListUtils.of(), ListUtils.of(), nodes, -2));

        // Should accept positive root
        Bdd bdd = new Bdd(params, ListUtils.of(), ListUtils.of(), nodes, 1);
        assertEquals(1, bdd.getRootRef());

        // Should accept FALSE terminal as root
        Bdd bdd2 = new Bdd(params, ListUtils.of(), ListUtils.of(), nodes, -1);
        assertEquals(-1, bdd2.getRootRef());
    }

    @Test
    void testBasicAccessors() {
        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder().name("Region").type(ParameterType.STRING).build())
                .build();
        Condition cond = Condition.builder().fn(TestHelpers.isSet("Region")).build();
        Rule rule = EndpointRule.builder().endpoint(TestHelpers.endpoint("https://example.com"));
        int[][] nodes = new int[][] {
                {-1, 1, -1},
                {0, 3, -1},
                {1, 1, -1}
        };

        Bdd bdd = new Bdd(params, ListUtils.of(cond), ListUtils.of(rule), nodes, 2);

        assertEquals(params, bdd.getParameters());
        assertEquals(1, bdd.getConditions().size());
        assertEquals(cond, bdd.getConditions().get(0));
        assertEquals(1, bdd.getConditionCount());
        assertEquals(1, bdd.getResults().size());
        assertEquals(rule, bdd.getResults().get(0));
        assertEquals(3, bdd.getNodes().length);
        assertEquals(2, bdd.getRootRef());
    }

    @Test
    void testFromRuleSet() {
        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(Parameters.builder()
                        .addParameter(Parameter.builder().name("Region").type(ParameterType.STRING).build())
                        .build())
                .addRule(EndpointRule.builder()
                        .conditions(Condition.builder().fn(TestHelpers.isSet("Region")).build())
                        .endpoint(TestHelpers.endpoint("https://example.com")))
                .build();

        Bdd bdd = Bdd.from(ruleSet);

        assertTrue(bdd.getConditionCount() > 0);
        assertFalse(bdd.getResults().isEmpty());
        assertTrue(bdd.getNodes().length > 1); // At least terminal + one node
    }

    @Test
    void testFromCfg() {
        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(Parameters.builder().build())
                .addRule(ErrorRule.builder().error("test error"))
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        Bdd bdd = Bdd.from(cfg);

        assertEquals(0, bdd.getConditionCount()); // No conditions
        assertFalse(bdd.getResults().isEmpty());
    }

    @Test
    void testTransform() {
        Bdd original = createSimpleBdd();

        // Test transform that returns same BDD
        Bdd same = original.transform(bdd -> bdd);
        assertEquals(original, same);

        // Test transform that modifies root
        Bdd modified = original.transform(bdd -> {
            return new Bdd(bdd.getParameters(), bdd.getConditions(), bdd.getResults(), bdd.getNodes(), 1);
        });

        assertNotEquals(original.getRootRef(), modified.getRootRef());
        assertEquals(1, modified.getRootRef());
    }

    @Test
    void testEquals() {
        Bdd bdd1 = createSimpleBdd();
        Bdd bdd2 = createSimpleBdd();

        assertEquals(bdd1, bdd2);
        assertEquals(bdd1.hashCode(), bdd2.hashCode());

        // Different root
        Bdd bdd3 = new Bdd(bdd1.getParameters(), bdd1.getConditions(), bdd1.getResults(), bdd1.getNodes(), 1);
        assertNotEquals(bdd1, bdd3);

        // Different conditions
        Condition newCond = Condition.builder().fn(TestHelpers.isSet("Bucket")).build();
        Bdd bdd4 = new Bdd(bdd1
                .getParameters(), ListUtils.of(newCond), bdd1.getResults(), bdd1.getNodes(), bdd1.getRootRef());
        assertNotEquals(bdd1, bdd4);

        // Different nodes
        int[][] newNodes = new int[][] {{-1, 1, -1}, {0, -1, 3}};
        Bdd bdd5 = new Bdd(bdd1.getParameters(), bdd1.getConditions(), bdd1.getResults(), newNodes, bdd1.getRootRef());
        assertNotEquals(bdd1, bdd5);
    }

    @Test
    void testToString() {
        Bdd bdd = createSimpleBdd();
        String str = bdd.toString();

        assertTrue(str.contains("Bdd{"));
        assertTrue(str.contains("conditions"));
        assertTrue(str.contains("results"));
        assertTrue(str.contains("root:"));
        assertTrue(str.contains("nodes"));
    }

    @Test
    void testToStringBuilder() {
        Bdd bdd = createSimpleBdd();
        StringBuilder sb = new StringBuilder();
        bdd.toString(sb);

        String str = sb.toString();
        assertTrue(str.contains("C0:")); // Condition index
        assertTrue(str.contains("R0:")); // Result index
        assertTrue(str.contains("terminal")); // Terminal node
    }

    @Test
    void testToNodeAndFromNode() {
        Bdd original = createSimpleBdd();

        Node node = original.toNode();
        assertTrue(node.isObjectNode());
        assertTrue(node.expectObjectNode().containsMember("version"));
        assertTrue(node.expectObjectNode().containsMember("conditions"));
        assertTrue(node.expectObjectNode().containsMember("results"));
        assertTrue(node.expectObjectNode().containsMember("nodes"));
        assertTrue(node.expectObjectNode().containsMember("root"));

        Bdd restored = Bdd.fromNode(node);
        assertEquals(original.getRootRef(), restored.getRootRef());
        assertEquals(original.getConditionCount(), restored.getConditionCount());
        assertEquals(original.getResults().size(), restored.getResults().size());
        assertEquals(original.getNodes().length, restored.getNodes().length);
    }

    @Test
    void testFromNodeWithInvalidVersion() {
        Node node = Node.objectNode()
                .withMember("version", "0.1") // Invalid version
                .withMember("parameters", Parameters.builder().build().toNode())
                .withMember("conditions", Node.arrayNode())
                .withMember("results", Node.arrayNode())
                .withMember("nodes", "")
                .withMember("nodeCount", 0)
                .withMember("root", 1);

        assertThrows(IllegalArgumentException.class, () -> Bdd.fromNode(node));
    }

    @Test
    void testToStringWithDifferentNodeTypes() {
        Parameters params = Parameters.builder().build();
        Condition cond = Condition.builder().fn(TestHelpers.isSet("Region")).build();
        Rule endpoint = EndpointRule.builder().endpoint(TestHelpers.endpoint("https://example.com"));
        Rule error = ErrorRule.builder().error("test error");

        int[][] nodes = new int[][] {
                {-1, 1, -1}, // terminal
                {0, 3, 4}, // condition node
                {1, 1, -1}, // endpoint result
                {2, 1, -1} // error result
        };

        Bdd bdd = new Bdd(params, ListUtils.of(cond), ListUtils.of(endpoint, error), nodes, 2);
        String str = bdd.toString();

        assertTrue(str.contains("Endpoint:"));
        assertTrue(str.contains("Error:"));
        assertTrue(str.contains("C0"));
        assertTrue(str.contains("R0"));
        assertTrue(str.contains("R1"));
    }

    private Bdd createSimpleBdd() {
        Parameters params = Parameters.builder().build();
        Condition cond = Condition.builder().fn(TestHelpers.isSet("Region")).build();
        Rule rule = EndpointRule.builder().endpoint(TestHelpers.endpoint("https://example.com"));
        int[][] nodes = new int[][] {
                {-1, 1, -1},
                {0, 3, -1},
                {1, 1, -1}
        };
        return new Bdd(params, ListUtils.of(cond), ListUtils.of(rule), nodes, 2);
    }
}
