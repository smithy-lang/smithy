/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath;

import static software.amazon.smithy.jmespath.FunctionDefinition.isType;
import static software.amazon.smithy.jmespath.FunctionDefinition.listOfType;
import static software.amazon.smithy.jmespath.FunctionDefinition.oneOf;
import static software.amazon.smithy.jmespath.ast.LiteralExpression.ANY;
import static software.amazon.smithy.jmespath.ast.LiteralExpression.ARRAY;
import static software.amazon.smithy.jmespath.ast.LiteralExpression.BOOLEAN;
import static software.amazon.smithy.jmespath.ast.LiteralExpression.EXPREF;
import static software.amazon.smithy.jmespath.ast.LiteralExpression.NULL;
import static software.amazon.smithy.jmespath.ast.LiteralExpression.NUMBER;
import static software.amazon.smithy.jmespath.ast.LiteralExpression.OBJECT;
import static software.amazon.smithy.jmespath.ast.LiteralExpression.STRING;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.jmespath.ast.AndExpression;
import software.amazon.smithy.jmespath.ast.ComparatorExpression;
import software.amazon.smithy.jmespath.ast.ComparatorType;
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

final class TypeChecker implements ExpressionVisitor<LiteralExpression> {

    private static final Map<String, FunctionDefinition> FUNCTIONS = new HashMap<>();

    static {
        FunctionDefinition.ArgValidator isAny = isType(RuntimeType.ANY);
        FunctionDefinition.ArgValidator isString = isType(RuntimeType.STRING);
        FunctionDefinition.ArgValidator isNumber = isType(RuntimeType.NUMBER);
        FunctionDefinition.ArgValidator isArray = isType(RuntimeType.ARRAY);

        FUNCTIONS.put("abs", new FunctionDefinition(NUMBER, isNumber));
        FUNCTIONS.put("avg", new FunctionDefinition(NUMBER, listOfType(RuntimeType.NUMBER)));
        FUNCTIONS.put("contains",
                new FunctionDefinition(
                        BOOLEAN,
                        oneOf(RuntimeType.ARRAY, RuntimeType.STRING),
                        isAny));
        FUNCTIONS.put("ceil", new FunctionDefinition(NUMBER, isNumber));
        FUNCTIONS.put("ends_with", new FunctionDefinition(NUMBER, isString, isString));
        FUNCTIONS.put("floor", new FunctionDefinition(NUMBER, isNumber));
        FUNCTIONS.put("join", new FunctionDefinition(STRING, isString, listOfType(RuntimeType.STRING)));
        FUNCTIONS.put("keys", new FunctionDefinition(ARRAY, isType(RuntimeType.OBJECT)));
        FUNCTIONS.put("length",
                new FunctionDefinition(
                        NUMBER,
                        oneOf(RuntimeType.STRING, RuntimeType.ARRAY, RuntimeType.OBJECT)));
        // TODO: Support expression reference return type validation?
        FUNCTIONS.put("map", new FunctionDefinition(ARRAY, isType(RuntimeType.EXPRESSION), isArray));
        // TODO: support array<X|Y>
        FUNCTIONS.put("max", new FunctionDefinition(NUMBER, isArray));
        FUNCTIONS.put("max_by", new FunctionDefinition(NUMBER, isArray, isType(RuntimeType.EXPRESSION)));
        FUNCTIONS.put("merge", new FunctionDefinition(OBJECT, Collections.emptyList(), isType(RuntimeType.OBJECT)));
        FUNCTIONS.put("min", new FunctionDefinition(NUMBER, isArray));
        FUNCTIONS.put("min_by", new FunctionDefinition(NUMBER, isArray, isType(RuntimeType.EXPRESSION)));
        FUNCTIONS.put("not_null", new FunctionDefinition(ANY, Collections.singletonList(isAny), isAny));
        FUNCTIONS.put("reverse", new FunctionDefinition(ARRAY, oneOf(RuntimeType.ARRAY, RuntimeType.STRING)));
        FUNCTIONS.put("sort", new FunctionDefinition(ARRAY, isArray));
        FUNCTIONS.put("sort_by", new FunctionDefinition(ARRAY, isArray, isType(RuntimeType.EXPRESSION)));
        FUNCTIONS.put("starts_with", new FunctionDefinition(BOOLEAN, isString, isString));
        FUNCTIONS.put("sum", new FunctionDefinition(NUMBER, listOfType(RuntimeType.NUMBER)));
        FUNCTIONS.put("to_array", new FunctionDefinition(ARRAY, isAny));
        FUNCTIONS.put("to_string", new FunctionDefinition(STRING, isAny));
        FUNCTIONS.put("to_number", new FunctionDefinition(NUMBER, isAny));
        FUNCTIONS.put("type", new FunctionDefinition(STRING, isAny));
        FUNCTIONS.put("values", new FunctionDefinition(ARRAY, isType(RuntimeType.OBJECT)));
    }

