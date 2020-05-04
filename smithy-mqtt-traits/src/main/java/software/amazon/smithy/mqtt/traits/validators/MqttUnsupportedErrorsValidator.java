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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.mqtt.traits.PublishTrait;
import software.amazon.smithy.mqtt.traits.SubscribeTrait;
import software.amazon.smithy.mqtt.traits.TopicBinding;
import software.amazon.smithy.utils.OptionalUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Validates that {@code @smithy.mqtt#publish} and {@code subscribe}
 * operations should not define errors.
 *
 * <p>Errors have to just come back in the event stream of the output.
 * This violation may be suppressed since it emits a DANGER event.
 */
@SmithyInternalApi
public final class MqttUnsupportedErrorsValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        return model.shapes(OperationShape.class)
                .filter(shape -> !shape.getErrors().isEmpty())
                .flatMap(shape -> OptionalUtils.stream(validateOperation(shape)))
                .collect(Collectors.toList());
    }

    private Optional<ValidationEvent> validateOperation(OperationShape shape) {
        return TopicBinding.getOperationMqttTrait(shape)
                .filter(trait -> trait instanceof PublishTrait || trait instanceof SubscribeTrait)
                .map(trait -> danger(shape, trait, String.format(
                        "Operations marked with the `%s` trait do not support errors.",
                        Trait.getIdiomaticTraitName(trait.toShapeId()))));
    }
}
