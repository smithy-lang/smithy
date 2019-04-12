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
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.AuthTrait;
import software.amazon.smithy.model.traits.Protocol;
import software.amazon.smithy.model.traits.ProtocolsTrait;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.OptionalUtils;

/**
 * Computes the effective authentication schemes of an operation
 * for specific protocols.
 */
public final class AuthIndex implements KnowledgeIndex {
    private final ShapeIndex index;

    /**
     * @param model Model to compute the index from.
     */
    public AuthIndex(Model model) {
        this.index = model.getShapeIndex();
    }

    /**
     * Gets the default authentication schemes supported by a service or
     * the entire list of all auth schemes if no auth trait is set on the
     * service shape in priority order.
     *
     * @param service Service to check.
     * @return Returns the list of auth schemes or an empty list if not found.
     */
    public List<String> getDefaultServiceSchemes(ToShapeId service) {
        return index.getShape(service.toShapeId())
                .flatMap(serviceShape -> OptionalUtils.or(serviceShape.getTrait(AuthTrait.class)
                        .map(AuthTrait::getValues),
                        () -> serviceShape.getTrait(ProtocolsTrait.class)
                                .map(ProtocolsTrait::getAllAuthSchemes)
                                .map(ListUtils::copyOf)))
                .orElse(ListUtils.of());
    }

    /**
     * Gets the effective authentication schemes of an operation bound
     * to a service in priority order.
     *
     * <p>The computed result is based on either the presence of the
     * {@code auth} trait applied to the operation, the {@code auth} trait
     * applied to the service it's bound to, or the entire list of
     * {@code auth} schemes supported by the protocols trait of the service.
     *
     * <p>This method will return an empty List in the case of invalid or
     * missing shapes.
     *
     * @param service Service that the operation is bound to.
     * @param operation Operation to check.
     * @return Returns the computed authentication schemes.
     */
    public List<String> getOperationSchemes(ToShapeId service, ToShapeId operation) {
        return index.getShape(operation.toShapeId())
                // Get the auth trait from the operation or the service.
                .map(shape -> shape.getTrait(AuthTrait.class)
                        .map(AuthTrait::getValues)
                        .orElseGet(() -> getDefaultServiceSchemes(service)))
                .orElse(ListUtils.of());
    }

    /**
     * Gets the effective authentication schemes of an operation bound
     * to a service that are compatible with a specific protocol of the
     * service in priority order.
     *
     * <p>The result is ordered by either the effective auth trait of the
     * operation or the list of protocols on the service.
     *
     * @param service Service that the operation is bound to.
     * @param operation Operation to check.
     * @param protocolName Protocol to limit the result.
     * @return Returns the computed authentication schemes compatible with the protocol.
     */
    public List<String> getOperationSchemes(ToShapeId service, ToShapeId operation, String protocolName) {
        // Get the authentication schemes of the protocol.
        List<String> protocolSchemes = index.getShape(service.toShapeId())
                .flatMap(serviceShape -> serviceShape.getTrait(ProtocolsTrait.class))
                .flatMap(protocolsTrait -> protocolsTrait.getProtocol(protocolName))
                .map(Protocol::getAuth)
                .orElse(ListUtils.of());
        // Get the schemes of the operation or service.
        List<String> schemes = getOperationSchemes(service, operation);
        return schemes.isEmpty()
               // Use the protocol schemes if the operation/service define no schemes.
               ? protocolSchemes
               // Return the intersection, preferring the sort order of the found auth trait.
               : intersection(schemes, protocolSchemes);
    }

    private static List<String> intersection(List<String> left, List<String> right) {
        List<String> leftCopy = new ArrayList<>(left);

        // The "none" scheme is special as it doesn't have to be explicitly
        // defined in the protocols list.
        if (left.contains(ProtocolsTrait.NONE_AUTH) && !right.contains(ProtocolsTrait.NONE_AUTH)) {
            List<String> copiedRight = new ArrayList<>(right);
            copiedRight.add(ProtocolsTrait.NONE_AUTH);
            leftCopy.retainAll(copiedRight);
        } else {
            leftCopy.retainAll(right);
        }

        return leftCopy;
    }
}
