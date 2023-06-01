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
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.rulesengine.language.Endpoint;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.rulesengine.language.visitors.TraversingVisitor;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;
import software.amazon.smithy.utils.ListUtils;

/**
 * Validator which verifies an endpoint with an authSchemes property conforms to a strict schema.
 */
public final class RuleSetAuthSchemesValidator extends AbstractValidator {
    private static final Identifier DISABLE_DOUBLE_ENCODING = Identifier.of("disableDoubleEncoding");
    private static final Identifier SIGNING_NAME = Identifier.of("signingName");
    private static final Identifier SIGNING_REGION = Identifier.of("signingRegion");
    private static final Identifier SIGNING_REGION_SET = Identifier.of("signingRegionSet");
    private static final Identifier NAME = Identifier.of("name");

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
                Optional<List<Literal>> authSchemeList = authSchemes.asTupleLiteral();
                if (!authSchemeList.isPresent()) {
                    return Stream.of(invalid(authSchemes.getSourceLocation(),
                            String.format("Expected `authSchemes` to be a list, found: `%s`", authSchemes)));
                } else {
                    for (Literal authScheme : authSchemeList.get()) {
                        Optional<Map<Identifier, Literal>> authSchemeMap = authScheme.asRecordLiteral();
                        if (authSchemeMap.isPresent()) {
                            events.addAll(validateAuthScheme(authSchemeMap.get(), authScheme.getSourceLocation()));
                        } else {
                            events.add(invalid(authSchemes.getSourceLocation(),
                                    String.format("Expected `authSchemes` to be a list of objects, but found: `%s`",
                                            authScheme)));
                        }
                    }
                }
            }

            return events.stream();
        }

        private List<ValidationEvent> validateAuthScheme(
                Map<Identifier, Literal> authScheme,
                SourceLocation sourceLocation
        ) {
            if (!authScheme.containsKey(NAME)) {
                return ListUtils.of(invalid(sourceLocation,
                        String.format("Expected `authSchemes` to have a `name` key but it did not: `%s`", authScheme)));
            }
            Literal name = authScheme.get(NAME);
            String schemeName = name.asStringLiteral().get().expectLiteral();
            if (schemeName.equals("sigv4")) {
                return validateSigv4(authScheme, sourceLocation);
            } else if (schemeName.equals("sigv4a")) {
                return validateSigv4a(authScheme, sourceLocation);
            } else if (schemeName.startsWith("beta-")) {
                return validateBeta(authScheme, sourceLocation);
            } else {
                return ListUtils.of(invalid(name, String.format("Unexpected auth scheme: `%s`", schemeName)));
            }
        }

        private List<ValidationEvent> validateSigv4a(
                Map<Identifier, Literal> authScheme,
                SourceLocation sourceLocation
        ) {
            List<ValidationEvent> events = noExtraProperties(authScheme,
                    ListUtils.of(SIGNING_NAME, SIGNING_REGION_SET, NAME, DISABLE_DOUBLE_ENCODING), sourceLocation);
            validatePropertyType(authScheme, SIGNING_NAME, Literal::asStringLiteral).ifPresent(events::add);
            validatePropertyType(authScheme, SIGNING_REGION_SET, Literal::asTupleLiteral).ifPresent(events::add);
            return events;
        }

        private List<ValidationEvent> validateSigv4(
                Map<Identifier, Literal> authScheme,
                SourceLocation sourceLocation
        ) {
            List<ValidationEvent> events = noExtraProperties(authScheme,
                    ListUtils.of(SIGNING_NAME, SIGNING_REGION, NAME, DISABLE_DOUBLE_ENCODING), sourceLocation);
            validatePropertyType(authScheme, SIGNING_NAME, Literal::asStringLiteral).ifPresent(events::add);
            validatePropertyType(authScheme, SIGNING_REGION, Literal::asStringLiteral).ifPresent(events::add);
            return events;
        }

        private List<ValidationEvent> validateBeta(
                Map<Identifier, Literal> authScheme,
                SourceLocation sourceLocation
        ) {
            List<ValidationEvent> events = hasAllKeys(authScheme,
                    ListUtils.of(SIGNING_NAME, NAME), sourceLocation);
            validatePropertyType(authScheme, SIGNING_NAME, Literal::asStringLiteral).ifPresent(events::add);
            return events;
        }

        private List<ValidationEvent> hasAllKeys(
                Map<Identifier, Literal> map,
                List<Identifier> keys,
                SourceLocation sourceLocation
        ) {
            List<ValidationEvent> events = new ArrayList<>();
            for (Identifier key : keys) {
                if (!map.containsKey(key)) {
                    invalid(sourceLocation, String.format("Missing key: `%s`", key));
                }
            }
            return events;
        }

        private List<ValidationEvent> noExtraProperties(
                Map<Identifier, Literal> properties,
                List<Identifier> allowedProperties,
                SourceLocation sourceLocation
        ) {
            List<ValidationEvent> events = new ArrayList<>();
            for (Identifier propertyName : properties.keySet()) {
                if (!allowedProperties.contains(propertyName)) {
                    events.add(invalid(sourceLocation, String.format("Unexpected key: `%s` (valid keys: %s)",
                            propertyName, allowedProperties)));
                }
            }
            return events;
        }

        private <U> Optional<ValidationEvent> validatePropertyType(
                Map<Identifier, Literal> properties,
                Identifier propertyName,
                Function<Literal, Optional<U>> validator
        ) {
            Literal value = properties.get(propertyName);
            if (value == null) {
                return Optional.of(invalid(propertyName,
                        String.format("Expected auth property `%s` but didn't find one", propertyName)));
            }

            if (!validator.apply(value).isPresent()) {
                return Optional.of(invalid(value,
                        String.format("Unexpected type for auth property `%s`, found: `%s`", propertyName, value)));
            }
            return Optional.empty();
        }

        private ValidationEvent invalid(FromSourceLocation source, String message) {
            return error(serviceShape, source, message);
        }
    }
}
