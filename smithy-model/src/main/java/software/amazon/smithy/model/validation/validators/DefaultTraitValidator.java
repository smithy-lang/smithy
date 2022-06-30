/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.NodeValidationVisitor;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SmithyInternalApi;

@SmithyInternalApi
public final class DefaultTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        NodeValidationVisitor visitor = null;

        // Validate that default values are appropriate for shapes.
        for (MemberShape shape : model.getMemberShapesWithTrait(DefaultTrait.class)) {
            DefaultTrait trait = shape.expectTrait(DefaultTrait.class);
            Node value = trait.toNode();

            if (visitor == null) {
                visitor = NodeValidationVisitor
                        .builder()
                        .model(model)
                        .eventId(getName())
                        .value(value)
                        .startingContext("Error validating @default trait")
                        .eventShapeId(shape.getId())
                        .build();
            } else {
                visitor.setValue(value);
                visitor.setEventShapeId(shape.getId());
            }

            events.addAll(shape.accept(visitor));
            switch (model.expectShape(shape.getTarget()).getType()) {
                case MAP:
                    value.asObjectNode().ifPresent(obj -> {
                        if (!obj.isEmpty()) {
                            events.add(error(shape, trait, "The @default value of a map must be an empty map"));
                        }
                    });
                    break;
                case LIST:
                case SET:
                    value.asArrayNode().ifPresent(array -> {
                        if (!array.isEmpty()) {
                            events.add(error(shape, trait, "The @default value of a list must be an empty list"));
                        }
                    });
                    break;
                case DOCUMENT:
                    value.asArrayNode().ifPresent(array -> {
                        if (!array.isEmpty()) {
                            events.add(error(shape, trait, "The @default value of a document cannot be a non-empty "
                                                           + "array"));
                        }
                    });
                    value.asObjectNode().ifPresent(obj -> {
                        if (!obj.isEmpty()) {
                            events.add(error(shape, trait, "The @default value of a document cannot be a non-empty "
                                                           + "object"));
                        }
                    });
                    break;
                default:
                    break;
            }
        }

        return events;
    }
}