    private final LiteralExpression current;
    private final Set<ExpressionProblem> problems;
    private LiteralExpression knownFunctionType = ANY;

    TypeChecker(LiteralExpression current, Set<ExpressionProblem> problems) {
        this.current = current;
        this.problems = problems;
    }

    @Override
    public LiteralExpression visitComparator(ComparatorExpression expression) {
        LiteralExpression left = expression.getLeft().accept(this);
        LiteralExpression right = expression.getRight().accept(this);
        LiteralExpression result = left.getType().compare(left, right, expression.getComparator());

        if (result.getType() == RuntimeType.NULL) {
            badComparator(expression, left.getType(), expression.getComparator());
        }

        return result;
    }

    @Override
    public LiteralExpression visitCurrentNode(CurrentExpression expression) {
        return current;
    }

    @Override
    public LiteralExpression visitExpressionType(ExpressionTypeExpression expression) {
        // Expression references are late bound, so the type is only known
        // when the reference is used in a function.
        expression.getExpression().accept(new TypeChecker(knownFunctionType, problems));
        return EXPREF;
    }

    @Override
    public LiteralExpression visitFlatten(FlattenExpression expression) {
        LiteralExpression result = expression.getExpression().accept(this);

        if (!result.isArrayValue()) {
            if (result.getType() != RuntimeType.ANY) {
                danger(expression, "Array flatten performed on " + result.getType());
            }
            return ARRAY;
        }

        // Perform the actual flattening.
        List<Object> flattened = new ArrayList<>();
        for (Object value : result.expectArrayValue()) {
            LiteralExpression element = LiteralExpression.from(value);
            if (element.isArrayValue()) {
                flattened.addAll(element.expectArrayValue());
            } else if (!element.isNullValue()) {
                flattened.add(element);
            }
        }

        return new LiteralExpression(flattened);
    }

    @Override
    public LiteralExpression visitField(FieldExpression expression) {
        if (current.isObjectValue()) {
            if (current.hasObjectField(expression.getName())) {
                return current.getObjectField(expression.getName());
            } else {
                danger(expression,
                        String.format(
                                "Object field '%s' does not exist in object with properties %s",
                                expression.getName(),
                                current.expectObjectValue().keySet()));
                return NULL;
            }
        }

        if (current.getType() != RuntimeType.ANY) {
            danger(expression,
                    String.format(
                            "Object field '%s' extraction performed on %s",
                            expression.getName(),
                            current.getType()));
        }

        return ANY;
    }

    @Override
    public LiteralExpression visitIndex(IndexExpression expression) {
        if (current.isArrayValue()) {
            return current.getArrayIndex(expression.getIndex());
        }

        if (current.getType() != RuntimeType.ANY) {
            danger(expression,
                    String.format(
                            "Array index '%s' extraction performed on %s",
                            expression.getIndex(),
                            current.getType()));
        }

        return ANY;
    }

