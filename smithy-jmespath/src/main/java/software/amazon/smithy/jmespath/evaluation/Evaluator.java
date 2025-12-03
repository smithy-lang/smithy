/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import software.amazon.smithy.jmespath.ExpressionVisitor;
import software.amazon.smithy.jmespath.JmespathException;
import software.amazon.smithy.jmespath.JmespathExceptionType;
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
import software.amazon.smithy.jmespath.functions.Function;
import software.amazon.smithy.jmespath.functions.FunctionRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

public class Evaluator<T> implements ExpressionVisitor<T> {

    private final JmespathRuntime<T> runtime;

    // TODO: Try making this state mutable instead of creating lots of sub-Evaluators
    private final T current;

    public Evaluator(T current, JmespathRuntime<T> runtime) {
        this.current = current;
        this.runtime = runtime;
    }

    public T visit(JmespathExpression expression) {
        return expression.accept(this);
    }

    @Override
    public T visitComparator(ComparatorExpression comparatorExpression) {
        T left = visit(comparatorExpression.getLeft());
        T right = visit(comparatorExpression.getRight());
        switch (comparatorExpression.getComparator()) {
            case EQUAL:
                return runtime.createBoolean(runtime.equal(left, right));
            case NOT_EQUAL:
                return runtime.createBoolean(!runtime.equal(left, right));
            // NOTE: Ordering operators >, >=, <, <= are only valid for numbers. All invalid
            // comparisons return null.
            case LESS_THAN:
                if (runtime.is(left, RuntimeType.NUMBER) && runtime.is(right, RuntimeType.NUMBER)) {
                    return runtime.createBoolean(runtime.compare(left, right) < 0);
                } else {
                    return runtime.createNull();
                }
            case LESS_THAN_EQUAL:
                if (runtime.is(left, RuntimeType.NUMBER) && runtime.is(right, RuntimeType.NUMBER)) {
                    return runtime.createBoolean(runtime.compare(left, right) <= 0);
                } else {
                    return runtime.createNull();
                }
            case GREATER_THAN:
                if (runtime.is(left, RuntimeType.NUMBER) && runtime.is(right, RuntimeType.NUMBER)) {
                    return runtime.createBoolean(runtime.compare(left, right) > 0);
                } else {
                    return runtime.createNull();
                }
            case GREATER_THAN_EQUAL:
                if (runtime.is(left, RuntimeType.NUMBER) && runtime.is(right, RuntimeType.NUMBER)) {
                    return runtime.createBoolean(runtime.compare(left, right) >= 0);
                } else {
                    return runtime.createNull();
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
        if (!runtime.is(value, RuntimeType.ARRAY)) {
            return runtime.createNull();
        }
        JmespathRuntime.ArrayBuilder<T> flattened = runtime.arrayBuilder();
        for (T val : runtime.toIterable(value)) {
            if (runtime.is(val, RuntimeType.ARRAY)) {
                flattened.addAll(val);
                continue;
            }
            flattened.add(val);
        }
        return flattened.build();
    }

    @Override
    public T visitFunction(FunctionExpression functionExpression) {
        Function function = FunctionRegistry.lookup(functionExpression.getName());
        if (function == null) {
            throw new JmespathException(JmespathExceptionType.UNKNOWN_FUNCTION, functionExpression.getName());
        }
        List<FunctionArgument<T>> arguments = new ArrayList<>();
        for (JmespathExpression expr : functionExpression.getArguments()) {
            if (expr instanceof ExpressionTypeExpression) {
                arguments.add(FunctionArgument.of(runtime, ((ExpressionTypeExpression)expr).getExpression()));
            } else {
                arguments.add(FunctionArgument.of(runtime, visit(expr)));
            }
        }
        return function.apply(runtime, arguments);
    }

    @Override
    public T visitField(FieldExpression fieldExpression) {
        return runtime.value(current, runtime.createString(fieldExpression.getName()));
    }

    @Override
    public T visitIndex(IndexExpression indexExpression) {
        int index = indexExpression.getIndex();
        if (!runtime.is(current, RuntimeType.ARRAY)) {
            return runtime.createNull();
        }
        // TODO: Capping at int here unnecessarily
        // Perhaps define intLength() and return -1 if it doesn't fit?
        // Although technically IndexExpression should be using a Number instead of an int in the first place
        int length = runtime.length(current).intValue();
        // Negative indices indicate reverse indexing in JMESPath
        if (index < 0) {
            index = length + index;
        }
        if (length <= index || index < 0) {
            return runtime.createNull();
        }
        return runtime.element(current, runtime.createNumber(index));
    }

    @Override
    public T visitLiteral(LiteralExpression literalExpression) {
        if (literalExpression.isNumberValue()) {
            return runtime.createNumber(literalExpression.expectNumberValue());
        } else if (literalExpression.isArrayValue()) {
            JmespathRuntime.ArrayBuilder<T> result = runtime.arrayBuilder();
            for (Object item : literalExpression.expectArrayValue()) {
                result.add(visit(LiteralExpression.from(item)));
            }
            return result.build();
        } else if (literalExpression.isObjectValue()) {
            JmespathRuntime.ObjectBuilder<T> result = runtime.objectBuilder();
            for (Map.Entry<String, Object> entry : literalExpression.expectObjectValue().entrySet()) {
                T key = runtime.createString(entry.getKey());
                T value = visit(LiteralExpression.from(entry.getValue()));
                result.put(key, value);
            }
            return result.build();
        } else if (literalExpression.isStringValue()) {
            return runtime.createString(literalExpression.expectStringValue());
        } else if (literalExpression.isBooleanValue()) {
            return runtime.createBoolean(literalExpression.expectBooleanValue());
        } else if (literalExpression.isNullValue()) {
            return runtime.createNull();
        }
        throw new IllegalArgumentException(String.format("Unrecognized literal: %s", literalExpression));
    }

    @Override
    public T visitMultiSelectList(MultiSelectListExpression multiSelectListExpression) {
        JmespathRuntime.ArrayBuilder<T> output = runtime.arrayBuilder();
        for (JmespathExpression exp : multiSelectListExpression.getExpressions()) {
            output.add(visit(exp));
        }
        // TODO: original smithy-java has output.isEmpty() ? null : Document.of(output);
        // but that doesn't seem to match the spec
        return output.build();
    }

    @Override
    public T visitMultiSelectHash(MultiSelectHashExpression multiSelectHashExpression) {
        JmespathRuntime.ObjectBuilder<T> output = runtime.objectBuilder();
        for (Map.Entry<String, JmespathExpression> expEntry : multiSelectHashExpression.getExpressions().entrySet()) {
            output.put(runtime.createString(expEntry.getKey()), visit(expEntry.getValue()));
        }
        // TODO: original smithy-java has output.isEmpty() ? null : Document.of(output);
        // but that doesn't seem to match the spec
        return output.build();
    }

    @Override
    public T visitAnd(AndExpression andExpression) {
        T left = visit(andExpression.getLeft());
        return runtime.isTruthy(left) ? visit(andExpression.getRight()) : left;
    }

    @Override
    public T visitOr(OrExpression orExpression) {
        T left = visit(orExpression.getLeft());
        if (runtime.isTruthy(left)) {
            return left;
        }
        return orExpression.getRight().accept(this);
    }

    @Override
    public T visitNot(NotExpression notExpression) {
        T output = visit(notExpression.getExpression());
        return runtime.createBoolean(!runtime.isTruthy(output));
    }

    @Override
    public T visitProjection(ProjectionExpression projectionExpression) {
        T resultList = visit(projectionExpression.getLeft());
        if (!runtime.is(resultList, RuntimeType.ARRAY)) {
            return runtime.createNull();
        }
        JmespathRuntime.ArrayBuilder<T> projectedResults = runtime.arrayBuilder();
        for (T result : runtime.toIterable(resultList)) {
            T projected = new Evaluator<T>(result, runtime).visit(projectionExpression.getRight());
            if (!runtime.typeOf(projected).equals(RuntimeType.NULL)) {
                projectedResults.add(projected);
            }
        }
        return projectedResults.build();
    }

    @Override
    public T visitFilterProjection(FilterProjectionExpression filterProjectionExpression) {
        T left = visit(filterProjectionExpression.getLeft());
        if (!runtime.is(left, RuntimeType.ARRAY)) {
            return runtime.createNull();
        }
        JmespathRuntime.ArrayBuilder<T> results = runtime.arrayBuilder();
        for (T val : runtime.toIterable(left)) {
            T output = new Evaluator<>(val, runtime).visit(filterProjectionExpression.getComparison());
            if (runtime.isTruthy(output)) {
                T result = new Evaluator<>(val, runtime).visit(filterProjectionExpression.getRight());
                if (!runtime.is(result, RuntimeType.NULL)) {
                    results.add(result);
                }
            }
        }
        return results.build();
    }

    @Override
    public T visitObjectProjection(ObjectProjectionExpression objectProjectionExpression) {
        T resultObject = visit(objectProjectionExpression.getLeft());
        if (!runtime.is(resultObject, RuntimeType.OBJECT)) {
            return runtime.createNull();
        }
        JmespathRuntime.ArrayBuilder<T> projectedResults = runtime.arrayBuilder();
        for (T member : runtime.toIterable(resultObject)) {
            T memberValue = runtime.value(resultObject, member);
            if (!runtime.is(memberValue, RuntimeType.NULL)) {
                T projectedResult = new Evaluator<T>(memberValue, runtime).visit(objectProjectionExpression.getRight());
                if (!runtime.is(projectedResult, RuntimeType.NULL)) {
                    projectedResults.add(projectedResult);
                }
            }
        }
        return projectedResults.build();
    }

    @Override
    public T visitSlice(SliceExpression sliceExpression) {
        return runtime.slice(current,
                optionalNumber(sliceExpression.getStart()),
                optionalNumber(sliceExpression.getStop()),
                runtime.createNumber(sliceExpression.getStep()));
    }

    private T optionalNumber(OptionalInt optionalInt) {
        if (optionalInt.isPresent()) {
            return runtime.createNumber(optionalInt.getAsInt());
        } else {
            return runtime.createNull();
        }
    }

    @Override
    public T visitSubexpression(Subexpression subexpression) {
        T left = visit(subexpression.getLeft());
        return new Evaluator<>(left, runtime).visit(subexpression.getRight());
    }
}
