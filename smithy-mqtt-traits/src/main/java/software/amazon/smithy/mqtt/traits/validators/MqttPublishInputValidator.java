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
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.mqtt.traits.PublishTrait;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Publish operations must not contain event streams.
 */
@SmithyInternalApi
public final class MqttPublishInputValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        OperationIndex operationIndex = OperationIndex.of(model);
        List<ValidationEvent> events = new ArrayList<>();
        for (OperationShape shape : model.getOperationShapesWithTrait(PublishTrait.class)) {
            validateOperation(model, shape, operationIndex.expectInputShape(shape), events);
        }
        return events;
    }

    private void validateOperation(
            Model model,
            OperationShape operation,
            StructureShape input,
            List<ValidationEvent> events
    ) {
        for (MemberShape member : input.getAllMembers().values()) {
            if (StreamingTrait.isEventStream(model, member)) {
                events.add(error(member, String.format(
                        "The input of `smithy.mqtt#publish` operations cannot contain event streams, "
                        + "and this member is used as part of the input of the `%s` operation.",
                        operation.getId())));
            }
        }
    }
}
