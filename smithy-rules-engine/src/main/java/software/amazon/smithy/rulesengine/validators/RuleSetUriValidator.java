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
import software.amazon.smithy.rulesengine.language.Endpoint;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Template;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.StringLiteral;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.language.syntax.rule.TreeRule;
import software.amazon.smithy.rulesengine.logic.bdd.EndpointBddTrait;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Validate that URIs start with a scheme.
 */
@SmithyUnstableApi
public final class RuleSetUriValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (ServiceShape serviceShape : model.getServiceShapes()) {
            visitRuleset(events, serviceShape, serviceShape.getTrait(EndpointRuleSetTrait.class).orElse(null));
            visitBdd(events, serviceShape, serviceShape.getTrait(EndpointBddTrait.class).orElse(null));
        }
        return events;
    }

    private void visitRuleset(List<ValidationEvent> events, ServiceShape serviceShape, EndpointRuleSetTrait trait) {
        if (trait != null) {
            for (Rule rule : trait.getEndpointRuleSet().getRules()) {
                traverse(events, serviceShape, rule);
            }
        }
    }

    private void visitBdd(List<ValidationEvent> events, ServiceShape serviceShape, EndpointBddTrait trait) {
        if (trait != null) {
            for (Rule result : trait.getResults()) {
                if (result instanceof EndpointRule) {
                    visitEndpoint(events, serviceShape, (EndpointRule) result);
                }
            }
        }
    }

    private void traverse(List<ValidationEvent> events, ServiceShape service, Rule rule) {
        if (rule instanceof EndpointRule) {
            visitEndpoint(events, service, (EndpointRule) rule);
        } else if (rule instanceof TreeRule) {
            TreeRule treeRule = (TreeRule) rule;
            for (Rule child : treeRule.getRules()) {
                traverse(events, service, child);
            }
        }
    }

    private void visitEndpoint(List<ValidationEvent> events, ServiceShape serviceShape, EndpointRule endpointRule) {
        Endpoint endpoint = endpointRule.getEndpoint();
        Expression url = endpoint.getUrl();
        if (url instanceof StringLiteral) {
            StringLiteral s = (StringLiteral) url;
            visitTemplate(events, serviceShape, s.value());
        }
    }

    private void visitTemplate(List<ValidationEvent> events, ServiceShape serviceShape, Template template) {
        Template.Part part = template.getParts().get(0);
        if (part instanceof Template.Literal) {
            String scheme = ((Template.Literal) part).getValue();
            if (!(scheme.startsWith("http://") || scheme.startsWith("https://"))) {
                events.add(error(serviceShape,
                        template,
                        "URI should start with `http://` or `https://` but the URI started with " + scheme));
            }
        }
    }
}
