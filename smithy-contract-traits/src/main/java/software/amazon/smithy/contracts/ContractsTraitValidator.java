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
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.jmespath.node.ModelJmespathUtils;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.ListUtils;

/**
 * Statically lints the {@code @contracts} expressions of each operation.
 *
 * <p>Whether a contract holds for a given call is checked by {@link ContractsTraitPlugin}
 * through the operation-call hook. This validator only checks that each expression is
 * well-formed and returns a boolean against the operation call's static type.
 */
public final class ContractsTraitValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        if (!model.isTraitApplied(ContractsTrait.class)) {
            return ListUtils.of();
        }

        List<ValidationEvent> events = new ArrayList<>();
        for (OperationShape operation : model.getOperationShapesWithTrait(ContractsTrait.class)) {
            ContractsTrait contracts = operation.expectTrait(ContractsTrait.class);
            for (Map.Entry<String, Condition> entry : contracts.getContracts().entrySet()) {
                events.addAll(validateContract(model, operation, entry.getKey(), entry.getValue()));
            }
        }
        return events;
    }

    private List<ValidationEvent> validateContract(
            Model model,
            OperationShape operation,
            String contractName,
            Condition contract
    ) {
        List<ValidationEvent> events = new ArrayList<>();

        LinterResult result = ModelJmespathUtils.lintOperationCall(model, operation, contract.getExpression());
        for (ExpressionProblem problem : result.getProblems()) {
            addJmespathEvent(events, operation, contractName, contract, problem);
        }
        if (result.getReturnType() != RuntimeType.BOOLEAN) {
            events.add(danger(operation,
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
            OperationShape operation,
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
        events.add(ValidationEvent.builder()
                .id(eventId)
                .shape(operation)
                .sourceLocation(contract.getSourceLocation())
                .severity(severity)
                .message(String.format("Contract `%s`: Problem found in JMESPath expression for contract %s: %s",
                        contractName,
                        contractName,
                        problemMessage))
                .build());
    }
}
