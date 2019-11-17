/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.linters;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.traits.AuthTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidatorService;
import software.amazon.smithy.utils.ListUtils;

/**
 * Emits a validation event if any service auth schemes match those listed
 * in the configuration as deprecated.
 *
 * <p>This validator accepts the following optional configuration options:
 *
 * <ul>
 *     <li>schemes: ([string]) A list of deprecated auth scheme names.
 *     <li>reason: (string) Provides a plain text message for a deprecated auth scheme.
 * </ul>
 */
public final class DeprecatedAuthSchemesValidator extends AbstractValidator {
    private static final List<String> ATTRIBUTES = ListUtils.of("schemes", "reason");

    private final List<String> schemes;
    private final String reason;

    private DeprecatedAuthSchemesValidator(List<String> schemes, String reason) {
        this.schemes = schemes;
        this.reason = reason;
    }

    public static final class Provider extends ValidatorService.Provider {
        public Provider() {
            super(DeprecatedAuthSchemesValidator.class, configuration -> {
                configuration.warnIfAdditionalProperties(ATTRIBUTES);
                List<String> schemes = Node.loadArrayOfString("schemes", configuration.expectMember("schemes"));
                String reason = configuration.getStringMemberOrDefault("reason", "");
                return new DeprecatedAuthSchemesValidator(schemes, reason);
            });
        }
    }

    @Override
    public List<ValidationEvent> validate(Model model) {
        return model.shapes(ServiceShape.class)
                .flatMap(shape -> Trait.flatMapStream(shape, AuthTrait.class))
                .flatMap(pair -> validateAuthSchemes(pair.getLeft(), pair.getRight()))
                .collect(Collectors.toList());
    }

    private Stream<ValidationEvent> validateAuthSchemes(ServiceShape service, AuthTrait trait) {
        return trait.getValues().stream()
                .filter(schemes::contains)
                .map(scheme -> warning(service, trait.getSourceLocation(), String.format("The %s auth scheme is "
                        + "deprecated. %s", scheme, reason)));
    }
}
