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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.EventStreamIndex;
import software.amazon.smithy.model.knowledge.EventStreamInfo;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.EventHeaderTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.mqtt.traits.SubscribeTrait;
import software.amazon.smithy.utils.OptionalUtils;

/**
 * Validates {@code subscribe} operation output.
 *
 * <ul>
 *     <li>Subscribe operations must use an event stream.</li>
 *     <li>Subscribe operations must not have initial events.</li>
 *     <li>Subscribe operations should not use eventHeader traits.</li>
 * </ul>
 */
public final class MqttSubscribeOutputValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        EventStreamIndex eventStreamIndex = model.getKnowledge(EventStreamIndex.class);
        return model.shapes(OperationShape.class)
                .flatMap(shape -> Trait.flatMapStream(shape, SubscribeTrait.class))
                .flatMap(pair -> validateOperation(model, pair.getLeft(), eventStreamIndex).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateOperation(
            Model model,
            OperationShape shape,
            EventStreamIndex eventStreamIndex
    ) {
        EventStreamInfo info = eventStreamIndex.getOutputInfo(shape).orElse(null);

        if (info == null) {
            // This is validated in the trait selector.
            return Collections.emptyList();
        }

        List<ValidationEvent> events = new ArrayList<>();

        if (info.hasInitialMessage()) {
            events.add(error(shape, shape.expectTrait(SubscribeTrait.class),
                             "Operations marked with the `smithy.mqtt#subscribe` trait must not utilize event "
                             + "streams with initial responses in their output structure."));
        }

        // Find events in the output's event stream that have members marked
        // with the eventHeader trait.
        getOutputEvents(info, model)
                .flatMap(target -> target.getAllMembers().values().stream()
                        .filter(member -> member.hasTrait(EventHeaderTrait.class)))
                .map(member -> danger(shape, "This member is used as part of an MQTT event stream event, and MQTT "
                                             + "event streams do not support the eventHeader trait."))
                .forEach(events::add);

        return events;
    }

    private Stream<StructureShape> getOutputEvents(EventStreamInfo info, Model model) {
        return info.getEventStreamTarget().accept(new ShapeVisitor.Default<Stream<StructureShape>>() {
            @Override
            public Stream<StructureShape> getDefault(Shape shape) {
                return Stream.empty();
            }

            @Override
            public Stream<StructureShape> unionShape(UnionShape shape) {
                return shape.getAllMembers().entrySet().stream()
                        .flatMap(member -> OptionalUtils.stream(model.getShape(member.getValue().getTarget())
                                .flatMap(Shape::asStructureShape)));
            }

            @Override
            public Stream<StructureShape> structureShape(StructureShape shape) {
                return Stream.of(info.getEventStreamTarget().expectStructureShape());
            }
        });
    }
}
