/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.knowledge;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.StreamingTrait;

/**
 * Contains extracted event stream information.
 */
public final class EventStreamInfo {
    private final OperationShape operation;
    private final StructureShape structure;
    private final MemberShape eventStreamMember;
    private final Shape eventStreamTarget;
    private final Map<String, MemberShape> initialMembers;
    private final Map<String, Shape> initialTargets;
    private final Map<String, StructureShape> events;
    private final StreamingTrait streamingTrait;

    EventStreamInfo(
            OperationShape operation,
            StreamingTrait streamingTrait,
            StructureShape structure,
            MemberShape eventStreamMember,
            Shape eventStreamTarget,
            Map<String, MemberShape> initialMembers,
            Map<String, Shape> initialTargets,
            Map<String, StructureShape> events
    ) {
        this.operation = operation;
        this.streamingTrait = streamingTrait;
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
    public StreamingTrait getStreamingTrait() {
        return streamingTrait;
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
     * EventStreamIndex index = EventStreamIndex.of(model);
     * EventStreamInfo info = index.getInputInfo(myShapeId);
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
        } else if (!(o instanceof EventStreamInfo)) {
            return false;
        }

        EventStreamInfo that = (EventStreamInfo) o;
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
