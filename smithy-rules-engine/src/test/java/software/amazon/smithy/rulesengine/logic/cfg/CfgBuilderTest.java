/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.cfg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.Endpoint;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.BooleanEquals;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.Not;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.StringEquals;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.syntax.parameters.ParameterType;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.ErrorRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.logic.ConditionReference;
import software.amazon.smithy.rulesengine.logic.TestHelpers;
import software.amazon.smithy.utils.ListUtils;

class CfgBuilderTest {

    private CfgBuilder builder;
    private EndpointRuleSet ruleSet;

    @BeforeEach
    void setUp() {
        Parameter region = Parameter.builder().name("region").type(ParameterType.STRING).build();
        Parameter useFips = Parameter.builder()
                .name("useFips")
                .type(ParameterType.BOOLEAN)
                .defaultValue(Value.booleanValue(false))
                .required(true)
                .build();
        ruleSet = EndpointRuleSet.builder()
                .parameters(Parameters.builder().addParameter(region).addParameter(useFips).build())
                .build();

        builder = new CfgBuilder(ruleSet);
    }

    @Test
    void buildRequiresNonNullRoot() {
        assertThrows(NullPointerException.class, () -> builder.build(null));
    }

    @Test
    void buildCreatesValidCfg() {
        CfgNode root = ResultNode.terminal();
        Cfg cfg = builder.build(root);

        assertNotNull(cfg);
        assertSame(root, cfg.getRoot());
        assertEquals(ruleSet.getParameters(), cfg.getParameters());
    }

    @Test
    void createResultNodesCachesIdenticalRules() {
        Rule rule1 = EndpointRule.builder().endpoint(TestHelpers.endpoint("https://example.com"));
        Rule rule2 = EndpointRule.builder().endpoint(TestHelpers.endpoint("https://example.com"));

        CfgNode node1 = builder.createResult(rule1);
        CfgNode node2 = builder.createResult(rule2);

        assertSame(node1, node2);
    }

    @Test
    void createResultNodesDistinguishesDifferentRules() {
        Rule rule1 = EndpointRule.builder().endpoint(TestHelpers.endpoint("https://example1.com"));
        Rule rule2 = EndpointRule.builder().endpoint(TestHelpers.endpoint("https://example2.com"));

        CfgNode node1 = builder.createResult(rule1);
        CfgNode node2 = builder.createResult(rule2);

        assertNotSame(node1, node2);
    }

    @Test
    void createResultStripsConditionsBeforeCaching() {
        Condition cond = Condition.builder().fn(TestHelpers.isSet("region")).build();
        Rule ruleWithCondition = EndpointRule.builder()
                .condition(cond)
                .endpoint(TestHelpers.endpoint("https://example.com"));
        Rule ruleWithoutCondition = EndpointRule.builder().endpoint(TestHelpers.endpoint("https://example.com"));

        CfgNode node1 = builder.createResult(ruleWithCondition);
        CfgNode node2 = builder.createResult(ruleWithoutCondition);

        assertSame(node1, node2);
    }

    @Test
    void createConditionCachesIdenticalNodes() {
        Condition cond = Condition.builder().fn(TestHelpers.isSet("region")).build();
        CfgNode trueBranch = ResultNode.terminal();
        CfgNode falseBranch = ResultNode.terminal();

        CfgNode node1 = builder.createCondition(cond, trueBranch, falseBranch);
        CfgNode node2 = builder.createCondition(cond, trueBranch, falseBranch);

        assertSame(node1, node2);
    }

    @Test
    void createConditionDistinguishesDifferentBranches() {
        Condition cond = Condition.builder().fn(TestHelpers.isSet("region")).build();
        CfgNode trueBranch1 = builder.createResult(
                EndpointRule.builder().endpoint(TestHelpers.endpoint("https://true1.com")));
        CfgNode trueBranch2 = builder.createResult(
                EndpointRule.builder().endpoint(TestHelpers.endpoint("https://true2.com")));
        CfgNode falseBranch = ResultNode.terminal();

        CfgNode node1 = builder.createCondition(cond, trueBranch1, falseBranch);
        CfgNode node2 = builder.createCondition(cond, trueBranch2, falseBranch);

        assertNotSame(node1, node2);
    }

    @Test
    void createConditionReferenceHandlesSimpleCondition() {
        Condition cond = Condition.builder().fn(TestHelpers.isSet("region")).build();

        ConditionReference ref = builder.createConditionReference(cond);

        assertNotNull(ref);
        assertEquals(cond, ref.getCondition());
        assertFalse(ref.isNegated());
    }

    @Test
    void createConditionReferenceCachesIdenticalConditions() {
        Condition cond1 = Condition.builder().fn(TestHelpers.isSet("region")).build();
        Condition cond2 = Condition.builder().fn(TestHelpers.isSet("region")).build();

        ConditionReference ref1 = builder.createConditionReference(cond1);
        ConditionReference ref2 = builder.createConditionReference(cond2);

        assertSame(ref1, ref2);
    }

