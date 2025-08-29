/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.aws.validators;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.rulesengine.aws.language.functions.AwsBuiltIns;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.traits.EndpointBddTrait;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;
import software.amazon.smithy.utils.SetUtils;

/**
 * Validator that AWS built-ins used in RuleSet parameters are supported.
 */
public class RuleSetAwsBuiltInValidator extends AbstractValidator {
    private static final Set<String> ADDITIONAL_CONSIDERATION_BUILT_INS = SetUtils.of(
            AwsBuiltIns.ACCOUNT_ID.getBuiltIn().get(),
            AwsBuiltIns.ACCOUNT_ID_ENDPOINT_MODE.getBuiltIn().get(),
            AwsBuiltIns.CREDENTIAL_SCOPE.getBuiltIn().get());
    private static final String ADDITIONAL_CONSIDERATION_MESSAGE = "The `%s` built-in used requires additional "
            + "consideration of the rules that use it.";

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();

        for (ServiceShape s : model.getServiceShapesWithTrait(EndpointRuleSetTrait.class)) {
            EndpointRuleSetTrait trait = s.expectTrait(EndpointRuleSetTrait.class);
            validateRuleSetAwsBuiltIns(events, s, trait.getEndpointRuleSet().getParameters());
        }

        for (ServiceShape s : model.getServiceShapesWithTrait(EndpointBddTrait.class)) {
            validateRuleSetAwsBuiltIns(events, s, s.expectTrait(EndpointBddTrait.class).getParameters());
        }

        return events;
    }

    private void validateRuleSetAwsBuiltIns(List<ValidationEvent> events, ServiceShape s, Iterable<Parameter> params) {
        for (Parameter parameter : params) {
            if (parameter.isBuiltIn()) {
                validateBuiltIn(events, s, parameter.getBuiltIn().get(), parameter);
            }
        }
    }

    private void validateBuiltIn(List<ValidationEvent> events, ServiceShape s, String name, FromSourceLocation source) {
        if (ADDITIONAL_CONSIDERATION_BUILT_INS.contains(name)) {
            events.add(danger(s, source, String.format(ADDITIONAL_CONSIDERATION_MESSAGE, name), name));
        }
    }
}
