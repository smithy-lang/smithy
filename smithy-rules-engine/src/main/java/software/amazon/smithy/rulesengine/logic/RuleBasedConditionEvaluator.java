/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic;

import software.amazon.smithy.rulesengine.language.evaluation.RuleEvaluator;
import software.amazon.smithy.rulesengine.language.evaluation.value.BooleanValue;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;

/**
 * Evaluates rules using a rules engine evaluator.
 */
public final class RuleBasedConditionEvaluator implements ConditionEvaluator {
    private final RuleEvaluator evaluator;
    private final Condition[] conditions;

    public RuleBasedConditionEvaluator(RuleEvaluator evaluator, Condition[] conditions) {
        this.evaluator = evaluator;
        this.conditions = conditions;
    }

    @Override
    public boolean test(int conditionIndex) {
        Condition condition = conditions[conditionIndex];
        Value value = evaluator.evaluateCondition(condition);
        return !value.isEmpty() && (!(value instanceof BooleanValue) || ((BooleanValue) value).getValue());
    }
}
