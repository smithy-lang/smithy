/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.validators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Model;
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
                    return Stream.of(emitter.apply(authSchemes,
                            String.format("Expected `authSchemes` to be a list, found: `%s`", authSchemes)));
                }

                Set<String> authSchemeNames = new HashSet<>();
                Set<String> duplicateAuthSchemeNames = new HashSet<>();
                for (Literal authSchemeEntry : authSchemeList.get()) {
                    Optional<Map<Identifier, Literal>> authSchemeMap = authSchemeEntry.asRecordLiteral();
                    if (authSchemeMap.isPresent()) {
                        // Validate the name property so that we can also check that they're unique.
                        Map<Identifier, Literal> authScheme = authSchemeMap.get();
                        Optional<ValidationEvent> event = validateAuthSchemeName(authScheme, authSchemeEntry);
                        if (event.isPresent()) {
                            events.add(event.get());
                            continue;
                        }
                        String schemeName = authScheme.get(NAME).asStringLiteral().get().expectLiteral();
                        if (!authSchemeNames.add(schemeName)) {
                            duplicateAuthSchemeNames.add(schemeName);
                        }

                        events.addAll(validateAuthScheme(schemeName, authScheme, authSchemeEntry));
                    } else {
                        events.add(emitter.apply(authSchemes,
                                String.format("Expected `authSchemes` to be a list of objects, but found: `%s`",
                                        authSchemeEntry)));
                    }
                }

                // Emit events for each duplicated auth scheme name.
                for (String duplicateAuthSchemeName : duplicateAuthSchemeNames) {
                    events.add(emitter.apply(authSchemes,
                            String.format("Found duplicate `name` of `%s` in the "
                                    + "`authSchemes` list", duplicateAuthSchemeName)));
                }
            }

            return events.stream();
        }

        private Optional<ValidationEvent> validateAuthSchemeName(
                Map<Identifier, Literal> authScheme,
                FromSourceLocation sourceLocation
        ) {
            if (!authScheme.containsKey(NAME) || !authScheme.get(NAME).asStringLiteral().isPresent()) {
                return Optional.of(error(serviceShape,
                        sourceLocation,
                        String.format("Expected `authSchemes` to have a `name` key with a string value but it did not: "
                                + "`%s`", authScheme)));
            }
            return Optional.empty();
        }

        private List<ValidationEvent> validateAuthScheme(
                String schemeName,
                Map<Identifier, Literal> authScheme,
                FromSourceLocation sourceLocation
        ) {
            List<ValidationEvent> events = new ArrayList<>();

            BiFunction<FromSourceLocation, String, ValidationEvent> emitter = getEventEmitter();

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
            return ListUtils.of(warning(serviceShape,
                    String.format("Did not find a validator for the `%s` "
                            + "auth scheme", schemeName)));
        }

        private BiFunction<FromSourceLocation, String, ValidationEvent> getEventEmitter() {
            return (sourceLocation, message) -> error(serviceShape, sourceLocation, message);
        }
    }
}
