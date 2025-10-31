package software.amazon.smithy.jmespath.traits;

import software.amazon.smithy.jmespath.ExpressionProblem;
import software.amazon.smithy.jmespath.JmespathException;
import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.jmespath.LinterResult;
import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.ast.LiteralExpression;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

import java.util.ArrayList;
import java.util.List;

public class ConstraintsTraitValidator {

    private static final String NON_SUPPRESSABLE_ERROR = "ConstraintsTrait";
    private static final String JMESPATH_PROBLEM = NON_SUPPRESSABLE_ERROR + "JmespathProblem";
    private static final String JMES_PATH_DANGER = "JmespathEventDanger";
    private static final String JMES_PATH_WARNING = "JmespathEventWarning";

    private final Model model;
    private final Shape shape;
    private final ConstraintsTrait constraints;
    private final List<ValidationEvent> events = new ArrayList<>();

    public ConstraintsTraitValidator(Model model, Shape shape) {
        this.model = model;
        this.shape = shape;
        this.constraints = shape.expectTrait(ConstraintsTrait.class);
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
                eventId = JMESPATH_PROBLEM + "." + JMES_PATH_DANGER + "." + shape;
                break;
            default:
                severity = Severity.WARNING;
                eventId = JMESPATH_PROBLEM + "." + JMES_PATH_WARNING + "." + shape;
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
                .shape(shape)
                .sourceLocation(constraints)
                .severity(severity)
                .message(String.format("Shape `%s`: %s", shape, message))
                .build());
    }
}
