/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.aws.validators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;
import software.amazon.smithy.rulesengine.traits.EndpointTestCase;
import software.amazon.smithy.rulesengine.traits.EndpointTestOperationInput;
import software.amazon.smithy.rulesengine.traits.EndpointTestsTrait;

/**
 * Validator that the params used in endpoint test cases are valid.
 */
public final class EndpointTestCasesParamsValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (ServiceShape serviceShape : model.getServiceShapesWithTrait(EndpointTestsTrait.class)) {
            events.addAll(validateTestParamsAreConsistent(serviceShape,
                    serviceShape.expectTrait(EndpointTestsTrait.class),
                    serviceShape.expectTrait(EndpointRuleSetTrait.class).getEndpointRuleSet()));
        }
        return events;
    }

    private List<ValidationEvent> validateTestParamsAreConsistent(
            ServiceShape serviceShape,
            EndpointTestsTrait testsTrait,
            EndpointRuleSet ruleSet
    ) {
        List<ValidationEvent> events = new ArrayList<>();

        Map<String, String> builtInParameters = new HashMap<>();
        ruleSet.getParameters().forEach(parameter -> {
            if (parameter.getBuiltIn().isPresent()) {
                builtInParameters.put(parameter.getName().getName().getValue(), parameter.getBuiltIn().get());
            }
        });

        for (EndpointTestCase testCase : testsTrait.getTestCases()) {
            if (testCase.getOperationInputs() == null
                    || testCase.getOperationInputs()
                            .stream()
                            .allMatch(opInputs -> opInputs.getBuiltInParams() == null)) {
                continue;
            }
            for (Entry<StringNode, Node> testCaseParam : testCase.getParams().getMembers().entrySet()) {
                if (builtInParameters.containsKey(testCaseParam.getKey().getValue())) {
                    testCase.getOperationInputs()
                            .stream()
                            .map(EndpointTestOperationInput::getBuiltInParams)
                            .filter(builtInParams -> builtInParams
                                    .containsMember(builtInParameters.get(testCaseParam.getKey().getValue())))
                            .map(builtInParams -> builtInParams
                                    .getMember(builtInParameters.get(testCaseParam.getKey().getValue())))
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .filter(value -> !value.equals(testCaseParam.getValue()))
                            .forEach(inconsistentValue -> events.add(error(
                                    serviceShape,
                                    "Inconsistent testcase parameters: " + testCaseParam.getValue() + " and "
                                            + inconsistentValue)));
                }
            }
        }
        return events;
    }
}
