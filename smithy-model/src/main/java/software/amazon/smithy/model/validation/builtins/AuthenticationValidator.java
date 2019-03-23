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

package software.amazon.smithy.model.validation.builtins;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.AuthenticationSchemeIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.AuthenticationSchemesTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;

/**
 * Validates that if a service or resource has an authenticationSchemes
 * trait, then each value in that trait is present in the authentication
 * trait of the service to which it is bound.
 */
public final class AuthenticationValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        var topDownIndex = model.getKnowledge(TopDownIndex.class);
        var authIndex = model.getKnowledge(AuthenticationSchemeIndex.class);
        return model.getShapeIndex().shapes(ServiceShape.class)
                .flatMap(service -> validateService(topDownIndex, authIndex, service))
                .collect(Collectors.toList());
    }

    private Stream<ValidationEvent> validateService(
            TopDownIndex topDownIndex,
            AuthenticationSchemeIndex authIndex,
            ServiceShape service
    ) {
        return Stream.concat(
                // Validate the schemes on the service itself.
                service.getTrait(AuthenticationSchemesTrait.class).stream()
                        .flatMap(trait -> validateSchemes(authIndex, service, service, trait).stream()),
               // Validate the schemes of each operation bound within the service.
               topDownIndex.getContainedOperations(service).stream()
                       .flatMap(operation -> operation.getTrait(AuthenticationSchemesTrait.class).stream()
                               .flatMap(trait -> validateSchemes(authIndex, service, operation, trait).stream()))
        );
    }

    private Optional<ValidationEvent> validateSchemes(
            AuthenticationSchemeIndex authIndex,
            ServiceShape service,
            Shape shape,
            AuthenticationSchemesTrait trait
    ) {
        // Validates that the authentication schemes resolved for an operation or
        // service are listed in the authentication schemes supported by the
        // service to which it is bound.
        var serviceSchemes = authIndex.getSupportedServiceSchemes(service);
        var copiedSchemes = new HashSet<>(trait.getValues());
        copiedSchemes.removeAll(serviceSchemes);

        if (copiedSchemes.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(error(shape, trait, String.format(
                "The following `authenticationSchemes` trait values resolved for this shape are not compatible "
                + "with the `authentication` schemes trait of the service, `%s`: [%s]. This service supports the "
                + "following authentication schemes: [%s]",
                service.getId(),
                ValidationUtils.tickedList(copiedSchemes),
                ValidationUtils.tickedList(serviceSchemes))));
    }
}
