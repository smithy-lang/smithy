/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.mqtt.traits;

import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.Trait;

/**
 * Contains computed information about the resolved MQTT topic bindings
 * of an operation.
 *
 * <p>Smithy models can contain a number of MQTT topics that are formed by
 * the {@code publish} and {@code subscribe} traits. Each of these
 * traits resolves to a single MQTT topic.
 *
 * <p>This class abstracts away the process of computing the payload of an
 * MQTT operation. Each topic binding provides the following information:
 *
 * <ul>
 *     <li>The operation in the model that created the binding.</li>
 *     <li>The MQTT topic.</li>
 *     <li>The payload shape that forms the payload of the topic.</li>
 *     <li>The optionally present input structure of the operation, which
 *     provides information like topic label bindings.</li>
 * </ul>
 *
 * <h2>@smithy.mqtt#publish topics</h2>
 *
 * Operations marked with {@code @smithy.mqtt#publish} resolve to a single topic that
 * is defined by topic property of the trait.
 *
 * <h2>subscribe topics</h2>
 *
 * Operations marked with {@code subscribe} resolve to a single topic
 * that is defined by the topic property of the trait.
 *
 * <h2>Payload resolution</h2>
 *
 * The payload binding of a topic binding can be resolved to either no
 * payload (e.g., publish operations with no input shape), a structure
 * payload, or a member of a structure. The target of a specific event
 * shape member can be used as a payload in {@code subscribe}
 * operations where an event member is marked with the {@code eventPayload}
 * trait.
 */
public final class TopicBinding<T extends Trait> {
    private final OperationShape operation;
    private final T mqttTrait;
    private final Topic topic;
    private final Shape payloadShape;
    private final StructureShape input;

    TopicBinding(
            OperationShape operation,
            T mqttTrait,
            Topic topic,
            Shape payloadShape,
            StructureShape input
    ) {
        this.operation = operation;
        this.mqttTrait = mqttTrait;
        this.topic = topic;
        this.payloadShape = payloadShape;
        this.input = input;
    }

    /**
     * Finds the MQTT binding trait associated with an operation.
     *
     * @param operation Operation to check.
     * @return Returns the optionally found MQTT binding trait.
     */
    public static Optional<? extends Trait> getOperationMqttTrait(Shape operation) {
        if (operation.hasTrait(PublishTrait.class)) {
            return operation.getTrait(PublishTrait.class);
        } else if (operation.hasTrait(SubscribeTrait.class)) {
            return operation.getTrait(SubscribeTrait.class);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Gets the trait that formed the binding of the operation to MQTT
     * for this topic.
     *
     * <p>The provided trait will be either an instance of
     * {@link PublishTrait} or {@link SubscribeTrait}.
     *
     * @return Returns the MQTT trait binding.
     */
    public T getMqttTrait() {
        return mqttTrait;
    }

    /**
     * Gets the operation shape related to the topic bindings.
     *
     * @return Returns the operation that has topic bindings.
     */
    public OperationShape getOperation() {
        return operation;
    }

    /**
     * Gets the resolved topic of the binding.
     *
     * @return Returns the resolved topic.
     */
    public Topic getTopic() {
        return topic;
    }

    /**
     * Gets the input shape related to this operation.
     *
     * @return Returns the optional input shape.
     * @deprecated Use getInputShape instead.
     */
    @Deprecated
    public Optional<StructureShape> getInput() {
        return Optional.ofNullable(input);
    }

    /**
     * Gets the input shape related to this operation.
     *
     * @return Returns the input shape.
     */
    public StructureShape getInputShape() {
        return input;
    }

    /**
     * Gets the payload shape of the topic.
     *
     * <p>The payload shape is either a structure for a single-event
     * event stream, or a union for a multi-event event stream.
     *
     * @return Returns the optional payload target.
     */
    public Optional<Shape> getPayloadShape() {
        return Optional.ofNullable(payloadShape);
    }

    /**
     * Returns true if the topic binding conflicts with another.
     *
     * <p>A topic binding is considered conflicting if both bindings have
     * conflicting topics determined through {@link Topic#conflictsWith}
     * and the topic bindings utilize a different payload. If both topics
     * do not have a payload then they are not conflicting. If both topics
     * have a payload, but they target the same shape, then they are not
     * conflicting.
     *
     * @param other Other topic binding to compare against.
     * @return Returns true if the topic bindings conflict.
     */
    public boolean conflictsWith(TopicBinding<?> other) {
        // The bindings can't conflict if they topics don't conflict.
        if (!getTopic().conflictsWith(other.getTopic())) {
            return false;
        }

        // If one has a payload and the other does not, then they conflict.
        if (payloadShape == null) {
            return other.payloadShape != null;
        } else if (other.payloadShape == null) {
            return true;
        }

        // Can only conflict at this point if they target different shapes.
        ShapeId targetA = getPayloadShape().get().getId();
        ShapeId targetB = other.getPayloadShape().get().getId();
        return !targetA.equals(targetB);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TopicBinding)) {
            return false;
        }
        TopicBinding that = (TopicBinding) o;
        return operation.equals(that.operation)
                && getInput().equals(that.getInput())
                && mqttTrait.equals(that.mqttTrait)
                && getTopic().equals(that.getTopic())
                && getPayloadShape().equals(that.getPayloadShape());
    }

    @Override
    public int hashCode() {
        return Objects.hash(operation.getId(), mqttTrait.toShapeId());
    }

    @Override
    public String toString() {
        return "TopicBinding{"
                + "operation=" + operation.getId()
                + ", input=" + getInput().map(Shape::getId).map(ShapeId::toString).orElse("null")
                + ", mqttTrait=" + mqttTrait.toShapeId()
                + ", topic=" + topic
                + ", payloadShape=" + String.valueOf(payloadShape)
                + '}';
    }
}
