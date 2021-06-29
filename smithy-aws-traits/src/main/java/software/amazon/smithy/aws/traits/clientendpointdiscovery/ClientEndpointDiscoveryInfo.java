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
