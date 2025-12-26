/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.cfg;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.IsSet;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.LibraryFunction;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.ErrorRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.language.syntax.rule.TreeRule;

/**
 * Rewrites isSet(f(x)) to _synthetic = f(x) for function calls.
 *
 * <p>This enables VariableConsolidationTransform to consolidate with real bindings
 * like url = f(x), eliminating redundant function calls.
 */
final class UnwrapIsSetTransform {
    private static final Logger LOGGER = Logger.getLogger(UnwrapIsSetTransform.class.getName());

    private int syntheticCounter = 0;
    private int unwrappedCount = 0;

    static EndpointRuleSet transform(EndpointRuleSet ruleSet) {
        UnwrapIsSetTransform t = new UnwrapIsSetTransform();
        List<Rule> transformed = new ArrayList<>();
        for (Rule rule : ruleSet.getRules()) {
            transformed.add(t.transformRule(rule));
        }
        if (t.unwrappedCount > 0) {
            LOGGER.info(String.format("Unwrap isSet: %d conditions rewritten", t.unwrappedCount));
        }
        return ruleSet.toBuilder().rules(transformed).build();
    }
    
    private Rule transformRule(Rule rule) {
        List<Condition> newConditions = transformConditions(rule.getConditions());
        
        if (rule instanceof TreeRule) {
            TreeRule tr = (TreeRule) rule;
            List<Rule> newRules = new ArrayList<>();
            for (Rule child : tr.getRules()) {
                newRules.add(transformRule(child));
            }
            return Rule.builder().conditions(newConditions).treeRule(newRules);
        } else if (rule instanceof EndpointRule) {
            return Rule.builder().conditions(newConditions).endpoint(((EndpointRule) rule).getEndpoint());
        } else if (rule instanceof ErrorRule) {
            return Rule.builder().conditions(newConditions).error(((ErrorRule) rule).getError());
        }
        return rule;
    }

    private List<Condition> transformConditions(List<Condition> conditions) {
        List<Condition> result = new ArrayList<>(conditions.size());
        for (Condition cond : conditions) {
            Condition rewritten = rewriteIsSet(cond);
            if (rewritten != cond) {
                unwrappedCount++;
            }
            result.add(rewritten);
        }
        return result;
    }

    private Condition rewriteIsSet(Condition condition) {
        // Only rewrite isSet(f(x)) where f(x) is a function call, not a variable reference
        if (!(condition.getFunction() instanceof IsSet)) {
            return condition;
        } else if (condition.getResult().isPresent()) {
            return condition;
        }

        Expression inner = condition.getFunction().getArguments().get(0);
        if (inner instanceof LibraryFunction) {
            LibraryFunction innerFn = (LibraryFunction) inner;
            String syntheticVar = "_isSet_" + (syntheticCounter++);
            unwrappedCount++;
            return condition.toBuilder()
                    .fn(innerFn)
                    .result(syntheticVar)
                    .build();
        }

        return condition;
    }
}
