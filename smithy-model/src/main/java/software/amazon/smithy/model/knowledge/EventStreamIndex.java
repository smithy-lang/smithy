/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.knowledge;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.StreamingTrait;

/**
 * Index of operation shapes to event stream information.
 *
 * <p>This knowledge index provides information about event streams in
 * operations, including the input/output member that contains an
 * event stream, the shape targeted by this member, and any additional
 * input/output members that form the initial message.
 */
public final class EventStreamIndex implements KnowledgeIndex {
    private static final Logger LOGGER = Logger.getLogger(EventStreamIndex.class.getName());

    private final Map<ShapeId, EventStreamInfo> inputInfo = new HashMap<>();
    private final Map<ShapeId, EventStreamInfo> outputInfo = new HashMap<>();

    public EventStreamIndex(Model model) {
        OperationIndex operationIndex = OperationIndex.of(model);

        for (OperationShape operation : model.getOperationShapes()) {
            computeEvents(model, operation, operationIndex.expectInputShape(operation), inputInfo);
            computeEvents(model, operation, operationIndex.expectOutputShape(operation), outputInfo);
        }
    }

    public static EventStreamIndex of(Model model) {
        return model.getKnowledge(EventStreamIndex.class, EventStreamIndex::new);
    }

    private void computeEvents(
            Model model,
            OperationShape operation,
            StructureShape shape,
            Map<ShapeId, EventStreamInfo> infoMap
    ) {
        for (MemberShape member : shape.getAllMembers().values()) {
            Shape target = model.expectShape(member.getTarget());
            if (target.hasTrait(StreamingTrait.class) && target.isUnionShape()) {
                createEventStreamInfo(model, operation, shape, member).ifPresent(info -> {
                    infoMap.put(operation.getId(), info);
                });
            }
        }
    }

    /**
     * Get event stream information for the input of an operation.
     *
     * <p>No result is returned if the provided operation shape does not
     * exist, if the targeted shape is not an operation, if the operation
     * does not contain an input structure or if the input structure does
     * not contain an input event stream.
     *
     * @param operationShape Operation or shape ID to retrieve information for.
     * @return Returns the optionally found input event stream information.
     */
    public Optional<EventStreamInfo> getInputInfo(ToShapeId operationShape) {
        return Optional.ofNullable(inputInfo.get(operationShape.toShapeId()));
    }

    /**
     * Get event stream information for the output of an operation.
     *
     * <p>No result is returned if the provided operation shape does not
     * exist, if the targeted shape is not an operation, if the operation
     * does not contain an output structure or if the output structure does
     * not contain an output event stream.
     *
     * @param operationShape Operation or shape ID to retrieve information for.
     * @return Returns the optionally found output event stream information.
     */
    public Optional<EventStreamInfo> getOutputInfo(ToShapeId operationShape) {
        return Optional.ofNullable(outputInfo.get(operationShape.toShapeId()));
    }

    private Optional<EventStreamInfo> createEventStreamInfo(
            Model model,
            OperationShape operation,
            StructureShape structure,
            MemberShape member
    ) {

        Shape eventStreamTarget = model.expectShape(member.getTarget());

        // Compute the events of the event stream.
        Map<String, StructureShape> events = new HashMap<>();
        if (eventStreamTarget.asUnionShape().isPresent()) {
            for (MemberShape unionMember : eventStreamTarget.asUnionShape().get().getAllMembers().values()) {
                model.getShape(unionMember.getTarget()).flatMap(Shape::asStructureShape).ifPresent(struct -> {
                    events.put(unionMember.getMemberName(), struct);
                });
            }
        } else if (eventStreamTarget.asStructureShape().isPresent()) {
            events.put(member.getMemberName(), eventStreamTarget.asStructureShape().get());
        } else {
            // If the event target is an invalid type, then we can't create the indexed result.
            LOGGER.severe(() -> String.format(
                    "Skipping event stream info for %s because the %s member target %s is not a structure or union",
                    operation.getId(),
                    member.getMemberName(),
                    member.getTarget()));
            return Optional.empty();
        }

        Map<String, MemberShape> initialMembers = new HashMap<>();
        Map<String, Shape> initialTargets = new HashMap<>();

        for (MemberShape structureMember : structure.getAllMembers().values()) {
            if (!structureMember.getMemberName().equals(member.getMemberName())) {
                model.getShape(structureMember.getTarget()).ifPresent(shapeTarget -> {
                    initialMembers.put(structureMember.getMemberName(), structureMember);
                    initialTargets.put(structureMember.getMemberName(), shapeTarget);
                });
            }
        }

        return Optional.of(new EventStreamInfo(
                operation,
                eventStreamTarget.expectTrait(StreamingTrait.class),
                structure,
                member,
                eventStreamTarget,
                initialMembers,
                initialTargets,
                events));
    }
}
