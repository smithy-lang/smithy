/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import java.util.List;
import java.util.Map;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.logic.ConditionInfo;

/**
 * Strategy interface for ordering conditions in a BDD.
 */
@FunctionalInterface
interface ConditionOrderingStrategy {
    /**
     * Orders the given conditions for BDD construction.
     *
     * @param conditions array of conditions to order
     * @param conditionInfos metadata about each condition
     * @return ordered list of conditions
     */
    List<Condition> orderConditions(Condition[] conditions, Map<Condition, ConditionInfo> conditionInfos);

    /**
     * Default ordering strategy that uses the existing ConditionOrderer.
     *
     * @return return the default ordering strategy.
     */
    static ConditionOrderingStrategy defaultOrdering() {
        return DefaultOrderingStrategy::orderConditions;
    }

    /**
     * Fixed ordering strategy that uses a pre-determined order.
     *
     * @return a fixed ordering strategy.
     */
    static ConditionOrderingStrategy fixed(List<Condition> ordering) {
        return (conditions, infos) -> ordering;
    }
}
