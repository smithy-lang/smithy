/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.mqtt.traits;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.EventStreamIndex;
import software.amazon.smithy.model.knowledge.EventStreamInfo;
import software.amazon.smithy.model.knowledge.KnowledgeIndex;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.Trait;

/**
 * Computes all of the MQTT {@link TopicBinding}s in a model.
 *
 * <p>This index is useful for things like finding the payload of an MQTT
 * topic on an operation and grabbing the event stream information of a
 * subscribe operation.
 *
 * <pre>
 * {@code
 * ResolvedTopicIndex resolvedIndex = ResolvedTopicIndex.of(model);
 * TopicBinding<PublishTrait> binding = resolvedIndex.getPublishBinding(myOperation).get();
 *
 * assert(binding.getTopic() instanceOf Topic);
 * assert(binding.getMqttTrait() instanceOf PublishTrait);
 * System.out.println(binding.getPayloadShape());
 * }
 * </pre>
 *
 * @see TopicBinding
 */
public final class ResolvedTopicIndex implements KnowledgeIndex {
    private final Map<ShapeId, TopicBinding<PublishTrait>> publishBindings = new HashMap<>();
    private final Map<ShapeId, TopicBinding<SubscribeTrait>> subscribeBindings = new HashMap<>();
    private final Map<ShapeId, EventStreamInfo> subscribeInfo = new HashMap<>();

    public ResolvedTopicIndex(Model model) {
        // Find all the MQTT topic bindings in the model.
        EventStreamIndex eventStreamIndex = EventStreamIndex.of(model);
        OperationIndex operationIndex = OperationIndex.of(model);

        model.shapes(OperationShape.class).forEach(operation -> {
            if (operation.hasTrait(PublishTrait.class)) {
                PublishTrait trait = operation.getTrait(PublishTrait.class).get();
                createPublishBindings(operationIndex, operation, trait);
            } else if (operation.hasTrait(SubscribeTrait.class)) {
                SubscribeTrait trait = operation.getTrait(SubscribeTrait.class).get();
                StructureShape input = operationIndex.expectInputShape(operation);
                createSubscribeBinding(input, eventStreamIndex, operation, trait);
            }
        });
    }

    public static ResolvedTopicIndex of(Model model) {
        return model.getKnowledge(ResolvedTopicIndex.class, ResolvedTopicIndex::new);
    }

    /**
     * Get all of the MQTT topic bindings of a specific operation.
     *
     * @param operation Operation that has MQTT bindings.
     * @return Returns the found MQTT bindings.
     */
    public List<TopicBinding<? extends Trait>> getOperationBindings(ToShapeId operation) {
        ShapeId id = operation.toShapeId();
        return topicBindings()
                .filter(binding -> binding.getOperation().getId().equals(id))
                .collect(Collectors.toList());
    }

    /**
     * Gets all resolved MQTT topic bindings in the model.
     *
     * @return Returns the stream of MQTT topic bindings in the model.
     */
    public Stream<TopicBinding<? extends Trait>> topicBindings() {
        return Stream.concat(publishBindings.values().stream(), subscribeBindings.values().stream());
    }

    /**
     * Gets the MQTT publish binding of an operation.
     *
     * @param operation Operation that has MQTT bindings.
     * @return Returns the optionally found MQTT publish bindings.
     */
    public Optional<TopicBinding<PublishTrait>> getPublishBinding(ToShapeId operation) {
        return Optional.ofNullable(publishBindings.get(operation.toShapeId()));
    }

    /**
     * Gets the MQTT subscribe binding of an operation.
     *
     * @param operation Operation that has MQTT bindings.
     * @return Returns the optionally found MQTT subscribe bindings.
     */
    public Optional<TopicBinding<SubscribeTrait>> getSubscribeBinding(ToShapeId operation) {
        return Optional.ofNullable(subscribeBindings.get(operation.toShapeId()));
    }

    /**
     * Get subscribe event stream info.
     *
     * <p>This information describes if the event stream contains a single
     * event, multiple events, and if there are any initial-request or
     * response members.
     *
     * @param operation Operation to get the event stream info o.
     * @return Returns the optionally found event stream info.
     */
    public Optional<EventStreamInfo> getSubcribeEventStreamInfo(ToShapeId operation) {
        return Optional.ofNullable(subscribeInfo.get(operation.toShapeId()));
    }

    private void createPublishBindings(
            OperationIndex operationIndex,
            OperationShape operation,
            PublishTrait trait
    ) {
        StructureShape input = operationIndex.expectInputShape(operation);
        TopicBinding<PublishTrait> topicBinding = new TopicBinding<>(operation, trait, trait.getTopic(), input, input);
        publishBindings.put(operation.getId(), topicBinding);
    }

    private void createSubscribeBinding(
            StructureShape input,
            EventStreamIndex eventStreamIndex,
            OperationShape operation,
            SubscribeTrait trait
    ) {
        EventStreamInfo outputInfo = eventStreamIndex.getOutputInfo(operation).orElse(null);

        // Subscribe operations must have an event stream. Omit the bindings
        // if an event stream is not found.
        if (outputInfo != null) {
            TopicBinding<SubscribeTrait> binding = new TopicBinding<>(
                    operation,
                    trait,
                    trait.getTopic(),
                    outputInfo.getEventStreamTarget(),
                    input);
            subscribeBindings.put(operation.getId(), binding);
            subscribeInfo.put(operation.getId(), outputInfo);
        }
    }
}
