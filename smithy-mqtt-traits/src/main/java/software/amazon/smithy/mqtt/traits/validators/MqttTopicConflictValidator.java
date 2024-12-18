/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.mqtt.traits.validators;

import java.util.List;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.mqtt.traits.ResolvedTopicIndex;
import software.amazon.smithy.mqtt.traits.TopicBinding;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Validates that no two MQTT topics conflict.
 *
 * <p>MQTT topics are allowed to conflict if both topics target the
 * same shape.
 */
@SmithyInternalApi
public final class MqttTopicConflictValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        ResolvedTopicIndex bindingIndex = ResolvedTopicIndex.of(model);

        // Find conflicting topic bindings for each resolved topic.
        return bindingIndex.topicBindings()
                .map(binding -> Pair.of(binding,
                        bindingIndex.topicBindings()
                                .filter(other -> other.conflictsWith(binding))
                                .collect(Collectors.toList())))
                // Only care when multiple entries are present.
                .filter(conflictingPair -> !conflictingPair.getRight().isEmpty())
                .map(conflictingPair -> invalidBindings(conflictingPair.getLeft(), conflictingPair.getRight()))
                .collect(Collectors.toList());
    }

    private ValidationEvent invalidBindings(
            TopicBinding<? extends Trait> binding,
            List<TopicBinding<?>> conflicts
    ) {
        String payloadShape = binding.getPayloadShape()
                .map(shape -> shape.getId().toString())
                .orElse("N/A");
        String conflictingStrings = conflicts.stream()
                .filter(b -> !b.equals(binding))
                .map(this::createConflictingBindingDescriptor)
                .sorted()
                .collect(Collectors.joining(", "));
        String message = String.format(
                "This shape resolves to an MQTT topic `%s` as part of the `%s` operation, and its payload of "
                        + "`%s` conflicts with other topic payloads: [%s]",
                binding.getTopic(),
                binding.getOperation().getId(),
                payloadShape,
                conflictingStrings);
        return error(binding.getOperation(), binding.getMqttTrait(), message);
    }

    private String createConflictingBindingDescriptor(TopicBinding<? extends Trait> binding) {
        return String.format(
                "`%s` trait payload %s of `%s`",
                Trait.getIdiomaticTraitName(binding.getMqttTrait().toShapeId()),
                binding.getPayloadShape().map(Shape::getId).map(id -> "`" + id + "`").orElse("N/A"),
                binding.getOperation().getId());
    }
}
