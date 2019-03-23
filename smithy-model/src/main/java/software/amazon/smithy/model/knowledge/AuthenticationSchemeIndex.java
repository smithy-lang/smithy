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

package software.amazon.smithy.model.knowledge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.AuthenticationSchemesTrait;
import software.amazon.smithy.model.traits.AuthenticationTrait;
import software.amazon.smithy.model.traits.ProtocolsTrait;

/**
 * Computes the effective authentication schemes of services and operations
 * bound to services and the list of operations compatible with each protocols
 * trait protocol of a service.
 */
public final class AuthenticationSchemeIndex implements KnowledgeIndex {

    /** The list of all supported schemes of every service shape. */
    private final Map<ShapeId, List<String>> supportedServiceSchemes = new HashMap<>();

    /** The explicitly set authentication schemes of services and operations. */
    private final Map<ShapeId, List<String>> explicitSchemeMappings = new HashMap<>();

    /**
     * The mapping of service shapes to protocol names to the list of
     * authentication schemes supported by that protocol.
     */
    private final Map<ShapeId, Map<String, List<String>>> serviceProtocolSchemes = new HashMap<>();

    /**
     * @param model Model to compute the index from.
     */
    public AuthenticationSchemeIndex(Model model) {
        var index = model.getKnowledge(TopDownIndex.class);
        model.getShapeIndex().shapes(ServiceShape.class).forEach(service -> {
            supportedServiceSchemes.put(service.getId(), copyAllServiceSchemes(service));
            copyAuthenticationSchemes(service)
                    .ifPresent(schemes -> explicitSchemeMappings.put(service.getId(), schemes));

            // Compute the supported protocol authentication schemes of each service.
            service.getTrait(ProtocolsTrait.class).ifPresent(protocols -> {
                Map<String, List<String>> protocolSchemes = new HashMap<>();
                serviceProtocolSchemes.put(service.getId(), protocolSchemes);
                protocols.getProtocols().forEach((protocolName, protocol) -> {
                    protocolSchemes.put(protocolName, List.copyOf(protocol.getAuthentication()));
                });
            });

            index.getContainedOperations(service)
                    .forEach(operation -> copyAuthenticationSchemes(operation)
                            .ifPresent(schemes -> explicitSchemeMappings.put(operation.getId(), schemes)));
        });
    }

    private static List<String> copyAllServiceSchemes(ServiceShape service) {
        return service.getTrait(AuthenticationTrait.class)
                .map(AuthenticationTrait::getAuthenticationSchemes)
                .map(Map::keySet)
                .map(List::copyOf)
                .orElse(List.of());
    }

    private static Optional<List<String>> copyAuthenticationSchemes(Shape shape) {
        return shape.getTrait(AuthenticationSchemesTrait.class)
                .map(AuthenticationSchemesTrait::getValues)
                .map(List::copyOf);
    }

    /**
     * Gets the ordered list of all of the supported schemes of a service.
     * shape based on the {@code authentication} trait.
     *
     * <p>If the service has the authenticationSchemes trait, then the result
     * will be ordered based on that trait value where the schemes listed in
     * the trait value appear first, followed by the remaining schemes
     * defined in the {@code authentication} trait. This done to help ensure
     * that implementations honor the explicit ordering defined in the model.
     *
     * @param service Service shape to check.
     * @return Returns the list of all supported authentication schemes.
     */
    public List<String> getSupportedServiceSchemes(ToShapeId service) {
        var supportedSchemes = supportedServiceSchemes.getOrDefault(service.toShapeId(), List.of());
        var sorted = explicitSchemeMappings.get(service.toShapeId());
        if (sorted == null) {
            return supportedSchemes;
        }

        List<String> result = new ArrayList<>(supportedSchemes.size());
        // Prevents adding values to the list that were errantly set in the
        // authenticationSchemes trait but not in the authentication trait.
        for (var scheme : sorted) {
            if (supportedSchemes.contains(scheme)) {
                result.add(scheme);
            }
        }

        // Add any remaining schemes to the end of the list.
        for (var scheme : supportedSchemes) {
            if (!result.contains(scheme)) {
                result.add(scheme);
            }
        }

        return result;
    }

    /**
     * Gets the list of ordered authentication schemes supported by a specific
     * protocol of a service.
     *
     * <p>If the protocol does not define a preferred list of schemes, then
     * all of the schemes supported by the service are returned.
     *
     * @param service Service to check.
     * @param protocolName Protocol name to check.
     * @return Returns the supported schemes of this protocol or an empty list.
     */
    public List<String> getSupportedServiceSchemes(ToShapeId service, String protocolName) {
        var explicitSchemes = serviceProtocolSchemes.getOrDefault(service.toShapeId(), Map.of())
                .getOrDefault(protocolName, List.of());
        return explicitSchemes.isEmpty() ? getSupportedServiceSchemes(service) : explicitSchemes;
    }

