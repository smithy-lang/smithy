/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import software.amazon.smithy.jmespath.ExpressionVisitor;
import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.jmespath.RuntimeType;
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
import software.amazon.smithy.jmespath.functions.FunctionArgument;
import software.amazon.smithy.jmespath.functions.FunctionDefinition;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Evaluator<T> implements ExpressionVisitor<T> {

    private final T current;
    private final Adaptor<T> adaptor;

    public Evaluator(T current, Adaptor<T> adaptor) {
        this.current = current;
        this.adaptor = adaptor;
    }

    public T visit(JmespathExpression expression) {
        if (current == null) {
            return null;
        }
        return expression.accept(this);
    }

    @Override
    public T visitComparator(ComparatorExpression comparatorExpression) {
        T left = visit(comparatorExpression.getLeft());
        T right = visit(comparatorExpression.getRight());
        switch (comparatorExpression.getComparator()) {
            case EQUAL:
                return adaptor.createBoolean(Objects.equals(left, right));
            case NOT_EQUAL:
                return adaptor.createBoolean(!Objects.equals(left, right));
            // NOTE: Ordering operators >, >=, <, <= are only valid for numbers. All invalid
            // comparisons return null.
            case LESS_THAN:
                if (adaptor.is(left, RuntimeType.NUMBER) && adaptor.is(right, RuntimeType.NUMBER)) {
                    return adaptor.createBoolean(adaptor.compare(left, right) < 0);
                } else {
                    return adaptor.createNull();
                }
            case LESS_THAN_EQUAL:
                if (adaptor.is(left, RuntimeType.NUMBER) && adaptor.is(right, RuntimeType.NUMBER)) {
                    return adaptor.createBoolean(adaptor.compare(left, right) <= 0);
                } else {
                    return adaptor.createNull();
                }
            case GREATER_THAN:
                if (adaptor.is(left, RuntimeType.NUMBER) && adaptor.is(right, RuntimeType.NUMBER)) {
                    return adaptor.createBoolean(adaptor.compare(left, right) > 0);
                } else {
                    return adaptor.createNull();
                }
            case GREATER_THAN_EQUAL:
                if (adaptor.is(left, RuntimeType.NUMBER) && adaptor.is(right, RuntimeType.NUMBER)) {
                    return adaptor.createBoolean(adaptor.compare(left, right) >= 0);
                } else {
                    return adaptor.createNull();
                }
            default:
                throw new IllegalArgumentException("Unsupported comparator: " + comparatorExpression.getComparator());
        }
    }

    @Override
    public T visitCurrentNode(CurrentExpression currentExpression) {
        return current;
    }

    @Override
    public T visitExpressionType(ExpressionTypeExpression expressionTypeExpression) {
        return expressionTypeExpression.getExpression().accept(this);
    }

    @Override
    public T visitFlatten(FlattenExpression flattenExpression) {
        T value = visit(flattenExpression.getExpression());

        // Only lists can be flattened.
        if (!adaptor.typeOf(value).equals(RuntimeType.ARRAY)) {
            return null;
        }
        Adaptor.ArrayBuilder<T> flattened = adaptor.arrayBuilder();
        for (T val : adaptor.toList(value)) {
            if (adaptor.typeOf(val).equals(RuntimeType.ARRAY)) {
                flattened.addAll(val);
                continue;
            }
            flattened.add(val);
        }
        return flattened.build();
    }

    @Override
    public T visitFunction(FunctionExpression functionExpression) {
        FunctionDefinition function = FunctionDefinition.from(functionExpression.getName());
        List<FunctionArgument<T>> arguments = new ArrayList<>();
        for (JmespathExpression expr : functionExpression.getArguments()) {
            if (expr instanceof ExpressionTypeExpression) {
                arguments.add(FunctionArgument.of(((ExpressionTypeExpression)expr).getExpression()));
            } else {
                arguments.add(FunctionArgument.of(visit(expr)));
            }
        }
        return function.apply(adaptor, arguments);
    }

    @Override
    public T visitField(FieldExpression fieldExpression) {
        return adaptor.getProperty(current, adaptor.createString(fieldExpression.getName()));
    }

    @Override
    public T visitIndex(IndexExpression indexExpression) {
        int index = indexExpression.getIndex();
        if (!adaptor.typeOf(current).equals(RuntimeType.ARRAY)) {
            return null;
        }
        List<T> list = adaptor.toList(current);
        // Negative indices indicate reverse indexing in JMESPath
        if (index < 0) {
            index = list.size() + index;
        }
        if (list.size() <= index || index < 0) {
            return null;
        }
        return list.get(index);
    }

    @Override
    public T visitLiteral(LiteralExpression literalExpression) {
        if (literalExpression.isNumberValue()) {
            // TODO: Remove this check by correcting behavior in smithy-jmespath to correctly
            //       handle int vs double
            Number value = literalExpression.expectNumberValue();
            if (value.doubleValue() == Math.floor(value.doubleValue())) {
                return adaptor.createNumber(value.longValue());
            }
        } else if (literalExpression.isArrayValue()) {
            Adaptor.ArrayBuilder<T> result = adaptor.arrayBuilder();
            for (Object item : literalExpression.expectArrayValue()) {
                result.add(visit(LiteralExpression.from(item)));
            }
            return result.build();
        } else if (literalExpression.isObjectValue()) {
            Adaptor.ObjectBuilder<T> result = adaptor.objectBuilder();
            for (Map.Entry<String, Object> entry : literalExpression.expectObjectValue().entrySet()) {
                T key = adaptor.createString(entry.getKey());
                T value = visit(LiteralExpression.from(entry.getValue()));
                result.put(key, value);
            }
            return result.build();
        } else if (literalExpression.isStringValue()) {
            return adaptor.createString(literalExpression.expectStringValue());
        } else if (literalExpression.isBooleanValue()) {
            return adaptor.createBoolean(literalExpression.expectBooleanValue());
        } else if (literalExpression.isNullValue()) {
            return adaptor.createNull();
        }
        throw new IllegalArgumentException(String.format("Unrecognized literal: %s", literalExpression));
    }

    @Override
    public T visitMultiSelectList(MultiSelectListExpression multiSelectListExpression) {
        Adaptor.ArrayBuilder<T> output = adaptor.arrayBuilder();
        for (JmespathExpression exp : multiSelectListExpression.getExpressions()) {
            output.add(visit(exp));
        }
        // TODO: original smithy-java has output.isEmpty() ? null : Document.of(output);
        // but that doesn't seem to match the spec
        return output.build();
    }

    @Override
    public T visitMultiSelectHash(MultiSelectHashExpression multiSelectHashExpression) {
        Adaptor.ObjectBuilder<T> output = adaptor.objectBuilder();
        for (Map.Entry<String, JmespathExpression> expEntry : multiSelectHashExpression.getExpressions().entrySet()) {
            output.put(adaptor.createString(expEntry.getKey()), visit(expEntry.getValue()));
        }
        // TODO: original smithy-java has output.isEmpty() ? null : Document.of(output);
        // but that doesn't seem to match the spec
        return output.build();
    }

    @Override
    public T visitAnd(AndExpression andExpression) {
        T left = visit(andExpression.getLeft());
        return adaptor.isTruthy(left) ? visit(andExpression.getRight()) : left;
    }

    @Override
    public T visitOr(OrExpression orExpression) {
        T left = visit(orExpression.getLeft());
        if (adaptor.isTruthy(left)) {
            return left;
        }
        return orExpression.getRight().accept(this);
    }

    @Override
    public T visitNot(NotExpression notExpression) {
        T output = visit(notExpression.getExpression());
        return adaptor.createBoolean(!adaptor.isTruthy(output));
    }

    @Override
    public T visitProjection(ProjectionExpression projectionExpression) {
        T resultList = visit(projectionExpression.getLeft());
        if (!adaptor.typeOf(resultList).equals(RuntimeType.ARRAY)) {
            return null;
        }
        Adaptor.ArrayBuilder<T> projectedResults = adaptor.arrayBuilder();
        for (T result : adaptor.toList(resultList)) {
            T projected = new Evaluator<T>(result, adaptor).visit(projectionExpression.getRight());
            if (!adaptor.typeOf(projected).equals(RuntimeType.NULL)) {
                projectedResults.add(projected);
            }
        }
        return projectedResults.build();
    }

    @Override
    public T visitFilterProjection(FilterProjectionExpression filterProjectionExpression) {
        T left = visit(filterProjectionExpression.getLeft());
        if (!adaptor.typeOf(left).equals(RuntimeType.ARRAY)) {
            return null;
        }
        Adaptor.ArrayBuilder<T> results = adaptor.arrayBuilder();
        for (T val : adaptor.toList(left)) {
            T output = new Evaluator<T>(val, adaptor).visit(filterProjectionExpression.getComparison());
            if (adaptor.isTruthy(output)) {
                T result = new Evaluator<T>(val, adaptor).visit(filterProjectionExpression.getRight());
                if (result != null) {
                    results.add(result);
                }
            }
        }
        return results.build();
    }

    @Override
    public T visitObjectProjection(ObjectProjectionExpression objectProjectionExpression) {
        T resultObject = visit(objectProjectionExpression.getLeft());
        if (!adaptor.typeOf(resultObject).equals(RuntimeType.OBJECT)) {
            return null;
        }
        Adaptor.ArrayBuilder<T> projectedResults = adaptor.arrayBuilder();
        for (T member : adaptor.getPropertyNames(resultObject)) {
            T memberValue = adaptor.getProperty(resultObject, member);
            if (!adaptor.typeOf(memberValue).equals(RuntimeType.NULL)) {
                T projectedResult = new Evaluator<T>(memberValue, adaptor).visit(objectProjectionExpression.getRight());
                if (projectedResult != null) {
                    projectedResults.add(projectedResult);
                }
            }
        }
        return projectedResults.build();
    }

    @Override
    public T visitSlice(SliceExpression sliceExpression) {
        Adaptor.ArrayBuilder<T> output = adaptor.arrayBuilder();
        List<T> currentList = adaptor.toList(current);
        int step = sliceExpression.getStep();
        int start = sliceExpression.getStart().orElseGet(() -> step > 0 ? 0 : currentList.size());
        if (start < 0) {
            start = currentList.size() + start;
        }
        int stop = sliceExpression.getStop().orElseGet(() -> step > 0 ? currentList.size() : 0);
        if (stop < 0) {
            stop = currentList.size() + stop;
        }

        if (start < stop) {
            for (int idx = start; idx < stop; idx += step) {
                output.add(currentList.get(idx));
            }
        } else {
            // List is iterating in reverse
            for (int idx = start; idx > stop; idx += step) {
                output.add(currentList.get(idx - 1));
            }
        }
        return output.build();
    }

    @Override
    public T visitSubexpression(Subexpression subexpression) {
        T left = visit(subexpression.getLeft());
        return new Evaluator<>(left, adaptor).visit(subexpression.getRight());
    }
}
