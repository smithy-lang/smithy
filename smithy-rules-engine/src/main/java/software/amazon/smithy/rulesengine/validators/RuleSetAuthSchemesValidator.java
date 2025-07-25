/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.validators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.rulesengine.language.Endpoint;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.language.syntax.rule.TreeRule;
import software.amazon.smithy.rulesengine.traits.BddTrait;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;

/**
 * Validator which verifies an endpoint with an authSchemes property conforms to a strict schema.
 */
public final class RuleSetAuthSchemesValidator extends AbstractValidator {
    public static final Identifier NAME = Identifier.of("name");

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
            for (Rule rule : trait.getEndpointRuleSet().getRules()) {
                traverse(events, serviceShape, rule);
            }
        }
    }

    private void visitBdd(List<ValidationEvent> events, ServiceShape serviceShape, BddTrait trait) {
        if (trait != null) {
            for (Rule result : trait.getBdd().getResults()) {
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

    private void visitEndpoint(List<ValidationEvent> events, ServiceShape service, EndpointRule endpointRule) {
        Endpoint endpoint = endpointRule.getEndpoint();
        Literal authSchemes = endpoint.getProperties().get(Identifier.of("authSchemes"));

        if (authSchemes != null) {
            List<Literal> authSchemeList = authSchemes.asTupleLiteral().orElse(null);
            if (authSchemeList == null) {
                events.add(error(service,
                        authSchemes,
                        String.format(
                                "Expected `authSchemes` to be a list, found: `%s`",
                                authSchemes)));
                return;
            }

            Set<String> authSchemeNames = new HashSet<>();
            Set<String> duplicateAuthSchemeNames = new HashSet<>();
            for (Literal authSchemeEntry : authSchemeList) {
                Map<Identifier, Literal> authSchemeMap = authSchemeEntry.asRecordLiteral().orElse(null);
                if (authSchemeMap == null) {
                    events.add(error(service,
                            authSchemes,
                            String.format(
                                    "Expected `authSchemes` to be a list of objects, but found: `%s`",
                                    authSchemeEntry)));
                    continue;
                }

                String schemeName = validateAuthSchemeName(events, service, authSchemeMap, authSchemeEntry);
                if (schemeName != null) {
                    if (!authSchemeNames.add(schemeName)) {
                        duplicateAuthSchemeNames.add(schemeName);
                    }
                    validateAuthScheme(events, service, schemeName, authSchemeMap, authSchemeEntry);
                }
            }

            // Emit events for each duplicated auth scheme name.
            for (String duplicateAuthSchemeName : duplicateAuthSchemeNames) {
                events.add(error(service,
                        authSchemes,
                        String.format(
                                "Found duplicate `name` of `%s` in the `authSchemes` list",
                                duplicateAuthSchemeName)));
            }
        }
    }

    private String validateAuthSchemeName(
            List<ValidationEvent> events,
            ServiceShape service,
            Map<Identifier, Literal> authScheme,
            FromSourceLocation sourceLocation
    ) {
        Literal nameLiteral = authScheme.get(NAME);
        if (nameLiteral == null) {
            events.add(error(service,
                    sourceLocation,
                    String.format(
                            "Expected `authSchemes` to have a `name` key with a string value but it did not: `%s`",
                            authScheme)));
            return null;
        }

        String name = nameLiteral.asStringLiteral().map(s -> s.expectLiteral()).orElse(null);
        if (name == null) {
            events.add(error(service,
                    sourceLocation,
                    String.format(
                            "Expected `authSchemes` to have a `name` key with a string value but it did not: `%s`",
                            authScheme)));
            return null;
        }

        return name;
    }

    private void validateAuthScheme(
            List<ValidationEvent> events,
            ServiceShape service,
            String schemeName,
            Map<Identifier, Literal> authScheme,
            FromSourceLocation sourceLocation
    ) {
        boolean validatedAuth = false;
        for (AuthSchemeValidator authSchemeValidator : EndpointRuleSet.getAuthSchemeValidators()) {
            if (authSchemeValidator.test(schemeName)) {
                events.addAll(authSchemeValidator.validateScheme(authScheme,
                        sourceLocation,
                        (location, message) -> error(service, location, message)));
                validatedAuth = true;
            }
        }

        if (!validatedAuth) {
            events.add(warning(service,
                    String.format(
                            "Did not find a validator for the `%s` auth scheme",
                            schemeName)));
        }
    }
}
