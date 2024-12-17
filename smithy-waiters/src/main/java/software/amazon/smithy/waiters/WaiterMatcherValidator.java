/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.waiters;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.smithy.jmespath.ExpressionProblem;
import software.amazon.smithy.jmespath.JmespathException;
import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.jmespath.LinterResult;
import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.ast.LiteralExpression;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

final class WaiterMatcherValidator implements Matcher.Visitor<List<ValidationEvent>> {

    private static final String NON_SUPPRESSABLE_ERROR = "WaitableTrait";
    private static final String JMESPATH_PROBLEM = NON_SUPPRESSABLE_ERROR + "JmespathProblem";
    private static final String INVALID_ERROR_TYPE = NON_SUPPRESSABLE_ERROR + "InvalidErrorType";
    private static final String RETURN_TYPE_MISMATCH = "ReturnTypeMismatch";
    private static final String JMES_PATH_DANGER = "JmespathEventDanger";
    private static final String JMES_PATH_WARNING = "JmespathEventWarning";

    private final Model model;
    private final OperationShape operation;
    private final String waiterName;
    private final WaitableTrait waitable;
    private final List<ValidationEvent> events = new ArrayList<>();
    private final int acceptorIndex;

    WaiterMatcherValidator(Model model, OperationShape operation, String waiterName, int acceptorIndex) {
        this.model = Objects.requireNonNull(model);
        this.operation = Objects.requireNonNull(operation);
        this.waitable = operation.expectTrait(WaitableTrait.class);
        this.waiterName = Objects.requireNonNull(waiterName);
        this.acceptorIndex = acceptorIndex;
    }

    @Override
    public List<ValidationEvent> visitOutput(Matcher.OutputMember outputPath) {
        StructureShape struct = OperationIndex.of(model).expectOutputShape(operation);
        validatePathMatcher(createCurrentNodeFromShape(struct), outputPath.getValue());
        return events;
    }

    @Override
    public List<ValidationEvent> visitInputOutput(Matcher.InputOutputMember inputOutputMember) {
        OperationIndex index = OperationIndex.of(model);
        StructureShape input = index.expectInputShape(operation);
        StructureShape output = index.expectOutputShape(operation);
        Map<String, Object> composedMap = new LinkedHashMap<>();
        composedMap.put("input", createCurrentNodeFromShape(input).expectObjectValue());
        composedMap.put("output", createCurrentNodeFromShape(output).expectObjectValue());
        LiteralExpression composedData = new LiteralExpression(composedMap);
        validatePathMatcher(composedData, inputOutputMember.getValue());
        return events;
    }

    @Override
    public List<ValidationEvent> visitSuccess(Matcher.SuccessMember success) {
        return events;
    }

    @Override
    public List<ValidationEvent> visitErrorType(Matcher.ErrorTypeMember errorType) {
        // Ensure that the errorType is defined on the operation. There may be cases
        // where the errorType is framework based or lower level, so it might not be
        // defined in the actual model.
        String error = errorType.getValue();

        for (ShapeId errorId : operation.getErrors()) {
            if (error.equals(errorId.toString()) || error.equals(errorId.getName())) {
                return events;
            }
        }

        addEvent(Severity.WARNING,
                String.format(
                        "errorType '%s' not found on operation. This operation defines the following errors: %s",
                        error,
                        operation.getErrors()),
                INVALID_ERROR_TYPE,
                waiterName,
                String.valueOf(acceptorIndex));

        return events;
    }

    @Override
    public List<ValidationEvent> visitUnknown(Matcher.UnknownMember unknown) {
        // This is validated by model validation. No need to do more here.
        return events;
    }

    private void validatePathMatcher(LiteralExpression input, PathMatcher pathMatcher) {
        RuntimeType returnType = validatePath(input, pathMatcher.getPath());

        switch (pathMatcher.getComparator()) {
            case BOOLEAN_EQUALS:
                // A booleanEquals comparator requires an `expected` value of "true" or "false".
                if (!pathMatcher.getExpected().equals("true") && !pathMatcher.getExpected().equals("false")) {
                    addEvent(Severity.ERROR,
                            String.format(
                                    "Waiter acceptors with a %s comparator must set their `expected` value to 'true' or "
                                            + "'false', but found '%s'.",
                                    PathComparator.BOOLEAN_EQUALS,
                                    pathMatcher.getExpected()),
                            NON_SUPPRESSABLE_ERROR);
                }
                validateReturnType(pathMatcher.getComparator(), RuntimeType.BOOLEAN, returnType);
                break;
            case STRING_EQUALS:
                validateReturnType(pathMatcher.getComparator(), RuntimeType.STRING, returnType);
                break;
            default: // array operations
                validateReturnType(pathMatcher.getComparator(), RuntimeType.ARRAY, returnType);
        }
    }

    private RuntimeType validatePath(LiteralExpression input, String path) {
        try {
            JmespathExpression expression = JmespathExpression.parse(path);
            LinterResult result = expression.lint(input);
            for (ExpressionProblem problem : result.getProblems()) {
                addJmespathEvent(path, problem);
            }
            return result.getReturnType();
        } catch (JmespathException e) {
            addEvent(Severity.ERROR,
                    String.format(
                            "Invalid JMESPath expression (%s): %s",
                            path,
                            e.getMessage()),
                    NON_SUPPRESSABLE_ERROR);
            return RuntimeType.ANY;
        }
    }

    private void validateReturnType(PathComparator comparator, RuntimeType expected, RuntimeType actual) {
        if (actual != RuntimeType.ANY && actual != expected) {
            addEvent(Severity.DANGER,
                    String.format(
                            "Waiter acceptors with a %s comparator must return a `%s` type, but this acceptor was "
                                    + "statically determined to return a `%s` type.",
                            comparator,
                            expected,
                            actual),
                    JMESPATH_PROBLEM,
                    RETURN_TYPE_MISMATCH,
                    waiterName,
                    String.valueOf(acceptorIndex));
        }
    }

    // Lint using an ANY type or using the modeled shape as the starting data.
    private LiteralExpression createCurrentNodeFromShape(Shape shape) {
        return shape == null
                ? LiteralExpression.ANY
                : new LiteralExpression(shape.accept(new ModelRuntimeTypeGenerator(model)));
    }

    private void addJmespathEvent(String path, ExpressionProblem problem) {
        Severity severity;
        String eventId;
        switch (problem.severity) {
            case ERROR:
                severity = Severity.ERROR;
                eventId = NON_SUPPRESSABLE_ERROR;
                break;
            case DANGER:
                severity = Severity.DANGER;
                eventId = JMESPATH_PROBLEM + "." + JMES_PATH_DANGER + "." + waiterName + "." + acceptorIndex;
                break;
            default:
                severity = Severity.WARNING;
                eventId = JMESPATH_PROBLEM + "." + JMES_PATH_WARNING + "." + waiterName + "." + acceptorIndex;
                break;
        }

        String problemMessage = problem.message + " (" + problem.line + ":" + problem.column + ")";
        addEvent(severity,
                String.format("Problem found in JMESPath expression (%s): %s", path, problemMessage),
                eventId);
    }

    private void addEvent(Severity severity, String message, String... eventIdParts) {
        events.add(ValidationEvent.builder()
                .id(String.join(".", eventIdParts))
                .shape(operation)
                .sourceLocation(waitable)
                .severity(severity)
                .message(String.format("Waiter `%s`, acceptor %d: %s", waiterName, acceptorIndex, message))
                .build());
    }
}
