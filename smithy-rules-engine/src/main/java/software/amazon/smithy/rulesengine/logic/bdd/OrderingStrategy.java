/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import java.util.List;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.logic.cfg.Cfg;

/**
 * Strategy interface for ordering conditions in a BDD.
 */
@FunctionalInterface
interface OrderingStrategy {
    /**
     * Orders the given conditions for BDD construction.
     *
     * @param conditions array of conditions to order
     * @return ordered list of conditions
     */
    List<Condition> orderConditions(Condition[] conditions);

    /**
     * Creates an initial ordering strategy using the given CFG.
     *
     * @param cfg CFG to process.
     * @return the initial ordering strategy.
     */
    static OrderingStrategy initialOrdering(Cfg cfg) {
        return new InitialOrdering(cfg);
    }

    /**
     * Fixed ordering strategy that uses a pre-determined order.
     */
    static OrderingStrategy fixed(List<Condition> ordering) {
        return conditions -> ordering;
    }
}
