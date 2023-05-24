/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.rulesengine.validators;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.eval.TestEvaluator;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;
import software.amazon.smithy.rulesengine.traits.EndpointTestCase;
import software.amazon.smithy.rulesengine.traits.EndpointTestsTrait;

public class RuleSetTestCaseValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (ServiceShape serviceShape : model.getServiceShapesWithTrait(EndpointRuleSetTrait.class)) {
            if (serviceShape.hasTrait(EndpointTestsTrait.class)) {
                EndpointRuleSet ruleSet = serviceShape.expectTrait(EndpointRuleSetTrait.class).getEndpointRuleSet();
                EndpointTestsTrait testsTrait = serviceShape.expectTrait(EndpointTestsTrait.class);

                // Test/Rule evaluation throws RuntimeExceptions when evaluating, wrap these
                // up into ValidationEvents for automatic validation.
                for (EndpointTestCase endpointTestCase : testsTrait.getTestCases()) {
                    try {
                        TestEvaluator.evaluate(ruleSet, endpointTestCase);
                    } catch (RuntimeException e) {
                        events.add(error(serviceShape, endpointTestCase, e.getMessage()));
                    }
                }
            }
        }
        return events;
    }
}
