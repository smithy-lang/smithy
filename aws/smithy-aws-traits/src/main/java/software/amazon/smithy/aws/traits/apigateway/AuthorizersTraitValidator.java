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

package software.amazon.smithy.aws.traits.apigateway;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.traits.ProtocolsTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.utils.SetUtils;

/**
 * Each authorizers must match one of the authentication schemes on the service.
 */
public class AuthorizersTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        return model.getShapeIndex().shapes(ServiceShape.class)
                .flatMap(this::validateService)
                .collect(Collectors.toList());
    }

    private Stream<ValidationEvent> validateService(ServiceShape service) {
        Set<String> schemeNames = service.getTrait(ProtocolsTrait.class)
                .map(ProtocolsTrait::getAllAuthSchemes)
                .orElse(SetUtils.of());
        Set<String> authorizerNames = service.getTrait(AuthorizersTrait.class)
                .map(AuthorizersTrait::getAllAuthorizers)
                .map(map -> new HashSet<>(map.keySet()))
                .orElseGet(HashSet::new);

        authorizerNames.removeAll(schemeNames);
        return authorizerNames.stream().map(name -> error(service, String.format(
                "Invalid `%s` entry `%s` does not match one of the `protocols` trait `auth` schemes defined "
                + "on the service: [%s]",
                AuthorizersTrait.NAME, name, ValidationUtils.tickedList(schemeNames))));
    }
}
