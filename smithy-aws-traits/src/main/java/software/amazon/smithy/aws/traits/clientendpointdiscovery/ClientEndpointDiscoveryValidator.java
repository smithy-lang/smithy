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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

@SmithyInternalApi
public final class ClientEndpointDiscoveryValidator extends AbstractValidator {
    private static final Set<String> VALID_INPUT_MEMBERS = SetUtils.of("Operation", "Identifiers");
    private static final String MISSING_ERROR_DEFINITION = "MissingErrorDefinition";
    private static final String UNBOUND_OPERATION = "UnboundOperation";
    private static final String NO_OPERATIONS = "NoOperations";

    @Override
    public List<ValidationEvent> validate(Model model) {
        ClientEndpointDiscoveryIndex discoveryIndex = ClientEndpointDiscoveryIndex.of(model);
        OperationIndex opIndex = OperationIndex.of(model);

        List<ValidationEvent> validationEvents = new ArrayList<>();
        Map<ServiceShape, ClientEndpointDiscoveryTrait> endpointDiscoveryServices = new HashMap<>();
        for (ServiceShape service : model.getServiceShapesWithTrait(ClientEndpointDiscoveryTrait.class)) {
            ClientEndpointDiscoveryTrait trait = service.expectTrait(ClientEndpointDiscoveryTrait.class);
            endpointDiscoveryServices.put(service, trait);
            validationEvents.addAll(validateTrait(service, trait));
        }

        validationEvents.addAll(endpointDiscoveryServices.values().stream()
                .map(ClientEndpointDiscoveryTrait::getOperation)
                .map(operation -> model.getShape(operation).flatMap(Shape::asOperationShape))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .flatMap(endpointOperation -> validateEndpointOperation(
                        model, opIndex, endpointOperation).stream())
                .collect(Collectors.toList()));

        validationEvents.addAll(validateServices(discoveryIndex, endpointDiscoveryServices));
        validationEvents.addAll(validateOperations(model, discoveryIndex, endpointDiscoveryServices));
        return validationEvents;
    }

    private List<ValidationEvent> validateTrait(ServiceShape service, ClientEndpointDiscoveryTrait trait) {
        if (!trait.getOptionalError().isPresent()) {
            return ListUtils.of(danger(service, trait,
                    "Services SHOULD define an error which indicates an endpoint is invalid.",
                    MISSING_ERROR_DEFINITION));
        }
        return Collections.emptyList();
    }

    private List<ValidationEvent> validateServices(
            ClientEndpointDiscoveryIndex discoveryIndex,
            Map<ServiceShape, ClientEndpointDiscoveryTrait> endpointDiscoveryServices
    ) {
        List<ValidationEvent> validationEvents = endpointDiscoveryServices.entrySet().stream()
                .filter(pair -> !pair.getKey().getAllOperations().contains(pair.getValue().getOperation()))
                .map(pair -> error(pair.getKey(), String.format(
                        "The operation `%s` must be bound to the service `%s` to use it as the endpoint operation.",
                        pair.getValue().getOperation(), pair.getKey()),
                        UNBOUND_OPERATION, pair.getValue().getOperation().getName()
                ))
                .collect(Collectors.toList());

        validationEvents.addAll(endpointDiscoveryServices.keySet().stream()
                .filter(service -> discoveryIndex.getEndpointDiscoveryOperations(service).isEmpty())
                .map(service -> warning(service, String.format(
                        "The service `%s` is configured to use endpoint discovery, but has no operations bound with "
                                + "the `%s` trait.",
                        service.getId().toString(),
                        ClientDiscoveredEndpointTrait.ID.toString()),
                        NO_OPERATIONS
                ))
                .collect(Collectors.toList()));
        return validationEvents;
    }

