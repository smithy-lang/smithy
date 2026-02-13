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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static software.amazon.smithy.jmespath.evaluation.EvaluationUtils.foldLeft;
import static software.amazon.smithy.jmespath.evaluation.EvaluationUtils.ifThenElse;

public class AbstractEvaluator<T> implements ExpressionVisitor<T> {

    private final JmespathAbstractRuntime<T> runtime;
    protected final FunctionRegistry<T> functions;

    // We could make this state mutable instead of creating lots of sub-Evaluators.
    // This would make evaluation not thread-safe, but it's unclear how much that matters.
    protected final T current;

    public AbstractEvaluator(T current, JmespathAbstractRuntime<T> abstractRuntime) {
        this(current, abstractRuntime, FunctionRegistry.getSPIRegistry());
    }

    public AbstractEvaluator(T current, JmespathAbstractRuntime<T> runtime, FunctionRegistry<T> functions) {
        this.current = current;
        this.runtime = runtime;
        this.functions = functions;
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
                return runtime.abstractEqual(left, right);
            case NOT_EQUAL:
                return ifThenElse(runtime, functions,
                        runtime.abstractEqual(left, right),
                        runtime.createBoolean(false),
                        runtime.createBoolean(true));
            // NOTE: Ordering operators >, >=, <, <= are only valid for numbers. All invalid
            // comparisons return null.
            // TODO: Need abstract versions
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

        return ifThenElse(runtime, functions, runtime.abstractIs(value, RuntimeType.ARRAY),
                foldLeft(runtime, functions,
                        runtime.arrayBuilder().build(),
                        JmespathExpression.parse("concat([0], to_array([1]))"),
                        value),
                runtime.createNull());
    }

    @Override
    public T visitFunction(FunctionExpression functionExpression) {
        // TODO: Change API so we can resolve ahead of time once
        Function<T> resolved = functions.lookup(runtime, functionExpression.getName());
        List<FunctionArgument<T>> arguments = new ArrayList<>();
        for (JmespathExpression expr : functionExpression.getArguments()) {
            if (expr instanceof ExpressionTypeExpression) {
                arguments.add(runtime.createFunctionArgument(((ExpressionTypeExpression) expr).getExpression()));
            } else {
                arguments.add(runtime.createFunctionArgument(visit(expr)));
            }
        }
        return resolved.apply(runtime, functions, arguments);
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
        int length = runtime.length(current);
        // Negative indices indicate reverse indexing in JMESPath
        if (index < 0) {
            index = length + index;
        }
        if (length <= index || index < 0) {
            return runtime.createNull();
        }
        return runtime.element(current, index);
    }

    @Override
    public T visitLiteral(LiteralExpression literalExpression) {
        // TODO: Handle when the literal is already wrapping a T
        if (literalExpression.isStringValue()) {
            return runtime.createString(literalExpression.expectStringValue());
        } else if (literalExpression.isBooleanValue()) {
            return runtime.createBoolean(literalExpression.expectBooleanValue());
        } else if (literalExpression.isNumberValue()) {
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
        T result = output.build();

        return ifThenElse(runtime, functions,
                runtime.abstractIs(current, RuntimeType.NULL),
                current,
                result);
    }

    @Override
    public T visitMultiSelectHash(MultiSelectHashExpression multiSelectHashExpression) {
        JmespathRuntime.ObjectBuilder<T> output = runtime.objectBuilder();
        for (Map.Entry<String, JmespathExpression> expEntry : multiSelectHashExpression.getExpressions().entrySet()) {
            output.put(runtime.createString(expEntry.getKey()), visit(expEntry.getValue()));
        }
        T result = output.build();

        return ifThenElse(runtime, functions,
                runtime.abstractIs(current, RuntimeType.NULL),
                current,
                result);
    }

    @Override
    public T visitAnd(AndExpression andExpression) {
        T left = visit(andExpression.getLeft());
        T right = visit(andExpression.getRight());

        return ifThenElse(runtime, functions,
                left, right, left);
    }

    @Override
    public T visitOr(OrExpression orExpression) {
        T left = visit(orExpression.getLeft());
        T right = visit(orExpression.getRight());

        return ifThenElse(runtime, functions,
                left, left, right);
    }

    @Override
    public T visitNot(NotExpression notExpression) {
        T output = visit(notExpression.getExpression());

        return ifThenElse(runtime, functions,
                output, runtime.createBoolean(false), runtime.createBoolean(true));
    }

    @Override
    public T visitProjection(ProjectionExpression projectionExpression) {
        T left = visit(projectionExpression.getLeft());
        JmespathExpression rightExpr = projectionExpression.getRight();

        return ifThenElse(runtime, functions,
                runtime.abstractIs(left, RuntimeType.ARRAY),
                foldLeft(runtime, functions,
                        runtime.arrayBuilder().build(),
                        JmespathExpression.parse("append_if_not_null(acc, eval(<rightExpr>, element))"),
                        left),
                runtime.createNull());
    }

    @Override
    public T visitFilterProjection(FilterProjectionExpression filterProjectionExpression) {
        T left = visit(filterProjectionExpression.getLeft());
        JmespathExpression condExpr = filterProjectionExpression.getComparison();
        JmespathExpression rightExpr = filterProjectionExpression.getRight();

        return ifThenElse(runtime, functions, runtime.abstractIs(left, RuntimeType.ARRAY),
               foldLeft(runtime, functions,
                        runtime.arrayBuilder().build(),
                        JmespathExpression.parse("append_if_not_null(acc, if(eval(<condExpr>, element), eval(<rightExpr>, element), null))"),
                        left),
               runtime.createNull());
    }

    @Override
    public T visitObjectProjection(ObjectProjectionExpression objectProjectionExpression) {
        T left = visit(objectProjectionExpression.getLeft());
        JmespathExpression rightExpr = objectProjectionExpression.getRight();

        return ifThenElse(runtime, functions, runtime.abstractIs(left, RuntimeType.ARRAY),
                foldLeft(runtime, functions,
                        runtime.arrayBuilder().build(),
                        JmespathExpression.parse("append_if_not_null(acc, if(value(<left>, element) != null, eval(<rightExpr>, value(<left>, element)), null))"),
                        left),
                runtime.createNull());
    }

    @Override
    public T visitSlice(SliceExpression sliceExpression) {
        if (!runtime.is(current, RuntimeType.ARRAY)) {
            return runtime.createNull();
        }

        int length = runtime.length(current);

        int step = sliceExpression.getStep();
        if (step == 0) {
            throw new JmespathException(JmespathExceptionType.INVALID_VALUE, "invalid-value");
        }

        int start;
        if (!sliceExpression.getStart().isPresent()) {
            start = step > 0 ? 0 : length - 1;
        } else {
            start = sliceExpression.getStart().getAsInt();
            if (start < 0) {
                start = length + start;
            }
            if (start < 0) {
                start = 0;
            } else if (start > length - 1) {
                start = length - 1;
            }
        }

        int stop;
        if (!sliceExpression.getStop().isPresent()) {
            stop = step > 0 ? length : -1;
        } else {
            stop = sliceExpression.getStop().getAsInt();
            if (stop < 0) {
                stop = length + stop;
            }

            if (stop < 0) {
                stop = -1;
            } else if (stop > length) {
                stop = length;
            }
        }

        return runtime.slice(current, start, stop, step);
    }

    @Override
    public T visitSubexpression(Subexpression subexpression) {
        T left = visit(subexpression.getLeft());
        return new AbstractEvaluator<>(left, runtime).visit(subexpression.getRight());
    }
}
