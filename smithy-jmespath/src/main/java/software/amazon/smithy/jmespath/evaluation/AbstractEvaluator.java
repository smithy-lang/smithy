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
import software.amazon.smithy.jmespath.SubstitutionVisitor;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

// TODO: Consider whether "abstract" is confusing (since the class is not abstract, the interpretation is)
// TODO: Difference between abstract meaning "can't read concrete java values from T values"
// and abstract meaning "loses information and approximates".
// Question is whether the former is useful without the latter (i.e. abstracting implies
// multiple possible values implies can't read Java values)
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

    public JmespathAbstractRuntime<T> runtime() {
        return runtime;
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
                return ifThenElse(
                        runtime.abstractEqual(left, right),
                        runtime.createBoolean(false),
                        runtime.createBoolean(true));
            // NOTE: Ordering operators >, >=, <, <= are only valid for numbers. All invalid
            // comparisons return null.
            case LESS_THAN:
                return runtime.abstractLessThan(left, right);
            case LESS_THAN_EQUAL:
                return not(runtime.abstractLessThan(right, left));
            case GREATER_THAN:
                return runtime.abstractLessThan(right, left);
            case GREATER_THAN_EQUAL:
                return not(runtime.abstractLessThan(left, right));
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

        return ifThenElse(runtime.abstractIs(value, RuntimeType.ARRAY),
                foldLeft(runtime.arrayBuilder().build(),
                        JmespathExpression.parse("concat(acc, to_array(element))"),
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
        return resolved.apply(this, arguments);
    }

    @Override
    public T visitField(FieldExpression fieldExpression) {
        return runtime.value(current, runtime.createString(fieldExpression.getName()));
    }

    @Override
    public T visitIndex(IndexExpression indexExpression) {
        T index = runtime.createNumber(indexExpression.getIndex());
        T length = runtime.abstractLength(current);
        T adjustedIndex = ifThenElse(
                runtime.abstractLessThan(index, runtime.createNumber(0)),
                add(length, index),
                index);
        T result = runtime.abstractElement(current, adjustedIndex);

        return ifThenElse(
                runtime.abstractIs(current, RuntimeType.ARRAY),
                result,
                runtime.createNull());
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

        return ifThenElse(
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

        return ifThenElse(
                runtime.abstractIs(current, RuntimeType.NULL),
                current,
                result);
    }

    @Override
    public T visitAnd(AndExpression andExpression) {
        T left = visit(andExpression.getLeft());
        T right = visit(andExpression.getRight());

        return ifThenElse(left, right, left);
    }

    @Override
    public T visitOr(OrExpression orExpression) {
        T left = visit(orExpression.getLeft());
        T right = visit(orExpression.getRight());

        return ifThenElse(left, left, right);
    }

    @Override
    public T visitNot(NotExpression notExpression) {
        T output = visit(notExpression.getExpression());

        return ifThenElse(output, runtime.createBoolean(false), runtime.createBoolean(true));
    }

    private static final JmespathExpression PROJECTION_FOLDER_TEMPLATE =
            JmespathExpression.parse("append_if_not_null(acc, eval('rightExpr', element))");

    @Override
    public T visitProjection(ProjectionExpression projectionExpression) {
        T left = visit(projectionExpression.getLeft());
        JmespathExpression rightExpr = projectionExpression.getRight();
        JmespathExpression folder = substitute(LiteralExpression.from("rightExpr"), rightExpr, PROJECTION_FOLDER_TEMPLATE);

        return ifThenElse(
                runtime.abstractIs(left, RuntimeType.ARRAY),
                foldLeft(runtime.arrayBuilder().build(),
                        folder,
                        left),
                runtime.createNull());
    }

    private static final JmespathExpression FILTER_PROJECTION_FOLDER_TEMPLATE =
            JmespathExpression.parse("append_if_not_null(acc, if(eval('condExpr', element), eval('rightExpr', element), null))");

    @Override
    public T visitFilterProjection(FilterProjectionExpression filterProjectionExpression) {
        T left = visit(filterProjectionExpression.getLeft());
        JmespathExpression condExpr = filterProjectionExpression.getComparison();
        JmespathExpression rightExpr = filterProjectionExpression.getRight();
        JmespathExpression folder = substitute(
                LiteralExpression.from("rightExpr"), rightExpr,
                LiteralExpression.from("condExpr"), condExpr,
                PROJECTION_FOLDER_TEMPLATE);

        return ifThenElse(runtime.abstractIs(left, RuntimeType.ARRAY),
               foldLeft(runtime.arrayBuilder().build(), folder, left),
               runtime.createNull());
    }

    private static final JmespathExpression OBJECT_PROJECTION_FOLDER_TEMPLATE =
            JmespathExpression.parse("append_if_not_null(acc, if(value('left', element) != null, eval('rightExpr', value('left', element)), null))");

    @Override
    public T visitObjectProjection(ObjectProjectionExpression objectProjectionExpression) {
        T left = visit(objectProjectionExpression.getLeft());
        JmespathExpression rightExpr = objectProjectionExpression.getRight();
        JmespathExpression folder = substitute(
                LiteralExpression.from("left"), LiteralExpression.from(left),
                LiteralExpression.from("rightExpr"), rightExpr,
                PROJECTION_FOLDER_TEMPLATE);

        return ifThenElse(runtime.abstractIs(left, RuntimeType.ARRAY),
                foldLeft(runtime.arrayBuilder().build(), folder, left),
                runtime.createNull());
    }

    @Override
    public T visitSlice(SliceExpression sliceExpression) {
        // Just abstract this as an arbitrary subset of array elements for simplicity
        // A fully precise abstract implementation of the logic would be a real pain
        // and not worth the extra precision.
        return ifThenElse(runtime.abstractIs(current, RuntimeType.ARRAY),
                foldLeft(runtime.arrayBuilder().build(),
                        JmespathExpression.parse("append_if_not_null(acc, either(element, `null`))"),
                        current),
                runtime.createNull());
    }

    @Override
    public T visitSubexpression(Subexpression subexpression) {
        T left = visit(subexpression.getLeft());
        return new AbstractEvaluator<>(left, runtime, functions).visit(subexpression.getRight());
    }

    // Helpers

    public T ifThenElse(T condition, T then, T otherwise) {
        return functions.lookup(runtime, "if").apply(this, condition, then, otherwise);
    }

    public T not(T value) {
        return ifThenElse(value, runtime.createBoolean(false), runtime.createBoolean(true));
    }

    public T add(T left, T right) {
        return functions.lookup(runtime, "add").apply(this, Arrays.asList(
                runtime.createFunctionArgument(left),
                runtime.createFunctionArgument(right)
        ));
    }

    public T foldLeft(T init, JmespathExpression folder, T collection) {
        return functions.lookup(runtime, "fold_left").apply(this, Arrays.asList(
                runtime.createFunctionArgument(init),
                runtime.createFunctionArgument(folder),
                runtime.createFunctionArgument(collection)
        ));
    }

    public T createAny() {
        return Arrays.stream(RuntimeType.values())
                .map(runtime::createAny)
                .reduce(runtime::either)
                .orElseThrow(NoSuchElementException::new);
    }

    JmespathExpression substitute(JmespathExpression from, JmespathExpression to, JmespathExpression expression) {
        return expression.accept(new SubstitutionVisitor(e -> {
            if (e.equals(from)) {
                return to;
            }
            return null;
        }));
    }

    JmespathExpression substitute(JmespathExpression from1, JmespathExpression to1, JmespathExpression from2, JmespathExpression to2, JmespathExpression expression) {
        return expression.accept(new SubstitutionVisitor(e -> {
            if (e.equals(from1)) {
                return to1;
            }
            if (e.equals(from2)) {
                return to2;
            }
            return null;
        }));
    }

    JmespathExpression substitute(Map<JmespathExpression, JmespathExpression> substitutions, JmespathExpression expression) {
        return expression.accept(new SubstitutionVisitor(substitutions::get));
    }
}
