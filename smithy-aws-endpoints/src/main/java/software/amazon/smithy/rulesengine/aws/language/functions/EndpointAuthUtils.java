/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.aws.language.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.rulesengine.language.Endpoint;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.rulesengine.validators.AuthSchemeValidator;
import software.amazon.smithy.rulesengine.validators.RuleSetAuthSchemesValidator;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;

/**
 * Utilities for constructing and validating AWS-specific authentication components for rule-sets.
 */
public final class EndpointAuthUtils {
    private static final String SIGV_4 = "sigv4";
    private static final String SIG_V4A = "sigv4a";
    private static final String SIGNING_NAME = "signingName";
    private static final String SIGNING_REGION = "signingRegion";
    private static final String SIGNING_REGION_SET = "signingRegionSet";

    private static final Identifier ID_SIGNING_NAME = Identifier.of("signingName");
    private static final Identifier ID_SIGNING_REGION = Identifier.of("signingRegion");
    private static final Identifier ID_SIGNING_REGION_SET = Identifier.of("signingRegionSet");
    private static final Identifier ID_DISABLE_DOUBLE_ENCODING = Identifier.of("disableDoubleEncoding");
    private static final Identifier ID_DISABLE_NORMALIZE_PATH = Identifier.of("disableNormalizePath");

    private EndpointAuthUtils() {}

    /**
     * Adds SigV4 authentication to the provided endpoint builder.
     *
     * @param builder the endpoint builder to augment.
     * @param signingRegion the signing region to use when signing.
     * @param signingService the name of the service to sign with.
     * @return the updated endpoint builder.
     */
    public static Endpoint.Builder sigv4(Endpoint.Builder builder, Literal signingRegion, Literal signingService) {
        return builder.addAuthScheme(SIGV_4, MapUtils.of(
                SIGNING_NAME, signingService,
                SIGNING_REGION, signingRegion));
    }

    /**
     * Adds SigV4a authentication to the provided endpoint builder.
     *
     * @param builder the endpoint builder to augment.
     * @param signingRegionSet the set of signing regions to use when signing.
     * @param signingService the name of the service to sign with.
     * @return the updated endpoint builder.
     */
    public static Endpoint.Builder sigv4a(
            Endpoint.Builder builder,
            List<Literal> signingRegionSet,
            Literal signingService
    ) {
        return builder.addAuthScheme(SIG_V4A, MapUtils.of(
                SIGNING_NAME, signingService,
                SIGNING_REGION_SET, Literal.tupleLiteral(signingRegionSet)));
    }

    static final class SigV4SchemeValidator implements AuthSchemeValidator {
        SigV4SchemeValidator() {}

        @Override
        public boolean test(String name) {
            return name.equals("sigv4");
        }

        @Override
        public List<ValidationEvent> validateScheme(
                Map<Identifier, Literal> authScheme,
                FromSourceLocation sourceLocation,
                BiFunction<FromSourceLocation, String, ValidationEvent> emitter
        ) {
            List<ValidationEvent> events = noExtraProperties(emitter, sourceLocation, authScheme,
                    ListUtils.of(RuleSetAuthSchemesValidator.NAME,
                            ID_SIGNING_NAME,
                            ID_SIGNING_REGION,
                            ID_DISABLE_DOUBLE_ENCODING,
                            ID_DISABLE_NORMALIZE_PATH));

            // Validate shared Sigv4 properties.
            events.addAll(SigV4SchemeValidator.validateOptionalSharedProperties(authScheme, emitter));
            // Signing region is also optional.
            if (authScheme.containsKey(ID_SIGNING_REGION)) {
                validateStringProperty(emitter, authScheme, ID_SIGNING_REGION).ifPresent(events::add);
            }
            return events;
        }

        private static List<ValidationEvent> validateOptionalSharedProperties(
                Map<Identifier, Literal> authScheme,
                BiFunction<FromSourceLocation, String, ValidationEvent> emitter
        ) {
            List<ValidationEvent> events = new ArrayList<>();
            // The following properties are only type checked if present.
            if (authScheme.containsKey(ID_SIGNING_NAME)) {
                validateStringProperty(emitter, authScheme, ID_SIGNING_NAME).ifPresent(events::add);
            }
            if (authScheme.containsKey(ID_DISABLE_DOUBLE_ENCODING)) {
                validateBooleanProperty(emitter, authScheme, ID_DISABLE_DOUBLE_ENCODING).ifPresent(events::add);
            }
            if (authScheme.containsKey(ID_DISABLE_NORMALIZE_PATH)) {
                validateBooleanProperty(emitter, authScheme, ID_DISABLE_NORMALIZE_PATH).ifPresent(events::add);
            }
            return events;
        }
    }

    static final class SigV4aSchemeValidator implements AuthSchemeValidator {
        SigV4aSchemeValidator() {}

        @Override
        public boolean test(String name) {
            return name.equals("sigv4a");
        }

