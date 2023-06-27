/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.validators;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.rulesengine.language.Endpoint;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.TraversingVisitor;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;
import software.amazon.smithy.utils.ListUtils;

/**
 * Validator which verifies an endpoint with an authSchemes property conforms to a strict schema.
 */
public final class RuleSetAuthSchemesValidator extends AbstractValidator {
    public static final Identifier NAME = Identifier.of("name");

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (ServiceShape serviceShape : model.getServiceShapesWithTrait(EndpointRuleSetTrait.class)) {
            Validator validator = new Validator(serviceShape);
            events.addAll(validator.visitRuleset(
                    serviceShape.expectTrait(EndpointRuleSetTrait.class).getEndpointRuleSet())
                                  .collect(Collectors.toList()));
        }
        return events;
    }

    private class Validator extends TraversingVisitor<ValidationEvent> {
        private final ServiceShape serviceShape;

        Validator(ServiceShape serviceShape) {
            this.serviceShape = serviceShape;
        }

        @Override
        public Stream<ValidationEvent> visitEndpoint(Endpoint endpoint) {
            List<ValidationEvent> events = new ArrayList<>();

            Literal authSchemes = endpoint.getProperties().get(Identifier.of("authSchemes"));
            if (authSchemes != null) {
                BiFunction<FromSourceLocation, String, ValidationEvent> emitter = getEventEmitter();
                Optional<List<Literal>> authSchemeList = authSchemes.asTupleLiteral();
                if (!authSchemeList.isPresent()) {
                    return Stream.of(emitter.apply(authSchemes.getSourceLocation(),
                            String.format("Expected `authSchemes` to be a list, found: `%s`", authSchemes)));
                }
                for (Literal authScheme : authSchemeList.get()) {
                    Optional<Map<Identifier, Literal>> authSchemeMap = authScheme.asRecordLiteral();
                    if (authSchemeMap.isPresent()) {
                        events.addAll(validateAuthScheme(authSchemeMap.get(), authScheme.getSourceLocation()));
                    } else {
                        events.add(emitter.apply(authSchemes.getSourceLocation(),
                                String.format("Expected `authSchemes` to be a list of objects, but found: `%s`",
                                        authScheme)));
                    }
                }
            }

            return events.stream();
        }

        private List<ValidationEvent> validateAuthScheme(
                Map<Identifier, Literal> authScheme,
                SourceLocation sourceLocation
        ) {
            BiFunction<FromSourceLocation, String, ValidationEvent> emitter = getEventEmitter();
            if (!authScheme.containsKey(NAME)) {
                return ListUtils.of(emitter.apply(sourceLocation,
                        String.format("Expected `authSchemes` to have a `name` key but it did not: `%s`", authScheme)));
            }

            List<ValidationEvent> events = new ArrayList<>();
            Literal name = authScheme.get(NAME);
            String schemeName = name.asStringLiteral().get().expectLiteral();

            boolean validatedAuth = false;
            for (AuthSchemeValidator authSchemeValidator : EndpointRuleSet.getAuthSchemeValidators()) {
                if (authSchemeValidator.test(schemeName)) {
                    events.addAll(authSchemeValidator.validateScheme(authScheme, sourceLocation, emitter));
                    validatedAuth = true;
                }
            }

            if (validatedAuth) {
                return events;
            }
            return ListUtils.of(emitter.apply(name, String.format("Unexpected auth scheme: `%s`", schemeName)));
        }

        private BiFunction<FromSourceLocation, String, ValidationEvent> getEventEmitter() {
            return (sourceLocation, message) -> error(serviceShape, sourceLocation, message);
        }
    }
}
