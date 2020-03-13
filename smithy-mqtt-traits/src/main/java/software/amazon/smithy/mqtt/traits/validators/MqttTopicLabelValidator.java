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

package software.amazon.smithy.mqtt.traits.validators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.mqtt.traits.PublishTrait;
import software.amazon.smithy.mqtt.traits.SubscribeTrait;
import software.amazon.smithy.mqtt.traits.Topic;
import software.amazon.smithy.mqtt.traits.TopicLabelTrait;

/**
 * Validates that labels are correct for MQTT topics on
 * {@code subscribe} and {@code publish} operations.
 *
 * <ul>
 *     <li>Operation input is required when a topic has labels.</li>
 *     <li>Topic labels must be be found in the input.</li>
 *     <li>The input must not contain extraneous labels.</li>
 * </ul>
 */
public class MqttTopicLabelValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        return model.shapes(OperationShape.class)
                .map(MqttTopicLabelValidator::createTopics)
                .filter(Objects::nonNull)
                .flatMap(topics -> validateMqtt(model, topics).stream())
                .collect(Collectors.toList());
    }

    private static TopicCollection createTopics(OperationShape shape) {
        if (shape.hasTrait(SubscribeTrait.class)) {
            SubscribeTrait trait = shape.expectTrait(SubscribeTrait.class);
            List<Topic> bindings = Collections.singletonList(trait.getTopic());
            return new TopicCollection(shape, trait, bindings);
        } else if (shape.hasTrait(PublishTrait.class)) {
            PublishTrait trait = shape.expectTrait(PublishTrait.class);
            List<Topic> bindings = Collections.singletonList(trait.getTopic());
            return new TopicCollection(shape, trait, bindings);
        } else {
            return null;
        }
    }

    private List<ValidationEvent> validateMqtt(Model model, TopicCollection topics) {
        Set<String> labels = topics.getLabels();
        StructureShape input = topics.operation.getInput()
                .flatMap(model::getShape)
                .flatMap(Shape::asStructureShape)
                .orElse(null);

        if (!labels.isEmpty() && input == null) {
            return Collections.singletonList(error(topics.operation, topics.trait, String.format(
                    "Operation MQTT trait, `%s`, contains topic labels, [%s], but the operation has no input",
                    Trait.getIdiomaticTraitName(topics.trait.toShapeId()),
                    ValidationUtils.tickedList(labels))));
        }

        if (input == null) {
            // No labels, and no input.
            return Collections.emptyList();
        }

        List<ValidationEvent> events = new ArrayList<>();
        for (MemberShape member : input.getAllMembers().values()) {
            if (member.hasTrait(TopicLabelTrait.class)) {
                if (labels.contains(member.getMemberName())) {
                    labels.remove(member.getMemberName());
                } else {
                    events.add(error(member, member.expectTrait(TopicLabelTrait.class), String.format(
                            "This member is marked with the `smithy.mqtt#topicLabel` trait, but when this member is "
                            + "used as part of the input of the `%s` operation, a corresponding label cannot be "
                            + "found in the `%s` trait",
                            topics.operation.getId(),
                            Trait.getIdiomaticTraitName(topics.trait.toShapeId()))));
                }
            }
        }

        if (!labels.isEmpty()) {
            events.add(error(topics.operation, topics.trait, String.format(
                    "The `%s` trait contains the following topic labels that could not be found in the input "
                    + "structure of the operation or were not marked with the `smithy.mqtt#topicLabel` trait: [%s]",
                    Trait.getIdiomaticTraitName(topics.trait.toShapeId()),
                    ValidationUtils.tickedList(labels))));
        }

        return events;
    }

    private static final class TopicCollection {
        final List<Topic> topics;
        final OperationShape operation;
        final Trait trait;

        TopicCollection(OperationShape operation, Trait trait, List<Topic> topics) {
            this.operation = operation;
            this.trait = trait;
            this.topics = topics;
        }

        Set<String> getLabels() {
            return topics.stream()
                    .flatMap(topic -> topic.getLabels().stream().map(Topic.Level::getContent))
                    .collect(Collectors.toSet());
        }
    }
}
