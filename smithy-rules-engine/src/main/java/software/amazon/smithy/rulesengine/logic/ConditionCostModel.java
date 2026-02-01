/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic;

import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.FunctionDefinition;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.LibraryFunction;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;

/**
 * Estimates the relative runtime cost of evaluating rule engine conditions.
 *
 * <p>Costs are relative values where lower numbers indicate cheaper operations. The absolute
 * values don't matter - only their relative ordering affects optimization decisions.
 *
 * @see FunctionDefinition#getCost()
 */
public interface ConditionCostModel {
    /**
     * Creates a default cost model that derives costs from function definitions.
     *
     * <p>This model uses {@link LibraryFunction#getFunctionDefinition()}'s cost and applies
     * a multiplier of 2 for nested function arguments.
     *
     * @return a new default cost model
     */
    static ConditionCostModel createDefault() {
        return createDefault(2);
    }

    /**
     * Create a cost model that assigns equal cost (1) to all conditions.
     *
     * <p>Use this when cost-based ordering should be disabled, allowing other factors
     * (CFG structure, cone size, dependencies) to determine condition order exclusively.
     *
     * @return uniform cost model
     */
    static ConditionCostModel createUniform() {
        return condition -> 1;
    }

    /**
     * Creates a default cost model with a custom multiplier for nested functions.
     *
     * <p>The cost of a condition is computed as:
     * <pre>
     * cost = baseCost + sum(nestedFunctionCost * nestedMultiplier)
     * </pre>
     *
     * <p>For example, with a multiplier of 2, the condition {@code isSet(parseUrl(endpoint))}
     * would have cost: {@code 10 + (200 * 2) = 410}, reflecting that the expensive parseUrl
     * must be evaluated before the cheap isSet can run.
     *
     * @param nestedMultiplier multiplier applied to nested function costs (typically 2-3)
     * @return a new cost model with the specified multiplier
     */
    static ConditionCostModel createDefault(int nestedMultiplier) {
        return new ConditionCostModel() {
            @Override
            public int cost(Condition condition) {
                return computeCost(condition.getFunction());
            }

            private int computeCost(LibraryFunction fn) {
                int base = fn.getFunctionDefinition().getCost();
                int argCost = 0;
                for (Expression arg : fn.getArguments()) {
                    if (arg instanceof LibraryFunction) {
                        argCost += computeCost((LibraryFunction) arg) * nestedMultiplier;
                    }
                }
                return base + argCost;
            }
        };
    }

    /**
     * Estimates the relative cost of evaluating a condition.
     *
     * <p>Lower values indicate cheaper operations that should be evaluated earlier when BDD structure permits.
     *
     * @param condition the condition to evaluate
     * @return relative cost estimate (lower = cheaper)
     */
    int cost(Condition condition);
}
