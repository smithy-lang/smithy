/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import software.amazon.smithy.jmespath.ast.AndExpression;
import software.amazon.smithy.jmespath.ast.ComparatorExpression;
import software.amazon.smithy.jmespath.ast.CurrentExpression;
import software.amazon.smithy.jmespath.ast.ExpressionTypeExpression;
import software.amazon.smithy.jmespath.ast.FieldExpression;
import software.amazon.smithy.jmespath.ast.FilterProjectionExpression;
import software.amazon.smithy.jmespath.ast.FlattenExpression;
import software.amazon.smithy.jmespath.ast.FunctionExpression;
import software.amazon.smithy.jmespath.ast.IndexExpression;
import software.amazon.smithy.jmespath.ast.LiteralExpression;
import software.amazon.smithy.jmespath.ast.MultiSelectHashExpression;
import software.amazon.smithy.jmespath.ast.MultiSelectListExpression;
import software.amazon.smithy.jmespath.ast.NotExpression;
import software.amazon.smithy.jmespath.ast.ObjectProjectionExpression;
import software.amazon.smithy.jmespath.ast.OrExpression;
import software.amazon.smithy.jmespath.ast.ProjectionExpression;
import software.amazon.smithy.jmespath.ast.SliceExpression;
import software.amazon.smithy.jmespath.ast.Subexpression;

public class SubstitutionVisitor implements ExpressionVisitor<JmespathExpression> {

    private final Function<JmespathExpression, JmespathExpression> substitution;

    public SubstitutionVisitor(Function<JmespathExpression, JmespathExpression> substitution) {
        this.substitution = substitution;
    }

    private JmespathExpression visit(JmespathExpression expression) {
        JmespathExpression result = substitution.apply(expression);
        return result != null ? result : expression.accept(this);
    }

    @Override
    public JmespathExpression visitComparator(ComparatorExpression expression) {
        return new ComparatorExpression(expression.getComparator(), visit(expression.getLeft()), visit(expression.getRight()), expression.getLine(), expression.getColumn());
    }

    @Override
    public JmespathExpression visitCurrentNode(CurrentExpression expression) {
        return expression;
    }

    @Override
    public JmespathExpression visitExpressionType(ExpressionTypeExpression expression) {
        return new ExpressionTypeExpression(visit(expression.getExpression()), expression.getLine(), expression.getColumn());
    }

    @Override
    public JmespathExpression visitFlatten(FlattenExpression expression) {
        return new FlattenExpression(visit(expression.getExpression()), expression.getLine(), expression.getColumn());
    }

    @Override
    public JmespathExpression visitFunction(FunctionExpression expression) {
        List<JmespathExpression> args = new ArrayList<>();
        for (JmespathExpression arg : expression.getArguments()) {
            args.add(visit(arg));
        }
        return new FunctionExpression(expression.getName(), args, expression.getLine(), expression.getColumn());
    }

    @Override
    public JmespathExpression visitField(FieldExpression expression) {
        return expression;
    }

    @Override
    public JmespathExpression visitIndex(IndexExpression expression) {
        return expression;
    }

    @Override
    public JmespathExpression visitLiteral(LiteralExpression expression) {
        return expression;
    }

    @Override
    public JmespathExpression visitMultiSelectList(MultiSelectListExpression expression) {
        List<JmespathExpression> exprs = new ArrayList<>();
        for (JmespathExpression expr : expression.getExpressions()) {
            exprs.add(visit(expr));
        }
        return new MultiSelectListExpression(exprs, expression.getLine(), expression.getColumn());
    }

    @Override
    public JmespathExpression visitMultiSelectHash(MultiSelectHashExpression expression) {
        Map<String, JmespathExpression> exprs = new LinkedHashMap<>();
        for (Map.Entry<String, JmespathExpression> entry : expression.getExpressions().entrySet()) {
            exprs.put(entry.getKey(), visit(entry.getValue()));
        }
        return new MultiSelectHashExpression(exprs, expression.getLine(), expression.getColumn());
    }

    @Override
    public JmespathExpression visitAnd(AndExpression expression) {
        return new AndExpression(visit(expression.getLeft()), visit(expression.getRight()), expression.getLine(), expression.getColumn());
    }

    @Override
    public JmespathExpression visitOr(OrExpression expression) {
        return new OrExpression(visit(expression.getLeft()), visit(expression.getRight()), expression.getLine(), expression.getColumn());
    }

    @Override
    public JmespathExpression visitNot(NotExpression expression) {
        return new NotExpression(visit(expression.getExpression()), expression.getLine(), expression.getColumn());
    }

    @Override
    public JmespathExpression visitProjection(ProjectionExpression expression) {
        return new ProjectionExpression(visit(expression.getLeft()), visit(expression.getRight()), expression.getLine(), expression.getColumn());
    }

    @Override
    public JmespathExpression visitFilterProjection(FilterProjectionExpression expression) {
        return new FilterProjectionExpression(visit(expression.getLeft()), visit(expression.getComparison()), visit(expression.getRight()), expression.getLine(), expression.getColumn());
    }

    @Override
    public JmespathExpression visitObjectProjection(ObjectProjectionExpression expression) {
        return new ObjectProjectionExpression(visit(expression.getLeft()), visit(expression.getRight()), expression.getLine(), expression.getColumn());
    }

    @Override
    public JmespathExpression visitSlice(SliceExpression expression) {
        return expression;
    }

    @Override
    public JmespathExpression visitSubexpression(Subexpression expression) {
        return new Subexpression(visit(expression.getLeft()), visit(expression.getRight()), expression.getLine(), expression.getColumn());
    }
}
