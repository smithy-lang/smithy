/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.validators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.logic.bdd.BddTrait;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;
import software.amazon.smithy.rulesengine.traits.EndpointTestCase;
import software.amazon.smithy.rulesengine.traits.EndpointTestOperationInput;
import software.amazon.smithy.rulesengine.traits.EndpointTestsTrait;

/**
 * Validates that the built-in's specified on parameters are supported.
 */
public final class RuleSetBuiltInValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();

        for (ServiceShape s : model.getServiceShapesWithTrait(BddTrait.class)) {
            validateParams(events, s, s.expectTrait(BddTrait.class).getParameters());
        }

        for (ServiceShape s : model.getServiceShapesWithTrait(EndpointRuleSetTrait.class)) {
            validateParams(events, s, s.expectTrait(EndpointRuleSetTrait.class).getEndpointRuleSet().getParameters());
        }

        for (ServiceShape s : model.getServiceShapesWithTrait(EndpointTestsTrait.class)) {
            validateTestBuiltIns(events, s, s.expectTrait(EndpointTestsTrait.class));
        }

        return events;
    }

    private void validateParams(List<ValidationEvent> events, ServiceShape service, Iterable<Parameter> params) {
        for (Parameter parameter : params) {
            if (parameter.isBuiltIn()) {
                validateBuiltIn(events, service, parameter.getBuiltIn().get(), parameter, "RuleSet");
            }
        }
    }

    private void validateTestBuiltIns(List<ValidationEvent> events, ServiceShape service, EndpointTestsTrait suite) {
        int testIndex = 0;
        for (EndpointTestCase testCase : suite.getTestCases()) {
            int inputIndex = 0;
            for (EndpointTestOperationInput operationInput : testCase.getOperationInputs()) {
                for (StringNode builtInNode : operationInput.getBuiltInParams().getMembers().keySet()) {
                    validateBuiltIn(events,
                            service,
                            builtInNode.getValue(),
                            operationInput,
                            "TestCase",
                            String.valueOf(testIndex),
                            "Inputs",
                            String.valueOf(inputIndex));
                }
                inputIndex++;
            }
            testIndex++;
        }
    }

    private void validateBuiltIn(
            List<ValidationEvent> events,
            ServiceShape service,
            String name,
            FromSourceLocation source,
            String... eventIdSuffixes
    ) {
        if (!EndpointRuleSet.hasBuiltIn(name)) {
            String msg = String.format("The `%s` built-in used is not registered, valid built-ins: %s",
                    name,
                    EndpointRuleSet.getKeyString());
            events.add(error(service, source, msg, String.join(".", Arrays.asList(eventIdSuffixes))));
        }
    }
}
