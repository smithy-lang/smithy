/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.smithy.jmespath.ExpressionVisitor;
import software.amazon.smithy.jmespath.JmespathException;
import software.amazon.smithy.jmespath.JmespathExpression;
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
import software.amazon.smithy.utils.ListUtils;

/**
 * The JMESPath subset that resource lifecycle trait locators and {@code ...From} pointers
 * may use: field access, subexpressions, wildcard projections ({@code [*]}), flatten
 * ({@code []}), and the current node.
 *
 * <p>This is the subset {@link JmespathShapeWalker} can follow, so a path must be checked
 * here before it is walked.
 */
final class LifecycleTraitJmespath {

    private LifecycleTraitJmespath() {}

    /**
     * Parses a path, returning null when it is invalid or uses expressions outside the
     * supported subset.
     *
     * <p>For callers that skip such paths because {@link ResourceLifecycleTraitValidator}
     * already reports them.
     *
     * @param path The JMESPath expression to parse.
     * @return The parsed expression, or null.
     */
    static JmespathExpression parse(String path) {
        JmespathExpression parsed;
        try {
            parsed = JmespathExpression.parse(path);
        } catch (JmespathException e) {
            return null;
        }
        return unsupportedExpressions(parsed).isEmpty() ? parsed : null;
    }

    /**
     * Lists the expressions in a parsed path that fall outside the supported subset.
     *
     * @param expression The parsed expression.
     * @return A label for each unsupported expression, or an empty list.
     */
    static List<String> unsupportedExpressions(JmespathExpression expression) {
        return expression.accept(new UnsupportedJmesPathVisitor());
    }

    private static final class UnsupportedJmesPathVisitor implements ExpressionVisitor<List<String>> {

        @Override
        public List<String> visitComparator(ComparatorExpression expression) {
            return ListUtils.of("comparator");
        }

        @Override
        public List<String> visitCurrentNode(CurrentExpression expression) {
            return Collections.emptyList();
        }

        @Override
        public List<String> visitExpressionType(ExpressionTypeExpression expression) {
            return expression.getExpression().accept(this);
        }

        @Override
        public List<String> visitFlatten(FlattenExpression expression) {
            return expression.getExpression().accept(this);
        }

        @Override
        public List<String> visitFunction(FunctionExpression expression) {
            return ListUtils.of("`" + expression.getName() + "` function");
        }

        @Override
        public List<String> visitField(FieldExpression expression) {
            return Collections.emptyList();
        }

        @Override
        public List<String> visitIndex(IndexExpression expression) {
            return ListUtils.of("index");
        }

        @Override
        public List<String> visitLiteral(LiteralExpression expression) {
            return ListUtils.of("literal");
        }

        @Override
        public List<String> visitMultiSelectList(MultiSelectListExpression expression) {
            return ListUtils.of("multiselect list");
        }

        @Override
        public List<String> visitMultiSelectHash(MultiSelectHashExpression expression) {
            return ListUtils.of("multiselect hash");
        }

        @Override
        public List<String> visitAnd(AndExpression expression) {
            return ListUtils.of("and");
        }

        @Override
        public List<String> visitOr(OrExpression expression) {
            return ListUtils.of("or");
        }

        @Override
        public List<String> visitNot(NotExpression expression) {
            return ListUtils.of("not");
        }

        @Override
        public List<String> visitProjection(ProjectionExpression expression) {
            List<String> unsupported = new ArrayList<>();
            unsupported.addAll(expression.getLeft().accept(this));
            unsupported.addAll(expression.getRight().accept(this));
            return Collections.unmodifiableList(unsupported);
        }

        @Override
        public List<String> visitFilterProjection(FilterProjectionExpression expression) {
            return ListUtils.of("filter projection");
        }

        @Override
        public List<String> visitObjectProjection(ObjectProjectionExpression expression) {
            List<String> unsupported = new ArrayList<>();
            unsupported.addAll(expression.getLeft().accept(this));
            unsupported.addAll(expression.getRight().accept(this));
            return Collections.unmodifiableList(unsupported);
        }

        @Override
        public List<String> visitSlice(SliceExpression expression) {
            return ListUtils.of("slice");
        }

        @Override
        public List<String> visitSubexpression(Subexpression expression) {
            List<String> unsupported = new ArrayList<>();
            unsupported.addAll(expression.getLeft().accept(this));
            unsupported.addAll(expression.getRight().accept(this));
            return Collections.unmodifiableList(unsupported);
        }
    }
}
