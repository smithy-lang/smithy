/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Reference;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.IsSet;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.ParseUrl;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;

public class ConditionCostModelTest {

    @Test
    public void testUniformCostModel() {
        ConditionCostModel uniform = ConditionCostModel.createUniform();
        Condition complex = Condition.builder().fn(ParseUrl.ofExpressions(Reference.of("url"))).build();

        assertEquals(1, uniform.cost(complex));
    }

    @Test
    public void testDefaultCostModel() {
        ConditionCostModel model = ConditionCostModel.createDefault();

        // Simple conditions should be cheaper
        Condition isSet = Condition.builder().fn(IsSet.ofExpressions(Reference.of("param"))).build();
        Condition parseUrl = Condition.builder().fn(ParseUrl.ofExpressions(Reference.of("url"))).build();

        int isSetCost = model.cost(isSet);
        int parseUrlCost = model.cost(parseUrl);

        assertTrue(isSetCost < parseUrlCost, "isSet should be cheaper than parseUrl");
    }

    @Test
    public void testNestedFunctionCosts() {
        ConditionCostModel model = ConditionCostModel.createDefault(2);

        // Simple isSet
        Condition simpleIsSet = Condition.builder()
                .fn(IsSet.ofExpressions(Reference.of("param")))
                .build();

        // Nested: isSet(parseUrl(endpoint))
        Condition nestedIsSet = Condition.builder()
                .fn(IsSet.ofExpressions(ParseUrl.ofExpressions(Reference.of("endpoint"))))
                .build();

        int simpleCost = model.cost(simpleIsSet);
        int nestedCost = model.cost(nestedIsSet);

        assertTrue(nestedCost > simpleCost, "Nested function should be more expensive");
        assertTrue(nestedCost > simpleCost * 5, "Nested cost should be significantly higher");
    }
}