    @Test
    void createConditionReferenceHandlesNegation() {
        Condition innerCond = Condition.builder().fn(TestHelpers.isSet("region")).build();
        Condition negatedCond = Condition.builder().fn(Not.ofExpressions(innerCond.getFunction())).build();
        ConditionReference ref = builder.createConditionReference(negatedCond);

        assertNotNull(ref);
        assertTrue(ref.isNegated());
        assertEquals(innerCond.getFunction(), ref.getCondition().getFunction());
    }

    @Test
    void createConditionReferenceSharesInfoForNegatedAndNonNegated() {
        Condition cond = Condition.builder().fn(TestHelpers.isSet("region")).build();
        Condition negatedCond = Condition.builder().fn(Not.ofExpressions(cond.getFunction())).build();

        ConditionReference ref1 = builder.createConditionReference(cond);
        ConditionReference ref2 = builder.createConditionReference(negatedCond);

        assertEquals(ref1.getCondition(), ref2.getCondition());
        assertFalse(ref1.isNegated());
        assertTrue(ref2.isNegated());
    }

    @Test
    void createConditionReferenceHandlesBooleanEqualsCanonicalizations() {
        // Test booleanEquals(useFips, false) -> booleanEquals(useFips, true) with negation
        Expression ref = Expression.getReference(Identifier.of("useFips"));
        Condition cond = Condition.builder().fn(BooleanEquals.ofExpressions(ref, false)).build();

        ConditionReference condRef = builder.createConditionReference(cond);

        // Should be canonicalized to booleanEquals(useFips, true) with negation
        assertTrue(condRef.isNegated());
        assertInstanceOf(BooleanEquals.class, condRef.getCondition().getFunction());

        BooleanEquals fn = (BooleanEquals) condRef.getCondition().getFunction();
        assertEquals(ref, fn.getArguments().get(0));
        assertEquals(Literal.booleanLiteral(true), fn.getArguments().get(1));
    }

    @Test
    void createConditionReferenceDoesNotCanonicalizeWithoutDefault() {
        // Test that booleanEquals(region, false) is not canonicalized (no default)
        Expression ref = Expression.getReference(Identifier.of("region"));
        Condition cond = Condition.builder().fn(BooleanEquals.ofExpressions(ref, false)).build();

        ConditionReference condRef = builder.createConditionReference(cond);

        assertFalse(condRef.isNegated());
        assertEquals(cond.getFunction(), condRef.getCondition().getFunction());
    }

    @Test
    void createConditionReferenceHandlesCommutativeCanonicalizations() {
        Expression ref = Expression.getReference(Identifier.of("region"));

        // Create conditions with different argument orders
        Condition cond1 = Condition.builder().fn(StringEquals.ofExpressions(ref, "us-east-1")).build();
        Condition cond2 = Condition.builder().fn(StringEquals.ofExpressions(Expression.of("us-east-1"), ref)).build();

        ConditionReference ref1 = builder.createConditionReference(cond1);
        ConditionReference ref2 = builder.createConditionReference(cond2);

        // Both should produce equivalent canonicalized references.
        // They should have the same underlying condition after canonicalization
        assertEquals(ref1.getCondition(), ref2.getCondition());
        assertEquals(ref1.isNegated(), ref2.isNegated());
    }

    @Test
    void createConditionReferenceHandlesVariableBinding() {
        Condition cond = Condition.builder()
                .fn(TestHelpers.parseUrl("{url}"))
                .result(Identifier.of("parsedUrl"))
                .build();

        ConditionReference ref = builder.createConditionReference(cond);

        assertNotNull(ref);
        assertEquals(cond, ref.getCondition());
    }

    @Test
    void createConditionHandlesComplexNesting() {
        // Build a nested structure to test caching
        CfgNode endpoint1 = builder.createResult(
                EndpointRule.builder().endpoint(TestHelpers.endpoint("https://endpoint1.com")));
        CfgNode endpoint2 = builder.createResult(
                EndpointRule.builder().endpoint(TestHelpers.endpoint("https://endpoint2.com")));
        CfgNode errorNode = builder.createResult(ErrorRule.builder().error("Invalid configuration"));

        Condition cond1 = Condition.builder().fn(TestHelpers.isSet("region")).build();
        Condition cond2 = Condition.builder().fn(TestHelpers.stringEquals("region", "us-east-1")).build();

        // Create nested conditions
        CfgNode inner = builder.createCondition(cond2, endpoint1, endpoint2);
        CfgNode outer = builder.createCondition(cond1, inner, errorNode);

        assertInstanceOf(ConditionNode.class, outer);
        ConditionNode outerNode = (ConditionNode) outer;
        assertEquals(cond1, outerNode.getCondition().getCondition());
        assertSame(inner, outerNode.getTrueBranch());
        assertSame(errorNode, outerNode.getFalseBranch());
    }

