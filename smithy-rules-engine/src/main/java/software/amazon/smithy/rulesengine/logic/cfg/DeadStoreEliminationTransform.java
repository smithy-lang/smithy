/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.cfg;

import java.util.logging.Logger;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;

/**
 * Removes bindings from conditions when the bound variable is never used.
 *
 * <p>As part of the optimization pipeline, we rewrite some expressions to create a
 * variable binding in case the expressions can be consolidated with
 * VariableConsolidationTransform. When they can't, it would leave a dangling binding
 * that isn't used, so this transform rewrites those conditions to not create
 * these dead stores.
 */
final class DeadStoreEliminationTransform extends TreeMapper {
    private static final Logger LOGGER = Logger.getLogger(DeadStoreEliminationTransform.class.getName());

    private int eliminated = 0;
    private final VariableAnalysis analysis;

    private DeadStoreEliminationTransform(VariableAnalysis analysis) {
        this.analysis = analysis;
    }

    static EndpointRuleSet transform(EndpointRuleSet ruleSet) {
        DeadStoreEliminationTransform t = new DeadStoreEliminationTransform(VariableAnalysis.analyze(ruleSet));
        EndpointRuleSet result = t.endpointRuleSet(ruleSet);
        if (t.eliminated > 0) {
            LOGGER.info(() -> "Dead store elimination: " + t.eliminated + " bindings removed");
        }
        return result;
    }

    @Override
    public Condition condition(Rule rule, Condition cond) {
        if (cond.getResult().isPresent()) {
            String varName = cond.getResult().get().toString();
            if (analysis.getReferenceCount(varName) == 0) {
                eliminated++;
                return Condition.builder().fn(cond.getFunction()).build();
            }
        }
        return cond;
    }
}
