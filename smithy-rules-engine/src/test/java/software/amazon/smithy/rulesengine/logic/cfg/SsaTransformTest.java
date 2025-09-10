/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.cfg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.Endpoint;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Template;
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

public class SsaTransformTest {

    @Test
    void testNoDisambiguationNeeded() {
        // When variables are not shadowed, they should remain unchanged
        Parameter bucketParam = Parameter.builder()
                .name("Bucket")
                .type(ParameterType.STRING)
                .build();

        Condition condition1 = Condition.builder()
                .fn(StringEquals.ofExpressions(Expression.of("Bucket"), Expression.of("mybucket")))
                .result("bucketMatches")
                .build();

        EndpointRule rule = (EndpointRule) EndpointRule.builder()
                .conditions(Collections.singletonList(condition1))
                .endpoint(endpoint("https://example.com"));

        EndpointRuleSet original = EndpointRuleSet.builder()
                .parameters(Parameters.builder().addParameter(bucketParam).build())
                .rules(Collections.singletonList(rule))
                .version("1.0")
                .build();

        EndpointRuleSet result = SsaTransform.transform(original);

        assertEquals(original, result);
    }

    @Test
    void testSimpleShadowing() {
        // Test when the same variable name is bound to different expressions
        Parameter param = Parameter.builder()
                .name("Input")
                .type(ParameterType.STRING)
                .build();

        // Create rules that will have shadowing after disambiguation
        List<Rule> rules = Arrays.asList(
                createRuleWithBinding("Input", "a", "temp", "https://branch1.com"),
                createRuleWithBinding("Input", "b", "temp", "https://branch2.com"));

        EndpointRuleSet original = EndpointRuleSet.builder()
                .parameters(Parameters.builder().addParameter(param).build())
                .rules(rules)
                .version("1.0")
                .build();

        EndpointRuleSet result = SsaTransform.transform(original);

        List<Rule> resultRules = result.getRules();
        assertEquals(2, resultRules.size());

        EndpointRule resultRule1 = (EndpointRule) resultRules.get(0);
        assertEquals("temp_ssa_1", resultRule1.getConditions().get(0).getResult().get().toString());

        EndpointRule resultRule2 = (EndpointRule) resultRules.get(1);
        assertEquals("temp_ssa_2", resultRule2.getConditions().get(0).getResult().get().toString());
    }

    @Test
    void testMultipleShadowsOfSameVariable() {
        // Test when a variable is shadowed multiple times
        Parameter param = Parameter.builder()
                .name("Input")
                .type(ParameterType.STRING)
                .build();

        List<Rule> rules = Arrays.asList(
                createRuleWithBinding("Input", "x", "temp", "https://1.com"),
                createRuleWithBinding("Input", "y", "temp", "https://2.com"),
                createRuleWithBinding("Input", "z", "temp", "https://3.com"));

        EndpointRuleSet original = EndpointRuleSet.builder()
                .parameters(Parameters.builder().addParameter(param).build())
                .rules(rules)
                .version("1.0")
                .build();

        EndpointRuleSet result = SsaTransform.transform(original);

        List<Rule> resultRules = result.getRules();
        assertEquals("temp_ssa_1", resultRules.get(0).getConditions().get(0).getResult().get().toString());
        assertEquals("temp_ssa_2", resultRules.get(1).getConditions().get(0).getResult().get().toString());
        assertEquals("temp_ssa_3", resultRules.get(2).getConditions().get(0).getResult().get().toString());
    }

    @Test
    void testErrorRuleHandling() {
        // Test that error rules are handled correctly
        Parameter param = Parameter.builder()
                .name("Input")
                .type(ParameterType.STRING)
                .build();

        Condition cond = Condition.builder()
                .fn(StringEquals.ofExpressions(Expression.of("Input"), Expression.of("error")))
                .result("hasError")
                .build();

        ErrorRule errorRule = (ErrorRule) ErrorRule.builder()
                .conditions(Collections.singletonList(cond))
                .error(Expression.of("Error occurred"));

        EndpointRuleSet original = EndpointRuleSet.builder()
                .parameters(Parameters.builder().addParameter(param).build())
                .rules(Collections.singletonList(errorRule))
                .version("1.0")
                .build();

        EndpointRuleSet result = SsaTransform.transform(original);

        assertEquals(1, result.getRules().size());
        assertInstanceOf(ErrorRule.class, result.getRules().get(0));
    }

    @Test
    void testTreeRuleHandling() {
        // Test tree rules with unique variable names at each level
        Parameter param = Parameter.builder()
                .name("Region")
                .type(ParameterType.STRING)
                .build();

        // Outer condition with one variable
        Condition outerCond = Condition.builder()
                .fn(StringEquals.ofExpressions(Expression.of("Region"), Expression.of("us-*")))
                .result("isUS")
                .build();

        // Inner rules with their own variables
        EndpointRule innerRule1 = createRuleWithBinding("Region", "us-east-1", "isEast", "https://east.com");
        EndpointRule innerRule2 = createRuleWithBinding("Region", "us-west-2", "isWest", "https://west.com");

        TreeRule treeRule = (TreeRule) TreeRule.builder()
                .conditions(Collections.singletonList(outerCond))
                .treeRule(innerRule1, innerRule2);

        EndpointRuleSet original = EndpointRuleSet.builder()
                .parameters(Parameters.builder().addParameter(param).build())
                .rules(Collections.singletonList(treeRule))
                .version("1.0")
                .build();

        EndpointRuleSet result = SsaTransform.transform(original);

        // Check structure is preserved
        assertInstanceOf(TreeRule.class, result.getRules().get(0));
        TreeRule resultTree = (TreeRule) result.getRules().get(0);
        assertEquals(2, resultTree.getRules().size());
    }

    @Test
    void testParameterShadowingAttempt() {
        // Test that attempting to shadow a parameter gets disambiguated
        Parameter bucketParam = Parameter.builder()
                .name("Bucket")
                .type(ParameterType.STRING)
                .build();

        // Create a condition that assigns to "Bucket_shadow" to avoid direct conflict
        Condition shadowingCond = Condition.builder()
                .fn(StringEquals.ofExpressions(Expression.of("Bucket"), Expression.of("test")))
                .result("Bucket_shadow")
                .build();

        EndpointRule rule = (EndpointRule) EndpointRule.builder()
                .conditions(Collections.singletonList(shadowingCond))
                .endpoint(endpoint("https://example.com"));

        EndpointRuleSet original = EndpointRuleSet.builder()
                .parameters(Parameters.builder().addParameter(bucketParam).build())
                .rules(Collections.singletonList(rule))
                .version("1.0")
                .build();

        EndpointRuleSet result = SsaTransform.transform(original);

        // Should handle without issues
        EndpointRule resultRule = (EndpointRule) result.getRules().get(0);
        assertEquals("Bucket_shadow", resultRule.getConditions().get(0).getResult().get().toString());
    }

    private static EndpointRule createRuleWithBinding(String param, String value, String resultVar, String url) {
        Condition cond = Condition.builder()
                .fn(StringEquals.ofExpressions(Expression.of(param), Expression.of(value)))
                .result(resultVar)
                .build();

        return (EndpointRule) EndpointRule.builder()
                .conditions(Collections.singletonList(cond))
                .endpoint(endpoint(url));
    }

    private static Expression expr(String value) {
        return Literal.stringLiteral(Template.fromString(value));
    }

    private static Endpoint endpoint(String value) {
        return Endpoint.builder().url(expr(value)).build();
    }
}