        @Override
        public List<ValidationEvent> validateScheme(
                Map<Identifier, Literal> authScheme,
                FromSourceLocation sourceLocation,
                BiFunction<FromSourceLocation, String, ValidationEvent> emitter
        ) {
            List<ValidationEvent> events = noExtraProperties(emitter, sourceLocation, authScheme,
                    ListUtils.of(RuleSetAuthSchemesValidator.NAME,
                            ID_SIGNING_NAME,
                            ID_SIGNING_REGION_SET,
                            ID_DISABLE_DOUBLE_ENCODING,
                            ID_DISABLE_NORMALIZE_PATH));

            // The `signingRegionSet` property will always be present.
            Optional<ValidationEvent> event = validatePropertyType(emitter, authScheme.get(ID_SIGNING_REGION_SET),
                    ID_SIGNING_REGION_SET, Literal::asTupleLiteral, "an array<string>");
            // If we don't have a tuple, that's our main error.
            // Otherwise, validate each entry is a string.
            if (event.isPresent()) {
                events.add(event.get());
            } else {
                List<Literal> signingRegionSet = authScheme.get(ID_SIGNING_REGION_SET).asTupleLiteral().get();
                if (signingRegionSet.isEmpty()) {
                    events.add(emitter.apply(authScheme.get(ID_SIGNING_REGION_SET),
                            "The `signingRegionSet` property must not be an empty list."));
                } else {
                    for (Literal signingRegion : signingRegionSet) {
                        validatePropertyType(emitter, signingRegion, Identifier.of("signingRegionSet.Value"),
                                Literal::asStringLiteral, "a string").ifPresent(events::add);
                    }
                }
            }

            // Validate shared Sigv4 properties.
            events.addAll(SigV4SchemeValidator.validateOptionalSharedProperties(authScheme, emitter));
            return events;
        }
    }

    static final class BetaSchemeValidator implements AuthSchemeValidator {
        BetaSchemeValidator() {}

        @Override
        public boolean test(String name) {
            return name.startsWith("beta-");
        }

        @Override
        public List<ValidationEvent> validateScheme(
                Map<Identifier, Literal> authScheme,
                FromSourceLocation sourceLocation,
                BiFunction<FromSourceLocation, String, ValidationEvent> emitter
        ) {
            List<ValidationEvent> events = hasAllKeys(emitter, authScheme,
                    ListUtils.of(RuleSetAuthSchemesValidator.NAME, ID_SIGNING_NAME), sourceLocation);
            validateStringProperty(emitter, authScheme, ID_SIGNING_NAME).ifPresent(events::add);
            return events;
        }

        private List<ValidationEvent> hasAllKeys(
                BiFunction<FromSourceLocation, String, ValidationEvent> emitter,
                Map<Identifier, Literal> authScheme,
                List<Identifier> requiredKeys,
                FromSourceLocation sourceLocation
        ) {
            List<ValidationEvent> events = new ArrayList<>();
            for (Identifier key : requiredKeys) {
                if (!authScheme.containsKey(key)) {
                    emitter.apply(sourceLocation, String.format("Missing key: `%s`", key));
                }
            }
            return events;
        }
    }

    private static List<ValidationEvent> noExtraProperties(
            BiFunction<FromSourceLocation, String, ValidationEvent> emitter,
            FromSourceLocation sourceLocation,
            Map<Identifier, Literal> properties,
            List<Identifier> allowedProperties
    ) {
        List<ValidationEvent> events = new ArrayList<>();
        for (Identifier propertyName : properties.keySet()) {
            if (!allowedProperties.contains(propertyName)) {
                events.add(emitter.apply(sourceLocation, String.format("Unexpected key: `%s` (valid keys: %s)",
                        propertyName, allowedProperties)));
            }
        }
        return events;
    }

    private static Optional<ValidationEvent> validateBooleanProperty(
            BiFunction<FromSourceLocation, String, ValidationEvent> emitter,
            Map<Identifier, Literal> properties,
            Identifier propertyName
    ) {
        return validatePropertyType(emitter, properties.get(propertyName), propertyName,
                Literal::asBooleanLiteral, "a boolean");
    }

    private static Optional<ValidationEvent> validateStringProperty(
            BiFunction<FromSourceLocation, String, ValidationEvent> emitter,
            Map<Identifier, Literal> properties,
            Identifier propertyName
    ) {
        return validatePropertyType(emitter, properties.get(propertyName), propertyName,
                Literal::asStringLiteral, "a string");
    }

    private static <U> Optional<ValidationEvent> validatePropertyType(
            BiFunction<FromSourceLocation, String, ValidationEvent> emitter,
            Literal value,
            Identifier propertyName,
            Function<Literal, Optional<U>> validator,
            String expectedType
    ) {
        if (value == null) {
            return Optional.of(emitter.apply(propertyName,
                    String.format("Expected auth property `%s` of %s type but didn't find one",
                            propertyName, expectedType)));
        }

        if (!validator.apply(value).isPresent()) {
            return Optional.of(emitter.apply(value,
                    String.format("Unexpected type for auth property `%s`, found `%s` but expected %s value",
                            propertyName, value, expectedType)));
        }
        return Optional.empty();
    }
}
