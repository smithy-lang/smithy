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
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SetUtils;

public class ClientEndpointDiscoveryValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        ShapeIndex shapeIndex = model.getShapeIndex();
        ClientEndpointDiscoveryIndex discoveryIndex = model.getKnowledge(ClientEndpointDiscoveryIndex.class);
        OperationIndex opIndex = model.getKnowledge(OperationIndex.class);

        Map<ServiceShape, ClientEndpointDiscoveryTrait> endpointDiscoveryServices = shapeIndex
                .shapes(ServiceShape.class)
                .flatMap(service -> Trait.flatMapStream(service, ClientEndpointDiscoveryTrait.class))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

        List<ValidationEvent> validationEvents = endpointDiscoveryServices.values().stream()
                .map(ClientEndpointDiscoveryTrait::getOperation)
                .map(operation -> shapeIndex.getShape(operation).flatMap(Shape::asOperationShape))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .flatMap(endpointOperation -> validateEndpointOperation(
                        shapeIndex, opIndex, endpointOperation).stream())
                .collect(Collectors.toList());

        validationEvents.addAll(validateServices(discoveryIndex, endpointDiscoveryServices));
        validationEvents.addAll(validateOperations(shapeIndex, discoveryIndex, endpointDiscoveryServices));
        return validationEvents;
    }

    private List<ValidationEvent> validateServices(
            ClientEndpointDiscoveryIndex discoveryIndex,
            Map<ServiceShape, ClientEndpointDiscoveryTrait> endpointDiscoveryServices
    ) {
        List<ValidationEvent> validationEvents = endpointDiscoveryServices.entrySet().stream()
                .filter(pair -> !pair.getKey().getAllOperations().contains(pair.getValue().getOperation()))
                .map(pair -> error(pair.getKey(), String.format(
                        "The operation `%s` must be bound to the service `%s` to use it as the endpoint operation.",
                        pair.getValue().getOperation(), pair.getKey()
                )))
                .collect(Collectors.toList());

        validationEvents.addAll(endpointDiscoveryServices.keySet().stream()
                .filter(service -> discoveryIndex.getEndpointDiscoveryOperations(service).isEmpty())
                .map(service -> warning(service, String.format(
                        "The service `%s` is configured to use endpoint discovery, but has no operations bound with "
                                + "the `%s` trait.",
                        service.getId().toString(),
                        ClientDiscoveredEndpointTrait.ID.toString()
                )))
                .collect(Collectors.toList()));
        return validationEvents;
    }

    private List<ValidationEvent> validateOperations(
            ShapeIndex shapeIndex,
            ClientEndpointDiscoveryIndex discoveryIndex,
            Map<ServiceShape, ClientEndpointDiscoveryTrait> endpointDiscoveryServices
    ) {
        return shapeIndex.shapes(OperationShape.class)
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
                .filter(discoveryInfo -> !operation.getErrors().contains(discoveryInfo.getError().getId()))
                .map(discoveryInfo -> error(operation, String.format(
                        "The operation `%s` is marked with `%s` and is bound to the service "
                                + "`%s` but does not have the required error `%s`.",
                        operation.getId(),
                        ClientDiscoveredEndpointTrait.ID.toString(),
                        discoveryInfo.getService().getId(),
                        discoveryInfo.getError().getId()
                )))
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateEndpointOperation(
            ShapeIndex shapeIndex, OperationIndex opIndex, OperationShape operation
    ) {
        List<ValidationEvent> events = new ArrayList<>();
        opIndex.getInput(operation).ifPresent(input -> {
            Set<String> memberNames = SetUtils.copyOf(input.getMemberNames());
            if (!memberNames.equals(SetUtils.of("Operation", "Identifiers"))) {
                events.add(error(input, String.format(
                        "Input for endpoint discovery operation `%s` may only have the members Operation and "
                                + "Identifiers but found: %s",
                        operation.getId().toString(),
                        String.join(", ", memberNames)
                )));
            }

            input.getMember("Operation")
                    .flatMap(member -> shapeIndex.getShape(member.getTarget()))
                    .filter(shape -> !shape.isStringShape())
                    .ifPresent(shape -> events.add(error(
                            shape, "The Operation member of an endpoint discovery operation must be a string")));

            input.getMember("Identifiers")
                    .map(member -> Pair.of(member, shapeIndex.getShape(member.getTarget())))
                    .ifPresent(pair -> {
                        Optional<MapShape> map = pair.getRight().flatMap(Shape::asMapShape);
                        if (map.isPresent()) {
                            Optional<Shape> value = shapeIndex.getShape(map.get().getValue().getTarget());
                            if (value.isPresent() && value.get().isStringShape()) {
                                return;
                            }
                        }
                        events.add(error(pair.getLeft(), "The Identifiers member of an endpoint discovery "
                                + "operation must be a map whose keys and values are strings."));
                    });
        });

        Optional<StructureShape> output = opIndex.getOutput(operation);
        if (!output.isPresent()) {
            events.add(error(operation, "Endpoint discovery operations must have an output."));
            return events;
        }

        Map<String, MemberShape> outputMembers = output.get().getAllMembers();
        if (outputMembers.size() != 1 || !outputMembers.containsKey("Endpoints")) {
            events.add(error(output.get(), String.format(
                    "Endpoint discovery operation output may only contain an `Endpoints` member, but found: %s",
                    String.join(",", outputMembers.keySet())
            )));
        }

        Optional.ofNullable(outputMembers.get("Endpoints"))
                .map(member -> Pair.of(member, shapeIndex.getShape(member.getTarget())))
                .ifPresent(pair -> {
                    Optional<ListShape> listShape = pair.getRight().flatMap(Shape::asListShape);
                    if (!listShape.isPresent()) {
                        events.add(error(pair.getLeft(), "The output member `Endpoints` on an endpoint discovery "
                                + "operation must be a list."));
                        return;
                    }

                    Optional<StructureShape> listMember = shapeIndex.getShape(listShape.get().getMember().getTarget())
                            .flatMap(Shape::asStructureShape);
                    if (!listMember.isPresent()) {
                        events.add(error(listShape.get(), "The member of the Endpoints list in an "
                                + "endpoint discovery operation must be a structure shape."));
                        return;
                    }

                    Optional<MemberShape> addressMember = listMember.get().getMember("Address");
                    Optional<Shape> address = addressMember.flatMap(member -> shapeIndex.getShape(member.getTarget()));
                    if (address.isPresent() && !address.get().isStringShape()) {
                        events.add(error(addressMember.get(), "The `Address` member of the `Endpoint` shape must "
                                + "be a string type."));
                    }

                    Optional<MemberShape> cachePeriodMember = listMember.get().getMember("CachePeriodInMinutes");
                    Optional<Shape> cachePeriod = cachePeriodMember
                            .flatMap(member -> shapeIndex.getShape(member.getTarget()));
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
}
