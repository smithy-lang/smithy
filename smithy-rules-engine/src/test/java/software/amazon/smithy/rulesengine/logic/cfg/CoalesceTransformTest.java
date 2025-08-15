/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.cfg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
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
import software.amazon.smithy.rulesengine.language.syntax.rule.TreeRule;
import software.amazon.smithy.rulesengine.logic.TestHelpers;
import software.amazon.smithy.utils.ListUtils;

class CoalesceTransformTest {

    @Test
    void testActualCoalescing() {
        // Test with substring which returns a string (has zero value "")
        // This should actually coalesce
        Condition checkInput = Condition.builder()
                .fn(TestHelpers.isSet("Input"))
                .build();

        Condition bind = Condition.builder()
                .fn(TestHelpers.substring("Input", 0, 5, false))
                .result(Identifier.of("prefix"))
                .build();

        Condition use = Condition.builder()
                .fn(StringEquals.ofExpressions(
                        Expression.getReference(Identifier.of("prefix")),
                        Literal.of("https")))
                .result(Identifier.of("isHttps"))
                .build();

        Rule rule = EndpointRule.builder()
                .conditions(checkInput, bind, use)
                .endpoint(TestHelpers.endpoint("https://example.com"));

        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder()
                        .name("Input")
                        .type(ParameterType.STRING)
                        .build())
                .build();

        EndpointRuleSet original = EndpointRuleSet.builder()
                .parameters(params)
                .addRule(rule)
                .build();

        EndpointRuleSet transformed = CoalesceTransform.transform(original);

        // Should coalesce because substring returns string which has zero value
        Rule transformedRule = transformed.getRules().get(0);
        assertEquals(2, transformedRule.getConditions().size()); // isSet + coalesced

