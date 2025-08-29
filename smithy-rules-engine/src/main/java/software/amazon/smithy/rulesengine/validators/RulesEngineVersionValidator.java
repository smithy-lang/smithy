/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.validators;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.RulesVersion;
import software.amazon.smithy.rulesengine.language.syntax.SyntaxElement;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.ErrorRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.language.syntax.rule.TreeRule;
import software.amazon.smithy.rulesengine.traits.EndpointBddTrait;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;

/**
 * Validates that the rules engine version of a trait only uses compatible features.
 */
public final class RulesEngineVersionValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();

        for (ServiceShape s : model.getServiceShapesWithTrait(EndpointBddTrait.class)) {
            validateBdd(events, s, s.expectTrait(EndpointBddTrait.class));
        }

        for (ServiceShape s : model.getServiceShapesWithTrait(EndpointRuleSetTrait.class)) {
            validateTree(events, s, s.expectTrait(EndpointRuleSetTrait.class));
        }

        return events;
    }

    private void validateBdd(List<ValidationEvent> events, ServiceShape service, EndpointBddTrait trait) {
        RulesVersion version = trait.getVersion();

        for (Condition condition : trait.getConditions()) {
            validateSyntaxElement(events, service, condition, version);
        }

        for (Rule result : trait.getResults()) {
            validateRule(events, service, result, version);
        }
    }

    private void validateTree(List<ValidationEvent> events, ServiceShape service, EndpointRuleSetTrait trait) {
        EndpointRuleSet rules = trait.getEndpointRuleSet();
        RulesVersion version = rules.getRulesVersion();
        for (Rule rule : rules.getRules()) {
            validateRule(events, service, rule, version);
        }
    }

    private void validateRule(List<ValidationEvent> events, ServiceShape service, Rule rule, RulesVersion version) {
        for (Condition condition : rule.getConditions()) {
            validateSyntaxElement(events, service, condition, version);
            validateSyntaxElement(events, service, condition.getFunction(), version);
            for (Expression arg : condition.getFunction().getArguments()) {
                validateSyntaxElement(events, service, arg, version);
            }
        }

        if (rule instanceof TreeRule) {
            for (Rule nestedRule : ((TreeRule) rule).getRules()) {
                validateRule(events, service, nestedRule, version);
            }
        } else if (rule instanceof EndpointRule) {
            EndpointRule endpointRule = (EndpointRule) rule;
            validateSyntaxElement(events, service, endpointRule.getEndpoint().getUrl(), version);
            for (List<Expression> headerValues : endpointRule.getEndpoint().getHeaders().values()) {
                for (Expression expr : headerValues) {
                    validateSyntaxElement(events, service, expr, version);
                }
            }
        } else if (rule instanceof ErrorRule) {
            validateSyntaxElement(events, service, ((ErrorRule) rule).getError(), version);
        }
    }

    private void validateSyntaxElement(
            List<ValidationEvent> events,
            ServiceShape service,
            SyntaxElement element,
            RulesVersion declaredVersion
    ) {
        RulesVersion requiredVersion = element.availableSince();

        if (!declaredVersion.isAtLeast(requiredVersion)) {
            SourceLocation s = element instanceof FromSourceLocation
                    ? ((FromSourceLocation) element).getSourceLocation()
                    : element.toExpression().getSourceLocation();
            String msg = String.format(
                    "%s requires rules engine version >= %s, but ruleset declares version %s",
                    element.getClass().getSimpleName(),
                    requiredVersion,
                    declaredVersion);
            events.add(error(service, s, msg));
        }
    }
}
