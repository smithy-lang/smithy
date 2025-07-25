/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.validators;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.evaluation.TestEvaluator;
import software.amazon.smithy.rulesengine.logic.bdd.Bdd;
import software.amazon.smithy.rulesengine.traits.BddTrait;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;
import software.amazon.smithy.rulesengine.traits.EndpointTestCase;
import software.amazon.smithy.rulesengine.traits.EndpointTestsTrait;

/**
 * Validator to ensure that test cases for rule-sets pass type checking evaluation.
 */
public class RuleSetTestCaseValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (ServiceShape serviceShape : model.getServiceShapesWithTrait(EndpointTestsTrait.class)) {
            EndpointTestsTrait testsTrait = serviceShape.expectTrait(EndpointTestsTrait.class);
            if (serviceShape.hasTrait(EndpointRuleSetTrait.class)) {
                validate(serviceShape, testsTrait, events);
            } else if (serviceShape.hasTrait(BddTrait.class)) {
                validateBdd(serviceShape, testsTrait, events);
            }
        }
        return events;
    }

    // Test/Rule evaluation throws RuntimeExceptions when evaluating, wrap these
    // up into ValidationEvents for automatic validation.
    private void validate(ServiceShape serviceShape, EndpointTestsTrait testsTrait, List<ValidationEvent> events) {
        EndpointRuleSet ruleSet = serviceShape.expectTrait(EndpointRuleSetTrait.class).getEndpointRuleSet();
        for (EndpointTestCase endpointTestCase : testsTrait.getTestCases()) {
            try {
                TestEvaluator.evaluate(ruleSet, endpointTestCase);
            } catch (RuntimeException e) {
                events.add(error(serviceShape, endpointTestCase, e.getMessage()));
            }
        }
    }

    private void validateBdd(ServiceShape serviceShape, EndpointTestsTrait testsTrait, List<ValidationEvent> events) {
        Bdd bdd = serviceShape.expectTrait(BddTrait.class).getBdd();
        for (EndpointTestCase endpointTestCase : testsTrait.getTestCases()) {
            try {
                TestEvaluator.evaluate(bdd, endpointTestCase);
            } catch (RuntimeException e) {
                events.add(error(serviceShape, endpointTestCase, e.getMessage()));
            }
        }
    }
}