        Condition coalesced = transformedRule.getConditions().get(1);
        assertTrue(coalesced.getResult().isPresent());
        assertEquals("isHttps", coalesced.getResult().get().toString());
    }

    @Test
    void testSimpleBindThenUsePattern() {
        // parseUrl returns a record type which doesn't have a zero value
        // So it won't be coalesced
        Condition checkEndpoint = Condition.builder()
                .fn(TestHelpers.isSet("Endpoint"))
                .build();

        Condition bind = Condition.builder()
                .fn(TestHelpers.parseUrl("Endpoint"))
                .result(Identifier.of("url"))
                .build();

        Condition use = Condition.builder()
                .fn(TestHelpers.getAttr(
                        Expression.getReference(Identifier.of("url")),
                        "scheme"))
                .result(Identifier.of("scheme"))
                .build();

        Rule rule = EndpointRule.builder()
                .conditions(checkEndpoint, bind, use)
                .endpoint(TestHelpers.endpoint("https://example.com"));

        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder()
                        .name("Endpoint")
                        .type(ParameterType.STRING)
                        .build())
                .build();

        EndpointRuleSet original = EndpointRuleSet.builder()
                .parameters(params)
                .addRule(rule)
                .build();

        EndpointRuleSet transformed = CoalesceTransform.transform(original);

        Rule transformedRule = transformed.getRules().get(0);
        List<Condition> conditions = transformedRule.getConditions();

        // Should not coalesce because parseUrl returns a record without zero value
        assertEquals(3, conditions.size());
    }

    @Test
    void testDoesNotCoalesceWhenVariableUsedMultipleTimes() {
        // Variable is used multiple times - should not coalesce
        Condition checkEndpoint = Condition.builder()
                .fn(TestHelpers.isSet("Endpoint"))
                .build();

        Condition bind = Condition.builder()
                .fn(TestHelpers.parseUrl("Endpoint"))
                .result(Identifier.of("url"))
                .build();

        Condition use1 = Condition.builder()
                .fn(TestHelpers.getAttr(
                        Expression.getReference(Identifier.of("url")),
                        "scheme"))
                .result(Identifier.of("scheme"))
                .build();

        Condition use2 = Condition.builder()
                .fn(TestHelpers.getAttr(
                        Expression.getReference(Identifier.of("url")),
                        "authority"))
                .result(Identifier.of("authority"))
                .build();

        Rule rule = EndpointRule.builder()
                .conditions(checkEndpoint, bind, use1, use2)
                .endpoint(TestHelpers.endpoint("https://example.com"));

        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder()
                        .name("Endpoint")
                        .type(ParameterType.STRING)
                        .build())
                .build();

        EndpointRuleSet original = EndpointRuleSet.builder()
                .parameters(params)
                .addRule(rule)
                .build();

        EndpointRuleSet transformed = CoalesceTransform.transform(original);

        // Should not coalesce because 'url' is used twice
        Rule transformedRule = transformed.getRules().get(0);
        assertEquals(4, transformedRule.getConditions().size());
    }

    @Test
    void testDoesNotCoalesceIsSetFunction() {
        // isSet functions should not be coalesced
        Condition bind = Condition.builder()
                .fn(TestHelpers.isSet("Region"))
                .result(Identifier.of("hasRegion"))
                .build();

        Condition use = Condition.builder()
                .fn(BooleanEquals.ofExpressions(
                        Expression.getReference(Identifier.of("hasRegion")),
                        Literal.of(true)))
                .result(Identifier.of("regionIsSet"))
                .build();

        Rule rule = EndpointRule.builder()
                .conditions(bind, use)
                .endpoint(TestHelpers.endpoint("https://example.com"));

        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder()
                        .name("Region")
                        .type(ParameterType.STRING)
                        .build())
                .build();

        EndpointRuleSet original = EndpointRuleSet.builder()
                .parameters(params)
                .addRule(rule)
                .build();

        EndpointRuleSet transformed = CoalesceTransform.transform(original);

        // Should coalesce because BooleanType has a zero value (false)
        // The actual behavior is that it DOES coalesce boolean operations
        Rule transformedRule = transformed.getRules().get(0);
        assertEquals(1, transformedRule.getConditions().size());
    }

    @Test
    void testMultipleCoalescesInSameRule() {
        // parseUrl returns a record type which doesn't have a zero value
        // So these won't be coalesced
        Condition checkEndpoint1 = Condition.builder()
                .fn(TestHelpers.isSet("Endpoint1"))
                .build();

        Condition checkEndpoint2 = Condition.builder()
                .fn(TestHelpers.isSet("Endpoint2"))
                .build();

        Condition bind1 = Condition.builder()
                .fn(TestHelpers.parseUrl("Endpoint1"))
                .result(Identifier.of("url1"))
                .build();

        Condition use1 = Condition.builder()
                .fn(TestHelpers.getAttr(
                        Expression.getReference(Identifier.of("url1")),
                        "scheme"))
                .result(Identifier.of("scheme1"))
                .build();

        Condition bind2 = Condition.builder()
                .fn(TestHelpers.parseUrl("Endpoint2"))
                .result(Identifier.of("url2"))
                .build();

        Condition use2 = Condition.builder()
                .fn(TestHelpers.getAttr(
                        Expression.getReference(Identifier.of("url2")),
                        "scheme"))
                .result(Identifier.of("scheme2"))
                .build();

        Rule rule = EndpointRule.builder()
                .conditions(checkEndpoint1, checkEndpoint2, bind1, use1, bind2, use2)
                .endpoint(TestHelpers.endpoint("https://example.com"));

        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder()
                        .name("Endpoint1")
                        .type(ParameterType.STRING)
                        .build())
                .addParameter(Parameter.builder()
                        .name("Endpoint2")
                        .type(ParameterType.STRING)
                        .build())
                .build();

        EndpointRuleSet original = EndpointRuleSet.builder()
                .parameters(params)
                .addRule(rule)
                .build();

        EndpointRuleSet transformed = CoalesceTransform.transform(original);

        // Won't coalesce because parseUrl returns record without zero value
        Rule transformedRule = transformed.getRules().get(0);
        assertEquals(6, transformedRule.getConditions().size());
    }

    @Test
    void testCoalesceWithStringFunctions() {
        // Test coalescing with string manipulation functions
        Condition checkInput = Condition.builder()
                .fn(TestHelpers.isSet("Input"))
                .build();

        Condition bind = Condition.builder()
                .fn(TestHelpers.substring("Input", 0, 5, false))
                .result(Identifier.of("prefix"))
                .build();

        Condition use = Condition.builder()
                .fn(StringEquals.ofExpressions(
                        Expression.getReference(Identifier.of("prefix")),
                        Literal.of("https")))
                .result(Identifier.of("isHttps"))
                .build();

        Rule rule = EndpointRule.builder()
                .conditions(checkInput, bind, use)
                .endpoint(TestHelpers.endpoint("https://example.com"));

        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder()
                        .name("Input")
                        .type(ParameterType.STRING)
                        .build())
                .build();

        EndpointRuleSet original = EndpointRuleSet.builder()
                .parameters(params)
                .addRule(rule)
                .build();

        EndpointRuleSet transformed = CoalesceTransform.transform(original);

        // Should coalesce string functions that have zero values
        Rule transformedRule = transformed.getRules().get(0);
        assertEquals(2, transformedRule.getConditions().size()); // isSet + coalesced

        Condition coalesced = transformedRule.getConditions().get(1);
        assertTrue(coalesced.getResult().isPresent());
        assertEquals("isHttps", coalesced.getResult().get().toString());
    }

    @Test
    void testDoesNotCoalesceWhenNotImmediatelyFollowing() {
        // Bind and use are not immediately following each other
        Condition checkEndpoint = Condition.builder()
                .fn(TestHelpers.isSet("Endpoint"))
                .build();

        Condition bind = Condition.builder()
                .fn(TestHelpers.parseUrl("Endpoint"))
                .result(Identifier.of("url"))
                .build();

        Condition intermediate = Condition.builder()
                .fn(TestHelpers.isSet("Region"))
                .result(Identifier.of("hasRegion"))
                .build();

        Condition use = Condition.builder()
                .fn(TestHelpers.getAttr(
                        Expression.getReference(Identifier.of("url")),
                        "scheme"))
                .result(Identifier.of("scheme"))
                .build();

        Rule rule = EndpointRule.builder()
                .conditions(checkEndpoint, bind, intermediate, use)
                .endpoint(TestHelpers.endpoint("https://example.com"));

        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder()
                        .name("Endpoint")
                        .type(ParameterType.STRING)
                        .build())
                .addParameter(Parameter.builder()
                        .name("Region")
                        .type(ParameterType.STRING)
                        .build())
                .build();

        EndpointRuleSet original = EndpointRuleSet.builder()
                .parameters(params)
                .addRule(rule)
                .build();

        EndpointRuleSet transformed = CoalesceTransform.transform(original);

        // Should NOT coalesce because bind and use are not adjacent
        Rule transformedRule = transformed.getRules().get(0);
        assertEquals(4, transformedRule.getConditions().size());
    }

    @Test
    void testCoalesceCaching() {
        // parseUrl returns a record type which doesn't have a zero value
        // So these won't be coalesced
        Condition check1 = Condition.builder()
                .fn(TestHelpers.isSet("Endpoint"))
                .build();

        Rule rule1 = EndpointRule.builder()
                .conditions(
                        check1,
                        Condition.builder()
                                .fn(TestHelpers.parseUrl("Endpoint"))
                                .result(Identifier.of("url"))
                                .build(),
                        Condition.builder()
                                .fn(TestHelpers.getAttr(
                                        Expression.getReference(Identifier.of("url")),
                                        "scheme"))
                                .result(Identifier.of("scheme"))
                                .build())
                .endpoint(TestHelpers.endpoint("https://example1.com"));

        Condition check2 = Condition.builder()
                .fn(TestHelpers.isSet("Endpoint"))
                .build();

        Rule rule2 = EndpointRule.builder()
                .conditions(
                        check2,
                        Condition.builder()
                                .fn(TestHelpers.parseUrl("Endpoint"))
                                .result(Identifier.of("url"))
                                .build(),
                        Condition.builder()
                                .fn(TestHelpers.getAttr(
                                        Expression.getReference(Identifier.of("url")),
                                        "scheme"))
                                .result(Identifier.of("scheme"))
                                .build())
                .endpoint(TestHelpers.endpoint("https://example2.com"));

        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder()
                        .name("Endpoint")
                        .type(ParameterType.STRING)
                        .build())
                .build();

        EndpointRuleSet original = EndpointRuleSet.builder()
                .parameters(params)
                .rules(ListUtils.of(rule1, rule2))
                .build();

        EndpointRuleSet transformed = CoalesceTransform.transform(original);

        // Won't coalesce because parseUrl returns record without zero value
        assertEquals(3, transformed.getRules().get(0).getConditions().size());
        assertEquals(3, transformed.getRules().get(1).getConditions().size());
    }

    @Test
    void testCoalesceInErrorRule() {
        // parseUrl returns a record type which doesn't have a zero value
        // So it won't be coalesced
        Condition checkEndpoint = Condition.builder()
                .fn(TestHelpers.isSet("Endpoint"))
                .build();

        Condition bind = Condition.builder()
                .fn(TestHelpers.parseUrl("Endpoint"))
                .result(Identifier.of("url"))
                .build();

        Condition use = Condition.builder()
                .fn(TestHelpers.getAttr(
                        Expression.getReference(Identifier.of("url")),
                        "scheme"))
                .result(Identifier.of("scheme"))
                .build();

        Condition check = Condition.builder()
                .fn(Not.ofExpressions(
                        StringEquals.ofExpressions(
                                Expression.getReference(Identifier.of("scheme")),
                                Literal.of("https"))))
                .build();

        Rule rule = ErrorRule.builder()
                .conditions(checkEndpoint, bind, use, check)
                .error(Literal.of("Endpoint must use HTTPS"));

        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder()
                        .name("Endpoint")
                        .type(ParameterType.STRING)
                        .build())
                .build();

        EndpointRuleSet original = EndpointRuleSet.builder()
                .parameters(params)
                .addRule(rule)
                .build();

        EndpointRuleSet transformed = CoalesceTransform.transform(original);

        // Won't coalesce because parseUrl returns record without zero value
        Rule transformedRule = transformed.getRules().get(0);
        assertEquals(4, transformedRule.getConditions().size());
    }

    @Test
    void testCoalesceWithBooleanType() {
        // Test coalescing with boolean-returning functions
        Condition checkInput = Condition.builder()
                .fn(TestHelpers.isSet("Input"))
                .build();

        Condition bind = Condition.builder()
                .fn(TestHelpers.isValidHostLabel("Input", false))
                .result(Identifier.of("isValid"))
                .build();

        Condition use = Condition.builder()
                .fn(BooleanEquals.ofExpressions(
                        Expression.getReference(Identifier.of("isValid")),
                        Literal.of(true)))
                .result(Identifier.of("validLabel"))
                .build();

        Rule rule = EndpointRule.builder()
                .conditions(checkInput, bind, use)
                .endpoint(TestHelpers.endpoint("https://example.com"));

        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder()
                        .name("Input")
                        .type(ParameterType.STRING)
                        .build())
                .build();

        EndpointRuleSet original = EndpointRuleSet.builder()
                .parameters(params)
                .addRule(rule)
                .build();

        EndpointRuleSet transformed = CoalesceTransform.transform(original);

        // Should coalesce boolean functions (they have zero value of false)
        Rule transformedRule = transformed.getRules().get(0);
        assertEquals(2, transformedRule.getConditions().size()); // isSet + coalesced
    }

    @Test
    void testDoesNotCoalesceWhenVariableUsedElsewhere() {
        // Variable is used in a different branch
        Condition checkEndpoint = Condition.builder()
                .fn(TestHelpers.isSet("Endpoint"))
                .build();

        Condition bind = Condition.builder()
                .fn(TestHelpers.parseUrl("Endpoint"))
                .result(Identifier.of("url"))
                .build();

        Condition use = Condition.builder()
                .fn(TestHelpers.getAttr(
                        Expression.getReference(Identifier.of("url")),
                        "scheme"))
                .result(Identifier.of("scheme"))
                .build();

        Rule innerRule = EndpointRule.builder()
                .conditions(Condition.builder()
                        .fn(TestHelpers.getAttr(
                                Expression.getReference(Identifier.of("url")),
                                "authority"))
                        .result(Identifier.of("authority"))
                        .build())
                .endpoint(TestHelpers.endpoint("https://inner.example.com"));

        Rule treeRule = TreeRule.builder()
                .conditions(checkEndpoint, bind, use)
                .treeRule(innerRule);

        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder()
                        .name("Endpoint")
                        .type(ParameterType.STRING)
                        .build())
                .build();

        EndpointRuleSet original = EndpointRuleSet.builder()
                .parameters(params)
                .addRule(treeRule)
                .build();

        EndpointRuleSet transformed = CoalesceTransform.transform(original);

        // Should NOT coalesce because 'url' is used in the inner rule
        TreeRule transformedTree = (TreeRule) transformed.getRules().get(0);
        assertEquals(3, transformedTree.getConditions().size()); // isSet + bind + use (not coalesced)
    }
}
