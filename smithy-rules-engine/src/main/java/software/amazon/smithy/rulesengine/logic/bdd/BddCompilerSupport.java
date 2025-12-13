/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import java.util.List;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.logic.cfg.Cfg;

/**
 * Used by {@link SiftingOptimization} and {@link CostOptimization} to ensure consistent BDD compilation
 * and condition ordering operations.
 */
final class BddCompilerSupport {

    private BddCompilerSupport() {}

    static BddCompilationResult compile(Cfg cfg, List<Condition> ordering, BddBuilder builder) {
        builder.reset();
        BddCompiler compiler = new BddCompiler(cfg, OrderingStrategy.fixed(ordering), builder);
        Bdd bdd = compiler.compile();
        return new BddCompilationResult(bdd, compiler.getIndexedResults());
    }

    /**
     * Returns the node count excluding the terminal node.
     *
     * @param bdd the BDD
     * @return node count minus 1
     */
    static int nodeCount(Bdd bdd) {
        return bdd.getNodeCount() - 1;
    }

    /**
     * Moves a condition from one position to another, shifting intermediate elements.
     *
     * @param order the condition array to modify in place
     * @param from the source index
     * @param to the destination index
     */
    static void move(Condition[] order, int from, int to) {
        if (from == to) {
            return;
        }

        Condition moving = order[from];
        if (from < to) {
            System.arraycopy(order, from + 1, order, from, to - from);
        } else {
            System.arraycopy(order, to, order, to + 1, from - to);
        }

        order[to] = moving;
    }

    /**
     * Result of compiling a BDD with a specific ordering.
     */
    static final class BddCompilationResult {
        final Bdd bdd;
        final List<Rule> results;

        BddCompilationResult(Bdd bdd, List<Rule> results) {
            this.bdd = bdd;
            this.results = results;
        }
    }
}
