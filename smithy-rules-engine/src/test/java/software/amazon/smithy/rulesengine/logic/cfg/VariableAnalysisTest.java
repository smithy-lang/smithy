/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.cfg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.Endpoint;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Template;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.BooleanEquals;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.StringEquals;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.syntax.parameters.ParameterType;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.ErrorRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.language.syntax.rule.TreeRule;
import software.amazon.smithy.rulesengine.logic.TestHelpers;
import software.amazon.smithy.utils.ListUtils;

class VariableAnalysisTest {

    @Test
    void testSimpleVariableBinding() {
        // Rule with one variable binding
        Condition condition = Condition.builder()
                .fn(TestHelpers.isSet("Region"))
                .result(Identifier.of("hasRegion"))
                .build();

        Rule rule = EndpointRule.builder()
                .conditions(condition)
                .endpoint(TestHelpers.endpoint("https://example.com"));

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(Parameters.builder()
                        .addParameter(Parameter.builder().name("Region").type(ParameterType.STRING).build())
                        .build())
                .addRule(rule)
                .build();

        VariableAnalysis analysis = VariableAnalysis.analyze(ruleSet);

        assertTrue(analysis.getInputParams().contains("Region"));
        assertTrue(analysis.hasSingleBinding("hasRegion"));
        assertEquals(0, analysis.getReferenceCount("hasRegion"));
    }

    @Test
    void testVariableReference() {
        // Define and use a variable
        Condition define = Condition.builder()
                .fn(TestHelpers.isSet("Region"))
                .result(Identifier.of("hasRegion"))
                .build();

        Condition use = Condition.builder()
                .fn(BooleanEquals.ofExpressions(
                        Expression.getReference(Identifier.of("hasRegion")),
                        Literal.of(true)))
                .build();

        Rule rule = EndpointRule.builder()
                .conditions(define, use)
                .endpoint(TestHelpers.endpoint("https://example.com"));

        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder().name("Region").type(ParameterType.STRING).build())
                .build();

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(params)
                .addRule(rule)
                .build();

        VariableAnalysis analysis = VariableAnalysis.analyze(ruleSet);

        assertEquals(1, analysis.getReferenceCount("hasRegion"));
        assertTrue(analysis.isReferencedOnce("hasRegion"));

