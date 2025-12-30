/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.cfg;

import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.IsSet;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.LibraryFunction;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;

import java.util.logging.Logger;

/**
 * Assigns synthetic bindings to conditions that could benefit from variable consolidation.
 *
 * <p>This transform handles two cases:
 * <ol>
 *   <li>{@code isSet(f(x))} is rewritten to {@code _synthetic_N = f(x)}, unwrapping the isSet</li>
 *   <li>Bare function calls like {@code f(x)} become {@code _synthetic_N = f(x)}</li>
 * </ol>
 *
 * <p>This enables {@link VariableConsolidationTransform} to consolidate these synthetic bindings
 * with real bindings like {@code url = f(x)}, eliminating redundant function calls. If no consolidation later
 * occurs, then {@link DeadStoreEliminationTransform} can remove the unnecessary synthetic bindings.
 */
final class SyntheticBindingTransform extends TreeMapper {
    private static final Logger LOGGER = Logger.getLogger(SyntheticBindingTransform.class.getName());

    private int syntheticCounter = 0;
    private int transformedCount = 0;

    static EndpointRuleSet transform(EndpointRuleSet ruleSet) {
        SyntheticBindingTransform t = new SyntheticBindingTransform();
        EndpointRuleSet result = t.endpointRuleSet(ruleSet);
        if (t.transformedCount > 0) {
            LOGGER.info(() -> String.format("Synthetic binding: %d conditions transformed", t.transformedCount));
        }
        return result;
    }

    @Override
    public Condition condition(Rule rule, Condition cond) {
        // If it already has a binding, then nothing to do
        if (cond.getResult().isPresent()) {
            return cond;
        }

        LibraryFunction fn = cond.getFunction();

        // isSet(f(x)) where f(x) is a function call - unwrap and bind
        if (fn instanceof IsSet) {
            Expression inner = fn.getArguments().get(0);
            if (inner instanceof LibraryFunction) {
                transformedCount++;
                return cond.toBuilder().fn((LibraryFunction) inner).result(createSyntheticName(fn)).build();
            }
            return cond;
        }

        // Bare function call that doesn't return a boolean? add binding
        if (fn.getFunctionDefinition().getReturnType() != Type.booleanType()) {
            transformedCount++;
            return cond.toBuilder().result(createSyntheticName(fn)).build();
        }

        return cond;
    }

    private String createSyntheticName(LibraryFunction fn) {
        return "_synthetic_" + fn.getName() + "_" + (syntheticCounter++);
    }
}
