package software.amazon.smithy.jmespath.traits;

import software.amazon.smithy.jmespath.ExpressionProblem;
import software.amazon.smithy.jmespath.JmespathException;
import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.jmespath.LinterResult;
import software.amazon.smithy.jmespath.ast.LiteralExpression;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConstraintsTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        return model.shapes()
                .filter(shape -> shape.hasTrait(ConstraintsTrait.ID))
                .flatMap(shape -> validateShape(model, shape).stream())
                .collect(Collectors.toList());
    }

    private static final String NON_SUPPRESSABLE_ERROR = "ConstraintsTrait";
    private static final String JMESPATH_PROBLEM = NON_SUPPRESSABLE_ERROR + "JmespathProblem";
    private static final String JMES_PATH_DANGER = "JmespathEventDanger";
    private static final String JMES_PATH_WARNING = "JmespathEventWarning";

    // Lint using an ANY type or using the modeled shape as the starting data.
    private LiteralExpression createCurrentNodeFromShape(Model model, Shape shape) {
        return shape == null
                ? LiteralExpression.ANY
                : new LiteralExpression(shape.accept(new ModelRuntimeTypeGenerator(model)));
    }

    private List<ValidationEvent> validateShape(Model model, Shape shape) {
        List<ValidationEvent> events = new ArrayList<>();
        ConstraintsTrait constraints = shape.expectTrait(ConstraintsTrait.class);
        LiteralExpression input = createCurrentNodeFromShape(model, shape);

        for (Map.Entry<String, Constraint> entry : constraints.getValues().entrySet()) {
            events.addAll(validatePath(model, shape, constraints, input, entry.getValue().getPath()));
        }
        return events;
    }

    private List<ValidationEvent> validatePath(Model model, Shape shape, Trait trait, LiteralExpression input, String path) {
        try {
            List<ValidationEvent> events = new ArrayList<>();
            JmespathExpression expression = JmespathExpression.parse(path);
            LinterResult result = expression.lint(input);
            for (ExpressionProblem problem : result.getProblems()) {
                events.add(createJmespathEvent(shape, trait, path, problem));
            }
            // TODO: check that result.getReturnType() is boolean
            return events;
        } catch (JmespathException e) {
            return Collections.singletonList(error(
                    shape,
                    String.format(
                            "Invalid JMESPath expression (%s): %s",
                            path,
                            e.getMessage()),
                    NON_SUPPRESSABLE_ERROR));
        }
    }

    private ValidationEvent createJmespathEvent(Shape shape, Trait trait, String path, ExpressionProblem problem) {
        Severity severity;
        String eventId;
        switch (problem.severity) {
            case ERROR:
                severity = Severity.ERROR;
                eventId = NON_SUPPRESSABLE_ERROR;
                break;
            case DANGER:
                severity = Severity.DANGER;
                eventId = JMESPATH_PROBLEM + "." + JMES_PATH_DANGER + "." + shape;
                break;
            default:
                severity = Severity.WARNING;
                eventId = JMESPATH_PROBLEM + "." + JMES_PATH_WARNING + "." + shape;
                break;
        }

        String problemMessage = problem.message + " (" + problem.line + ":" + problem.column + ")";
        return createEvent(severity,
                shape,
                trait,
                String.format("Problem found in JMESPath expression (%s): %s", path, problemMessage),
                eventId);
    }

    private ValidationEvent createEvent(Severity severity, Shape shape, Trait trait, String message, String... eventIdParts) {
        return ValidationEvent.builder()
                .id(String.join(".", eventIdParts))
                .shape(shape)
                .sourceLocation(trait)
                .severity(severity)
                .message(String.format("Shape `%s`: %s", shape, message))
                .build();
    }
}
