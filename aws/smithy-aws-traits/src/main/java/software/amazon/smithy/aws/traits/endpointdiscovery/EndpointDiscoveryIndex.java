package software.amazon.smithy.aws.traits.endpointdiscovery;

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
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.Pair;

public final class EndpointDiscoveryIndex implements KnowledgeIndex {
    private final Map<ShapeId, Map<ShapeId, EndpointDiscoveryInfo>> endpointDiscoveryInfo = new HashMap<>();

    public EndpointDiscoveryIndex(Model model) {
        ShapeIndex index = model.getShapeIndex();
        TopDownIndex topDownIndex = model.getKnowledge(TopDownIndex.class);
        OperationIndex opIndex = model.getKnowledge(OperationIndex.class);

        index.shapes(ServiceShape.class)
                .flatMap(service -> Trait.flatMapStream(service, EndpointDiscoveryTrait.class))
                .forEach(servicePair -> {
                    ServiceShape service = servicePair.getLeft();
                    ShapeId endpointOperationId = servicePair.getRight().getOperation();
                    ShapeId endpointErrorId = servicePair.getRight().getError();

                    Optional<OperationShape> endpointOperation = index.getShape(endpointOperationId)
                            .flatMap(Shape::asOperationShape);
                    Optional<StructureShape> endpointError = index.getShape(endpointErrorId)
                            .flatMap(Shape::asStructureShape);

                    if (endpointOperation.isPresent() && endpointError.isPresent()) {
                        Map<ShapeId, EndpointDiscoveryInfo> serviceInfo = getOperations(
                                service, endpointOperation.get(), endpointError.get(), topDownIndex, opIndex);
                        if (!serviceInfo.isEmpty()) {
                            endpointDiscoveryInfo.put(service.getId(), serviceInfo);
                        }
                    }
                });
    }

    private Map<ShapeId, EndpointDiscoveryInfo> getOperations(
            ServiceShape service,
            OperationShape endpointOperation,
            StructureShape endpointError,
            TopDownIndex topDownIndex,
            OperationIndex opIndex
    ) {
        return topDownIndex.getContainedOperations(service).stream()
                .flatMap(operation -> Trait.flatMapStream(operation, DiscoveredEndpointTrait.class))
                .map(pair -> {
                    OperationShape operation = pair.getLeft();
                    List<MemberShape> discoveryIds = getDiscoveryIds(opIndex, operation);
                    EndpointDiscoveryInfo info = new EndpointDiscoveryInfo(
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
                .filter(member -> member.hasTrait(EndpointDiscoveryIdTrait.class))
                .forEach(discoveryIds::add));
        return discoveryIds;
    }

    public Optional<EndpointDiscoveryInfo> getEndpointDiscoveryInfo(ToShapeId service, ToShapeId operation) {
        return Optional.ofNullable(endpointDiscoveryInfo.get(service.toShapeId()))
                .flatMap(mappings -> Optional.ofNullable(mappings.get(operation.toShapeId())));
    }

    public Set<ShapeId> getEndpointDiscoveryOperations(ToShapeId service) {
        return Optional.ofNullable(endpointDiscoveryInfo.get(service.toShapeId()))
                .flatMap(mappings -> Optional.of(mappings.keySet()))
                .orElse(new HashSet<>());
    }
}
