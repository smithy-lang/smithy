/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.syntax.parameters.ParameterType;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.ErrorRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.NoMatchRule;
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
    void testEquals() {
        Bdd bdd1 = createSimpleBdd();
        Bdd bdd2 = createSimpleBdd();

        assertEquals(bdd1, bdd2);
        assertEquals(bdd1.hashCode(), bdd2.hashCode());

        // Different root - use a different value than what createSimpleBdd returns
        Bdd bdd3 = new Bdd(bdd1.getParameters(), bdd1.getConditions(), bdd1.getResults(), bdd1.getNodes(), -1);
        assertNotEquals(bdd1, bdd3);

        // Different conditions
        Condition newCond = Condition.builder().fn(TestHelpers.isSet("Bucket")).build();
        Bdd bdd4 = new Bdd(bdd1
                .getParameters(), ListUtils.of(newCond), bdd1.getResults(), bdd1.getNodes(), bdd1.getRootRef());
        assertNotEquals(bdd1, bdd4);

        // Different nodes
        int[][] newNodes = new int[][] {{-1, 1, -1}, {0, -1, Bdd.RESULT_OFFSET + 1}};
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
        assertTrue(node.expectObjectNode().containsMember("conditions"));
        assertTrue(node.expectObjectNode().containsMember("results"));
        assertTrue(node.expectObjectNode().containsMember("nodes"));
        assertTrue(node.expectObjectNode().containsMember("root"));

        // Original has 2 results: NoMatchRule at 0, endpoint at 1
        // Serialized should only have 1 result (the endpoint)
        int serializedResultCount = node.expectObjectNode()
                .expectArrayMember("results")
                .getElements()
                .size();
        assertEquals(1, serializedResultCount);

        Bdd restored = Bdd.fromNode(node);
        assertEquals(original.getRootRef(), restored.getRootRef());
        assertEquals(original.getConditionCount(), restored.getConditionCount());
        assertEquals(original.getResults().size(), restored.getResults().size());
        assertEquals(original.getNodes().length, restored.getNodes().length);

        // Verify NoMatchRule was restored at index 0
        assertInstanceOf(NoMatchRule.class, restored.getResults().get(0));
    }

    @Test
    void testToStringWithDifferentNodeTypes() {
        Parameters params = Parameters.builder().build();

        // Two conditions
        Condition cond1 = Condition.builder().fn(TestHelpers.isSet("Region")).build();
        Condition cond2 = Condition.builder().fn(TestHelpers.booleanEquals("UseFips", true)).build();

        // Two endpoint results
        Rule endpoint1 = EndpointRule.builder().endpoint(TestHelpers.endpoint("https://example.com"));
        Rule endpoint2 = EndpointRule.builder().endpoint(TestHelpers.endpoint("https://example-fips.com"));

        // NoMatchRule MUST be at index 0
        List<Rule> results = new ArrayList<>();
        results.add(NoMatchRule.INSTANCE); // Index 0 - always NoMatch
        results.add(endpoint1); // Index 1
        results.add(endpoint2); // Index 2

        // BDD structure referencing the correct indices
        int[][] nodes = new int[][] {
                {-1, 1, -1}, // 0: terminal node
                {0, 2, -1}, // 1: if Region is set, go to node 2, else FALSE
                {1, Bdd.RESULT_OFFSET + 2, Bdd.RESULT_OFFSET + 1} // 2: if UseFips, return result 2, else result 1
        };

        Bdd bdd = new Bdd(params, ListUtils.of(cond1, cond2), results, nodes, 1);
        String str = bdd.toString();

        assertTrue(str.contains("Endpoint:"));
        assertTrue(str.contains("C0"));
        assertTrue(str.contains("C1"));
        assertTrue(str.contains("R0"));
        assertTrue(str.contains("R1"));
        assertTrue(str.contains("R2"));
        assertTrue(str.contains("NoMatchRule")); // R0 will show as NoMatchRule

        // Test serialization doesn't include NoMatchRule
        Node serialized = bdd.toNode();
        assertEquals(2,
                serialized.expectObjectNode()
                        .expectArrayMember("results")
                        .getElements()
                        .size()); // Only the two endpoints, not NoMatch
    }

    private Bdd createSimpleBdd() {
        Parameters params = Parameters.builder().build();
        Condition cond = Condition.builder().fn(TestHelpers.isSet("Region")).build();
        Rule endpoint = EndpointRule.builder().endpoint(TestHelpers.endpoint("https://example.com"));

        // NoMatchRule MUST be at index 0
        List<Rule> results = new ArrayList<>();
        results.add(NoMatchRule.INSTANCE); // Index 0 - always NoMatch
        results.add(endpoint); // Index 1 - the actual endpoint

        int[][] nodes = new int[][] {
                {-1, 1, -1}, // 0: terminal
                {0, Bdd.RESULT_OFFSET + 1, -1} // 1: if cond true, return result 1 (endpoint), else FALSE
        };

        return new Bdd(params, ListUtils.of(cond), results, nodes, 1);
    }

    // Used to regenerate BDD test cases for errorfiles
    //    @Test
    //    void generateValidBddEncoding() {
    //        Parameter region = Parameter.builder()
    //                .name("Region")
    //                .type(ParameterType.STRING)
    //                .required(true)
    //                .documentation("The AWS region")
    //                .build();
    //
    //        Parameter useFips = Parameter.builder()
    //                .name("UseFips")
    //                .type(ParameterType.BOOLEAN)
    //                .required(true)
    //                .defaultValue(software.amazon.smithy.rulesengine.language.evaluation.value.Value.booleanValue(false))
    //                .documentation("Use FIPS endpoints")
    //                .build();
    //
    //        Parameters params = Parameters.builder()
    //                .addParameter(region)
    //                .addParameter(useFips)
    //                .build();
    //
    //        Condition useFipsTrue = Condition.builder()
    //                .fn(BooleanEquals.ofExpressions(
    //                        Expression.getReference(Identifier.of("UseFips")),
    //                        Expression.of(true)))
    //                .build();
    //
    //        // Create endpoints
    //        Endpoint normalEndpoint = Endpoint.builder()
    //                .url(Expression.of("https://service.{Region}.amazonaws.com"))
    //                .build();
    //
    //        Endpoint fipsEndpoint = Endpoint.builder()
    //                .url(Expression.of("https://service-fips.{Region}.amazonaws.com"))
    //                .build();
    //
    //        Rule fipsRule = EndpointRule.builder()
    //                .condition(useFipsTrue)
    //                .endpoint(fipsEndpoint);
    //
    //        Rule normalRule = EndpointRule.builder()
    //                .endpoint(normalEndpoint);
    //
    //        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
    //                .parameters(params)
    //                .rules(Arrays.asList(fipsRule, normalRule))
    //                .build();
    //
    //        Cfg cfg = Cfg.from(ruleSet);
    //        Bdd bdd = Bdd.from(cfg);
    //
    //        BddTrait trait = BddTrait.builder().bdd(bdd).build();
    //        BddTraitValidator validator = new BddTraitValidator();
    //        ServiceShape service = ServiceShape.builder().id("foo#Bar").addTrait(trait).build();
    //        Model model = Model.builder().addShape(service).build();
    //        System.out.println(validator.validate(model));
    //
    //        System.out.println(bdd);
    //
    //        // Get the base64 encoded nodes
    //        System.out.println(Node.prettyPrintJson(trait.toNode()));
    //    }
}
