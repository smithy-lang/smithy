/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.HttpBinding;
import software.amazon.smithy.model.knowledge.HttpBindingIndex;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.ProtocolDefinitionTrait;
import software.amazon.smithy.model.traits.TrailerTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;

/**
 * Validates that if a payload trait is present then all members of the
 * input of an operation are bound to part of the message.
 *
 * <p>Members marked with the trailer trait are never bound to the HTTP
 * message by HTTP binding traits, so each one gets its own event rather than
 * contributing to the ERROR emitted for other unbound members. The event is a
 * WARNING if at least one protocol is applied to the services that contain
 * the member's structure and every one of those protocols lists the trailer
 * trait in its protocol definition, and a suppressible DANGER otherwise.
 */
public final class HttpPayloadValidator extends AbstractValidator {
    private static final String UNBOUND_TRAILER = "UnboundTrailer";

    @Override
    public List<ValidationEvent> validate(Model model) {
        // Skip the expensive checks if neither trait that leaves members unbound is used.
        if (!model.isTraitApplied(HttpPayloadTrait.class) && !model.isTraitApplied(TrailerTrait.class)) {
            return Collections.emptyList();
        }

        OperationIndex opIndex = OperationIndex.of(model);
        HttpBindingIndex bindings = HttpBindingIndex.of(model);
        List<ValidationEvent> events = new ArrayList<>();
        // A set, so a trailer member of an output shared by several operations is only reported once.
        Set<MemberShape> trailers = new LinkedHashSet<>();

        for (OperationShape operation : model.getOperationShapesWithTrait(HttpTrait.class)) {
            events.addAll(validateOperation(bindings, opIndex, operation, trailers));
        }

        for (StructureShape structure : model.getStructureShapesWithTrait(ErrorTrait.class)) {
            events.addAll(validatePayload(structure.getId(), structure, bindings, false, trailers));
        }

        if (!trailers.isEmpty()) {
            Map<ShapeId, Set<ShapeId>> structureProtocols = findStructureProtocols(model, opIndex);
            for (MemberShape trailer : trailers) {
                events.add(validateTrailer(model, trailer, structureProtocols.get(trailer.getContainer())));
            }
        }

        return events;
    }

    // Maps each output and error structure to the protocol traits of every service that contains it.
    private static Map<ShapeId, Set<ShapeId>> findStructureProtocols(Model model, OperationIndex opIndex) {
        ServiceIndex serviceIndex = ServiceIndex.of(model);
        TopDownIndex topDownIndex = TopDownIndex.of(model);
        Map<ShapeId, Set<ShapeId>> result = new HashMap<>();
        for (ServiceShape service : model.getServiceShapes()) {
            Set<ShapeId> protocols = serviceIndex.getProtocols(service).keySet();
            Set<StructureShape> structures = new LinkedHashSet<>(opIndex.getErrors(service));
            for (OperationShape operation : topDownIndex.getContainedOperations(service)) {
                structures.add(opIndex.expectOutputShape(operation));
                structures.addAll(opIndex.getErrors(operation));
            }
            for (StructureShape structure : structures) {
                result.computeIfAbsent(structure.getId(), id -> new TreeSet<>()).addAll(protocols);
            }
        }
        return result;
    }

    private List<ValidationEvent> validateOperation(
            HttpBindingIndex bindings,
            OperationIndex opIndex,
            OperationShape shape,
            Set<MemberShape> trailers
    ) {
        List<ValidationEvent> events = new ArrayList<>();
        events.addAll(validatePayload(shape.getId(), opIndex.expectInputShape(shape), bindings, true, trailers));
        events.addAll(validatePayload(shape.getId(), opIndex.expectOutputShape(shape), bindings, false, trailers));
        return events;
    }

    // Unbound trailer members are collected into trailers rather than reported here.
    private List<ValidationEvent> validatePayload(
            ShapeId subject,
            StructureShape inputOrError,
            HttpBindingIndex bindings,
            boolean request,
            Set<MemberShape> trailers
    ) {
        Map<String, HttpBinding> resolved = request
                ? bindings.getRequestBindings(subject)
                : bindings.getResponseBindings(subject);
        List<String> unbound = new ArrayList<>();

        for (HttpBinding binding : resolved.values()) {
            if (binding.getLocation() != HttpBinding.Location.UNBOUND) {
                continue;
            }
            MemberShape member = binding.getMember();
            if (member.hasTrait(TrailerTrait.ID)) {
                trailers.add(member);
            } else {
                unbound.add(binding.getMemberName());
            }
        }

        if (!unbound.isEmpty()) {
            return Collections.singletonList(error(inputOrError,
                    String.format(
                            "A member of this structure is marked with the `httpPayload` trait, but the following "
                                    + "structure members are not explicitly bound to the HTTP message: %s",
                            ValidationUtils.tickedList(unbound))));
        }

        return Collections.emptyList();
    }

    // protocols is null if no service contains the member's structure.
    private ValidationEvent validateTrailer(Model model, MemberShape member, Set<ShapeId> protocols) {
        String prefix = "This member is marked with the `trailer` trait, so it is not explicitly bound to the HTTP "
                + "message. ";
        String suppress = " Suppress this event if the protocol this model is used with defines how this member "
                + "is populated.";

        if (protocols == null) {
            return danger(member,
                    prefix + "No service contains the structure of this member, so no protocol defines how it is "
                            + "populated." + suppress,
                    UNBOUND_TRAILER);
        }

        if (protocols.isEmpty()) {
            return danger(member,
                    prefix + "No protocol is applied to the services that contain the structure of this member, so "
                            + "no protocol defines how it is populated." + suppress,
                    UNBOUND_TRAILER);
        }

        List<ShapeId> unsupported = new ArrayList<>();
        for (ShapeId protocol : protocols) {
            if (!model.expectShape(protocol)
                    .expectTrait(ProtocolDefinitionTrait.class)
                    .getTraits()
                    .contains(TrailerTrait.ID)) {
                unsupported.add(protocol);
            }
        }

        if (unsupported.isEmpty()) {
            return warning(member,
                    prefix + "Every protocol of the services that contain the structure of this member lists the "
                            + "`trailer` trait in its protocol definition, so those protocols define how it is "
                            + "populated.",
                    UNBOUND_TRAILER);
        }

        return danger(member,
                prefix + "The following protocols of the services that contain the structure of this member do "
                        + "not list the `trailer` trait in their protocol definition, so they may not define how "
                        + "it is populated: " + ValidationUtils.tickedList(unsupported) + "." + suppress,
                UNBOUND_TRAILER);
    }
}
