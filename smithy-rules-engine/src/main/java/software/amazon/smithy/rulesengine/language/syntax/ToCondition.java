/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.syntax;

import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Supplies functionality to be coercible into {@link Condition}s for
 * use in composing rule-sets in code.
 */
@SmithyInternalApi
public interface ToCondition {
    /**
     * Convert this into a condition builder for compositional use.
     *
     * @return the condition builder.
     */
    Condition.Builder toConditionBuilder();

    /**
     * Convert this into a condition.
     *
     * @return the condition.
     */
    default Condition toCondition() {
        return toConditionBuilder().build();
    }

    /**
     * Converts this function into a condition which stores the output in the named result.
     *
     * @param result the name of the result parameter.
     * @return the function as a condition.
     */
    default Condition toCondition(String result) {
        return toConditionBuilder().result(result).build();
    }
}
