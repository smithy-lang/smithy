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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.AuthTrait;
import software.amazon.smithy.model.traits.Protocol;
import software.amazon.smithy.model.traits.ProtocolsTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.utils.ListUtils;

/**
 * Validates that service shapes and every operation bound within a service
 * marked with the auth trait correspond to an auth scheme defined in
 * one of the protocols of the service (with the exception of "none").
 * This will also validate that every protocols trait uses a unique
 * set of protocol names.
 *
 * <p>For example, if an operation is bound to a service and has an auth trait
 * with a value of "http-basic", but no protocol on the service supports the
 * "http-basic" auth scheme, then this validator will emit an ERROR.
 */
public final class AuthValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        TopDownIndex topDownIndex = model.getKnowledge(TopDownIndex.class);
        return model.getShapeIndex().shapes(ServiceShape.class)
                .flatMap(service -> validateService(topDownIndex, service).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateService(
            TopDownIndex topDownIndex,
            ServiceShape service
    ) {
        Optional<ProtocolsTrait> protocols = service.getTrait(ProtocolsTrait.class);
        if (protocols.isEmpty()) {
            return ListUtils.of();
        }

        List<ValidationEvent> result = new ArrayList<>();
        // Validate the schemes on the service itself.
        service.getTrait(AuthTrait.class).stream()
                .flatMap(trait -> validateSchemes(protocols.get(), service, service, trait).stream())
                .forEach(result::add);
        // Validate the schemes of each operation bound within the service.
        topDownIndex.getContainedOperations(service).stream()
                .flatMap(operation -> operation.getTrait(AuthTrait.class).stream()
                        .flatMap(trait -> validateSchemes(protocols.get(), service, operation, trait).stream()))
                .forEach(result::add);
        // Validate for unique protocol names.
        validateUniqueNames(service, protocols.get()).ifPresent(result::add);

        return result;
    }

    private Optional<ValidationEvent> validateSchemes(
            ProtocolsTrait protocols,
            ServiceShape service,
            Shape shape,
            AuthTrait trait
    ) {
        // Validates that the authentication schemes resolved for an operation or
        // service are listed in the authentication schemes supported by the
        // service protocols to which it is bound.
        Set<String> serviceSchemes = protocols.getAllAuthSchemes();
        Set<String> copiedSchemes = new HashSet<>(trait.getValues());
        copiedSchemes.removeAll(serviceSchemes);

        // Remove the "none" scheme since it does not need to be explicitly
        // listed in any protocol.
        copiedSchemes.remove(ProtocolsTrait.NONE_AUTH);

        if (copiedSchemes.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(error(shape, trait, String.format(
                "The following `auth` trait values are not compatible with the `auth` schemes listed in the "
                + "`protocols` trait of the service, `%s`: [%s]. This service supports the following "
                + "authentication schemes: [%s]",
                service.getId(),
                ValidationUtils.tickedList(copiedSchemes),
                ValidationUtils.tickedList(serviceSchemes))));
    }

    private Optional<ValidationEvent> validateUniqueNames(ServiceShape shape, ProtocolsTrait protocols) {
        // Check for protocols with conflicting names.
        List<String> result = protocols.getProtocols().stream()
                .collect(Collectors.groupingBy(Protocol::getName))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());

        if (result.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(error(shape, protocols, String.format(
                "`protocols` trait contains conflicting protocol names: %s", result)));
    }
}
