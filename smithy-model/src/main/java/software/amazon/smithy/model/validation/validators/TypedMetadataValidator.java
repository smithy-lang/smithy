/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIdSyntaxException;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.NodeValidationVisitor;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates that typed metadata matches the structure of its declared shape.
 */
public final class TypedMetadataValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        NodeValidationVisitor nodeValidator = NodeValidationVisitor.builder()
                .eventId("MetadataType")
                .model(model)
                .value(Node.nullNode())
                .build();

        List<ValidationEvent> events = new ArrayList<>();
        Map<String, Node> allMetadata = model.getMetadata();
        for (Map.Entry<String, Node> entry : allMetadata.entrySet()) {
            if (entry.getValue().isObjectNode()) {
                ObjectNode node = entry.getValue().expectObjectNode();
                // NOTE: Doesn't check if __type__ isn't a string
                StringNode typeId = node.getStringMember("__type__").orElse(null);
                if (typeId != null) {
                    try {
                        ShapeId shapeId = ShapeId.from(typeId.getValue());
                        Shape shape = model.getShape(shapeId).orElse(null);
                        if (shape != null) {
                            if (!shape.isStructureShape()) {
                                events.add(ValidationEvent.builder()
                                        .id("MetadataType.InvalidShapeType")
                                        .shapeId(shapeId)
                                        .severity(Severity.ERROR)
                                        .sourceLocation(typeId)
                                        .message("Metadata type must be a structure shape, but `" + typeId.getValue()
                                                + "` is a " + shape.getType().toString() + ".")
                                        .build());
                            } else {
                                // TODO: Don't emit 'Member __type__ does not exist' warning. This filter should only apply
                                //       specifically to the __type__ member in the top-level value. We could filter it out here.
                                nodeValidator.setEventShapeId(shapeId);
                                nodeValidator.setValue(node);
                                events.addAll(shape.accept(nodeValidator));
                            }
                        } else {
                            events.add(ValidationEvent.builder()
                                    .id("MetadataType.ShapeNotFound")
                                    .shapeId(shapeId)
                                    .severity(Severity.ERROR)
                                    .sourceLocation(typeId)
                                    .message("Shape `" + typeId.getValue() + "` not found.")
                                    .build());
                        }
                    } catch (ShapeIdSyntaxException e) {
                        events.add(ValidationEvent.builder()
                                .id("MetadataType.InvalidShapeId")
                                .severity(Severity.ERROR)
                                .sourceLocation(typeId)
                                .message("`__type__` must be a valid shape id.")
                                .build());
                    }
                }
            }
        }

        return events;
    }
}
