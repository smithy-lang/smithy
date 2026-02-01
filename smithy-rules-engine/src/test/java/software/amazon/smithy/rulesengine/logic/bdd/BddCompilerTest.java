/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.syntax.parameters.ParameterType;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.ErrorRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.language.syntax.rule.TreeRule;
import software.amazon.smithy.rulesengine.logic.TestHelpers;
import software.amazon.smithy.rulesengine.logic.cfg.Cfg;
import software.amazon.smithy.rulesengine.traits.EndpointBddTrait;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;

class BddCompilerTest {

    // Common parameters used across tests
    private static final Parameter REGION_PARAM = Parameter.builder()
            .name("Region")
            .type(ParameterType.STRING)
            .build();

    private static final Parameter BUCKET_PARAM = Parameter.builder()
            .name("Bucket")
            .type(ParameterType.STRING)
            .build();

    private static final Parameter A_PARAM = Parameter.builder()
            .name("A")
            .type(ParameterType.STRING)
            .build();

    private static final Parameter B_PARAM = Parameter.builder()
            .name("B")
            .type(ParameterType.STRING)
            .build();

    @Test
    void testCompileSimpleEndpointRule() {
        // Single rule with one condition
        Rule rule = EndpointRule.builder()
                .conditions(Condition.builder().fn(TestHelpers.isSet("Region")).build())
                .endpoint(TestHelpers.endpoint("https://example.com"));

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(Parameters.builder().addParameter(REGION_PARAM).build())
                .addRule(rule)
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        BddCompiler compiler = new BddCompiler(cfg, new BddBuilder());

        Bdd bdd = compiler.compile();

        assertNotNull(bdd);
        assertEquals(1, bdd.getConditionCount());
        // Results: NoMatchRule (for false branch) and endpoint (for true branch)
        assertEquals(2, bdd.getResultCount());
        assertTrue(bdd.getRootRef() != 0);
    }

    @Test
    void testCompileErrorRule() {
        // Error rule instead of endpoint
        Rule rule = ErrorRule.builder()
                .conditions(Condition.builder().fn(TestHelpers.isSet("Bucket")).build())
                .error("Bucket is required");

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(Parameters.builder().addParameter(BUCKET_PARAM).build())
                .addRule(rule)
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        BddCompiler compiler = new BddCompiler(cfg, new BddBuilder());

        Bdd bdd = compiler.compile();

        assertEquals(1, bdd.getConditionCount());
        // Results: NoMatchRule (for false branch) and error (for true branch)
        assertEquals(2, bdd.getResultCount());
    }

    @Test
    void testCompileTreeRule() {
        // Nested tree rule
        Rule nestedRule = EndpointRule.builder()
                .conditions(Condition.builder().fn(TestHelpers.isSet("Bucket")).build())
                .endpoint(TestHelpers.endpoint("https://bucket.example.com"));
        Rule treeRule = TreeRule.builder()
                .conditions(Condition.builder().fn(TestHelpers.isSet("Region")).build())
                .treeRule(nestedRule);
        Parameters params = Parameters.builder().addParameter(REGION_PARAM).addParameter(BUCKET_PARAM).build();
        EndpointRuleSet ruleSet = EndpointRuleSet.builder().parameters(params).addRule(treeRule).build();

        Cfg cfg = Cfg.from(ruleSet);
        BddCompiler compiler = new BddCompiler(cfg, new BddBuilder());

        Bdd bdd = compiler.compile();

        assertEquals(2, bdd.getConditionCount());
        assertTrue(bdd.getNodeCount() >= 2); // Should have multiple nodes
    }

    @Test
    void testCompileWithCustomOrdering() {
        // Multiple conditions to test ordering
        Rule rule = EndpointRule.builder()
                .conditions(Condition.builder().fn(TestHelpers.isSet("A")).build(),
                        Condition.builder().fn(TestHelpers.isSet("B")).build())
                .endpoint(TestHelpers.endpoint("https://example.com"));
        Parameters params = Parameters.builder().addParameter(A_PARAM).addParameter(B_PARAM).build();
        EndpointRuleSet ruleSet = EndpointRuleSet.builder().parameters(params).addRule(rule).build();

        Cfg cfg = Cfg.from(ruleSet);

        // Get the actual conditions from the CFG after SSA transform
        Condition[] cfgConditions = cfg.getConditions();

        // Find the conditions that correspond to A and B
        Condition condA = null;
        Condition condB = null;
        for (Condition c : cfgConditions) {
            String condStr = c.toString();
            if (condStr.contains("isSet(A)")) {
                condA = c;
            } else if (condStr.contains("isSet(B)")) {
                condB = c;
            }
        }

        assertNotNull(condA, "Could not find condition for A");
        assertNotNull(condB, "Could not find condition for B");

        // Use fixed ordering (B before A)
        OrderingStrategy customOrdering = OrderingStrategy.fixed(Arrays.asList(condB, condA));

        BddCompiler compiler = new BddCompiler(cfg, customOrdering, new BddBuilder());
        Bdd bdd = compiler.compile();

        List<Condition> orderedConditions = compiler.getOrderedConditions();

        // Verify ordering was applied
        assertEquals(2, bdd.getConditionCount());
        assertEquals(2, orderedConditions.size());
        // Verify B comes before A in the ordering
        assertEquals(condB, orderedConditions.get(0));
        assertEquals(condA, orderedConditions.get(1));
    }

