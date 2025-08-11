/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.cfg;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Reference;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Template;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.FunctionNode;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.LibraryFunction;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.RecordLiteral;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.StringLiteral;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.TupleLiteral;

/**
 * Utility for rewriting references within expression trees.
 */
final class ReferenceRewriter {

    private final Function<Reference, Expression> referenceTransformer;
    private final Predicate<Expression> shouldRewrite;

    /**
     * Creates a new reference rewriter.
     *
     * @param referenceTransformer function to transform references
     * @param shouldRewrite predicate to determine if an expression needs rewriting
     */
    ReferenceRewriter(
            Function<Reference, Expression> referenceTransformer,
            Predicate<Expression> shouldRewrite
    ) {
        this.referenceTransformer = referenceTransformer;
        this.shouldRewrite = shouldRewrite;
    }

    /**
     * Rewrites references within an expression tree.
     *
     * @param expression the expression to rewrite
     * @return the rewritten expression, or the original if no changes needed
     */
    public Expression rewrite(Expression expression) {
        if (!shouldRewrite.test(expression)) {
            return expression;
        }

        if (expression instanceof StringLiteral) {
            return rewriteStringLiteral((StringLiteral) expression);
        } else if (expression instanceof TupleLiteral) {
            return rewriteTupleLiteral((TupleLiteral) expression);
        } else if (expression instanceof RecordLiteral) {
            return rewriteRecordLiteral((RecordLiteral) expression);
        } else if (expression instanceof Reference) {
            return referenceTransformer.apply((Reference) expression);
        } else if (expression instanceof LibraryFunction) {
            return rewriteLibraryFunction((LibraryFunction) expression);
        }

        return expression;
    }

    private Expression rewriteStringLiteral(StringLiteral str) {
        Template template = str.value();
        if (template.isStatic()) {
            return str;
        }

        StringBuilder templateBuilder = new StringBuilder();
        boolean changed = false;

        for (Template.Part part : template.getParts()) {
            if (part instanceof Template.Dynamic) {
                Template.Dynamic dynamic = (Template.Dynamic) part;
                Expression original = dynamic.toExpression();
                Expression rewritten = rewrite(original);
                if (rewritten != original) {
                    changed = true;
                }
                templateBuilder.append('{').append(rewritten).append('}');
            } else {
                templateBuilder.append(((Template.Literal) part).getValue());
            }
        }

        return changed ? Literal.stringLiteral(Template.fromString(templateBuilder.toString())) : str;
    }

    private Expression rewriteTupleLiteral(TupleLiteral tuple) {
        List<Literal> rewrittenMembers = new ArrayList<>();
        boolean changed = false;

        for (Literal member : tuple.members()) {
            Literal rewritten = (Literal) rewrite(member);
            rewrittenMembers.add(rewritten);
            if (rewritten != member) {
                changed = true;
            }
        }

        return changed ? Literal.tupleLiteral(rewrittenMembers) : tuple;
    }

    private Expression rewriteRecordLiteral(RecordLiteral record) {
        Map<Identifier, Literal> rewrittenMembers = new LinkedHashMap<>();
        boolean changed = false;

        for (Map.Entry<Identifier, Literal> entry : record.members().entrySet()) {
            Literal original = entry.getValue();
            Literal rewritten = (Literal) rewrite(original);
            rewrittenMembers.put(entry.getKey(), rewritten);
            if (rewritten != original) {
                changed = true;
            }
        }

        return changed ? Literal.recordLiteral(rewrittenMembers) : record;
    }

    private Expression rewriteLibraryFunction(LibraryFunction fn) {
        List<Expression> rewrittenArgs = new ArrayList<>();
        boolean changed = false;

        for (Expression arg : fn.getArguments()) {
            Expression rewritten = rewrite(arg);
            rewrittenArgs.add(rewritten);
            if (rewritten != arg) {
                changed = true;
            }
        }

        if (!changed) {
            return fn;
        }

        FunctionNode node = FunctionNode.builder()
                .name(Node.from(fn.getName()))
                .arguments(rewrittenArgs)
                .build();
        return fn.getFunctionDefinition().createFunction(node);
    }

    /**
     * Creates a simple rewriter that replaces specific references.
     *
     * @param replacements map of variable names to replacement expressions
     * @return a reference rewriter that performs the replacements
     */
    public static ReferenceRewriter forReplacements(Map<String, Expression> replacements) {
        return new ReferenceRewriter(
                ref -> replacements.getOrDefault(ref.getName().toString(), ref),
                expr -> expr.getReferences().stream().anyMatch(replacements::containsKey));
    }
}
