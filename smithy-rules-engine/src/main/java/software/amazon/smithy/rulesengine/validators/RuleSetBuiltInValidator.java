/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.validators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
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
        for (ServiceShape serviceShape : model.getServiceShapesWithTrait(EndpointRuleSetTrait.class)) {
            events.addAll(validateRuleSetBuiltIns(serviceShape,
                    serviceShape.expectTrait(EndpointRuleSetTrait.class)
                            .getEndpointRuleSet()));
        }

        for (ServiceShape serviceShape : model.getServiceShapesWithTrait(EndpointTestsTrait.class)) {
            events.addAll(validateTestTraitBuiltIns(serviceShape, serviceShape.expectTrait(EndpointTestsTrait.class)));
        }
        return events;
    }

    private List<ValidationEvent> validateRuleSetBuiltIns(ServiceShape serviceShape, EndpointRuleSet ruleSet) {
        List<ValidationEvent> events = new ArrayList<>();
        for (Parameter parameter : ruleSet.getParameters()) {
            if (parameter.isBuiltIn()) {
                validateBuiltIn(serviceShape, parameter.getBuiltIn().get(), parameter, "RuleSet")
                        .ifPresent(events::add);
            }
        }
        return events;
    }

    private List<ValidationEvent> validateTestTraitBuiltIns(ServiceShape serviceShape, EndpointTestsTrait testSuite) {
        List<ValidationEvent> events = new ArrayList<>();
        int testIndex = 0;
        for (EndpointTestCase testCase : testSuite.getTestCases()) {
            int inputIndex = 0;
            for (EndpointTestOperationInput operationInput : testCase.getOperationInputs()) {
                for (StringNode builtInNode : operationInput.getBuiltInParams().getMembers().keySet()) {
                    validateBuiltIn(serviceShape,
                            builtInNode.getValue(),
                            operationInput,
                            "TestCase",
                            String.valueOf(testIndex),
                            "Inputs",
                            String.valueOf(inputIndex))
                            .ifPresent(events::add);
                }
                inputIndex++;
            }
            testIndex++;
        }
        return events;
    }

    private Optional<ValidationEvent> validateBuiltIn(
            ServiceShape serviceShape,
            String builtInName,
            FromSourceLocation source,
            String... eventIdSuffixes
    ) {
        if (!EndpointRuleSet.hasBuiltIn(builtInName)) {
            return Optional.of(error(serviceShape,
                    source,
                    String.format(
                            "The `%s` built-in used is not registered, valid built-ins: %s",
                            builtInName,
                            EndpointRuleSet.getKeyString()),
                    String.join(".", Arrays.asList(eventIdSuffixes))));
        }
        return Optional.empty();
    }
}