    @Test
    void createConditionReferenceIgnoresNegationWithVariableBinding() {
        // Negation with variable binding should not unwrap
        Condition innerCond = Condition.builder().fn(TestHelpers.isSet("region")).build();

        Condition negatedWithBinding = Condition.builder()
                .fn(Not.ofExpressions(innerCond.getFunction()))
                .result(Identifier.of("notRegionSet"))
                .build();

        ConditionReference ref = builder.createConditionReference(negatedWithBinding);

        // Should not be treated as simple negation due to variable binding
        assertFalse(ref.isNegated());
        assertInstanceOf(Not.class, ref.getCondition().getFunction());
        assertEquals(negatedWithBinding, ref.getCondition());
    }

    @Test
    void createResultPreservesHeadersAndPropertiesInSignature() {
        // Create endpoints with same URL but different headers
        Map<String, List<Expression>> headers1 = new HashMap<>();
        headers1.put("x-custom", Collections.singletonList(Expression.of("value1")));

        Map<String, List<Expression>> headers2 = new HashMap<>();
        headers2.put("x-custom", Collections.singletonList(Expression.of("value2")));

        Rule rule1 = EndpointRule.builder()
                .endpoint(Endpoint.builder()
                        .url(Expression.of("https://example.com"))
                        .headers(headers1)
                        .build());
        Rule rule2 = EndpointRule.builder()
                .endpoint(Endpoint.builder()
                        .url(Expression.of("https://example.com"))
                        .headers(headers2)
                        .build());

        EndpointRuleSet ruleSetWithHeaders = EndpointRuleSet.builder()
                .parameters(ruleSet.getParameters())
                .rules(ListUtils.of(rule1, rule2))
                .build();
        CfgBuilder convergenceBuilder = new CfgBuilder(ruleSetWithHeaders);

        CfgNode node1 = convergenceBuilder.createResult(rule1);
        CfgNode node2 = convergenceBuilder.createResult(rule2);

        // Different headers mean different signatures - no convergence
        assertNotSame(node1, node2);
    }

    @Test
    void createResultWithStructurallyIdenticalEndpointsCreatesConvergenceNode() {
        // Create two rules with structurally identical endpoints but different variable names
        Rule rule1 = EndpointRule.builder()
                .endpoint(TestHelpers.endpoint("https://{region1}.example.com"));
        Rule rule2 = EndpointRule.builder()
                .endpoint(TestHelpers.endpoint("https://{region2}.example.com"));

        // Create parameters for the variables used in the endpoints
        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder()
                        .name("region1")
                        .type(ParameterType.STRING)
                        .defaultValue(Value.stringValue("a"))
                        .required(true)
                        .build())
                .addParameter(Parameter.builder()
                        .name("region2")
                        .type(ParameterType.STRING)
                        .defaultValue(Value.stringValue("a"))
                        .required(true)
                        .build())
                .build();

        EndpointRuleSet ruleSetWithEndpoints = EndpointRuleSet.builder()
                .parameters(params)
                .rules(ListUtils.of(rule1, rule2))
                .build();
        CfgBuilder convergenceBuilder = new CfgBuilder(ruleSetWithEndpoints);

        CfgNode node1 = convergenceBuilder.createResult(rule1);
        CfgNode node2 = convergenceBuilder.createResult(rule2);

