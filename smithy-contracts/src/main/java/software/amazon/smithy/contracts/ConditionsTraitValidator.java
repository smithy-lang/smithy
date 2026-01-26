/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.contracts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.smithy.jmespath.ExpressionProblem;
import software.amazon.smithy.jmespath.LinterResult;
import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.jmespath.node.ModelJmespathUtilities;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

public class ConditionsTraitValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        if (!model.isTraitApplied(ConditionsTrait.class)) {
            return ListUtils.of();
        }
        
        List<ValidationEvent> events = new ArrayList<>();
        for (Shape shape : model.getShapesWithTrait(ConditionsTrait.ID)) {
            validateShape(model, shape, events);
        }
        return events;
    }

    private void validateShape(Model model, Shape shape, List<ValidationEvent> events) {
        ConditionsTrait conditions = shape.expectTrait(ConditionsTrait.class);

        for (Map.Entry<String, Condition> entry : conditions.getConditions().entrySet()) {
            events.addAll(validateCondition(model, shape, entry.getKey(), entry.getValue()));
        }
        return events;
    }

    private List<
            ValidationEvent> validateCondition(Model model, Shape shape, String conditionName, Condition condition) {
        List<ValidationEvent> events = new ArrayList<>();

        LinterResult result = ModelJmespathUtilities.lint(model, shape, condition.getParsedExpression());
        for (ExpressionProblem problem : result.getProblems()) {
            addJmespathEvent(events, shape, conditionName, condition, problem);
        }
        if (result.getReturnType() != RuntimeType.BOOLEAN) {
            events.add(danger(shape,
                    String.format(
                            "Condition %s expression must return a boolean type, but this expression was "
                                    + "statically determined to return a `%s` type.",
                            conditionName,
                            result.getReturnType())));
        }

        return events;
    }

    private void addJmespathEvent(
            List<ValidationEvent> events,
            Shape shape,
            String conditionName,
            Condition condition,
            ExpressionProblem problem
    ) {
        Severity severity;
        String eventId;
        switch (problem.severity) {
            case ERROR:
                severity = Severity.ERROR;
                eventId = getName();
                break;
            case DANGER:
                severity = Severity.DANGER;
                eventId = getName() + "." + ModelJmespathUtilities.JMESPATH_PROBLEM + "."
                        + ModelJmespathUtilities.JMES_PATH_DANGER + "." + conditionName;
                break;
            default:
                severity = Severity.WARNING;
                eventId = getName() + "." + ModelJmespathUtilities.JMESPATH_PROBLEM + "."
                        + ModelJmespathUtilities.JMES_PATH_WARNING + "." + conditionName;
                break;
        }

        String problemMessage = problem.message + " (" + problem.line + ":" + problem.column + ")";
        addEvent(events,
                severity,
                shape,
                conditionName,
                condition,
                String.format("Problem found in JMESPath expression (%s): %s",
                        condition.getExpression(),
                        problemMessage),
                eventId);
    }

    private void addEvent(
            List<ValidationEvent> events,
            Severity severity,
            Shape shape,
            String conditionName,
            Condition condition,
            String message,
            String... eventIdParts
    ) {
        events.add(ValidationEvent.builder()
                .id(String.join(".", eventIdParts))
                .shape(shape)
                .sourceLocation(condition.getSourceLocation())
                .severity(severity)
                .message(String.format("Condition `%s`: %s", conditionName, message))
                .build());
    }
}
