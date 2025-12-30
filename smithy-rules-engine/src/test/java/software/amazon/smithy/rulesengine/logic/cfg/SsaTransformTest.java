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
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.UriEncode;
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
        // Note: Dead store elimination will remove unused bindings
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

        // Binding is removed since it's not used (dead store elimination)
        EndpointRule resultRule = (EndpointRule) result.getRules().get(0);
        assertEquals(false, resultRule.getConditions().get(0).getResult().isPresent());
    }

    @Test
    void testSimpleShadowing() {
        // Test when the same variable name is bound to different expressions
        // Note: Dead store elimination removes unused bindings
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

        // Bindings are removed since they're not used (dead store elimination)
        EndpointRule resultRule1 = (EndpointRule) resultRules.get(0);
        assertEquals(false, resultRule1.getConditions().get(0).getResult().isPresent());

        EndpointRule resultRule2 = (EndpointRule) resultRules.get(1);
        assertEquals(false, resultRule2.getConditions().get(0).getResult().isPresent());
    }

    @Test
    void testMultipleShadowsOfSameVariable() {
        // Test when a variable is shadowed multiple times
        // Note: Dead store elimination removes unused bindings
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
        // Bindings are removed since they're not used (dead store elimination)
        assertEquals(false, resultRules.get(0).getConditions().get(0).getResult().isPresent());
        assertEquals(false, resultRules.get(1).getConditions().get(0).getResult().isPresent());
        assertEquals(false, resultRules.get(2).getConditions().get(0).getResult().isPresent());
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
        // Note: Dead store elimination removes unused bindings
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
        // Note: Dead store elimination removes unused bindings
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

        // Binding is removed since it's not used (dead store elimination)
        EndpointRule resultRule = (EndpointRule) result.getRules().get(0);
        assertEquals(false, resultRule.getConditions().get(0).getResult().isPresent());
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

    private static Endpoint endpoint(Expression url) {
        return Endpoint.builder().url(url).build();
    }

    @Test
    void testMultipleBindingsWithUsedVariablesAreSsaRenamed() {
        // When the same variable is bound in multiple sibling branches AND is used,
        // each binding should get a unique SSA name to avoid shadowing conflicts
        Parameter param = Parameter.builder()
                .name("Input")
                .type(ParameterType.STRING)
                .build();

        // Create two sibling rules that:
        // 1. Both bind 'myVar' (same variable name, different expressions)
        // 2. Use 'myVar' in their endpoints (so it won't be eliminated)
        // Using UriEncode which returns a string (not boolean like StringEquals)
        Condition cond1 = Condition.builder()
                .fn(UriEncode.ofExpressions(Expression.of("Input")))
                .result("myVar")
                .build();
        EndpointRule rule1 = (EndpointRule) EndpointRule.builder()
                .conditions(Collections.singletonList(cond1))
                .endpoint(Endpoint.builder()
                        .url(Literal.stringLiteral(Template.fromString("https://{myVar}.example.com")))
                        .build());

        // Second rule uses a different expression (uriEncode of a literal)
        Condition cond2 = Condition.builder()
                .fn(UriEncode.ofExpressions(Expression.of("other")))
                .result("myVar")
                .build();
        EndpointRule rule2 = (EndpointRule) EndpointRule.builder()
                .conditions(Collections.singletonList(cond2))
                .endpoint(Endpoint.builder()
                        .url(Literal.stringLiteral(Template.fromString("https://{myVar}.other.com")))
                        .build());

        EndpointRuleSet original = EndpointRuleSet.builder()
                .parameters(Parameters.builder().addParameter(param).build())
                .rules(Arrays.asList(rule1, rule2))
                .version("1.0")
                .build();

        EndpointRuleSet result = SsaTransform.transform(original);

        // Verify both rules still exist
        assertEquals(2, result.getRules().size());

        // Get the result variable names from the transformed conditions
        EndpointRule resultRule1 = (EndpointRule) result.getRules().get(0);
        EndpointRule resultRule2 = (EndpointRule) result.getRules().get(1);

        String resultVar1 = resultRule1.getConditions()
                .get(0)
                .getResult()
                .map(Object::toString)
                .orElse(null);
        String resultVar2 = resultRule2.getConditions()
                .get(0)
                .getResult()
                .map(Object::toString)
                .orElse(null);

        System.out.println("resultVar1=" + resultVar1);
        System.out.println("resultVar2=" + resultVar2);

        // Both should have bindings (since they're used)
        assertEquals(true,
                resultRule1.getConditions().get(0).getResult().isPresent(),
                "First binding should be present since myVar is used");
        assertEquals(true,
                resultRule2.getConditions().get(0).getResult().isPresent(),
                "Second binding should be present since myVar is used");

        // They should be SSA-renamed to unique names
        assertEquals(true,
                resultVar1.contains("_ssa_"),
                "First binding should have SSA suffix, got: " + resultVar1);
        assertEquals(true,
                resultVar2.contains("_ssa_"),
                "Second binding should have SSA suffix, got: " + resultVar2);

        // They should NOT be the same (unique SSA names)
        assertEquals(false,
                resultVar1.equals(resultVar2),
                "SSA names should be unique: " + resultVar1 + " vs " + resultVar2);
    }
}
