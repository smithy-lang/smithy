/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.rulesengine.traits;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.error.RuleError;
import software.amazon.smithy.rulesengine.validators.StandaloneRulesetValidator;
import software.amazon.smithy.rulesengine.validators.ValidationError;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Validates that the Endpoints 2.0 {@link EndpointRuleSetTrait} are correct and
 * do not match any prohibited patterns.
 */
@SmithyInternalApi
public final class EndpointRuleSetTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (ServiceShape service : model.getServiceShapesWithTrait(EndpointRuleSetTrait.class)) {
            validateService(service, service.expectTrait(EndpointRuleSetTrait.class)).ifPresent(events::add);
        }
        return events;
    }

    /**
     * Validates an Endpoint rule set.
     *
     * @param ruleset Endpoint rule set to validate.
     * @throws IllegalArgumentException if the endpoint rule set is invalid.
     */
    public static void validateRuleSet(EndpointRuleSet ruleset) {
        List<ValidationError> messages = StandaloneRulesetValidator.validate(ruleset, null)
                .collect(Collectors.toList());

        if (!messages.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "Invalid Endpoint rule set: %s", messages.toString()));
        }
    }

    private Optional<ValidationEvent> validateService(ServiceShape service, EndpointRuleSetTrait trait) {
        try {
            EndpointRuleSet value = EndpointRuleSet.fromNode(trait.getRuleSet(), false);
            validateRuleSet(value);
            value.typecheck();
            return Optional.empty();
        } catch (IllegalArgumentException | RuleError e) {
            return Optional.of(error(service, trait, e.getMessage()));
        }
    }
}