    @Override
    public LiteralExpression visitLiteral(LiteralExpression expression) {
        return expression;
    }

    @Override
    public LiteralExpression visitMultiSelectList(MultiSelectListExpression expression) {
        List<Object> values = new ArrayList<>();
        for (JmespathExpression e : expression.getExpressions()) {
            values.add(e.accept(this).getValue());
        }
        return new LiteralExpression(values);
    }

    @Override
    public LiteralExpression visitMultiSelectHash(MultiSelectHashExpression expression) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, JmespathExpression> entry : expression.getExpressions().entrySet()) {
            result.put(entry.getKey(), entry.getValue().accept(this).getValue());
        }
        return new LiteralExpression(result);
    }

    @Override
    public LiteralExpression visitAnd(AndExpression expression) {
        LiteralExpression leftResult = expression.getLeft().accept(this);
        // Visit right side regardless of the evaluation of the left side to validate the result.
        LiteralExpression rightResult = expression.getRight().accept(this);
        // If LHS is falsey, return LHS. Otherwise, return RHS.
        return leftResult.isTruthy() ? rightResult : leftResult;
    }

    @Override
    public LiteralExpression visitOr(OrExpression expression) {
        LiteralExpression leftResult = expression.getLeft().accept(this);
        // Visit right side regardless of the evaluation of the left side to validate the result.
        LiteralExpression rightResult = expression.getRight().accept(this);
        return leftResult.isTruthy() ? leftResult : rightResult;
    }

    @Override
    public LiteralExpression visitNot(NotExpression expression) {
        LiteralExpression result = expression.getExpression().accept(this);
        return new LiteralExpression(!result.isTruthy());
    }

    @Override
    public LiteralExpression visitProjection(ProjectionExpression expression) {
        LiteralExpression leftResult = expression.getLeft().accept(this);

        // If LHS is not an array, then just do basic checks on RHS using ANY + ARRAY.
        if (!leftResult.isArrayValue() || leftResult.expectArrayValue().isEmpty()) {
            if (leftResult.getType() != RuntimeType.ANY && !leftResult.isArrayValue()) {
                danger(expression, "Array projection performed on " + leftResult.getType());
            }
            // Run RHS once using an ANY to test it too.
            expression.getRight().accept(new TypeChecker(ANY, problems));
            return ARRAY;
        } else {
            // LHS is an array, so do the projection.
            List<Object> result = new ArrayList<>();
            for (Object value : leftResult.expectArrayValue()) {
                TypeChecker checker = new TypeChecker(LiteralExpression.from(value), problems);
                result.add(expression.getRight().accept(checker).getValue());
            }
            return new LiteralExpression(result);
        }
    }

    @Override
    public LiteralExpression visitObjectProjection(ObjectProjectionExpression expression) {
        LiteralExpression leftResult = expression.getLeft().accept(this);

        // If LHS is not an object, then just do basic checks on RHS using ANY + OBJECT.
        if (!leftResult.isObjectValue()) {
            if (leftResult.getType() != RuntimeType.ANY) {
                danger(expression, "Object projection performed on " + leftResult.getType());
            }
            TypeChecker checker = new TypeChecker(ANY, problems);
            expression.getRight().accept(checker);
            return OBJECT;
        }

        // LHS is an object, so do the projection.
        List<Object> result = new ArrayList<>();
        for (Object value : leftResult.expectObjectValue().values()) {
            TypeChecker checker = new TypeChecker(LiteralExpression.from(value), problems);
            result.add(expression.getRight().accept(checker).getValue());
        }

        return new LiteralExpression(result);
    }

    @Override
    public LiteralExpression visitFilterProjection(FilterProjectionExpression expression) {
        LiteralExpression leftResult = expression.getLeft().accept(this);

        // If LHS is not an array or is empty, then just do basic checks on RHS using ANY + ARRAY.
        if (!leftResult.isArrayValue() || leftResult.expectArrayValue().isEmpty()) {
            if (!leftResult.isArrayValue() && leftResult.getType() != RuntimeType.ANY) {
                danger(expression, "Filter projection performed on " + leftResult.getType());
            }
            // Check the comparator and RHS.
            TypeChecker rightVisitor = new TypeChecker(ANY, problems);
            expression.getComparison().accept(rightVisitor);
            expression.getRight().accept(rightVisitor);
            return ARRAY;
        }

        // It's a non-empty array, perform the actual filter.
        List<Object> result = new ArrayList<>();
        for (Object value : leftResult.expectArrayValue()) {
            LiteralExpression literalValue = LiteralExpression.from(value);
            TypeChecker rightVisitor = new TypeChecker(literalValue, problems);
            LiteralExpression comparisonValue = expression.getComparison().accept(rightVisitor);
            if (comparisonValue.isTruthy()) {
                LiteralExpression rightValue = expression.getRight().accept(rightVisitor);
                if (!rightValue.isNullValue()) {
                    result.add(rightValue.getValue());
                }
            }
        }

        return new LiteralExpression(result);
    }

    @Override
    public LiteralExpression visitSlice(SliceExpression expression) {
        // We don't need to actually perform a slice here since this is just basic static analysis.
        if (current.isArrayValue()) {
            return current;
        }

        if (current.getType() != RuntimeType.ANY) {
            danger(expression, "Slice performed on " + current.getType());
        }

        return ARRAY;
    }

    @Override
    public LiteralExpression visitSubexpression(Subexpression expression) {
        LiteralExpression leftResult = expression.getLeft().accept(this);
        TypeChecker rightVisitor = new TypeChecker(leftResult, problems);
        return expression.getRight().accept(rightVisitor);
    }

    @Override
    public LiteralExpression visitFunction(FunctionExpression expression) {
        List<LiteralExpression> arguments = new ArrayList<>();

        // Give expression references the right context.
        TypeChecker checker = new TypeChecker(current, problems);
        checker.knownFunctionType = current;

        for (JmespathExpression arg : expression.getArguments()) {
            arguments.add(arg.accept(checker));
        }

        FunctionDefinition def = FUNCTIONS.get(expression.getName());

        // Function must be known.
        if (def == null) {
            err(expression, "Unknown function: " + expression.getName());
            return ANY;
        }

        // Positional argument arity must match.
        if (arguments.size() < def.arguments.size()
                || (def.variadic == null && arguments.size() > def.arguments.size())) {
            err(expression,
                    expression.getName() + " function expected " + def.arguments.size()
                            + " arguments, but was given " + arguments.size());
        } else {
            for (int i = 0; i < arguments.size(); i++) {
                String error = null;
                if (def.arguments.size() > i) {
                    error = def.arguments.get(i).validate(arguments.get(i));
                } else if (def.variadic != null) {
                    error = def.variadic.validate(arguments.get(i));
                }
                if (error != null) {
                    err(expression.getArguments().get(i),
                            expression.getName() + " function argument " + i + " error: " + error);
                }
            }
        }

        return def.returnValue;
    }

    private void err(JmespathExpression e, String message) {
        problems.add(new ExpressionProblem(ExpressionProblem.Severity.ERROR, e.getLine(), e.getColumn(), message));
    }

    private void danger(JmespathExpression e, String message) {
        problems.add(new ExpressionProblem(ExpressionProblem.Severity.DANGER, e.getLine(), e.getColumn(), message));
    }

    private void warn(JmespathExpression e, String message) {
        problems.add(new ExpressionProblem(ExpressionProblem.Severity.WARNING, e.getLine(), e.getColumn(), message));
    }

    private void badComparator(JmespathExpression expression, RuntimeType type, ComparatorType comparatorType) {
        warn(expression, "Invalid comparator '" + comparatorType + "' for " + type);
    }
}
