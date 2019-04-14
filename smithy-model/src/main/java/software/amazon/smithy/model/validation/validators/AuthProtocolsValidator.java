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

package software.amazon.smithy.model.validation.validators;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.traits.AuthTrait;
import software.amazon.smithy.model.traits.Protocol;
import software.amazon.smithy.model.traits.ProtocolsTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.OptionalUtils;

/**
 * Validates that each operation in the closure of a service resolves to a
 * set of authentication schemes that is compatible with at least 1
 * authentication scheme of each protocol trait protocol listed on the
 * service to which it is bound (if the service defines protocols, and
 * only for protocols that list explicitly supported authentication schemes).
 */
public final class AuthProtocolsValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        TopDownIndex topDownIndex = model.getKnowledge(TopDownIndex.class);
        return model.getShapeIndex().shapes(ServiceShape.class)
                .flatMap(service -> validateOperationAgainstProtocols(topDownIndex, service))
                .collect(Collectors.toList());
    }

    private Stream<ValidationEvent> validateOperationAgainstProtocols(TopDownIndex index, ServiceShape service) {
        ProtocolsTrait protocolsTrait = service.getTrait(ProtocolsTrait.class).orElse(null);
        if (protocolsTrait == null) {
            return Stream.empty();
        }

        // Ensure that every operation is valid for this protocol.
        return protocolsTrait.getProtocols().stream()
                .flatMap(protocol -> index.getContainedOperations(service).stream()
                        .flatMap(operation -> OptionalUtils.stream(
                                validateOperationSchemesAgainstProtocols(service, operation, protocol))));
    }

    private Optional<ValidationEvent> validateOperationSchemesAgainstProtocols(
            ServiceShape service,
            OperationShape operation,
            Protocol protocol
    ) {
        // Either the operation or the service has an "auth" trait.
        AuthTrait authTrait = OptionalUtils.or(operation.getTrait(AuthTrait.class),
                () -> service.getTrait(AuthTrait.class)).orElse(null);

        // If no auth trait was found, then assume the operation is
        // compatible with all auth schemes.
        if (authTrait == null) {
            return Optional.empty();
        }

        // Check if any of the schemes on the operation or service are also
        // supported by the protocol.
        List<String> supportedSchemes = protocol.getAuth();
        List<String> values = authTrait.getValues();

        // Each protocols trait is assumed to support "none".
        if (values.contains(ProtocolsTrait.NONE_AUTH) || values.stream().anyMatch(supportedSchemes::contains)) {
            return Optional.empty();
        }

        return Optional.of(warning(operation, String.format(
                "The `auth` trait resolved for this operation, %s, is not compatible with the `%s` "
                + "protocol of the `%s` service: %s",
                authTrait.getValues(), protocol.getName(), service.getId(), protocol.getAuth())));
    }
}
