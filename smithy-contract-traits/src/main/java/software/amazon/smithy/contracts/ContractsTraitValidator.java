/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.contracts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.jmespath.ExpressionProblem;
import software.amazon.smithy.jmespath.LinterResult;
import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.evaluation.Evaluator;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.jmespath.node.ModelJmespathUtils;
import software.amazon.smithy.model.jmespath.node.NodeJmespathRuntime;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.ExamplesTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.validators.ExamplesTraitValidator;
import software.amazon.smithy.utils.ListUtils;

/**
 * Ensures that contract traits are valid.
 */
public final class ContractsTraitValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        if (!model.isTraitApplied(ContractsTrait.class)) {
            return ListUtils.of();
        }

        List<ValidationEvent> events = new ArrayList<>();
        for (Shape shape : model.getShapesWithTrait(ContractsTrait.ID)) {
            validateShape(model, shape, events);
        }
        return events;
    }

    private void validateShape(Model model, Shape shape, List<ValidationEvent> events) {
        ContractsTrait contracts = shape.expectTrait(ContractsTrait.class);

        for (Map.Entry<String, Condition> entry : contracts.getContracts().entrySet()) {
            events.addAll(validateContract(model, shape, entry.getKey(), entry.getValue()));
        }

        // A contract constrains an operation call. The example calls declared by
        // @examples are the calls available at model-build time, so evaluate each
        // contract against each example call. (Operation calls are not instances of
        // operations, so this does not run through node-instance validation.)
        if (shape.isOperationShape() && shape.hasTrait(ExamplesTrait.class)) {
            evaluateExampleCalls(model, shape.asOperationShape().get(), contracts, events);
        }
    }

    private void evaluateExampleCalls(
            Model model,
            OperationShape operation,
            ContractsTrait contracts,
            List<ValidationEvent> events
    ) {
        ExamplesTrait examples = operation.expectTrait(ExamplesTrait.class);
        for (ExamplesTrait.Example example : examples.getExamples()) {
            ObjectNode call = ExamplesTraitValidator.buildOperationCall(model, operation, example);
            Severity severity = example.getAllowConstraintErrors() ? Severity.WARNING : Severity.ERROR;
            for (Map.Entry<String, Condition> entry : contracts.getContracts().entrySet()) {
                evaluateExampleCall(operation, example, call, severity, entry.getKey(), entry.getValue(), events);
            }
        }
    }

    private void evaluateExampleCall(
            OperationShape operation,
            ExamplesTrait.Example example,
            ObjectNode call,
            Severity severity,
            String contractName,
            Condition contract,
            List<ValidationEvent> events
    ) {
        Evaluator<Node> evaluator = new Evaluator<>(call, NodeJmespathRuntime.INSTANCE);
        Node result = evaluator.visit(contract.getExpression());
        if (!result.expectBooleanNode().getValue()) {
            events.add(ValidationEvent.builder()
                    .id(getName() + "." + contractName)
                    .shape(operation)
                    .sourceLocation(contract.getSourceLocation())
                    .severity(severity)
                    .message(String.format(
                            "Example `%s`: Call of `%s` must match the %s contract expression",
                            example.getTitle(),
                            operation.getId(),
                            contractName))
                    .build());
        }
    }

    private List<ValidationEvent> validateContract(
            Model model,
            Shape shape,
            String contractName,
            Condition contract
    ) {
        List<ValidationEvent> events = new ArrayList<>();

        LinterResult result = ModelJmespathUtils.lint(model, shape, contract.getExpression());
        for (ExpressionProblem problem : result.getProblems()) {
            addJmespathEvent(events, shape, contractName, contract, problem);
        }
        if (result.getReturnType() != RuntimeType.BOOLEAN) {
            events.add(danger(shape,
                    String.format(
                            "Contract %s expression must return a boolean type, but this expression was "
                                    + "statically determined to return a `%s` type.",
                            contractName,
                            result.getReturnType())));
        }

        return events;
    }

    private void addJmespathEvent(
            List<ValidationEvent> events,
            Shape shape,
            String contractName,
            Condition contract,
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
                eventId = getName() + "." + ModelJmespathUtils.JMES_PATH_PROBLEM + "."
                        + ModelJmespathUtils.JMES_PATH_DANGER + "." + contractName;
                break;
            default:
                severity = Severity.WARNING;
                eventId = getName() + "." + ModelJmespathUtils.JMES_PATH_PROBLEM + "."
                        + ModelJmespathUtils.JMES_PATH_WARNING + "." + contractName;
                break;
        }

        String problemMessage = problem.message + " (" + problem.line + ":" + problem.column + ")";
        addEvent(events,
                severity,
                shape,
                contractName,
                contract,
                String.format("Problem found in JMESPath expression for contract %s: %s",
                        contractName,
                        problemMessage),
                eventId);
    }

    private void addEvent(
            List<ValidationEvent> events,
            Severity severity,
            Shape shape,
            String contractName,
            Condition contract,
            String message,
            String... eventIdParts
    ) {
        events.add(ValidationEvent.builder()
                .id(String.join(".", eventIdParts))
                .shape(shape)
                .sourceLocation(contract.getSourceLocation())
                .severity(severity)
                .message(String.format("Contract `%s`: %s", contractName, message))
                .build());
    }
}