    private List<ValidationEvent> validateOperations(
            Model model,
            ClientEndpointDiscoveryIndex discoveryIndex,
            Map<ServiceShape, ClientEndpointDiscoveryTrait> endpointDiscoveryServices
    ) {
        return model.shapes(OperationShape.class)
                .filter(operation -> operation.hasTrait(ClientDiscoveredEndpointTrait.class))
                .map(operation -> {
                    List<ClientEndpointDiscoveryInfo> infos = endpointDiscoveryServices.keySet().stream()
                            .map(service -> discoveryIndex.getEndpointDiscoveryInfo(service, operation))
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .collect(Collectors.toList());
                    return Pair.of(operation, infos);
                })
                .flatMap(pair -> validateOperation(pair.getLeft(), pair.getRight()).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateOperation(OperationShape operation, List<ClientEndpointDiscoveryInfo> infos) {
        if (infos.isEmpty()) {
            return Collections.singletonList(error(operation, String.format(
                    "The operation `%s` is marked with `%s` but is not attached to a "
                            + "service with the `%s` trait.",
                    operation.getId().toString(),
                    ClientDiscoveredEndpointTrait.ID.toString(),
                    ClientEndpointDiscoveryTrait.ID.toString()
            )));
        }

        return infos.stream()
                .filter(discoveryInfo -> discoveryInfo.getOptionalError().isPresent())
                .filter(discoveryInfo -> !operation.getErrors().contains(
                        discoveryInfo.getOptionalError().get().getId()))
                .map(discoveryInfo -> error(operation, String.format(
                        "The operation `%s` is marked with `%s` and is bound to the service "
                                + "`%s` but does not have the required error `%s`.",
                        operation.getId(),
                        ClientDiscoveredEndpointTrait.ID.toString(),
                        discoveryInfo.getService().getId(),
                        discoveryInfo.getOptionalError().get().getId()
                )))
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateEndpointOperation(
            Model model, OperationIndex opIndex, OperationShape operation
    ) {
        List<ValidationEvent> events = new ArrayList<>();
        events.addAll(validateEndpointOperationInput(model, opIndex.expectInputShape(operation), operation));
        StructureShape output = opIndex.expectOutputShape(operation);

        Map<String, MemberShape> outputMembers = output.getAllMembers();
        if (outputMembers.size() != 1 || !outputMembers.containsKey("Endpoints")) {
            events.add(error(operation, String.format(
                    "Endpoint discovery operation output must only contain an `Endpoints` member, but found: [%s]",
                    String.join(",", outputMembers.keySet())
            )));
        }

        Optional.ofNullable(outputMembers.get("Endpoints"))
                .map(member -> Pair.of(member, model.getShape(member.getTarget())))
                .ifPresent(pair -> {
                    Optional<ListShape> listShape = pair.getRight().flatMap(Shape::asListShape);
                    if (!listShape.isPresent()) {
                        events.add(error(pair.getLeft(), "The output member `Endpoints` on an endpoint discovery "
                                + "operation must be a list."));
                        return;
                    }

                    Optional<StructureShape> listMember = model.getShape(listShape.get().getMember().getTarget())
                            .flatMap(Shape::asStructureShape);
                    if (!listMember.isPresent()) {
                        events.add(error(listShape.get(), "The member of the Endpoints list in an "
                                + "endpoint discovery operation must be a structure shape."));
                        return;
                    }

                    Optional<MemberShape> addressMember = listMember.get().getMember("Address");
                    Optional<Shape> address = addressMember.flatMap(member -> model.getShape(member.getTarget()));
                    if (address.isPresent() && !address.get().isStringShape()) {
                        events.add(error(addressMember.get(), "The `Address` member of the `Endpoint` shape must "
                                + "be a string type."));
                    }

                    Optional<MemberShape> cachePeriodMember = listMember.get().getMember("CachePeriodInMinutes");
                    Optional<Shape> cachePeriod = cachePeriodMember
                            .flatMap(member -> model.getShape(member.getTarget()));
                    if (cachePeriod.isPresent() && !cachePeriod.get().isLongShape()) {
                        events.add(error(cachePeriodMember.get(), "The `CachePeriodInMinutes` member of the "
                                + "`Endpoint` shape must be a long type."));
                    }

                    Set<String> memberNames = SetUtils.copyOf(listMember.get().getMemberNames());
                    if (!memberNames.equals(SetUtils.of("Address", "CachePeriodInMinutes"))) {
                        events.add(error(listMember.get(), String.format(
                                "The `Endpoint` shape must only have the members `Address` and `CachePeriodInMinutes`, "
                                        + "but found: %s",
                                String.join(", ", memberNames)
                        )));
                    }
                });

        return events;
    }

    private List<ValidationEvent> validateEndpointOperationInput(
            Model model, StructureShape input, OperationShape operation
    ) {
        List<ValidationEvent> events = new ArrayList<>();
        Set<String> memberNames = SetUtils.copyOf(input.getMemberNames());
        if (!VALID_INPUT_MEMBERS.containsAll(memberNames)) {
            events.add(error(input, String.format(
                    "Input for endpoint discovery operation `%s` may only have the members Operation and "
                            + "Identifiers but found: %s",
                    operation.getId().toString(),
                    String.join(", ", memberNames)
            )));
        }

        input.getMember("Operation")
                .flatMap(member -> model.getShape(member.getTarget()))
                .filter(shape -> !shape.isStringShape())
                .ifPresent(shape -> events.add(error(
                        shape, "The Operation member of an endpoint discovery operation must be a string")));

        input.getMember("Identifiers")
                .map(member -> Pair.of(member, model.getShape(member.getTarget())))
                .ifPresent(pair -> {
                    Optional<MapShape> map = pair.getRight().flatMap(Shape::asMapShape);
                    if (map.isPresent()) {
                        Optional<Shape> value = model.getShape(map.get().getValue().getTarget());
                        if (value.isPresent() && value.get().isStringShape()) {
                            return;
                        }
                    }
                    events.add(error(pair.getLeft(), "The Identifiers member of an endpoint discovery "
                            + "operation must be a map whose keys and values are strings."));
                });
        return events;
    }
}
