/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.aws.validators;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.rulesengine.aws.language.functions.AwsBuiltIns;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
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
        for (ServiceShape serviceShape : model.getServiceShapesWithTrait(EndpointRuleSetTrait.class)) {
            events.addAll(validateRuleSetAwsBuiltIns(serviceShape, serviceShape.expectTrait(EndpointRuleSetTrait.class)
                    .getEndpointRuleSet()));
        }
        return events;
    }

    private List<ValidationEvent> validateRuleSetAwsBuiltIns(ServiceShape serviceShape, EndpointRuleSet ruleSet) {
        List<ValidationEvent> events = new ArrayList<>();
        for (Parameter parameter : ruleSet.getParameters()) {
            if (parameter.isBuiltIn()) {
                validateBuiltIn(serviceShape, parameter.getBuiltIn().get(), parameter).ifPresent(events::add);
            }
        }
        return events;
    }

    private Optional<ValidationEvent> validateBuiltIn(
            ServiceShape serviceShape,
            String builtInName,
            FromSourceLocation source
    ) {
        if (ADDITIONAL_CONSIDERATION_BUILT_INS.contains(builtInName)) {
            return Optional.of(danger(
                    serviceShape, source,
                    String.format(ADDITIONAL_CONSIDERATION_MESSAGE, builtInName),
                    builtInName));
        }
        return Optional.empty();
    }
}
