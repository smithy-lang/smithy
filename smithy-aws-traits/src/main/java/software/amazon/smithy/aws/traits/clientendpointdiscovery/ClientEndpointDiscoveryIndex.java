/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.clientendpointdiscovery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.KnowledgeIndex;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.ToShapeId;

public final class ClientEndpointDiscoveryIndex implements KnowledgeIndex {
    private final Map<ShapeId, Map<ShapeId, ClientEndpointDiscoveryInfo>> endpointDiscoveryInfo = new HashMap<>();

    public ClientEndpointDiscoveryIndex(Model model) {
        TopDownIndex topDownIndex = TopDownIndex.of(model);
        OperationIndex opIndex = OperationIndex.of(model);

        for (ServiceShape service : model.getServiceShapesWithTrait(ClientEndpointDiscoveryTrait.class)) {
            ClientEndpointDiscoveryTrait trait = service.expectTrait(ClientEndpointDiscoveryTrait.class);
            ShapeId endpointOperationId = trait.getOperation();
            Optional<ShapeId> endpointErrorId = trait.getOptionalError();

            Optional<OperationShape> endpointOperation = model.getShape(endpointOperationId)
                    .flatMap(Shape::asOperationShape);
            Optional<StructureShape> endpointError = endpointErrorId
                    .flatMap(model::getShape)
                    .flatMap(Shape::asStructureShape);

            if (endpointOperation.isPresent()) {
                Map<ShapeId, ClientEndpointDiscoveryInfo> serviceInfo = getOperations(
                        service,
                        endpointOperation.get(),
                        endpointError.orElse(null),
                        topDownIndex,
                        opIndex);
                if (!serviceInfo.isEmpty()) {
                    endpointDiscoveryInfo.put(service.getId(), serviceInfo);
                }
            }
        }
    }

    public static ClientEndpointDiscoveryIndex of(Model model) {
        return model.getKnowledge(ClientEndpointDiscoveryIndex.class, ClientEndpointDiscoveryIndex::new);
    }

    private Map<ShapeId, ClientEndpointDiscoveryInfo> getOperations(
            ServiceShape service,
            OperationShape endpointOperation,
            StructureShape endpointError,
            TopDownIndex topDownIndex,
            OperationIndex opIndex
    ) {
        Map<ShapeId, ClientEndpointDiscoveryInfo> result = new HashMap<>();
        for (OperationShape operation : topDownIndex.getContainedOperations(service)) {
            operation.getTrait(ClientDiscoveredEndpointTrait.class).ifPresent(trait -> {
                List<MemberShape> discoveryIds = getDiscoveryIds(opIndex, operation);
                ClientEndpointDiscoveryInfo info = new ClientEndpointDiscoveryInfo(
                        service,
                        operation,
                        endpointOperation,
                        endpointError,
                        discoveryIds,
                        trait.isRequired());
                result.put(operation.getId(), info);
            });
        }
        return result;
    }

    private List<MemberShape> getDiscoveryIds(OperationIndex opIndex, OperationShape operation) {
        List<MemberShape> discoveryIds = new ArrayList<>();
        for (MemberShape member : opIndex.expectInputShape(operation).getAllMembers().values()) {
            if (member.hasTrait(ClientEndpointDiscoveryIdTrait.class)) {
                discoveryIds.add(member);
            }
        }
        return discoveryIds;
    }

    public Optional<ClientEndpointDiscoveryInfo> getEndpointDiscoveryInfo(ToShapeId service, ToShapeId operation) {
        return Optional.ofNullable(endpointDiscoveryInfo.get(service.toShapeId()))
                .flatMap(mappings -> Optional.ofNullable(mappings.get(operation.toShapeId())));
    }

    public Set<ShapeId> getEndpointDiscoveryOperations(ToShapeId service) {
        return Optional.ofNullable(endpointDiscoveryInfo.get(service.toShapeId()))
                .flatMap(mappings -> Optional.of(mappings.keySet()))
                .orElse(new HashSet<>());
    }
}
