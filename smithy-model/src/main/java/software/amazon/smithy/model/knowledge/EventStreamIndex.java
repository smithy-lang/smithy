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
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.InputEventStreamTrait;
import software.amazon.smithy.model.traits.OutputEventStreamTrait;
import software.amazon.smithy.model.traits.StringTrait;

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
        ShapeIndex index = model.getShapeIndex();
        OperationIndex operationIndex = model.getKnowledge(OperationIndex.class);

        model.getShapeIndex().shapes(OperationShape.class).forEach(operation -> {
            operation.getTrait(InputEventStreamTrait.class).ifPresent(trait -> {
                operationIndex.getInput(operation)
                        .flatMap(input -> createEventStreamInfo(index, operation, input, trait))
                        .ifPresent(info -> inputInfo.put(operation.getId(), info));
            });
            operation.getTrait(OutputEventStreamTrait.class).ifPresent(trait -> {
                operationIndex.getOutput(operation)
                        .flatMap(output -> createEventStreamInfo(index, operation, output, trait))
                        .ifPresent(info -> outputInfo.put(operation.getId(), info));
            });
        });
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
            ShapeIndex index,
            OperationShape operation,
            StructureShape structure,
            StringTrait trait
    ) {
        String eventStreamMemberName = trait.getValue();

        MemberShape eventStreamMember = structure.getMember(eventStreamMemberName).orElse(null);
        if (eventStreamMember == null) {
            LOGGER.severe(() -> String.format(
                    "Skipping event stream info for %s because the member %s does not exist",
                    operation.getId(), eventStreamMemberName));
            return Optional.empty();
        }

        Shape eventStreamTarget = index.getShape(eventStreamMember.getTarget()).orElse(null);
        if (eventStreamTarget == null) {
            LOGGER.severe(String.format(
                    "Skipping event stream info for %s because the %s member target %s does not exist",
                    operation.getId(), eventStreamMemberName, eventStreamMember.getTarget()));
            return Optional.empty();
        }

        // Compute the events of the event stream.
        Map<String, StructureShape> events = new HashMap<>();
        if (eventStreamTarget.asUnionShape().isPresent()) {
            for (MemberShape member : eventStreamTarget.asUnionShape().get().getAllMembers().values()) {
                index.getShape(member.getTarget()).flatMap(Shape::asStructureShape).ifPresent(struct -> {
                    events.put(member.getMemberName(), struct);
                });
            }
        } else if (eventStreamTarget.asStructureShape().isPresent()) {
            events.put(trait.getValue(), eventStreamTarget.asStructureShape().get());
        } else {
            // If the event target is an invalid type, then we can't create the indexed result.
            LOGGER.severe(() -> String.format(
                    "Skipping event stream info for %s because the %s member target %s is not a structure or union",
                    operation.getId(), eventStreamMemberName, eventStreamMember.getTarget()));
            return Optional.empty();
        }

        Map<String, MemberShape> initialMembers = new HashMap<>();
        Map<String, Shape> initialTargets = new HashMap<>();

        for (MemberShape member : structure.getAllMembers().values()) {
            if (!member.getMemberName().equals(eventStreamMemberName)) {
                index.getShape(member.getTarget()).ifPresent(shapeTarget -> {
                    initialMembers.put(member.getMemberName(), member);
                    initialTargets.put(member.getMemberName(), shapeTarget);
                });
            }
        }

        return Optional.of(new EventStreamInfo(
                operation, trait, structure,
                eventStreamMember, eventStreamTarget,
                initialMembers, initialTargets,
                events));
    }
}
