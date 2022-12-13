/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.aws.apigateway.traits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Validates if authorizers traits are well-defined.
 */
@SmithyInternalApi
public final class AuthorizersTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        return model.shapes(ServiceShape.class)
                .map(service -> validate(model, service))
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validate(Model model, ServiceShape service) {
        Map<String, AuthorizerDefinition> authorizers = service.getTrait(AuthorizersTrait.class)
                .map(AuthorizersTrait::getAuthorizers)
                .orElseGet(HashMap::new);

        List<ValidationEvent> validationEvents = new ArrayList<>();

        Optional<ValidationEvent> authSchemaValidation =
                validateAuthSchema(authorizers, model, service);
        authSchemaValidation.ifPresent(validationEvents::add);

        Optional<ValidationEvent> enableSimpleResponsesValidation =
                validateEnableSimpleResponsesConfig(authorizers, service);
        enableSimpleResponsesValidation.ifPresent(validationEvents::add);

        return validationEvents;
    }

    /**
     * Each authorizer resolved within a service must use a scheme that
     * matches one of the schemes of the protocols of the service.
     */
    private Optional<ValidationEvent> validateAuthSchema(Map<String, AuthorizerDefinition> authorizers,
                                                         Model model,
                                                         ServiceShape service) {
        Set<ShapeId> authSchemes = ServiceIndex.of(model).getAuthSchemes(service).keySet();

        String invalidMappings = authorizers.entrySet().stream()
                .filter(entry -> !authSchemes.contains(entry.getValue().getScheme()))
                .map(entry -> entry.getKey() + " -> " + entry.getValue().getScheme())
                .sorted()
                .collect(Collectors.joining(", "));

        if (invalidMappings.isEmpty()) {
            return Optional.empty();
        }

        AuthorizersTrait authorizersTrait = service.getTrait(AuthorizersTrait.class).get();
        return Optional.of(error(service, authorizersTrait, String.format(
                "Each `scheme` of the `%s` trait must target one of the auth schemes applied to the service "
                + "(i.e., [%s]). The following mappings of authorizer names to schemes are invalid: %s",
                AuthorizersTrait.ID,
                ValidationUtils.tickedList(authSchemes),
                invalidMappings)));
    }

    /**
     * Each authorizer with the enableSimpleResponses member defined
     * should have the authorizedPayloadFormatVersion member set to 2.0.
     */
    private Optional<ValidationEvent> validateEnableSimpleResponsesConfig(Map<String, AuthorizerDefinition> authorizers,
                                                                          ServiceShape service) {
        String invalidConfigs = authorizers.entrySet().stream()
                .filter(entry -> entry.getValue().getEnableSimpleResponses().isPresent())
                .filter(entry -> entry.getValue().getAuthorizerPayloadFormatVersion().isPresent())
                .filter(entry -> !entry.getValue().getAuthorizerPayloadFormatVersion().get().equals("2.0"))
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.joining(", "));

        if (invalidConfigs.isEmpty()) {
            return Optional.empty();
        }

        AuthorizersTrait authorizersTrait = service.getTrait(AuthorizersTrait.class).get();
        return Optional.of(error(service, authorizersTrait, String.format(
                "The enableSimpleResponses member of %s is only supported when authorizedPayloadFormatVersion "
                + "is 2.0. The following authorizers are misconfigured: %s",
                AuthorizersTrait.ID,
                invalidConfigs
        )));
    }

}
