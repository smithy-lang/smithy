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

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.NestedPropertiesTrait;
import software.amazon.smithy.model.traits.NotPropertyTrait;
import software.amazon.smithy.model.traits.PropertyTrait;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Index of member shapes -> property name, and general queries on if a member shape is a property.
 */
@SmithyUnstableApi
public final class MemberPropertyIndex implements KnowledgeIndex {
    private final WeakReference<Model> model;
    private final OperationIndex operationIndex;
    private final Set<ShapeId> notPropertyMetaTraitSet;
    private final Map<ShapeId, Boolean> memberShapeDoesNotRequireProperty = new HashMap<>();
    private final Map<ShapeId, String> memberShapeToPropertyName = new HashMap<>();
    private final Map<ShapeId, ShapeId> operationToInputPropertiesShape = new HashMap<>();
    private final Map<ShapeId, ShapeId> operationToOutputPropertiesShape = new HashMap<>();

    private MemberPropertyIndex(Model model) {
        this.model = new WeakReference<>(model);
        this.notPropertyMetaTraitSet = computeNotPropertyTraits();
        this.operationIndex = OperationIndex.of(model);
        IdentifierBindingIndex identifierIndex = IdentifierBindingIndex.of(model);

        for (ResourceShape resourceShape : model.getResourceShapes()) {
            Set<String> propertyNames = resourceShape.getProperties().keySet();
            for (ShapeId operationShapeId : resourceShape.getAllOperations()) {
                OperationShape operationShape = (OperationShape) model.getShape(operationShapeId).get();
                Shape inputPropertiesShape = getInputPropertiesShape(operationShape);
                operationToInputPropertiesShape.put(operationShapeId, inputPropertiesShape.getId());
                for (MemberShape memberShape : inputPropertiesShape.members()) {
                    if (identifierIndex.getOperationInputBindings(resourceShape, operationShape).values()
                            .contains(memberShape.getMemberName())) {
                        memberShapeDoesNotRequireProperty.put(memberShape.toShapeId(), true);
                    } else {
                        memberShapeDoesNotRequireProperty.put(memberShape.toShapeId(),
                                doesNotRequireProperty(memberShape));
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
                Shape outputPropertiesShape = getOutputPropertiesShape(operationShape);
                operationToOutputPropertiesShape.put(operationShapeId, outputPropertiesShape.getId());
                for (MemberShape memberShape : outputPropertiesShape.members()) {
                    if (identifierIndex.getOperationInputBindings(resourceShape, operationShape).values()
                            .contains(memberShape.getMemberName())) {
                        memberShapeDoesNotRequireProperty.put(memberShape.toShapeId(), true);
                    } else {
                        memberShapeDoesNotRequireProperty.put(memberShape.toShapeId(),
                                doesNotRequireProperty(memberShape));
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

    /**
     * Gets the property name for a given member shape. Returns empty optional if the
     * member shape does not correspond to a property.
     *
     * @param memberShapeId the ShapeId of the member shape to get the property name for.
     * @return the property name for a given member shape if there is one.
     */
    public Optional<String> getPropertyName(ShapeId memberShapeId) {
        return Optional.ofNullable(memberShapeToPropertyName.get(memberShapeId));
    }

    /**
     * Returns true if a member shape positively maps to a property.
     *
     * {@see getPropertyName} will return a non-empty Optional if this method
     * returns true.
     *
     * @param memberShapeId the ShapeId of the member shape to check
     * @return true if member shape maps to a property on the given resource
     */
    public boolean isMemberShapeProperty(ShapeId memberShapeId) {
        return memberShapeToPropertyName.containsKey(memberShapeId);
    }

    /**
     * Resolves and returns the output shape of an operation that contains the
     * top-level resource bound
     * properties. Handles adjustments made with @nestedProperties trait.
     *
     * @param operation operation to retrieve output properties shape for.
     * @return the output shape of an operation that contains top-level resource
     *  properties.
     */
    public Shape getOutputPropertiesShape(OperationShape operation) {
        return getPropertiesShape(operationIndex.getOutputMembers(operation).values(),
                model.get().expectShape(operation.getOutputShape()));
    }

    /**
     * Resolves and returns the input shape of an operation that contains the
     * top-level resource bound
     * properties. Handles adjustments made with @nestedProperties trait.
     *
     * @param operation operation to retrieve output properties shape for
     * @return the input shape of an operation that contains top-level resource
     *  properties.
     */
    public Shape getInputPropertiesShape(OperationShape operation) {
        return getPropertiesShape(operationIndex.getInputMembers(operation).values(),
                model.get().expectShape(operation.getInputShape()));
    }

    /**
     * Returns true if member is required to have an associated property mapping.
     *
     * @return True if input/output member is required to have a property mapping.
     */
    public boolean doesMemberShapeRequireProperty(ShapeId memberShapeId) {
        return !memberShapeDoesNotRequireProperty.getOrDefault(memberShapeId, false);
    }

    private Set<ShapeId> computeNotPropertyTraits() {
        return model.get().shapes().filter(shape -> shape.hasTrait(NotPropertyTrait.class))
            .map(shape -> shape.toShapeId())
            .collect(Collectors.toSet());
    }

    private Optional<String> getPropertyTraitName(MemberShape memberShape) {
        return memberShape.getTrait(PropertyTrait.class).flatMap(PropertyTrait::getName);
    }

    private boolean doesNotRequireProperty(MemberShape memberShape) {
        return memberShape.hasTrait(NotPropertyTrait.class) || notPropertyMetaTraitSet.contains(memberShape.getId());
    }

    private Shape getPropertiesShape(Collection<MemberShape> members, Shape presumedShape) {
        return members.stream()
                .filter(member -> member.hasTrait(NestedPropertiesTrait.class))
                .map(member -> model.get().expectShape(member.getTarget()))
                .findAny().orElse(presumedShape);
    }
}
