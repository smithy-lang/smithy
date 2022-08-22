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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.rulesengine.language.Endpoint;
import software.amazon.smithy.rulesengine.language.EndpointRuleset;
import software.amazon.smithy.rulesengine.language.lang.Identifier;
import software.amazon.smithy.rulesengine.language.lang.expr.Literal;
import software.amazon.smithy.rulesengine.language.visit.TraversingVisitor;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public final class AuthSchemesValidator {
    private static final Identifier DISABLE_DOUBLE_ENCODING = Identifier.of("disableDoubleEncoding");
    private static final Identifier SIGNING_NAME = Identifier.of("signingName");
    private static final Identifier SIGNING_REGION_SET = Identifier.of("signingRegionSet");
    private static final Identifier NAME = Identifier.of("name");

    private AuthSchemesValidator() {
    }

    public static Stream<ValidationError> validateRuleset(EndpointRuleset ruleset) {
        return new Validator().visitRuleset(ruleset);
    }

    private static class Validator extends TraversingVisitor<ValidationError> {
        public static final Identifier SIGNING_REGION = Identifier.of("signingRegion");

        @Override
        public Stream<ValidationError> visitEndpoint(Endpoint endpoint) {
            Literal authSchemes = endpoint.getProperties().get(Identifier.of("authSchemes"));
            if (authSchemes == null) {
                return Stream.empty();
            }
            Optional<List<Literal>> authSchemeList = authSchemes.asTuple();
            if (!authSchemeList.isPresent()) {
                return Stream.of(new ValidationError(ValidationErrorType.INVALID_AUTH_SCHEMES,
                        String.format("Expected authSchemes to be a list, found: %s", authSchemes),
                        authSchemes.getSourceLocation()));
            } else {
                return authSchemeList.get().stream().flatMap(authScheme -> {
                    Optional<Map<Identifier, Literal>> authSchemeMap = authScheme.asObject();
                    if (!authSchemeMap.isPresent()) {
                        return Stream.of(new ValidationError(ValidationErrorType.INVALID_AUTH_SCHEMES,
                                String.format("Expected authSchemes to be a list of objects, but found: %s",
                                        authScheme), authScheme.getSourceLocation()));
                    }
                    return validateAuthScheme(authSchemeMap.get(), authScheme.getSourceLocation());
                });
            }
        }

        private Stream<ValidationError> validateAuthScheme(
                Map<Identifier, Literal> authScheme,
                SourceLocation sourceLocation
        ) {
            if (!authScheme.containsKey(NAME)) {
                return Stream.of(new ValidationError(ValidationErrorType.INVALID_AUTH_SCHEMES,
                        String.format("Expected authSchemes to have a name key but it did not: `%s`", authScheme),
                        sourceLocation));
            }
            Literal name = authScheme.get(NAME);
            String schemeName = name.expectLiteralString();
            switch (schemeName) {
                case "sigv4":
                    return validateSigv4(authScheme, sourceLocation);
                case "sigv4a":
                    return validateSigv4a(authScheme, sourceLocation);
                default:
                    return Stream.of(new ValidationError(ValidationErrorType.INVALID_AUTH_SCHEMES,
                            String.format("unexpected auth scheme name: `%s`", schemeName), name.getSourceLocation()));
            }
        }

        private Stream<ValidationError> noExtraKeys(
                Map<Identifier, Literal> map,
                List<Identifier> keys,
                SourceLocation sourceLocation
        ) {
            return map.keySet().stream().flatMap(key -> {
                if (!keys.contains(key)) {
                    return Stream.of(new ValidationError(ValidationErrorType.INVALID_AUTH_SCHEMES,
                            String.format("Unexpected key: `%s` (valid keys: %s)", key, keys), sourceLocation));
                } else {
                    return Stream.empty();
                }
            });
        }

        private Stream<ValidationError> validateSigv4a(
                Map<Identifier, Literal> authScheme,
                SourceLocation sourceLocation
        ) {
            List<ValidationError> extraKeys = noExtraKeys(authScheme,
                    ListUtils.of(SIGNING_NAME, SIGNING_REGION_SET, NAME, DISABLE_DOUBLE_ENCODING), sourceLocation)
                    .collect(Collectors.toList());
            if (!extraKeys.isEmpty()) {
                return extraKeys.stream();
            }
            return Stream.concat(propertyIs(authScheme, SIGNING_NAME, Literal::asString),
                    propertyIs(authScheme, SIGNING_REGION_SET, Literal::asTuple));
        }

        private Stream<ValidationError> validateSigv4(
                Map<Identifier, Literal> authScheme,
                SourceLocation sourceLocation
        ) {
            List<ValidationError> extraKeys = noExtraKeys(authScheme,
                    ListUtils.of(SIGNING_NAME, SIGNING_REGION, NAME, DISABLE_DOUBLE_ENCODING), sourceLocation)
                    .collect(Collectors.toList());
            if (!extraKeys.isEmpty()) {
                return extraKeys.stream();
            }
            return Stream.concat(propertyIs(authScheme, SIGNING_NAME, Literal::asString),
                    propertyIs(authScheme, SIGNING_REGION, Literal::asString));
        }

        private <U> Stream<ValidationError> propertyIs(
                Map<Identifier, Literal> map,
                Identifier name,
                Function<Literal, Optional<U>> validator
        ) {
            Literal value = map.get(name);
            if (!validator.apply(value).isPresent()) {
                return Stream.of(new ValidationError(ValidationErrorType.INVALID_AUTH_SCHEMES,
                        String.format("unexpected value for %s, found: `%s`", name, value),
                        value.getSourceLocation()));
            }
            return Stream.empty();
        }
    }
}
