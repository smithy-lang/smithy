/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.cfg;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.ErrorRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.language.syntax.rule.TreeRule;

/**
 * Assigns synthetic variable names to bare function calls (conditions without result bindings).
 *
 * <p>This enables VariableConsolidationTransform to consolidate bare function calls with
 * their corresponding bindings. For example:
 * <pre>
 *   parseURL(Endpoint)        // bare truthy check
 *   url = parseURL(Endpoint)  // binding
 * </pre>
 * Becomes:
 * <pre>
 *   _bare_0 = parseURL(Endpoint)  // synthetic binding
 *   url = parseURL(Endpoint)      // original binding
 * </pre>
 *
 * <p>VariableConsolidationTransform will then consolidate _bare_0 → url, making them
 * the same condition and eliminating the redundant function call.
 */
final class BareFunctionBindingTransform {
    private static final Logger LOGGER = Logger.getLogger(BareFunctionBindingTransform.class.getName());
    
    private int syntheticVarCounter = 0;
    private int transformedCount = 0;

    static EndpointRuleSet transform(EndpointRuleSet ruleSet) {
        BareFunctionBindingTransform t = new BareFunctionBindingTransform();
        List<Rule> transformed = new ArrayList<>();
        for (Rule rule : ruleSet.getRules()) {
            transformed.add(t.transformRule(rule));
        }
        if (t.transformedCount > 0) {
            LOGGER.info(String.format("Bare function binding: %d conditions transformed", t.transformedCount));
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
            if (!cond.getResult().isPresent() && !isSimpleCheck(cond)) {
                // Bare function call - assign a synthetic variable
                String syntheticVar = "_bare_" + (syntheticVarCounter++);
                result.add(cond.toBuilder().result(syntheticVar).build());
                transformedCount++;
            } else {
                result.add(cond);
            }
        }
        return result;
    }

    private boolean isSimpleCheck(Condition cond) {
        // Don't transform simple boolean checks like isSet, booleanEquals, stringEquals, etc.
        // These don't benefit from binding since they return boolean, not optional values.
        String fnName = cond.getFunction().getName();
        return fnName.equals("isSet")
                || fnName.equals("booleanEquals")
                || fnName.equals("stringEquals")
                || fnName.equals("not")
                || fnName.equals("isValidHostLabel");
    }
}
