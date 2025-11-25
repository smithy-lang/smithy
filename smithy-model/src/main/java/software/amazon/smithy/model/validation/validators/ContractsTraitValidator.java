package software.amazon.smithy.model.validation.validators;

import software.amazon.smithy.jmespath.ExpressionProblem;
import software.amazon.smithy.jmespath.JmespathException;
import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.jmespath.LinterResult;
import software.amazon.smithy.jmespath.ast.LiteralExpression;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.ContractsTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ContractsTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        return model.shapes()
                .filter(shape -> shape.hasTrait(ContractsTrait.ID))
                .flatMap(shape -> validateShape(model, shape).stream())
                .collect(Collectors.toList());
    }

    private static final String NON_SUPPRESSABLE_ERROR = "ContractsTrait";

    private List<ValidationEvent> validateShape(Model model, Shape shape) {
        List<ValidationEvent> events = new ArrayList<>();
        ContractsTrait constraints = shape.expectTrait(ContractsTrait.class);

        for (Map.Entry<String, ContractsTrait.Contract> entry : constraints.getValues().entrySet()) {
            events.addAll(validatePath(model, shape, constraints, entry.getValue().getExpression()));
        }
        return events;
    }

    private List<ValidationEvent> validatePath(Model model, Shape shape, Trait trait, String path) {
        try {
            List<ValidationEvent> events = new ArrayList<>();
            JmespathExpression.parse(path);

            // Not using expression.lint() here because we require positive and negative examples instead,
            // which are checked with the interpreter.
            // Given linting just selects a single dummy value and evaluates the expression against it,
            // it would be strictly less powerful when applied here anyway.

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
}
