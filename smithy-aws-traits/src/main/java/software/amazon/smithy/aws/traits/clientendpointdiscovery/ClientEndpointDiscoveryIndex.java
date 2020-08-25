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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.Pair;

public final class ClientEndpointDiscoveryIndex implements KnowledgeIndex {
    private final Map<ShapeId, Map<ShapeId, ClientEndpointDiscoveryInfo>> endpointDiscoveryInfo = new HashMap<>();

    public ClientEndpointDiscoveryIndex(Model model) {
        TopDownIndex topDownIndex = TopDownIndex.of(model);
        OperationIndex opIndex = OperationIndex.of(model);

        model.shapes(ServiceShape.class)
                .flatMap(service -> Trait.flatMapStream(service, ClientEndpointDiscoveryTrait.class))
                .forEach(servicePair -> {
                    ServiceShape service = servicePair.getLeft();
                    ShapeId endpointOperationId = servicePair.getRight().getOperation();
                    ShapeId endpointErrorId = servicePair.getRight().getError();

                    Optional<OperationShape> endpointOperation = model.getShape(endpointOperationId)
                            .flatMap(Shape::asOperationShape);
                    Optional<StructureShape> endpointError = model.getShape(endpointErrorId)
                            .flatMap(Shape::asStructureShape);

                    if (endpointOperation.isPresent() && endpointError.isPresent()) {
                        Map<ShapeId, ClientEndpointDiscoveryInfo> serviceInfo = getOperations(
                                service, endpointOperation.get(), endpointError.get(), topDownIndex, opIndex);
                        if (!serviceInfo.isEmpty()) {
                            endpointDiscoveryInfo.put(service.getId(), serviceInfo);
                        }
                    }
                });
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
        return topDownIndex.getContainedOperations(service).stream()
                .flatMap(operation -> Trait.flatMapStream(operation, ClientDiscoveredEndpointTrait.class))
                .map(pair -> {
                    OperationShape operation = pair.getLeft();
                    List<MemberShape> discoveryIds = getDiscoveryIds(opIndex, operation);
                    ClientEndpointDiscoveryInfo info = new ClientEndpointDiscoveryInfo(
                            service,
                            operation,
                            endpointOperation,
                            endpointError,
                            discoveryIds,
                            pair.getRight().isRequired()
                    );
                    return Pair.of(operation.getId(), info);
                })
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }

    private List<MemberShape> getDiscoveryIds(OperationIndex opIndex, OperationShape operation) {
        List<MemberShape> discoveryIds = new ArrayList<>();
        opIndex.getInput(operation).ifPresent(input -> input.getAllMembers().values().stream()
                .filter(member -> member.hasTrait(ClientEndpointDiscoveryIdTrait.class))
                .forEach(discoveryIds::add));
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
