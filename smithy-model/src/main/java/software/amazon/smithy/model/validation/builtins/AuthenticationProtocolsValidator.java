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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.AuthenticationSchemeIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.traits.ProtocolsTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;

/**
 * Validates that each operation in the closure of a service resolves to a
 * set of authentication schemes that is compatible with at least 1
 * authentication scheme of each protocol trait protocol listed on the
 * service to which it is bound (if the service defines protocols, and
 * only for protocols that list explicitly supported authentication schemes).
 */
public final class AuthenticationProtocolsValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        var topDownIndex = model.getKnowledge(TopDownIndex.class);
        var authIndex = model.getKnowledge(AuthenticationSchemeIndex.class);
        return model.getShapeIndex().shapes(ServiceShape.class)
                .flatMap(service -> validateOperationSchemesAgainstProtocols(topDownIndex, authIndex, service))
                .collect(Collectors.toList());
    }

    private Stream<ValidationEvent> validateOperationSchemesAgainstProtocols(
            TopDownIndex index,
            AuthenticationSchemeIndex authIndex,
            ServiceShape service
    ) {
        var protocolsTrait = service.getTrait(ProtocolsTrait.class).orElse(null);
        if (protocolsTrait == null) {
            return Stream.empty();
        }

        return protocolsTrait.getProtocols().entrySet().stream()
                // Ignore protocols that don't define explicit schemes.
                .filter(entry -> !entry.getValue().getAuthentication().isEmpty())
                // Ensure that every operation is valid for this protocol.
                .flatMap(entry -> index.getContainedOperations(service).stream()
                        .flatMap(operation -> validateOperationSchemesAgainstProtocols(
                                authIndex, service, operation, entry.getKey()).stream()));
    }

    private Optional<ValidationEvent> validateOperationSchemesAgainstProtocols(
            AuthenticationSchemeIndex authIndex,
            ServiceShape service,
            OperationShape operation,
            String protocolName
    ) {
        if (authIndex.isCompatibleWithService(service, operation, protocolName)) {
            return Optional.empty();
        }

        return Optional.of(warning(operation, String.format(
                "The `authenticationSchemes` trait resolved for this operation, [%s], is not compatible with "
                + "the authentication schemes of the `%s` protocol of the `%s` service to which this operation "
                + "is bound: [%s]. The authentication scheme for this operation over the `%s` protocol is undefined.",
                ValidationUtils.tickedList(authIndex.getOperationSchemes(service, operation)),
                protocolName,
                service.getId(),
                ValidationUtils.tickedList(authIndex.getSupportedServiceSchemes(service, protocolName)),
                protocolName)));
    }
}
