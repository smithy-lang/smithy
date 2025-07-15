/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic;

import java.util.Collections;
import java.util.Set;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;

/**
 * Information about a condition.
 */
public interface ConditionInfo {
    /**
     * Create a new ConditionInfo from the given condition.
     *
     * @param condition Condition to compute.
     * @return the created ConditionInfo.
     */
    static ConditionInfo from(Condition condition) {
        return new ConditionInfoImpl(condition);
    }

    /**
     * Get the underlying condition.
     *
     * @return condition.
     */
    Condition getCondition();

    /**
     * Get the complexity of the condition.
     *
     * @return the complexity.
     */
    default int getComplexity() {
        return 1;
    }

    /**
     * Get the references used by the condition.
     *
     * @return the references.
     */
    default Set<String> getReferences() {
        return Collections.emptySet();
    }

    /**
     * Get the name of the variable this condition defines, if any, or null.
     *
     * @return the defined variable name or null.
     */
    default String getReturnVariable() {
        return getCondition().getResult().map(Identifier::toString).orElse(null);
    }
}
