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
import software.amazon.smithy.model.SourceLocation;
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

    private static final Identifier DISABLE_DOUBLE_ENCODING = Identifier.of("disableDoubleEncoding");
    private static final Identifier DISABLE_NORMALIZE_PATH = Identifier.of("disableNormalizePath");

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
                SourceLocation sourceLocation,
                BiFunction<FromSourceLocation, String, ValidationEvent> emitter
        ) {
            Identifier signingRegion = Identifier.of(SIGNING_REGION);
            Identifier signingName = Identifier.of(SIGNING_NAME);
            List<ValidationEvent> events = noExtraProperties(emitter, sourceLocation, authScheme,
                    ListUtils.of(RuleSetAuthSchemesValidator.NAME,
                            signingName, signingRegion, DISABLE_DOUBLE_ENCODING, DISABLE_NORMALIZE_PATH));
            validatePropertyType(emitter, authScheme,
                    signingName, Literal::asStringLiteral).ifPresent(events::add);
            validatePropertyType(emitter, authScheme,
                    signingRegion, Literal::asStringLiteral).ifPresent(events::add);
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
                SourceLocation sourceLocation,
                BiFunction<FromSourceLocation, String, ValidationEvent> emitter
        ) {
            Identifier signingRegionSet = Identifier.of(SIGNING_REGION_SET);
            Identifier signingName = Identifier.of(SIGNING_NAME);
            List<ValidationEvent> events = noExtraProperties(emitter, sourceLocation, authScheme,
                    ListUtils.of(RuleSetAuthSchemesValidator.NAME,
                            signingName, signingRegionSet, DISABLE_DOUBLE_ENCODING, DISABLE_NORMALIZE_PATH));
            validatePropertyType(emitter, authScheme,
                    signingName, Literal::asStringLiteral).ifPresent(events::add);
            validatePropertyType(emitter, authScheme,
                    signingRegionSet, Literal::asTupleLiteral).ifPresent(events::add);
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
                SourceLocation sourceLocation,
                BiFunction<FromSourceLocation, String, ValidationEvent> emitter
        ) {
            Identifier signingName = Identifier.of(SIGNING_NAME);
            List<ValidationEvent> events = hasAllKeys(emitter, authScheme,
                    ListUtils.of(RuleSetAuthSchemesValidator.NAME, signingName), sourceLocation);
            validatePropertyType(emitter, authScheme, signingName, Literal::asStringLiteral).ifPresent(events::add);
            return events;
        }

        private List<ValidationEvent> hasAllKeys(
                BiFunction<FromSourceLocation, String, ValidationEvent> emitter,
                Map<Identifier, Literal> authScheme,
                List<Identifier> requiredKeys,
                SourceLocation sourceLocation
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
            SourceLocation sourceLocation,
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

    private static <U> Optional<ValidationEvent> validatePropertyType(
            BiFunction<FromSourceLocation, String, ValidationEvent> emitter,
            Map<Identifier, Literal> properties,
            Identifier propertyName,
            Function<Literal, Optional<U>> validator
    ) {
        Literal value = properties.get(propertyName);
        if (value == null) {
            return Optional.of(emitter.apply(propertyName,
                    String.format("Expected auth property `%s` but didn't find one", propertyName)));
        }

        if (!validator.apply(value).isPresent()) {
            return Optional.of(emitter.apply(value,
                    String.format("Unexpected type for auth property `%s`, found: `%s`", propertyName, value)));
        }
        return Optional.empty();
    }
}
