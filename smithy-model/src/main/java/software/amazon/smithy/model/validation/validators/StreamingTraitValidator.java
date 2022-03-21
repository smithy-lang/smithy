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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.neighbor.RelationshipDirection;
import software.amazon.smithy.model.neighbor.RelationshipType;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.ProtocolDefinitionTrait;
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
        return events;
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
                        events.add(error(member, String.format(
                                "Member `%s` referencing @streaming shape `%s` must have the @httpPayload trait, "
                                + "as service `%s` has a protocol that supports @httpPayload.",
                                member.toShapeId(), member.getTarget(), service.toShapeId())));
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

        // Find any containers that reference a streaming trait.
        Set<Shape> streamingStructures = model.shapes(MemberShape.class)
                .filter(member -> member.getMemberTrait(model, StreamingTrait.class).isPresent())
                .map(member -> model.expectShape(member.getContainer()))
                .collect(Collectors.toSet());

        for (Shape shape : streamingStructures) {
            for (Relationship rel : provider.getNeighbors(shape)) {
                if (rel.getRelationshipType() != RelationshipType.INPUT
                        && rel.getRelationshipType() != RelationshipType.OUTPUT
                        && !isInputOutputMixinRelationship(provider, rel)
                        && rel.getRelationshipType().getDirection() == RelationshipDirection.DIRECTED) {
                    events.add(error(rel.getShape(), String.format(
                            "This shape has an invalid `%s` relationship to a structure, `%s`, that contains "
                            + "a stream", rel.getRelationshipType(), shape.getId())));
                }
            }
        }

        for (OperationShape operation : model.getOperationShapes()) {
            StructureShape output = model.expectShape(operation.getOutputShape(), StructureShape.class);
            for (MemberShape member : output.getAllMembers().values()) {
                Shape target = model.expectShape(member.getTarget());
                if (target.hasTrait(RequiresLengthTrait.class)) {
                    events.add(error(model.expectShape(member.getContainer()), String.format(
                            "Structures that contain a reference to a stream marked with the "
                            + "@requiresLength trait can only be used as operation inputs, but this "
                            + "structure is referenced from `%s` as output",
                            operation.getId())));
                }
            }
        }
    }

    private boolean isInputOutputMixinRelationship(NeighborProvider provider, Relationship rel) {
        // Mixins that contain streams are allowed to be mixed into
        // input and output shapes, but nowhere else.
        if (rel.getRelationshipType() == RelationshipType.MIXIN) {
            boolean foundInputOutput = false;
            for (Relationship mixinRel : provider.getNeighbors(rel.getShape())) {
                RelationshipType mixinRelType = mixinRel.getRelationshipType();
                // Inputs, outputs, and the containers where mixins are added are
                // all allowed as relationships.
                if (mixinRelType == RelationshipType.INPUT || mixinRelType == RelationshipType.OUTPUT) {
                    foundInputOutput = true;
                } else if (mixinRelType != RelationshipType.MEMBER_CONTAINER) {
                    // Other relationship types aren't allowed, so short-circuit.
                    return false;
                }
            }

            // At least one input or output relationship must be found for this
            // mixin relationship to be valid.
            return foundInputOutput;
        }
        return false;
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
                events.add(error(union, String.format(
                        "Each member of an event stream union must target a structure shape, but the following union "
                        + "members do not: [%s]", invalidMembers)));
            }
        }
    }
}