        // Both should return the same convergence node
        assertSame(node1, node2);
        assertInstanceOf(ConditionNode.class, node1);
    }

    @Test
    void createResultDistinguishesEndpointsWithDifferentStructure() {
        // Create rules with different endpoint structures
        Rule rule1 = EndpointRule.builder()
                .endpoint(TestHelpers.endpoint("https://{region}.example.com"));
        Rule rule2 = EndpointRule.builder()
                .endpoint(TestHelpers.endpoint("https://example.com/{region}"));

        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder()
                        .name("region")
                        .type(ParameterType.STRING)
                        .defaultValue(Value.stringValue("a"))
                        .required(true)
                        .build())
                .build();

        EndpointRuleSet ruleSetWithEndpoints = EndpointRuleSet.builder()
                .parameters(params)
                .rules(ListUtils.of(rule1, rule2))
                .build();
        CfgBuilder convergenceBuilder = new CfgBuilder(ruleSetWithEndpoints);

        CfgNode node1 = convergenceBuilder.createResult(rule1);
        CfgNode node2 = convergenceBuilder.createResult(rule2);

        // Different structures should not converge
        assertNotSame(node1, node2);
    }

    @Test
    void createResultWithIdenticalErrorsCreatesConvergenceNode() {
        // Create structurally identical error rules with different variable references
        Rule error1 = ErrorRule.builder()
                .error(Expression.of("Region {r1} is not supported"));
        Rule error2 = ErrorRule.builder()
                .error(Expression.of("Region {r2} is not supported"));

        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder()
                        .name("r1")
                        .type(ParameterType.STRING)
                        .defaultValue(Value.stringValue("a"))
                        .required(true)
                        .build())
                .addParameter(Parameter.builder()
                        .name("r2")
                        .type(ParameterType.STRING)
                        .defaultValue(Value.stringValue("a"))
                        .required(true)
                        .build())
                .build();

        EndpointRuleSet ruleSetWithErrors = EndpointRuleSet.builder()
                .parameters(params)
                .rules(ListUtils.of(error1, error2))
                .build();
        CfgBuilder convergenceBuilder = new CfgBuilder(ruleSetWithErrors);

        CfgNode node1 = convergenceBuilder.createResult(error1);
        CfgNode node2 = convergenceBuilder.createResult(error2);

        // Should converge to the same node
        assertSame(node1, node2);
    }

    @Test
    void createResultHandlesComplexTemplateConvergence() {
        // Create endpoints with complex templates that are structurally identical
        Rule rule1 = EndpointRule.builder()
                .endpoint(TestHelpers.endpoint("https://{svc}.{reg}.amazonaws.com/{path}"));
        Rule rule2 = EndpointRule.builder()
                .endpoint(TestHelpers.endpoint("https://{service}.{region}.amazonaws.com/{resource}"));

        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder()
                        .name("svc")
                        .type(ParameterType.STRING)
                        .defaultValue(Value.stringValue("a"))
                        .required(true)
                        .build())
                .addParameter(Parameter.builder()
                        .name("reg")
                        .type(ParameterType.STRING)
                        .defaultValue(Value.stringValue("a"))
                        .required(true)
                        .build())
                .addParameter(Parameter.builder()
                        .name("path")
                        .type(ParameterType.STRING)
                        .defaultValue(Value.stringValue("a"))
                        .required(true)
                        .build())
                .addParameter(Parameter.builder()
                        .name("service")
                        .type(ParameterType.STRING)
                        .defaultValue(Value.stringValue("a"))
                        .required(true)
                        .build())
                .addParameter(Parameter.builder()
                        .name("region")
                        .type(ParameterType.STRING)
                        .defaultValue(Value.stringValue("a"))
                        .required(true)
                        .build())
                .addParameter(Parameter.builder()
                        .name("resource")
                        .type(ParameterType.STRING)
                        .defaultValue(Value.stringValue("a"))
                        .required(true)
                        .build())
                .build();

        EndpointRuleSet ruleSetWithTemplates = EndpointRuleSet.builder()
                .parameters(params)
                .rules(ListUtils.of(rule1, rule2))
                .build();
        CfgBuilder convergenceBuilder = new CfgBuilder(ruleSetWithTemplates);

        CfgNode node1 = convergenceBuilder.createResult(rule1);
        CfgNode node2 = convergenceBuilder.createResult(rule2);

        // Structurally identical templates should converge
        assertSame(node1, node2);
    }

    @Test
    void createResultDoesNotConvergeWithTooManyDivergentPaths() {
        // Create many endpoints with different variable names at multiple positions
        // This should exceed MAX_DIVERGENT_PATHS_FOR_CONVERGENCE (5)
        List<Rule> rules = new ArrayList<>();
        Parameters.Builder paramsBuilder = Parameters.builder();

        for (int i = 0; i < 7; i++) {
            rules.add(EndpointRule.builder()
                    .endpoint(TestHelpers.endpoint(String.format("https://{var%d}.{reg%d}.example.com", i, i))));
            paramsBuilder.addParameter(Parameter.builder()
                    .name("var" + i)
                    .type(ParameterType.STRING)
                    .defaultValue(Value.stringValue("a"))
                    .required(true)
                    .build());
            paramsBuilder.addParameter(Parameter.builder()
                    .name("reg" + i)
                    .type(ParameterType.STRING)
                    .defaultValue(Value.stringValue("a"))
                    .required(true)
                    .build());
        }

        EndpointRuleSet ruleSetWithMany = EndpointRuleSet.builder()
                .parameters(paramsBuilder.build())
                .rules(rules)
                .build();
        CfgBuilder convergenceBuilder = new CfgBuilder(ruleSetWithMany);

        // With too many divergent paths, convergence should be skipped
        CfgNode firstNode = convergenceBuilder.createResult(rules.get(0));
        CfgNode lastNode = convergenceBuilder.createResult(rules.get(rules.size() - 1));

        // They should still be cached as the same due to interning,
        // but won't have phi node convergence due to performance limits
        assertSame(firstNode, lastNode);
    }
}