    @Test
    void testCompileEmptyRuleSet() {
        // No rules
        EndpointRuleSet ruleSet = EndpointRuleSet.builder().parameters(Parameters.builder().build()).build();

        Cfg cfg = Cfg.from(ruleSet);
        BddCompiler compiler = new BddCompiler(cfg, new BddBuilder());
        Bdd bdd = compiler.compile();

        assertEquals(0, bdd.getConditionCount());
        // Only NoMatchRule - the terminal now uses NoMatchRule.INSTANCE, not a separate null result
        assertEquals(1, bdd.getResultCount());
        // Should have at least terminal node
        assertEquals(1, bdd.getNodeCount());
    }

    @Test
    void testCompileSameResultMultiplePaths() {
        // Two rules leading to same endpoint
        Rule rule1 = EndpointRule.builder()
                .conditions(Condition.builder().fn(TestHelpers.isSet("Region")).build())
                .endpoint(TestHelpers.endpoint("https://example.com"));

        Rule rule2 = EndpointRule.builder()
                .conditions(Condition.builder().fn(TestHelpers.isSet("Bucket")).build())
                .endpoint(TestHelpers.endpoint("https://example.com"));

        Parameters params = Parameters.builder()
                .addParameter(REGION_PARAM)
                .addParameter(BUCKET_PARAM)
                .build();

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(params)
                .addRule(rule1)
                .addRule(rule2)
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        BddCompiler compiler = new BddCompiler(cfg, new BddBuilder());

        Bdd bdd = compiler.compile();
        List<Rule> results = compiler.getIndexedResults();

        // Should have 2 conditions
        assertEquals(2, bdd.getConditionCount());
        // Results: NoMatchRule at index 0, plus the endpoint(s)
        // The compiler may deduplicate identical endpoints or keep them separate
        assertTrue(bdd.getResultCount() >= 2);
        assertTrue(bdd.getResultCount() <= 3);

        // Verify NoMatchRule is always at index 0
        assertEquals("NoMatchRule", results.get(0).getClass().getSimpleName());
    }

    @Test
    void testCompileWithReduction() {
        // Test that the BDD is properly reduced after compilation
        Rule rule = EndpointRule.builder()
                .conditions(
                        Condition.builder().fn(TestHelpers.isSet("Region")).build(),
                        Condition.builder().fn(TestHelpers.isSet("Bucket")).build())
                .endpoint(TestHelpers.endpoint("https://example.com"));

        Parameters params = Parameters.builder()
                .addParameter(REGION_PARAM)
                .addParameter(BUCKET_PARAM)
                .build();

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(params)
                .addRule(rule)
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        BddBuilder builder = new BddBuilder();
        BddCompiler compiler = new BddCompiler(cfg, builder);

        Bdd bdd = compiler.compile();

        // The BDD should be reduced (no redundant nodes)
        assertNotNull(bdd);
        assertEquals(2, bdd.getConditionCount());

        // After reduction, we should have a minimal BDD
        // For 2 conditions with AND semantics leading to one endpoint:
        // We expect approximately 3-4 nodes (depending on the exact structure)
        assertTrue(bdd.getNodeCount() <= 5, "BDD should be reduced to minimal form");
    }

    @Test
    public void convertsModelsThatHaveNoCatchAllCondition() {
        // This model does not define a catch-all condition, so it falls through to a null result,
        // which maps to the NoMatchRule.
        Model model = Model.assembler()
                .addImport(getClass().getResource("get-attr.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();

        ServiceShape service = model.expectShape(
                ShapeId.from("smithy.tests.endpointrules.getattr#FizzBuzz"),
                ServiceShape.class);
        EndpointRuleSetTrait trait = service.expectTrait(EndpointRuleSetTrait.class);
        Cfg cfg = Cfg.from(trait.getEndpointRuleSet());
        EndpointBddTrait bdd = EndpointBddTrait.from(cfg);

        Assertions.assertDoesNotThrow(bdd::toNode);
    }

    @Test
    public void doesNotFailOnSplitTest() {
        // This test verifies that SSA transformation correctly handles transitive dependencies.
        //
        // The split.smithy model has 5 mutually exclusive branches (based on Limit being "0"-"4"),
        // each containing:
        //   parts = split(Input, Delimiter, <limit>)   // different expression per branch
        //   part1 = coalesce(getAttr(parts, "[0]"), "<null>")  // same expression text in all branches
        //   part2 = coalesce(getAttr(parts, "[1]"), "<null>")  // same expression text in all branches
        //   ... etc
        //
        // The SSA transform must recognize that:
        // 1. "parts" has different expressions per branch -> needs SSA renaming (parts_ssa_1, etc.)
        // 2. "part1" has identical expression TEXT but references "parts" which gets renamed
        //    -> after rewriting, expressions diverge -> also needs SSA renaming
        //
        // Without proper transitive dependency handling, all 5 "part1" bindings would get the same
        // SSA name, causing the BDD validator to reject them as "shadowing" when it type-checks
        // the flattened condition list.
        Model model = Model.assembler()
                .addImport(EndpointRuleSet.class.getResource("errorfiles/valid/split.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();

        ServiceShape service = model.expectShape(
                ShapeId.from("example#SplitTestService"),
                ServiceShape.class);
        EndpointRuleSetTrait trait = service.expectTrait(EndpointRuleSetTrait.class);
        Cfg cfg = Cfg.from(trait.getEndpointRuleSet());
        EndpointBddTrait bdd = EndpointBddTrait.from(cfg);

        Assertions.assertDoesNotThrow(bdd::toNode);
    }
}
