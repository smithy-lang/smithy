/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath;

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

/**
 * Visits each type of AST node.
 *
 * @param <T> Value returned from the visitor.
 */
public interface ExpressionVisitor<T> {

    T visitComparator(ComparatorExpression expression);

    T visitCurrentNode(CurrentExpression expression);

    T visitExpressionType(ExpressionTypeExpression expression);

    T visitFlatten(FlattenExpression expression);

    T visitFunction(FunctionExpression expression);

    T visitField(FieldExpression expression);

    T visitIndex(IndexExpression expression);

    T visitLiteral(LiteralExpression expression);

    T visitMultiSelectList(MultiSelectListExpression expression);

    T visitMultiSelectHash(MultiSelectHashExpression expression);

    T visitAnd(AndExpression expression);

    T visitOr(OrExpression expression);

    T visitNot(NotExpression expression);

    T visitProjection(ProjectionExpression expression);

    T visitFilterProjection(FilterProjectionExpression expression);

    T visitObjectProjection(ObjectProjectionExpression expression);

    T visitSlice(SliceExpression expression);

    T visitSubexpression(Subexpression expression);
}
