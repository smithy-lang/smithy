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
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.mqtt.traits.SubscribeTrait;
import software.amazon.smithy.mqtt.traits.TopicLabelTrait;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Validates {@code @smithy.mqtt#subscribe} operation input.
 *
 * <ul>
 *     <li>Subscribe operation input members must all have mqttLabel trait.</li>
 * </ul>
 */
@SmithyInternalApi
public final class MqttSubscribeInputValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        OperationIndex operationIndex = OperationIndex.of(model);
        List<ValidationEvent> events = new ArrayList<>();
        for (OperationShape shape : model.getOperationShapesWithTrait(SubscribeTrait.class)) {
            validateOperation(shape, operationIndex.expectInputShape(shape), events);
        }
        return events;
    }

    private void validateOperation(OperationShape operation, StructureShape input, List<ValidationEvent> events) {
        for (MemberShape member : input.getAllMembers().values()) {
            if (!member.hasTrait(TopicLabelTrait.class)) {
                events.add(error(member, String.format(
                        "All input members of an operation marked with the `smithy.mqtt#subscribe` trait "
                        + "must be marked with the `smithy.mqtt#topicLabel` trait, and this member is used "
                        + "as part of the input of the `%s` operation.", operation.getId())));
            }
        }
    }
}
