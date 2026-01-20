/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.contracts;

import java.util.ArrayList;
import java.util.HashMap;
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

    private static final String CONDITIONS_TRAIT = "ConditionsTrait";

    @Override
    public List<ValidationEvent> validate(Model model) {
        return model.shapes()
                .filter(shape -> shape.hasTrait(ConditionsTrait.ID))
                .flatMap(shape -> validateShape(model, shape).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateShape(Model model, Shape shape) {
        List<ValidationEvent> events = new ArrayList<>();
        ConditionsTrait conditions = shape.expectTrait(ConditionsTrait.class);

        Map<String, List<Condition>> idsToConditions = new HashMap<>();
        for (Condition condition : conditions.getConditions()) {
            idsToConditions.computeIfAbsent(condition.getId(), id -> new ArrayList<>()).add(condition);
            events.addAll(validateCondition(model, shape, condition));
        }
        for (Map.Entry<String, List<Condition>> entry : idsToConditions.entrySet()) {
            if (entry.getValue().size() > 1) {
                events.add(error(shape,
                        String.format(
                                "Conflicting `%s` conditions IDs found for ID `%s`",
                                ConditionsTrait.ID,
                                entry.getKey())));
            }
        }

        return events;
    }

    private List<ValidationEvent> validateCondition(Model model, Shape shape, Condition condition) {
        List<ValidationEvent> events = new ArrayList<>();

        LinterResult result = ModelJmespathUtilities.lint(model, shape, condition.getParsedExpression());
        for (ExpressionProblem problem : result.getProblems()) {
            addJmespathEvent(events, shape, condition, problem);
        }
        if (result.getReturnType() != RuntimeType.BOOLEAN) {
            events.add(danger(shape,
                    String.format(
                            "Condition %s expression must return a boolean type, but this expression was "
                                    + "statically determined to return a `%s` type.",
                            condition.getId(),
                            result.getReturnType())));
        }

        return events;
    }

    private void addJmespathEvent(
            List<ValidationEvent> events,
            Shape shape,
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
                        + ModelJmespathUtilities.JMES_PATH_DANGER + "." + condition.getId();
                break;
            default:
                severity = Severity.WARNING;
                eventId = getName() + "." + ModelJmespathUtilities.JMESPATH_PROBLEM + "."
                        + ModelJmespathUtilities.JMES_PATH_WARNING + "." + condition.getId();
                break;
        }

        String problemMessage = problem.message + " (" + problem.line + ":" + problem.column + ")";
        addEvent(events,
                severity,
                shape,
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
            Condition condition,
            String message,
            String... eventIdParts
    ) {
        events.add(ValidationEvent.builder()
                .id(String.join(".", eventIdParts))
                .shape(shape)
                .sourceLocation(condition.getSourceLocation())
                .severity(severity)
                .message(String.format("Condition `%s`: %s", condition.getId(), message))
                .build());
    }
}
