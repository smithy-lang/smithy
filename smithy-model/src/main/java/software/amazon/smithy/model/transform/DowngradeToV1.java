/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NullableIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.AddedDefaultTrait;
import software.amazon.smithy.model.traits.ClientOptionalTrait;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.NotPropertyTrait;
import software.amazon.smithy.model.traits.PropertyTrait;

final class DowngradeToV1 {

    Model transform(ModelTransformer transformer, Model model) {
        // Flatten and remove mixins since they aren't part of IDL 1.0
        model = transformer.flattenAndRemoveMixins(model);

        // Change enums to string shapes and intEnums to integers.
        model = downgradeEnums(transformer, model);

        // Remove resource properties
        model = removeResourceProperties(transformer, model);

        // Remove default traits that do not correlate to box traits from v1.
        model = removeUnnecessaryDefaults(transformer, model);

        return removeOtherV2Traits(transformer, model);
    }

    private Model downgradeEnums(ModelTransformer transformer, Model model) {
        Map<ShapeId, ShapeType> typeChanges = new HashMap<>();

        for (Shape shape : model.getEnumShapes()) {
            typeChanges.put(shape.getId(), ShapeType.STRING);
        }

        for (Shape shape : model.getIntEnumShapes()) {
            typeChanges.put(shape.getId(), ShapeType.INTEGER);
        }

        return transformer.changeShapeType(model, typeChanges);
    }

    private Model removeResourceProperties(ModelTransformer transformer, Model model) {
        List<Shape> updates = new ArrayList<>();

        // Remove the "properties" key from resources.
        for (ResourceShape shape : model.getResourceShapes()) {
            if (shape.hasProperties()) {
                updates.add(shape.toBuilder().properties(Collections.emptyMap()).build());
            }
        }

        // Remove @notProperty.
        for (Shape shape : model.getShapesWithTrait(NotPropertyTrait.class)) {
            updates.add(Shape.shapeToBuilder(shape).removeTrait(NotPropertyTrait.ID).build());
        }

        // Remove @property.
        for (Shape shape : model.getShapesWithTrait(PropertyTrait.class)) {
            updates.add(Shape.shapeToBuilder(shape).removeTrait(PropertyTrait.ID).build());
        }

        return transformer.replaceShapes(model, updates);
    }

    private Model removeUnnecessaryDefaults(ModelTransformer transformer, Model model) {
        Set<Shape> updates = new HashSet<>();

        // Remove addedDefault traits, and any found default trait if present.
        for (MemberShape shape : model.getMemberShapesWithTrait(AddedDefaultTrait.class)) {
            updates.add(shape.toBuilder().removeTrait(DefaultTrait.ID).removeTrait(AddedDefaultTrait.ID).build());
        }

        for (Shape shape : model.getShapesWithTrait(DefaultTrait.class)) {
            if (removeDefaultFromShape(shape, model)) {
                updates.add(Shape.shapeToBuilder(shape)
                        .removeTrait(DefaultTrait.ID)
                        .removeTrait(AddedDefaultTrait.ID)
                        .build());
            }
        }

        return transformer.replaceShapes(model, updates);
    }

    private boolean removeDefaultFromShape(Shape shape, Model model) {
        DefaultTrait trait = shape.expectTrait(DefaultTrait.class);

        // Members with a null default are considered boxed. Keep the trait to retain consistency with other
        // indexes and checks.
        if (trait.toNode().isNullNode()) {
            return false;
        }

        Shape target = model.expectShape(shape.asMemberShape().map(MemberShape::getTarget).orElse(shape.getId()));
        DefaultTrait targetDefault = target.getTrait(DefaultTrait.class).orElse(null);

        // If the target shape has no default trait or it isn't equal to the default trait of the member, then
        // the default value has no representation in Smithy 1.0 models.
        if (targetDefault == null || !targetDefault.toNode().equals(trait.toNode())) {
            return true;
        }

        return !NullableIndex.isDefaultZeroValueOfTypeInV1(trait.toNode(), target.getType());
    }

    private Model removeOtherV2Traits(ModelTransformer transformer, Model model) {
        Set<Shape> updates = new HashSet<>();

        for (StructureShape structure : model.getStructureShapes()) {
            for (MemberShape member : structure.getAllMembers().values()) {
                if (member.hasTrait(ClientOptionalTrait.class)) {
                    updates.add(member.toBuilder().removeTrait(ClientOptionalTrait.ID).build());
                }
            }
        }

        return transformer.replaceShapes(model, updates);
    }
}
