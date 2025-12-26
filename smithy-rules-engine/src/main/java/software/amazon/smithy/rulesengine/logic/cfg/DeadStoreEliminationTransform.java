/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.cfg;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.ErrorRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.language.syntax.rule.TreeRule;

/**
 * Removes bindings from conditions when the bound variable is never used.
 *
 * <p>As part of the optimization pipeline, we rewrite some expressions to create a
 * variable binding in case the expressions can be consolidated with
 * VariableConsolidationTransform. When they can't, it would leave a dangling binding
 * that isn't used, so this transform rewrites those conditions to not create
 * these dead stores.
 */
final class DeadStoreEliminationTransform implements Function<EndpointRuleSet, EndpointRuleSet> {
    private static final Logger LOGGER = Logger.getLogger(DeadStoreEliminationTransform.class.getName());

    private int eliminated = 0;
    private final VariableAnalysis analysis;
    private final List<Rule> transformed = new ArrayList<>();

    private DeadStoreEliminationTransform(VariableAnalysis analysis) {
        this.analysis = analysis;
    }

    static EndpointRuleSet transform(EndpointRuleSet ruleSet) {
        DeadStoreEliminationTransform xf = new DeadStoreEliminationTransform(VariableAnalysis.analyze(ruleSet));
        return xf.apply(ruleSet);
    }

    @Override
    public EndpointRuleSet apply(EndpointRuleSet ruleSet) {
        for (Rule rule : ruleSet.getRules()) {
            transformed.add(eliminateDeadStores(rule));
        }

        if (eliminated == 0) {
            return ruleSet;
        } else {
            LOGGER.info("Dead store elimination: " + eliminated + " bindings removed");
            return ruleSet.toBuilder().rules(transformed).build();
        }
    }

    private Rule eliminateDeadStores(Rule rule) {
        List<Condition> newConditions = new ArrayList<>();
        for (Condition cond : rule.getConditions()) {
            if (cond.getResult().isPresent()) {
                String varName = cond.getResult().get().toString();
                if (analysis.getReferenceCount(varName) == 0) {
                    newConditions.add(Condition.builder().fn(cond.getFunction()).build());
                    eliminated++;
                    continue;
                }
            }
            newConditions.add(cond);
        }

        if (rule instanceof TreeRule) {
            List<Rule> newRules = new ArrayList<>();
            for (Rule child : ((TreeRule) rule).getRules()) {
                newRules.add(eliminateDeadStores(child));
            }
            return Rule.builder().conditions(newConditions).treeRule(newRules);
        } else if (rule instanceof EndpointRule) {
            return Rule.builder().conditions(newConditions).endpoint(((EndpointRule) rule).getEndpoint());
        } else if (rule instanceof ErrorRule) {
            return Rule.builder().conditions(newConditions).error(((ErrorRule) rule).getError());
        } else {
            return rule;
        }
    }
}
