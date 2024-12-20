/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.neighbor.RelationshipDirection;
import software.amazon.smithy.model.neighbor.RelationshipType;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.ProtocolDefinitionTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.RequiresLengthTrait;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates the streaming trait.
 *
 * <p>Ensures that the targeted shapes are only referenced by top level input/output structures.
 *
 * <p>If the targeted shape is a union, ensures that all members are structures.
 *
 * <p>If used in the scope of any service that supports {@link software.amazon.smithy.model.traits.HttpPayloadTrait},
 * ensures that any blobs targeted also have @httpPayload applied.
 */
public final class StreamingTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        if (!model.isTraitApplied(StreamingTrait.class)) {
            return Collections.emptyList();
        }

        List<ValidationEvent> events = new ArrayList<>();
        validateStreamingTargets(model, events);
        validateAllEventStreamMembersTargetStructures(model, events);
        validateBlobTargetsArePayloads(model, events);
        validateBlobTargetsAreNonOptional(model, events);
        return events;
    }

    private void validateBlobTargetsAreNonOptional(Model model, List<ValidationEvent> events) {
        for (MemberShape member : model.toSet(MemberShape.class)) {
            Shape target = model.expectShape(member.getTarget());
            if (target.isBlobShape() && target.hasTrait(StreamingTrait.class)
                    && !(member.hasTrait(RequiredTrait.class) || member.hasTrait(DefaultTrait.class))) {
                events.add(error(member,
                        "Members that target blobs marked with the `streaming` trait MUST also be "
                                + "marked with the `required` or `default` trait."));
            }
        }
    }

    private void validateBlobTargetsArePayloads(Model model, List<ValidationEvent> events) {
        ServiceIndex serviceIndex = ServiceIndex.of(model);
        Set<ServiceShape> servicesWithPayloadSupportingProtocols = new HashSet<>();
        for (ServiceShape service : model.getServiceShapes()) {
            for (ShapeId protocolTrait : serviceIndex.getProtocols(service).keySet()) {
                if (protocolTraitSupportsHttpPayload(model, protocolTrait)) {
                    servicesWithPayloadSupportingProtocols.add(service);
                    break;
                }
            }
        }

        Walker walker = new Walker(model);
        for (ServiceShape service : servicesWithPayloadSupportingProtocols) {
            walker.iterateShapes(service).forEachRemaining(shape -> {
                if (shape.isMemberShape() && !shape.hasTrait(HttpPayloadTrait.class)) {
                    MemberShape member = shape.asMemberShape().get();
                    Shape target = model.expectShape(member.getTarget());
                    if (target.hasTrait(StreamingTrait.class)) {
                        events.add(error(member,
                                String.format(
                                        "Member `%s` referencing @streaming shape `%s` must have the @httpPayload trait, "
                                                + "as service `%s` has a protocol that supports @httpPayload.",
                                        member.toShapeId(),
                                        member.getTarget(),
                                        service.toShapeId())));
                    }
                }
            });
        }
    }

    private boolean protocolTraitSupportsHttpPayload(Model model, ShapeId protocolTrait) {
        return model.expectShape(protocolTrait)
                .expectTrait(ProtocolDefinitionTrait.class)
                .getTraits()
                .contains(HttpPayloadTrait.ID);
    }

    private void validateStreamingTargets(Model model, List<ValidationEvent> events) {
        NeighborProvider provider = NeighborProviderIndex.of(model).getReverseProvider();
        // Find members that target streaming shapes and validate things that target their containers.
        for (Shape shape : model.getShapesWithTrait(StreamingTrait.class)) {
            for (Relationship rel : provider.getNeighbors(shape)) {
                if (rel.getRelationshipType() == RelationshipType.MEMBER_TARGET) {
                    MemberShape member = rel.getShape().asMemberShape().get();
                    validateRef(model, member, provider, events);
                }
            }
        }
    }

    private void validateRef(Model model, MemberShape member, NeighborProvider reverse, List<ValidationEvent> events) {
        Shape target = model.expectShape(member.getTarget());
        Shape container = model.expectShape(member.getContainer());
        for (Relationship rel : reverse.getNeighbors(container)) {
            if (rel.getRelationshipType().getDirection() == RelationshipDirection.DIRECTED) {
                switch (rel.getRelationshipType()) {
                    case INPUT:
                    case MIXIN:
                        break;
                    case OUTPUT:
                        if (target.hasTrait(RequiresLengthTrait.class)) {
                            events.add(error(rel.getNeighborShape().get(),
                                    String.format(
                                            "Structures that contain a reference to a stream marked with the "
                                                    + "@requiresLength trait can only be used as operation inputs, but this "
                                                    + "structure is referenced from `%s` as %s",
                                            rel.getShape().getId(),
                                            rel.getRelationshipType().toString().toLowerCase(Locale.ENGLISH))));
                        }
                        break;
                    case MEMBER_TARGET:
                        events.add(error(rel.getShape(),
                                String.format(
                                        "Members cannot target structures that contain a stream, but this member targets %s",
                                        container.getId())));
                        break;
                    default:
                        events.add(error(rel.getShape(),
                                String.format(
                                        "This shape has an invalid `%s` relationship to a structure, `%s`, that contains "
                                                + "a stream",
                                        rel.getRelationshipType(),
                                        container.getId())));
                }
            }
        }
    }

    private void validateAllEventStreamMembersTargetStructures(Model model, List<ValidationEvent> events) {
        for (UnionShape union : model.getUnionShapesWithTrait(StreamingTrait.class)) {
            // Find members that don't reference a structure and combine
            // these member names into a comma separated list.
            StringJoiner joiner = new StringJoiner(", ");
            for (MemberShape member : union.members()) {
                Shape target = model.expectShape(member.getTarget());
                if (!target.isStructureShape()) {
                    joiner.add(member.getMemberName());
                }
            }
            String invalidMembers = joiner.toString();

            if (!invalidMembers.isEmpty()) {
                events.add(error(union,
                        String.format(
                                "Each member of an event stream union must target a structure shape, but the following union "
                                        + "members do not: [%s]",
                                invalidMembers)));
            }
        }
    }
}
