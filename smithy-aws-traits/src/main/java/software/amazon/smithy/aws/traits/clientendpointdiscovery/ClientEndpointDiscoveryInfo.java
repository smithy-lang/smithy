/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.clientendpointdiscovery;

import java.util.List;
import java.util.Optional;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.StructureShape;

public final class ClientEndpointDiscoveryInfo {

    private final ServiceShape service;
    private final OperationShape operation;
    private final OperationShape discoveryOperation;
    private final StructureShape error;
    private final List<MemberShape> discoveryIds;
    private final boolean required;

    ClientEndpointDiscoveryInfo(
            ServiceShape service,
            OperationShape operation,
            OperationShape discoveryOperation,
            StructureShape error,
            List<MemberShape> discoveryIds,
            boolean required
    ) {
        this.service = service;
        this.operation = operation;
        this.discoveryOperation = discoveryOperation;
        this.error = error;
        this.discoveryIds = discoveryIds;
        this.required = required;
    }

    public ServiceShape getService() {
        return service;
    }

    public OperationShape getOperation() {
        return operation;
    }

    public OperationShape getDiscoveryOperation() {
        return discoveryOperation;
    }

    /**
     * Deprecated in favor of {@link ClientEndpointDiscoveryInfo#getOptionalError()}.
     */
    @Deprecated
    public StructureShape getError() {
        return error;
    }

    public Optional<StructureShape> getOptionalError() {
        return Optional.ofNullable(error);
    }

    public List<MemberShape> getDiscoveryIds() {
        return discoveryIds;
    }

    public boolean isRequired() {
        return required;
    }
}
