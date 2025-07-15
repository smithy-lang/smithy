/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic;

import java.util.Set;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Template;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.GetAttr;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.LibraryFunction;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.StringLiteral;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;

/**
 * Default implementation of {@link ConditionInfo} that computes condition metadata.
 */
final class ConditionInfoImpl implements ConditionInfo {

    private final Condition condition;
    private final int complexity;
    private final Set<String> references;

    ConditionInfoImpl(Condition condition) {
        this.condition = condition;
        this.complexity = calculateComplexity(condition.getFunction());
        this.references = condition.getFunction().getReferences();
    }

    @Override
    public Condition getCondition() {
        return condition;
    }

    @Override
    public int getComplexity() {
        return complexity;
    }

    @Override
    public Set<String> getReferences() {
        return references;
    }

    @Override
    public String getReturnVariable() {
        return condition.getResult().map(Identifier::toString).orElse(null);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object == null || getClass() != object.getClass()) {
            return false;
        } else {
            return condition.equals(((ConditionInfoImpl) object).condition);
        }
    }

    @Override
    public int hashCode() {
        return condition.hashCode();
    }

    @Override
    public String toString() {
        return condition.toString();
    }

    private static int calculateComplexity(Expression e) {
        // Base complexity for this node
        int complexity = 1;

        if (e instanceof StringLiteral) {
            Template template = ((StringLiteral) e).value();
            if (!template.isStatic()) {
                if (template.getParts().size() > 1) {
                    // Single dynamic part is cheap, but multiple parts are expensive
                    complexity += 8;
                }
                for (Template.Part part : template.getParts()) {
                    // Add complexity from dynamic parts
                    if (part instanceof Template.Dynamic) {
                        Template.Dynamic dynamic = (Template.Dynamic) part;
                        complexity += calculateComplexity(dynamic.toExpression());
                    }
                }
            }
        } else if (e instanceof GetAttr) {
            complexity += calculateComplexity(((GetAttr) e).getTarget()) + 2;
        } else if (e instanceof LibraryFunction) {
            LibraryFunction l = (LibraryFunction) e;
            complexity += l.getFunctionDefinition().getCostHeuristic();
            for (Expression arg : l.getArguments()) {
                complexity += calculateComplexity(arg);
            }
        }

        return complexity;
    }
}
