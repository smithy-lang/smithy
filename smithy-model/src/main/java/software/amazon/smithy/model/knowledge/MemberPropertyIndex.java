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

package software.amazon.smithy.model.knowledge;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.NestedPropertiesTrait;
import software.amazon.smithy.model.traits.NotPropertyTrait;
import software.amazon.smithy.model.traits.PropertyTrait;

/**
 * Index of structure member -> property inclusion.
 */
public final class MemberPropertyIndex implements KnowledgeIndex {
    private final Map<ShapeId, Boolean> memberShapeDoesNotRequireProperty = new HashMap<>();
    private final Map<ShapeId, String> memberShapeToPropertyName = new HashMap<>();
    private final Map<ShapeId, ShapeId> operationToInputPropertiesShape = new HashMap<>();
    private final Map<ShapeId, ShapeId> operationToOutputPropertiesShape = new HashMap<>();

    private MemberPropertyIndex(Model model) {
        OperationIndex operationIndex = OperationIndex.of(model);
        IdentifierBindingIndex identifierIndex = IdentifierBindingIndex.of(model);

        for (ResourceShape resourceShape : model.getResourceShapes()) {
            Set<String> propertyNames = resourceShape.getProperties().keySet();
            for (ShapeId operationShapeId : resourceShape.getAllOperations()) {
                OperationShape operationShape = (OperationShape) model.getShape(operationShapeId).get();
                Shape inputPropertiesShape = getInputPropertiesShape(model, operationIndex, operationShape);
                operationToInputPropertiesShape.put(operationShapeId, inputPropertiesShape.getId());
                for (MemberShape memberShape : inputPropertiesShape.members()) {
                    if (identifierIndex.getOperationInputBindings(resourceShape, operationShape).values()
                            .contains(memberShape.getMemberName())) {
                        memberShapeDoesNotRequireProperty.put(memberShape.toShapeId(), true);
                    } else {
                        memberShapeDoesNotRequireProperty.put(memberShape.toShapeId(),
                                doesNotRequireProperty(model, memberShape));
                    }
                    if (doesMemberShapeRequireProperty(memberShape.getId())
                            || propertyNames.contains(memberShape.getMemberName())) {
                        memberShapeToPropertyName.put(memberShape.getId(),
                                getPropertyTraitName(memberShape).orElse(memberShape.getMemberName()));
                    }
                }
                // nesting is taking place, so index top level input/output members as not property.
                if (!inputPropertiesShape.getId().equals(operationShape.getInputShape())) {
                    for (MemberShape memberShape : model.expectShape(operationShape.getInputShape()).members()) {
                        memberShapeDoesNotRequireProperty.put(memberShape.toShapeId(), true);
                    }
                }
                Shape outputPropertiesShape = getOutputPropertiesShape(model, operationIndex, operationShape);
                operationToOutputPropertiesShape.put(operationShapeId, outputPropertiesShape.getId());
                for (MemberShape memberShape : outputPropertiesShape.members()) {
                    if (identifierIndex.getOperationInputBindings(resourceShape, operationShape).values()
                            .contains(memberShape.getMemberName())) {
                        memberShapeDoesNotRequireProperty.put(memberShape.toShapeId(), true);
                    } else {
                        memberShapeDoesNotRequireProperty.put(memberShape.toShapeId(),
                                doesNotRequireProperty(model, memberShape));
                    }
                    if (doesMemberShapeRequireProperty(memberShape.getId())
                            || propertyNames.contains(memberShape.getMemberName())) {
                        memberShapeToPropertyName.put(memberShape.getId(),
                                getPropertyTraitName(memberShape).orElse(memberShape.getMemberName()));
                    }
                }
                // nesting is taking place, so index top level input/output members as not property.
                if (!outputPropertiesShape.getId().equals(operationShape.getOutputShape())) {
                    for (MemberShape memberShape : model.expectShape(operationShape.getOutputShape()).members()) {
                        memberShapeDoesNotRequireProperty.put(memberShape.toShapeId(), true);
                    }
                }
            }
        }
    }

    public static MemberPropertyIndex of(Model model) {
        return model.getKnowledge(MemberPropertyIndex.class, MemberPropertyIndex::new);
    }

    private Optional<String> getPropertyTraitName(MemberShape memberShape) {
        return memberShape.getTrait(PropertyTrait.class).flatMap(PropertyTrait::getName);
    }

    public Optional<String> getPropertyName(ShapeId memberShapeId) {
        return Optional.ofNullable(memberShapeToPropertyName.get(memberShapeId));
    }

    /**
     * Returns true if member is required to have an associated property mapping.
     *
     * @return True if input/output member is required to have a property mapping.
     *
     */
    public boolean doesMemberShapeRequireProperty(ShapeId memberShapeId) {
        return !memberShapeDoesNotRequireProperty.getOrDefault(memberShapeId, false);
    }

    private boolean doesNotRequireProperty(
        Model model,
        MemberShape memberShape
    ) {
        if (memberShape.hasTrait(NotPropertyTrait.class)) {
            return true;
        }
        return memberShape.getAllTraits().values().stream().map(t -> model.expectShape(t.toShapeId()))
                .filter(t -> t.hasTrait(NotPropertyTrait.class)).findAny().isPresent();
    }

    /**
     * Returns true if member shape positively maps to a property on the given
     * resource.
     *
     * {@see getPropertyName} will return a non-empty Optional if this method returns true.
     *
     * @return True if member shape maps to a property on the given resource
     */
    public boolean isMemberShapeProperty(ShapeId memberShapeId) {
        return memberShapeToPropertyName.containsKey(memberShapeId);
    }

    /**
     * Resolves and returns the output shape of an operation that contains the top-level resource bound
     * properties. Handles adjustments made with @nestedProperties trait.
     *
     * @return the output shape of an operation that contains top-level resource properties.
     */
    public Shape getOutputPropertiesShape(
            Model model,
            OperationIndex operationIndex,
            OperationShape operation
    ) {
        return getPropertiesShape(operationIndex.getOutputMembers(operation).values(),
                model.expectShape(operation.getOutputShape()), model);
    }

    /**
     * Resolves and returns the input shape of an operation that contains the top-level resource bound
     * properties. Handles adjustments made with @nestedProperties trait.
     *
     * @return the input shape of an operation that contains top-level resource properties.
     */
    public Shape getInputPropertiesShape(
            Model model,
            OperationIndex operationIndex,
            OperationShape operation
    ) {
        return getPropertiesShape(operationIndex.getInputMembers(operation).values(),
                model.expectShape(operation.getInputShape()), model);
    }

    private Shape getPropertiesShape(
            Collection<MemberShape> members,
            Shape presumedShape,
            Model model
    ) {
        return members.stream()
                .filter(member -> member.hasTrait(NestedPropertiesTrait.class))
                .map(member -> model.expectShape(member.getTarget()))
                .findAny().orElse(presumedShape);
    }
}
