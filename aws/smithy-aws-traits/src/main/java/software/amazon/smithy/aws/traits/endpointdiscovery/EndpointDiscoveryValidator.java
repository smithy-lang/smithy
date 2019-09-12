package software.amazon.smithy.aws.traits.endpointdiscovery;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

public class EndpointDiscoveryValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        ShapeIndex shapeIndex = model.getShapeIndex();
        EndpointDiscoveryIndex discoveryIndex = model.getKnowledge(EndpointDiscoveryIndex.class);

        Set<ServiceShape> endpointDiscoveryServices = shapeIndex.shapes(ServiceShape.class)
                .filter(service -> service.hasTrait(EndpointDiscoveryTrait.class))
                .collect(Collectors.toSet());

        List<ValidationEvent> validationEvents = validateServices(discoveryIndex, endpointDiscoveryServices);
        validationEvents.addAll(validateOperations(shapeIndex, discoveryIndex, endpointDiscoveryServices));
        return validationEvents;
    }

    private List<ValidationEvent> validateServices(
            EndpointDiscoveryIndex discoveryIndex, Set<ServiceShape> endpointDiscoveryServices
    ) {
        List<ValidationEvent> validationEvents = endpointDiscoveryServices.stream()
                .flatMap(shape -> Trait.flatMapStream(shape, EndpointDiscoveryTrait.class))
                .filter(pair -> !pair.getLeft().getAllOperations().contains(pair.getRight().getOperation()))
                .map(pair -> error(pair.getLeft(), String.format(
                        "The operation `%s` must be bound to the service `%s` to use it as the endpoint operation.",
                        pair.getRight().getOperation(), pair.getLeft()
                )))
                .collect(Collectors.toList());

        validationEvents.addAll(endpointDiscoveryServices.stream()
                .filter(service -> discoveryIndex.getEndpointDiscoveryOperations(service).isEmpty())
                .map(service -> warning(service, String.format(
                        "The service `%s` is configured to use endpoint discovery, but has no operations bound with "
                                + "the `aws.api#discvoeredEndpoint` trait.",
                        service.getId().toString()
                )))
                .collect(Collectors.toList()));
        return validationEvents;
    }

    private List<ValidationEvent> validateOperations(
            ShapeIndex shapeIndex, EndpointDiscoveryIndex discoveryIndex, Set<ServiceShape> endpointDiscoveryServices
    ) {
        // Grab the set of operations which are bound to a service configured for endpoint discovery.
        Set<ShapeId> boundDiscoveryOperations = endpointDiscoveryServices.stream()
                .map(discoveryIndex::getEndpointDiscoveryOperations)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        return shapeIndex.shapes(OperationShape.class)
                // Find all operations that have the endpoint discovery trait.
                .filter(operation -> operation.hasTrait(DiscoveredEndpointTrait.class))
                // Now find the subset of those which are not bound to a service configured for endpoint discovery.
                .filter(operation -> !boundDiscoveryOperations.contains(operation.toShapeId()))
                .map(operation -> error(operation, String.format(
                        "The operation `%s` is marked with `aws.api#discoveredEndpoint` but is not attached to a "
                                + "service with the `aws.api#endpointDiscovery` trait.",
                        operation.getId().toString()
                )))
                .collect(Collectors.toList());
    }
}
