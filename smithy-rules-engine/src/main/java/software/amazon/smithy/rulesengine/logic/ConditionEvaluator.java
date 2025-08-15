/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic;

/**
 * Evaluates a single condition using a condition index.
 *
 * <p>This functional interface provides maximum flexibility for condition evaluation implementations. Implementations
 * are responsible maintaining their own internal state as methods are called (e.g., tracking variables).
 */
@FunctionalInterface
public interface ConditionEvaluator {
    /**
     * Evaluates the condition at the given index.
     *
     * @param conditionIndex the index of the condition to evaluate
     * @return true if the condition is satisfied, false otherwise
     */
    boolean test(int conditionIndex);
}
