/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.cfg;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Reference;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.BooleanEquals;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.Coalesce;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.IsSet;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.LibraryFunction;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;

/**
 * Coalesces consecutive isSet + booleanEquals patterns into a single coalesced check.
 *
 * <p>This transform identifies patterns where:
 * <ul>
 *   <li>{@code isSet(X)} is immediately followed by {@code booleanEquals(X, true)}</li>
 *   <li>{@code isSet(X)} is immediately followed by {@code booleanEquals(X, false)}</li>
 * </ul>
 *
 * <p>These patterns are replaced with:
 * <ul>
 *   <li>{@code booleanEquals(coalesce(X, false), true)} - equivalent to "X is set and true"</li>
 *   <li>{@code booleanEquals(coalesce(X, true), false)} - equivalent to "X is set and false"</li>
 * </ul>
 *
 * <p>This reduces the number of conditions in the BDD, improving both space and potentially node count.
 */
final class IsSetBooleanCoalesceTransform extends TreeMapper {
    private static final Logger LOGGER = Logger.getLogger(IsSetBooleanCoalesceTransform.class.getName());

    private int coalescedCount = 0;

    static EndpointRuleSet transform(EndpointRuleSet ruleSet) {
        IsSetBooleanCoalesceTransform t = new IsSetBooleanCoalesceTransform();
        List<Rule> transformedRules = new ArrayList<>();
        for (Rule rule : ruleSet.getRules()) {
            transformedRules.add(t.rule(rule));
        }

        if (t.coalescedCount > 0) {
            LOGGER.info(() -> String.format("IsSet+boolean coalesce: %d patterns collapsed", t.coalescedCount));
        }

        // Build the node representation manually to avoid type-checking, then deserialize
        // to get fresh Expression objects without cached types.
        ObjectNode.Builder builder = ruleSet.toNode().expectObjectNode().toBuilder();
        ArrayNode.Builder rulesBuilder = ArrayNode.builder();
        for (Rule rule : transformedRules) {
            rulesBuilder.withValue(rule.toNode());
        }
        builder.withMember("rules", rulesBuilder.build());

        return EndpointRuleSet.fromNode(builder.build());
    }

    /**
     * Overrides conditions processing to look at pairs of consecutive conditions.
     * This is necessary because we need to merge isSet(X) + booleanEquals(X, val) patterns.
     */
    @Override
    public List<Condition> conditions(Rule rule, List<Condition> conditions) {
        List<Condition> result = new ArrayList<>();
        int size = conditions.size();

        for (int i = 0; i < size; i++) {
            Condition current = conditions.get(i);
            // Check for isSet(X) pattern
            if (i + 1 < size && !current.getResult().isPresent() && current.getFunction() instanceof IsSet) {
                Expression isSetArg = current.getFunction().getArguments().get(0);
                if (isSetArg instanceof Reference) {
                    Condition next = conditions.get(i + 1);

                    // Check if next is booleanEquals(X, true/false)
                    Condition coalesced = tryCoalesce((Reference) isSetArg, next);
                    if (coalesced != null) {
                        result.add(coalesced);
                        coalescedCount++;
                        i++; // Skip the next condition since we ate it
                        continue;
                    }
                }
            }

            result.add(current);
        }

        return result;
    }

    private Condition tryCoalesce(Reference isSetRef, Condition next) {
        if (next.getResult().isPresent()) {
            return null; // Can't coalesce if next has a binding
        }

        LibraryFunction fn = next.getFunction();
        if (!(fn instanceof BooleanEquals)) {
            return null;
        }

        List<Expression> args = fn.getArguments();
        Expression arg1 = args.get(0);
        Expression arg2 = args.get(1);

        // Ensure the first argument is a reference to the isSet(X) condition
        if (!(arg1 instanceof Reference)) {
            return null;
        }
        Reference boolRef = (Reference) arg1;
        if (!boolRef.getName().equals(isSetRef.getName())) {
            return null;
        }

        // Ensure the second argument is a boolean literal
        if (!(arg2 instanceof Literal)) {
            return null;
        }
        Boolean literalValue = ((Literal) arg2).asBooleanLiteral().orElse(null);
        if (literalValue == null) {
            return null;
        }

        // Create a coalesced condition that sets the default to the opposite of the literal value:
        // isSet(X) && booleanEquals(X, true) -> booleanEquals(coalesce(X, false), true)
        // isSet(X) && booleanEquals(X, false) -> booleanEquals(coalesce(X, true), false)
        boolean defaultValue = !literalValue;

        // Create a fresh Reference to avoid sharing cached type information with other conditions
        Reference freshRef = Expression.getReference(isSetRef.getName());
        Expression coalesced = Coalesce.ofExpressions(freshRef, Expression.of(defaultValue));
        return Condition.builder().fn(BooleanEquals.ofExpressions(coalesced, literalValue)).build();
    }
}
