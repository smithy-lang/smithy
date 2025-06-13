/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.syntax.bdd;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Reference;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Template;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.BooleanEquals;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.LibraryFunction;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.StringEquals;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.StringLiteral;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;

public final class RulesBddCondition {

    // Heuristic based complexity score for each method.
    private static final int UNKNOWN_FUNCTION_COMPLEXITY = 4;

    private static final Map<String, Integer> COMPLEXITY_ASSIGNMENTS = new HashMap<>();
    static {
        COMPLEXITY_ASSIGNMENTS.put("isSet", 1);
        COMPLEXITY_ASSIGNMENTS.put("not", 2);
        COMPLEXITY_ASSIGNMENTS.put("booleanEquals", 3);
        COMPLEXITY_ASSIGNMENTS.put("stringEquals", 4);
        COMPLEXITY_ASSIGNMENTS.put("substring", 5);
        COMPLEXITY_ASSIGNMENTS.put("aws.partition", 6);
        COMPLEXITY_ASSIGNMENTS.put("getAttr", 7);
        COMPLEXITY_ASSIGNMENTS.put("uriEncode", 8);
        COMPLEXITY_ASSIGNMENTS.put("aws.parseArn", 9);
        COMPLEXITY_ASSIGNMENTS.put("isValidHostLabel", 10);
        COMPLEXITY_ASSIGNMENTS.put("parseURL", 11);
    }

    private final Condition condition;
    private boolean isStateful;
    private int complexity = 0;
    private int hash = 0;

    private RulesBddCondition(Condition condition, EndpointRuleSet ruleSet) {
        this.condition = condition;
        // Conditions that assign a value are always considered stateful.
        isStateful = condition.getResult().isPresent();
        crawlCondition(ruleSet, 0, condition.getFunction());
    }

    public static RulesBddCondition from(Condition condition, EndpointRuleSet ruleSet) {
        return new RulesBddCondition(canonicalizeCondition(condition), ruleSet);
    }

    // Canonicalize conditions such that variable references for booleanEquals and stringEquals come before
    // a literal. This ensures that these commutative functions count as a single variable and don't needlessly
    // bloat the BDD table.
    private static Condition canonicalizeCondition(Condition condition) {
        Expression func = condition.getFunction();
        if (func instanceof BooleanEquals) {
            BooleanEquals f = (BooleanEquals) func;
            if (f.getArguments().get(0) instanceof Literal && !(f.getArguments().get(1) instanceof Literal)) {
                // Flip the order to move the literal last.
                return condition.toBuilder().fn(BooleanEquals.ofExpressions(
                        f.getArguments().get(1),
                        f.getArguments().get(0)
                )).build();
            }
        } else if (func instanceof StringEquals) {
            StringEquals f = (StringEquals) func;
            if (f.getArguments().get(0) instanceof Literal && !(f.getArguments().get(1) instanceof Literal)) {
                // Flip the order to move the literal last.
                return condition.toBuilder().fn(StringEquals.ofExpressions(
                        f.getArguments().get(1),
                        f.getArguments().get(0)
                )).build();
            }
        }

        return condition;
    }

    public Condition getCondition() {
        return condition;
    }

    public int getComplexity() {
        return complexity;
    }

    public boolean isStateful() {
        return isStateful;
    }

    private void crawlCondition(EndpointRuleSet ruleSet, int depth, Expression e) {
        // Every level of nesting is an automatic complexity++.
        complexity++;
        if (e instanceof Literal) {
            walkLiteral(ruleSet, (Literal) e, depth);
        } else if (e instanceof Reference) {
            walkReference(ruleSet, (Reference) e);
        } else if (e instanceof LibraryFunction) {
            walkLibraryFunction(ruleSet, (LibraryFunction) e, depth);
        }
    }

    private void walkLiteral(EndpointRuleSet ruleSet, Literal l, int depth) {
        if (l instanceof StringLiteral) {
            StringLiteral s = (StringLiteral) l;
            Template template = s.value();
            if (!template.isStatic()) {
                complexity += 8;
                for (Template.Part part : template.getParts()) {
                    if (part instanceof Template.Dynamic) {
                        // Need to check for dynamic variables that reference non-global params.
                        // Also add to the score for each parameter.
                        Template.Dynamic dynamic = (Template.Dynamic) part;
                        crawlCondition(ruleSet, depth + 1, dynamic.toExpression());
                    }
                }
            }
        }
    }

    private void walkReference(EndpointRuleSet ruleSet, Reference r) {
        // It's stateful if the name referenced here is not an input parameter name.
        if (!ruleSet.getParameters().get(r.getName()).isPresent()) {
            isStateful = true;
        }
    }

    private void walkLibraryFunction(EndpointRuleSet ruleSet, LibraryFunction l, int depth) {
        // Track function complexity.
        Integer functionComplexity = COMPLEXITY_ASSIGNMENTS.get(l.getName());
        complexity += functionComplexity != null ? functionComplexity : UNKNOWN_FUNCTION_COMPLEXITY;
        // Crawl the arguments.
        for (Expression arg : l.getArguments()) {
            crawlCondition(ruleSet, depth + 1, arg);
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object == null || getClass() != object.getClass()) {
            return false;
        } else {
            RulesBddCondition that = (RulesBddCondition) object;
            return isStateful == that.isStateful
                   && complexity == that.complexity
                   && Objects.equals(condition, that.condition);
        }
    }

    @Override
    public int hashCode() {
        int result = hash;
        if (hash == 0) {
            result = Objects.hash(condition, isStateful, complexity);
            hash = result;
        }
        return result;
    }
}
