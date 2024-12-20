/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.neighbor.RelationshipType;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeType;
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
        NeighborProvider reverse = NeighborProviderIndex.of(model).getReverseProvider();

        for (Shape shape : model.getShapesWithTrait(DefaultTrait.class)) {
            // Validates both root level constraints and member constraints against the default value.
            DefaultTrait trait = shape.expectTrait(DefaultTrait.class);
            visitor = validateShapeValue(model, shape, trait, visitor, events);
            Node value = trait.toNode();

            if (shape.isMemberShape()) {
                continue;
            }

            // Validate that members that target this shape redefine the default value on the member too.
            for (Relationship rel : reverse.getNeighbors(shape)) {
                if (rel.getRelationshipType() == RelationshipType.MEMBER_TARGET) {
                    MemberShape member = rel.getShape().asMemberShape().orElseThrow(() -> {
                        return new ExpectationNotMetException("Expected shape to be a member", rel.getShape());
                    });
                    if (model.expectShape(member.getContainer()).getType() == ShapeType.STRUCTURE) {
                        DefaultTrait memberDefault = member.getTrait(DefaultTrait.class).orElse(null);
                        if (memberDefault == null) {
                            events.add(error(member,
                                    String.format(
                                            "Member targets %s, which requires that the member defines the same default "
                                                    + "of `%s` or `null`",
                                            shape.toShapeId(),
                                            Node.printJson(value))));
                        } else if (!memberDefault.toNode().isNullNode()
                                && !value.equals(member.expectTrait(DefaultTrait.class).toNode())) {
                            // The member trait is not set to null nor does it match the target defualt.
                            events.add(error(member,
                                    String.format(
                                            "Member defines a default value that differs from the default value of the "
                                                    + "target shape, %s. The member has a default of `%s`, but the target has a "
                                                    + "default of `%s`.",
                                            shape.toShapeId(),
                                            member.expectTrait(DefaultTrait.class).toNode(),
                                            Node.printJson(value))));
                        }
                    }
                }
            }
        }

        return events;
    }

    private NodeValidationVisitor validateShapeValue(
            Model model,
            Shape shape,
            DefaultTrait trait,
            NodeValidationVisitor visitor,
            List<ValidationEvent> events
    ) {
        Node value = trait.toNode();
        Shape shapeTarget = shape;

        if (shape.isMemberShape()) {
            shapeTarget = model.expectShape(shape.asMemberShape().get().getTarget());
            // Any member can set the default to null, overriding the default of the target shape
            // causing the member to be considered nullable.
            if (value.isNullNode()) {
                return visitor;
            }
        } else if (value.isNullNode()) {
            events.add(error(shape, trait, "The @default trait can be set to null only on members"));
            return visitor;
        }

        visitor = createOrReuseVisitor(model, visitor, value, shape);
        events.addAll(shape.accept(visitor));

        switch (shapeTarget.getType()) {
            case BLOB:
                try {
                    value.asStringNode().ifPresent(val -> {
                        Base64.getDecoder().decode(val.getValue().getBytes(StandardCharsets.UTF_8));
                    });
                } catch (IllegalArgumentException exc) {
                    events.add(warning(shape, trait, "The @default value of a blob should be a valid base64 string."));
                }
                break;
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
                        events.add(error(shape,
                                trait,
                                "The @default value of a document cannot be a non-empty "
                                        + "array"));
                    }
                });
                value.asObjectNode().ifPresent(obj -> {
                    if (!obj.isEmpty()) {
                        events.add(error(shape,
                                trait,
                                "The @default value of a document cannot be a non-empty "
                                        + "object"));
                    }
                });
                break;
            default:
                break;
        }

        return visitor;
    }

    private NodeValidationVisitor createOrReuseVisitor(
            Model model,
            NodeValidationVisitor visitor,
            Node value,
            Shape shape
    ) {
        if (visitor == null) {
            return NodeValidationVisitor
                    .builder()
                    .model(model)
                    .eventId(getName())
                    .value(value)
                    .startingContext("Error validating @default trait")
                    .eventShapeId(shape.getId())
                    // Use WARNING for range trait errors so that a Smithy model 1.0 to 2.0 conversion can automatically
                    // suppress any errors to losslessly handle the conversion.
                    .addFeature(NodeValidationVisitor.Feature.RANGE_TRAIT_ZERO_VALUE_WARNING)
                    .build();
        } else {
            visitor.setValue(value);
            visitor.setEventShapeId(shape.getId());
            return visitor;
        }
    }
}
