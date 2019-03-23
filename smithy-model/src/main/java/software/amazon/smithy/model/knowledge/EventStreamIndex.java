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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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

    private final Map<ShapeId, Info> inputInfo = new HashMap<>();
    private final Map<ShapeId, Info> outputInfo = new HashMap<>();

    public EventStreamIndex(Model model) {
        var index = model.getShapeIndex();
        var operationIndex = model.getKnowledge(OperationIndex.class);

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
    public Optional<Info> getInputInfo(ToShapeId operationShape) {
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
    public Optional<Info> getOutputInfo(ToShapeId operationShape) {
        return Optional.ofNullable(outputInfo.get(operationShape.toShapeId()));
    }

    private Optional<Info> createEventStreamInfo(
            ShapeIndex index,
            OperationShape operation,
            StructureShape structure,
            StringTrait trait
    ) {
        var eventStreamMemberName = trait.getValue();

        var eventStreamMember = structure.getMember(eventStreamMemberName).orElse(null);
        if (eventStreamMember == null) {
            LOGGER.severe(() -> String.format(
                    "Skipping event stream info for %s because the member %s does not exist",
                    operation.getId(), eventStreamMemberName));
            return Optional.empty();
        }

        var eventStreamTarget = index.getShape(eventStreamMember.getTarget()).orElse(null);
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

        return Optional.of(new Info(
                operation, trait, structure,
                eventStreamMember, eventStreamTarget,
                initialMembers, initialTargets,
                events));
    }

    /**
     * Contains extracted event stream information.
     */
    public static final class Info {
        private final OperationShape operation;
        private final StructureShape structure;
        private final MemberShape eventStreamMember;
        private final Shape eventStreamTarget;
        private final Map<String, MemberShape> initialMembers;
        private final Map<String, Shape> initialTargets;
        private final Map<String, StructureShape> events;
        private final StringTrait eventStreamTrait;

        private Info(
                OperationShape operation,
                StringTrait eventStreamTrait,
                StructureShape structure,
                MemberShape eventStreamMember,
                Shape eventStreamTarget,
                Map<String, MemberShape> initialMembers,
                Map<String, Shape> initialTargets,
                Map<String, StructureShape> events
        ) {
            this.operation = operation;
            this.eventStreamTrait = eventStreamTrait;
            this.structure = structure;
            this.eventStreamMember = eventStreamMember;
            this.eventStreamTarget = eventStreamTarget;
            this.initialMembers = Collections.unmodifiableMap(initialMembers);
            this.initialTargets = Collections.unmodifiableMap(initialTargets);
            this.events = events;
        }

        /**
         * Gets the operation associated with this data.
         *
         * @return Returns the associated operation.
         */
        public OperationShape getOperation() {
            return operation;
        }

        /**
         * Gets the event stream trait.
         *
         * @return Returns the event stream trait.
         */
        public StringTrait getEventStreamTrait() {
            return eventStreamTrait;
        }

        /**
         * Gets the input or output structure of the data.
         *
         * @return Returns the structure.
         */
        public StructureShape getStructure() {
            return structure;
        }

        /**
         * Gets the top-level input or output member that targets the
         * event stream.
         *
         * @return Returns the member.
         */
        public MemberShape getEventStreamMember() {
            return eventStreamMember;
        }

        /**
         * Gets the shape that is streamed over the event stream.
         *
         * <p>If the target shape is a structure, then the event stream
         * is a single-event event stream that streams events of a single
         * shape. If the target shape is a union, then the event stream is
         * a multi-event event stream that streams one or more named events
         * of various structure shapes.
         *
         * @return Returns a structure or union shape.
         */
        public Shape getEventStreamTarget() {
            return eventStreamTarget;
        }

        /**
         * Gets the initial message members that form the initial-request
         * or initial-response of the event stream.
         *
         * <p>The initial message members are the members of the input or
         * output of an operation that are not the target of an event stream.
         *
         * <p>The returned map is a map of member names to
         * {@link MemberShape}s. Use {@link #getInitialMessageTargets} to get
         * a mapping of member names to the shapes targeted by a member.
         *
         * <pre>{@code
         * EventStreamIndex index = model.getKnowledge(EventStreamIndex.class);
         * EventStreamIndex.Info info = index.getInputInfo(myShapeId);
         *
         * for (MemberShape member : info.getInitialMessageMembers()) {
         *     System.out.println("Initial message member: " + member);
         * }
         * }</pre>
         *
         * @return Returns the initial event members (if any).
         */
        public Map<String, MemberShape> getInitialMessageMembers() {
            return initialMembers;
        }

        /**
         * Gets the shapes targeted by the initial message members that are
         * not the target of an event stream.
         *
         * <p>The returned map contains the same information as
         * {@link #getInitialMessageMembers} except each value is the shape
         * targeted by the member.
         *
         * @return Returns the shapes targeted by the initial message members.
         *
         * @see #getInitialMessageMembers
         */
        public Map<String, Shape> getInitialMessageTargets() {
            return initialTargets;
        }

        /**
         * Checks if this is a single-event event stream, meaning that only
         * a single-event shape is streamed over the event stream zero or
         * more times.
         *
         * @return Returns true if this is a single-event event stream.
         */
        public boolean isSingleEvent() {
            return eventStreamTarget.isStructureShape();
        }

        /**
         * Returns true if the event stream has an initial-message, meaning
         * there are top-level members of the input or output of the operation
         * that are not the event stream member.
         *
         * @return Returns true if there is an initial message.
         */
        public boolean hasInitialMessage() {
            return !initialMembers.isEmpty();
        }

        /**
         * Gets all of the event stream events of the event stream.
         *
         * <p>The returned map is a mapping of the event name to the event.
         * For multi-event event streams, the name of hte event is the member
         * name of the multi-event union. For single-event event streams,
         * the name of the event is the name of the input or output structure
         * member referenced by the event stream trait.
         *
         * @return Returns a map of event stream events.
         */
        public Map<String, StructureShape> getEvents() {
            return events;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (!(o instanceof Info)) {
                return false;
            }

            var that = (Info) o;
            return operation.getId().equals(that.operation.getId())
                   && structure.getId().equals(that.structure.getId())
                   && eventStreamMember.getId().equals(that.eventStreamMember.getId())
                   && initialMembers.equals(that.initialMembers);
        }

        @Override
        public int hashCode() {
            return Objects.hash(operation.getId(), structure.getId());
        }
    }
}
