/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.neighbor.RelationshipDirection;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.ProtocolDefinitionTrait;
import software.amazon.smithy.model.traits.RequiresLengthTrait;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.Pair;

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

        List<ValidationEvent> events = validateStreamingTargets(model);
        events.addAll(validateAllEventStreamMembers(model));
        events.addAll(validateBlobTargetsArePayloads(model));
        return events;
    }

    private List<ValidationEvent> validateBlobTargetsArePayloads(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        ServiceIndex serviceIndex = ServiceIndex.of(model);
        Walker walker = new Walker(model);

        final Set<ServiceShape> servicesWithPayloadSupportingProtocols =
                model.getServiceShapes().stream().filter(service -> serviceIndex.getProtocols(service).values().stream()
                        .map(trait -> model.expectShape(trait.toShapeId()))
                        .map(traitShape -> traitShape.expectTrait(ProtocolDefinitionTrait.class))
                        .anyMatch(protocol -> protocol.getTraits().contains(HttpPayloadTrait.ID)))
                        .collect(Collectors.toSet());

        for (ServiceShape service : servicesWithPayloadSupportingProtocols) {
            walker.walkShapes(service).stream()
                    .filter(Shape::isMemberShape)
                    .map(shape -> shape.asMemberShape().get())
                    .filter(memberShape -> !memberShape.hasTrait(HttpPayloadTrait.ID))
                    .filter(memberShape -> model.expectShape(memberShape.getTarget()).isBlobShape())
                    .filter(memberShape -> model.expectShape(memberShape.getTarget()).hasTrait(StreamingTrait.ID))
                    .forEach(memberShape ->
                        events.add(error(memberShape, String.format("Member `%s` referencing "
                                + "@streaming shape `%s` must have the @httpPayload trait, "
                                + "as service `%s` has a protocol that supports @httpPayload.",
                                memberShape.toShapeId(), memberShape.getTarget(), service.toShapeId()))));
        }
        return events;
    }

    private List<ValidationEvent> validateStreamingTargets(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        NeighborProvider provider = NeighborProviderIndex.of(model).getReverseProvider();

        for (MemberShape member : model.getMemberShapes()) {
            Shape target = model.expectShape(member.getTarget());
            if (target.hasTrait(StreamingTrait.class)) {
                Shape container = model.expectShape(member.getContainer());
                for (Relationship rel : provider.getNeighbors(container)) {
                    validateStreamingTargetRel(container, target, rel, events);
                }
            }
        }

        return events;
    }

    private void validateStreamingTargetRel(
            Shape container,
            Shape target,
            Relationship rel,
            List<ValidationEvent> events
    ) {
        if (rel.getRelationshipType().getDirection() == RelationshipDirection.DIRECTED) {
            switch (rel.getRelationshipType()) {
                case INPUT:
                    break;
                case OUTPUT:
                    if (target.hasTrait(RequiresLengthTrait.class)) {
                        events.add(error(rel.getShape(), String.format(
                                "Structures that contain a reference to a stream marked with the "
                                + "@requiresLength trait can only be used as operation inputs, but this "
                                + "structure is referenced from `%s` as %s",
                                rel.getShape().getId(),
                                rel.getRelationshipType().toString().toLowerCase(Locale.ENGLISH))));
                    }
                    break;
                default:
                    events.add(error(rel.getShape(), String.format(
                            "This shape has an invalid `%s` relationship to a structure, `%s`, that contains "
                            + "a stream", rel.getRelationshipType(), container.getId())));
            }
        }
    }

    private List<ValidationEvent> validateAllEventStreamMembers(Model model) {
        return model.shapes(UnionShape.class)
                .filter(shape -> shape.hasTrait(StreamingTrait.class))
                .flatMap(shape -> validateUnionMembers(model, shape).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateUnionMembers(Model model, UnionShape shape) {
        // Find members that don't reference a structure and combine
        // these member names into a comma separated list.
        String invalidMembers = shape.getAllMembers().values().stream()
                .map(em -> Pair.of(em.getMemberName(), model.getShape(em.getTarget()).orElse(null)))
                .filter(pair -> pair.getRight() != null && !(pair.getRight() instanceof StructureShape))
                .map(Pair::getLeft)
                .sorted()
                .collect(Collectors.joining(", "));
        if (!invalidMembers.isEmpty()) {
            return ListUtils.of(error(shape, String.format(
                    "Each member of an event stream union must target a structure shape, but the following union "
                    + "members do not: [%s]", invalidMembers)));
        }
        return Collections.emptyList();
    }
}