        assertTrue(analysis.isSafeToInline("hasRegion"));
    }

    @Test
    void testMultipleBindings() {
        // Same variable assigned in different branches
        Rule rule1 = EndpointRule.builder()
                .conditions(Condition.builder()
                        .fn(TestHelpers.isSet("Region"))
                        .result(Identifier.of("x"))
                        .build())
                .endpoint(TestHelpers.endpoint("https://example1.com"));

        Rule rule2 = EndpointRule.builder()
                .conditions(Condition.builder()
                        .fn(TestHelpers.isSet("Bucket"))
                        .result(Identifier.of("x"))
                        .build())
                .endpoint(TestHelpers.endpoint("https://example2.com"));

        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder().name("Region").type(ParameterType.STRING).build())
                .addParameter(Parameter.builder().name("Bucket").type(ParameterType.STRING).build())
                .build();

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(params)
                .rules(ListUtils.of(rule1, rule2))
                .build();

        VariableAnalysis analysis = VariableAnalysis.analyze(ruleSet);

        assertFalse(analysis.hasSingleBinding("x"));

        // Not safe to inline when multiple bindings exist
        assertFalse(analysis.isSafeToInline("x"));

        // Should have different SSA names for different expressions
        Map<String, Map<String, String>> mappings = analysis.getExpressionMappings();
        assertNotNull(mappings.get("x"));
        assertEquals(2, mappings.get("x").size());
    }

    @Test
    void testMultipleReferences() {
        // Variable referenced multiple times
        Condition define = Condition.builder()
                .fn(TestHelpers.isSet("Region"))
                .result(Identifier.of("hasRegion"))
                .build();

        Condition use1 = Condition.builder()
                .fn(BooleanEquals.ofExpressions(
                        Expression.getReference(Identifier.of("hasRegion")),
                        Literal.of(true)))
                .build();

        Condition use2 = Condition.builder()
                .fn(BooleanEquals.ofExpressions(
                        Expression.getReference(Identifier.of("hasRegion")),
                        Literal.of(false)))
                .build();

        Rule rule = EndpointRule.builder()
                .conditions(define, use1, use2)
                .endpoint(TestHelpers.endpoint("https://example.com"));

        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder().name("Region").type(ParameterType.STRING).build())
                .build();

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(params)
                .addRule(rule)
                .build();

        VariableAnalysis analysis = VariableAnalysis.analyze(ruleSet);

        assertEquals(2, analysis.getReferenceCount("hasRegion"));
        assertFalse(analysis.isReferencedOnce("hasRegion"));

        assertFalse(analysis.isSafeToInline("hasRegion"));
    }

    @Test
    void testReferencesInEndpoint() {
        // Variable used in endpoint URL - just use the Region parameter directly
        Condition checkRegion = Condition.builder()
                .fn(TestHelpers.isSet("Region"))
                .build();

        Endpoint endpoint = Endpoint.builder()
                .url(Literal.stringLiteral(Template.fromString("https://{Region}.example.com")))
                .build();

        Rule rule = EndpointRule.builder()
                .conditions(checkRegion)
                .endpoint(endpoint);

        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder().name("Region").type(ParameterType.STRING).build())
                .build();

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(params)
                .addRule(rule)
                .build();

        VariableAnalysis analysis = VariableAnalysis.analyze(ruleSet);

        assertEquals(2, analysis.getReferenceCount("Region"));
    }

    @Test
    void testReferencesInErrorRule() {
        // First prove Region is set, then check if it's invalid
        Condition checkRegion = Condition.builder()
                .fn(TestHelpers.isSet("Region"))
                .result(Identifier.of("hasRegion"))
                .build();

        Condition checkInvalid = Condition.builder()
                .fn(StringEquals.ofExpressions(
                        Expression.getReference(Identifier.of("Region")),
                        Literal.of("invalid")))
                .result(Identifier.of("isInvalid"))
                .build();

        // Use the Region value directly in the error message
        Rule rule = ErrorRule.builder()
                .conditions(checkRegion, checkInvalid)
                .error(Literal.stringLiteral(Template.fromString("Invalid region: {Region}")));

        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder().name("Region").type(ParameterType.STRING).build())
                .build();

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(params)
                .addRule(rule)
                .build();

        VariableAnalysis analysis = VariableAnalysis.analyze(ruleSet);

        assertEquals(3, analysis.getReferenceCount("Region"));
        assertTrue(analysis.getReferenceCount("hasRegion") >= 0);
        assertEquals(0, analysis.getReferenceCount("isInvalid"));
    }

    @Test
    void testNestedTreeRuleAnalysis() {
        // Nested rules with variable bindings
        Condition outerDefine = Condition.builder()
                .fn(TestHelpers.isSet("Region"))
                .result(Identifier.of("hasRegion"))
                .build();

        Condition innerUse = Condition.builder()
                .fn(BooleanEquals.ofExpressions(
                        Expression.getReference(Identifier.of("hasRegion")),
                        Literal.of(true)))
                .build();

        Rule innerRule = EndpointRule.builder()
                .conditions(innerUse)
                .endpoint(TestHelpers.endpoint("https://example.com"));

        Rule treeRule = TreeRule.builder()
                .conditions(outerDefine)
                .treeRule(innerRule);

        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder().name("Region").type(ParameterType.STRING).build())
                .build();

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(params)
                .addRule(treeRule)
                .build();

        VariableAnalysis analysis = VariableAnalysis.analyze(ruleSet);

        assertTrue(analysis.hasSingleBinding("hasRegion"));
        assertEquals(1, analysis.getReferenceCount("hasRegion"));
        assertTrue(analysis.isSafeToInline("hasRegion"));
    }

    @Test
    void testInputParametersIdentified() {
        // Multiple input parameters
        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder().name("Region").type(ParameterType.STRING).build())
                .addParameter(Parameter.builder().name("Bucket").type(ParameterType.STRING).build())
                .addParameter(Parameter.builder().name("UseDualStack").type(ParameterType.BOOLEAN).build())
                .build();

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(params)
                .build();

        VariableAnalysis analysis = VariableAnalysis.analyze(ruleSet);

        assertEquals(3, analysis.getInputParams().size());
        assertTrue(analysis.getInputParams().contains("Region"));
        assertTrue(analysis.getInputParams().contains("Bucket"));
        assertTrue(analysis.getInputParams().contains("UseDualStack"));
    }

    @Test
    void testNoVariables() {
        // Simple ruleset with no variable bindings
        Rule rule = EndpointRule.builder()
                .conditions(Condition.builder().fn(TestHelpers.isSet("Region")).build())
                .endpoint(TestHelpers.endpoint("https://example.com"));

        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder().name("Region").type(ParameterType.STRING).build())
                .build();

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(params)
                .addRule(rule)
                .build();

        VariableAnalysis analysis = VariableAnalysis.analyze(ruleSet);

        // No variables bound
        assertEquals(0, analysis.getReferenceCount("anyVar"));
        assertFalse(analysis.hasSingleBinding("anyVar"));
        assertFalse(analysis.isSafeToInline("anyVar"));

        // But Region is an input parameter
        assertTrue(analysis.getInputParams().contains("Region"));
    }
}
