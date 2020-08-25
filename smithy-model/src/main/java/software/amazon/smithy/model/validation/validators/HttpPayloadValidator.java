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

package software.amazon.smithy.model.validation.validators;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.HttpBinding;
import software.amazon.smithy.model.knowledge.HttpBindingIndex;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;

/**
 * Validates that if a payload trait is present then all members of the
 * input of an operation are bound to part of the message.
 */
public final class HttpPayloadValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        // Skip the expensive checks if the httpPayload trait isn't event used.
        if (!model.isTraitApplied(HttpPayloadTrait.class)) {
            return Collections.emptyList();
        }

        OperationIndex opIndex = OperationIndex.of(model);
        HttpBindingIndex bindings = HttpBindingIndex.of(model);
        List<ValidationEvent> events = new ArrayList<>();
        events.addAll(model.shapes(OperationShape.class)
                .filter(shape -> shape.getTrait(HttpTrait.class).isPresent())
                .flatMap(shape -> validateOperation(bindings, opIndex, shape).stream())
                .collect(toList()));
        events.addAll(model.shapes(StructureShape.class)
                .flatMap(shape -> Trait.flatMapStream(shape, ErrorTrait.class))
                .flatMap(pair -> validateError(pair.getLeft(), bindings).stream())
                .collect(toList()));
        return events;
    }

    private List<ValidationEvent> validateOperation(
            HttpBindingIndex bindings,
            OperationIndex opIndex,
            OperationShape shape
    ) {
        List<ValidationEvent> events = new ArrayList<>();
        opIndex.getInput(shape.getId()).flatMap(struct -> validatePayload(shape.getId(), struct, bindings, true))
                        .ifPresent(events::add);
        opIndex.getOutput(shape.getId()).flatMap(struct -> validatePayload(shape.getId(), struct, bindings, false))
                        .ifPresent(events::add);
        return events;
    }

    private List<ValidationEvent> validateError(StructureShape shape, HttpBindingIndex bindings) {
        return validatePayload(shape.getId(), shape, bindings, false)
                .map(Collections::singletonList)
                .orElseGet(Collections::emptyList);
    }

    private Optional<ValidationEvent> validatePayload(
            ShapeId subject,
            StructureShape inputOrError,
            HttpBindingIndex bindings,
            boolean request
    ) {
        Map<String, HttpBinding> resolved = request
                ? bindings.getRequestBindings(subject)
                : bindings.getResponseBindings(subject);
        Set<String> unbound = resolved.entrySet().stream()
                .filter(binding -> binding.getValue().getLocation() == HttpBinding.Location.UNBOUND)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        if (!unbound.isEmpty()) {
            return Optional.of(error(inputOrError, String.format(
                    "A member of this structure is marked with the `httpPayload` trait, but the following "
                    + "structure members are not explicitly bound to the HTTP message: %s",
                    ValidationUtils.tickedList(unbound))));
        }

        return Optional.empty();
    }
}