    /**
     * Gets the default authentication schemes that are applied to every
     * operation in the closure of a service.
     *
     * <p>If the service has the {@code authenticationSchemes} trait, then
     * the value of that trait is returned from this method. Otherwise, the
     * return value is the entire list of schemes supported by the service
     * based on the {@code authentication} trait.
     *
     * <p>This method will return an empty list if the shape cannot be found,
     * isn't a valid service shape, or doesn't have any explicit schemes
     * configured.
     *
     * @param shapeId Service shape ID.
     * @return Returns the default authentication schemes of a service.
     */
    public List<String> getDefaultServiceSchemes(ToShapeId shapeId) {
        return Optional.ofNullable(explicitSchemeMappings.get(shapeId.toShapeId()))
                .orElseGet(() -> getSupportedServiceSchemes(shapeId.toShapeId()));
    }

    /**
     * Gets the effective authentication schemes of an operation bound
     * to a service.
     *
     * <p>The computed result is based on either the presence of the
     * {@code authenticationSchemes} trait applied to the operation, the
     * {@code authenticationSchemes} trait applied to the service it's bound
     * to, or the entire list of {@code authentication} schemes supported by
     * the service it's bound to.
     *
     * <p>This method will return an empty List in the case of invalid or
     * missing shapes.
     *
     * @param service Service that the operation is bound to.
     * @param operation Operation to check.
     * @return Returns the computed authentication schemes.
     */
    public List<String> getOperationSchemes(ToShapeId service, ToShapeId operation) {
        return Optional.ofNullable(explicitSchemeMappings.get(operation.toShapeId()))
                .map(Collections::unmodifiableList)
                .orElseGet(() -> getDefaultServiceSchemes(service));
    }

    /**
     * Gets the effective authentication schemes of an operation bound
     * to a service that are compatible with a specific protocol of the
     * service.
     *
     * <p>The result is ordered by either the effective authenticationSchemes
     * trait of the operation or service, or the protocols trait if an
     * authenticationSchemes trait is not found.
     *
     * @param service Service that the operation is bound to.
     * @param operation Operation to check.
     * @param protocolName Protocol to limit the result.
     * @return Returns the computed authentication schemes compatible with the protocol.
     */
    public List<String> getOperationSchemes(ToShapeId service, ToShapeId operation, String protocolName) {
        // Get the set of possible schemes that can be used with the protocol.
        // The result has to intersect with this set.
        var serviceSchemes = getSupportedServiceSchemes(service, protocolName);
        var operationSchemes = getOperationSchemes(service, operation);
        // Sort by the explicitly defined authenticationSchemes trait if present.
        // Otherwise, sort by the schemes defined on the service.
        return explicitSchemeMappings.containsKey(operation.toShapeId())
               ? intersection(operationSchemes, serviceSchemes)
               : intersection(serviceSchemes, operationSchemes);
    }

    private static List<String> intersection(List<String> left, List<String> right) {
        var leftCopy = new ArrayList<>(left);
        leftCopy.retainAll(right);
        return leftCopy;
    }

    /**
     * Checks if the given operation or service's resolved authentication
     * schemes is compatible with a service shape's authentication schemes
     * in the context of the loaded model.
     *
     * <p>If the given operation/service has the {@code authenticationSchemes}
     * trait, then this method ensures that the trait contains at least one of
     * the entries listed in the list of {@code authentication} trait schemes
     * of the service defined in the {@code authentication} trait.
     *
     * @param service Service shape to check.
     * @param operationOrService Operation or service shape to check.
     * @return Returns true if an authentication scheme is shared between the shapes.
     */
    public boolean isCompatibleWithService(ToShapeId service, ToShapeId operationOrService) {
        return isContaining(getOperationSchemes(service, operationOrService), getSupportedServiceSchemes(service));
    }

    private static boolean isContaining(Collection<?> needle, Collection<?> haystack) {
        return needle.stream().anyMatch(haystack::contains);
    }

    /**
     * Checks if the given operation's authentication schemes is compatible
     * with a service shape's authentication schemes for a specific protocol
     * in the context of the loaded model.
     *
     * <p>A protocol with an empty list of authentication schemes is always
     * considered a match as long as at least one of the authentication
     * schemes resolved for the operation is present in the
     * {@code authentication} schemes trait defined on the service.
     *
     * @param service Service shape to check.
     * @param operation Operation shape to check.
     * @param protocolName Protocol name to check against.
     * @return Returns true if an authentication scheme is shared between the shapes.
     */
    public boolean isCompatibleWithService(ToShapeId service, ToShapeId operation, String protocolName) {
        if (!isCompatibleWithService(service, operation)) {
            return false;
        }

        var protocolSchemes = getSupportedServiceSchemes(service, protocolName);
        return protocolSchemes.isEmpty() || isContaining(getOperationSchemes(service, operation), protocolSchemes);
    }
}
