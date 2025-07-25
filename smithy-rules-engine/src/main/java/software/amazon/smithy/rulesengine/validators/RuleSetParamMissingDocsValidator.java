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
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.traits.BddTrait;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;

/**
 * Validator to ensure that all parameters have documentation (in BDD and ruleset).
 */
public final class RuleSetParamMissingDocsValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (ServiceShape serviceShape : model.getServiceShapes()) {
            visitRuleset(events, serviceShape, serviceShape.getTrait(EndpointRuleSetTrait.class).orElse(null));
            visitBdd(events, serviceShape, serviceShape.getTrait(BddTrait.class).orElse(null));
        }
        return events;
    }

    private void visitRuleset(List<ValidationEvent> events, ServiceShape serviceShape, EndpointRuleSetTrait trait) {
        if (trait != null) {
            visitParams(events, serviceShape, trait.getEndpointRuleSet().getParameters());
        }
    }

    private void visitBdd(List<ValidationEvent> events, ServiceShape serviceShape, BddTrait trait) {
        if (trait != null) {
            visitParams(events, serviceShape, trait.getBdd().getParameters());
        }
    }

    public void visitParams(List<ValidationEvent> events, ServiceShape serviceShape, Iterable<Parameter> parameters) {
        for (Parameter parameter : parameters) {
            if (!parameter.getDocumentation().isPresent()) {
                events.add(warning(serviceShape,
                        parameter,
                        String.format("Parameter `%s` does not have documentation", parameter.getName())));
            }
        }
    }
}
