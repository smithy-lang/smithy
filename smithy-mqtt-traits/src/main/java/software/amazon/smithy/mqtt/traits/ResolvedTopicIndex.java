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
 * ResolvedTopicIndex resolvedIndex = model.getKnowledge(ResolvedTopicIndex.class);
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
        EventStreamIndex eventStreamIndex = model.getKnowledge(EventStreamIndex.class);
        OperationIndex operationIndex = model.getKnowledge(OperationIndex.class);

        model.shapes(OperationShape.class).forEach(operation -> {
            if (operation.hasTrait(PublishTrait.class)) {
                PublishTrait trait = operation.expectTrait(PublishTrait.class);
                createPublishBindings(operationIndex, operation, trait);
            } else if (operation.hasTrait(SubscribeTrait.class)) {
                SubscribeTrait trait = operation.expectTrait(SubscribeTrait.class);
                StructureShape input = operationIndex.getInput(operation).orElse(null);
                createSubscribeBinding(input, eventStreamIndex, operation, trait);
            }
        });
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
        TopicBinding<PublishTrait> topicBinding = operationIndex.getInput(operation)
                // Use the input to create a publish binding.
                .map(input -> new TopicBinding<>(operation, trait, trait.getTopic(), input, input))
                // The binding has no valid input.
                .orElseGet(() -> new TopicBinding<>(operation, trait, trait.getTopic(), null, null));

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
                    operation, trait, trait.getTopic(), outputInfo.getEventStreamTarget(), input);
            subscribeBindings.put(operation.getId(), binding);
            subscribeInfo.put(operation.getId(), outputInfo);
        }
    }
}
