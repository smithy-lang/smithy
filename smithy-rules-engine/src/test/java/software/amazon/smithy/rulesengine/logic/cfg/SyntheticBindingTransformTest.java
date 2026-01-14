/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.cfg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.IsSet;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.syntax.parameters.ParameterType;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.logic.TestHelpers;

class SyntheticBindingTransformTest {

    private static final Parameters PARAMS = Parameters.builder()
            .addParameter(Parameter.builder().name("Input").type(ParameterType.STRING).build())
            .build();

    @Test
    void unwrapsIsSetFunctionCall() {
        // isSet(substring(Input, 0, 5, false)) should become _synthetic_0 = substring(...)
        Condition isSetSubstring = Condition.builder()
                .fn(IsSet.ofExpressions(TestHelpers.substring("Input", 0, 5, false)))
                .build();

        Rule rule = EndpointRule.builder()
                .conditions(TestHelpers.isSet("Input"), isSetSubstring)
                .endpoint(TestHelpers.endpoint("https://example.com"));

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(PARAMS)
                .addRule(rule)
                .build();

        EndpointRuleSet result = SyntheticBindingTransform.transform(ruleSet);

        Condition transformed = result.getRules().get(0).getConditions().get(1);
        assertEquals("substring", transformed.getFunction().getName());
        assertTrue(transformed.getResult().isPresent());
        // Name includes the outer function (isSet) that was unwrapped
        assertTrue(transformed.getResult().get().toString().startsWith("_synthetic_isSet_"));
    }

    @Test
    void doesNotUnwrapIsSetReference() {
        // isSet(Input) should remain unchanged - it's checking a reference, not a function
        Condition isSetRef = Condition.builder()
                .fn(TestHelpers.isSet("Input"))
                .build();

        Rule rule = EndpointRule.builder()
                .conditions(isSetRef)
                .endpoint(TestHelpers.endpoint("https://example.com"));

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(PARAMS)
                .addRule(rule)
                .build();

        EndpointRuleSet result = SyntheticBindingTransform.transform(ruleSet);

        Condition transformed = result.getRules().get(0).getConditions().get(0);
        assertEquals("isSet", transformed.getFunction().getName());
        assertFalse(transformed.getResult().isPresent());
    }

    @Test
    void addsBindingToBareFunctionCall() {
        // substring(Input, 0, 5, false) should become _synthetic_0 = substring(...)
        Condition bareSubstring = Condition.builder()
                .fn(TestHelpers.substring("Input", 0, 5, false))
                .build();

        Rule rule = EndpointRule.builder()
                .conditions(TestHelpers.isSet("Input"), bareSubstring)
                .endpoint(TestHelpers.endpoint("https://example.com"));

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(PARAMS)
                .addRule(rule)
                .build();

        EndpointRuleSet result = SyntheticBindingTransform.transform(ruleSet);

        Condition transformed = result.getRules().get(0).getConditions().get(1);
        assertEquals("substring", transformed.getFunction().getName());
        assertTrue(transformed.getResult().isPresent());
        // Name includes the function name
        assertTrue(transformed.getResult().get().toString().startsWith("_synthetic_substring_"));
    }

    @Test
    void doesNotModifyExistingBinding() {
        // prefix = substring(Input, 0, 5, false) should remain unchanged
        Condition binding = Condition.builder()
                .fn(TestHelpers.substring("Input", 0, 5, false))
                .result(Identifier.of("prefix"))
                .build();

        Rule rule = EndpointRule.builder()
                .conditions(TestHelpers.isSet("Input"), binding)
                .endpoint(TestHelpers.endpoint("https://example.com"));

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(PARAMS)
                .addRule(rule)
                .build();

        EndpointRuleSet result = SyntheticBindingTransform.transform(ruleSet);

        Condition transformed = result.getRules().get(0).getConditions().get(1);
        assertEquals("substring", transformed.getFunction().getName());
        assertEquals("prefix", transformed.getResult().get().toString());
    }

    @Test
    void doesNotAddBindingToSimpleChecks() {
        // isSet, booleanEquals, stringEquals, not, isValidHostLabel should not get bindings
        Condition isSet = Condition.builder()
                .fn(TestHelpers.isSet("Input"))
                .build();

        Rule rule = EndpointRule.builder()
                .conditions(isSet)
                .endpoint(TestHelpers.endpoint("https://example.com"));

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(PARAMS)
                .addRule(rule)
                .build();

        EndpointRuleSet result = SyntheticBindingTransform.transform(ruleSet);

        Condition transformed = result.getRules().get(0).getConditions().get(0);
        assertEquals("isSet", transformed.getFunction().getName());
        assertFalse(transformed.getResult().isPresent());
    }
}
