/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.validators;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.parameters.BuiltIns;
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
            events.addAll(validateRuleSetBuiltIns(serviceShape, serviceShape.expectTrait(EndpointRuleSetTrait.class)
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
                validateBuiltIn(serviceShape, parameter.getBuiltIn().get(), parameter).ifPresent(events::add);
            }
        }
        return events;
    }

    private List<ValidationEvent> validateTestTraitBuiltIns(ServiceShape serviceShape, EndpointTestsTrait testSuite) {
        List<ValidationEvent> events = new ArrayList<>();
        for (EndpointTestCase testCase : testSuite.getTestCases()) {
            for (EndpointTestOperationInput operationInput : testCase.getOperationInputs()) {
                for (StringNode builtInNode : operationInput.getBuiltInParams().getMembers().keySet()) {
                    validateBuiltIn(serviceShape, builtInNode.getValue(), builtInNode);
                }
            }
        }
        return events;
    }

    private Optional<ValidationEvent> validateBuiltIn(
            ServiceShape serviceShape,
            String builtInName,
            FromSourceLocation source
    ) {
        if (!BuiltIns.containsBuiltIn(builtInName)) {
            return Optional.of(error(serviceShape, source,
                    String.format("`%s` is not a valid builtIn parameter (%s)", builtInName, BuiltIns.getKeyString())));
        }
        return Optional.empty();
    }
}
