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

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.utils.OptionalUtils;

/**
 * Each authorizer resolved within a service must use a scheme that
 * matches one of the schemes of the protocols of the service.
 */
public class AuthorizersTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        return model.shapes(ServiceShape.class)
                .flatMap(service -> OptionalUtils.stream(validateService(model, service)))
                .collect(Collectors.toList());
    }

    private Optional<ValidationEvent> validateService(Model model, ServiceShape service) {
        Set<ShapeId> authSchemes = model.getKnowledge(ServiceIndex.class).getAuthSchemes(service).keySet();

        // Create a comma separated string of authorizer names to schemes.
        String invalidMappings = service.getTrait(AuthorizersTrait.class)
                .map(AuthorizersTrait::getAllAuthorizers)
                .orElseGet(HashMap::new)
                .entrySet().stream()
                .filter(entry -> !authSchemes.contains(entry.getValue().getScheme()))
                .map(entry -> entry.getKey() + " -> " + entry.getValue().getScheme())
                .sorted()
                .collect(Collectors.joining(", "));

        if (invalidMappings.isEmpty()) {
            return Optional.empty();
        }

        AuthorizersTrait authorizersTrait = service.expectTrait(AuthorizersTrait.class);
        return Optional.of(error(service, authorizersTrait, String.format(
                "Each `scheme` of the `%s` trait must target one of the auth schemes applied to the service "
                + "(i.e., [%s]). The following mappings of authorizer names to schemes are invalid: %s",
                AuthorizersTrait.ID,
                ValidationUtils.tickedList(authSchemes),
                invalidMappings)));
    }
}
