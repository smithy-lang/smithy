/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.knowledge;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.NestedPropertiesTrait;
import software.amazon.smithy.model.traits.NotPropertyTrait;
import software.amazon.smithy.model.traits.PropertyTrait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Index of member shape to associated resource property information.
 */
@SmithyUnstableApi
public final class PropertyBindingIndex implements KnowledgeIndex {
    private final WeakReference<Model> model;
    private final OperationIndex operationIndex;
    private final Set<ShapeId> notPropertyMetaTraitSet;
    private final Map<ShapeId, Boolean> memberShapeDoesNotRequireProperty = new HashMap<>();
    private final Map<ShapeId, String> memberShapeToPropertyName = new HashMap<>();
    private final Map<ShapeId, ShapeId> operationToInputPropertiesShape = new HashMap<>();
    private final Map<ShapeId, ShapeId> operationToOutputPropertiesShape = new HashMap<>();
    private final Set<ShapeId> collectionBoundOperations = new HashSet<>();

    private PropertyBindingIndex(Model model) {
        this.model = new WeakReference<>(model);
        this.notPropertyMetaTraitSet = computeNotPropertyTraits();
        this.operationIndex = OperationIndex.of(model);
        IdentifierBindingIndex identifierIndex = IdentifierBindingIndex.of(model);

        for (ResourceShape resourceShape : model.getResourceShapes()) {
            Set<String> propertyNames = resourceShape.getProperties().keySet();
            for (ShapeId operationShapeId : resourceShape.getAllOperations()) {
                OperationShape operationShape = (OperationShape) model.getShape(operationShapeId).get();
                if (CollectionElementResolver.isElementCarrier(resourceShape, operationShapeId)) {
                    collectionBoundOperations.add(operationShapeId);
                    indexCollectionBoundOperation(model, resourceShape, operationShape, identifierIndex);
                    continue;
                }
                Shape inputPropertiesShape = getInputPropertiesShape(operationShape);
                operationToInputPropertiesShape.put(operationShapeId, inputPropertiesShape.getId());
                for (MemberShape memberShape : inputPropertiesShape.members()) {
                    if (identifierIndex.getOperationInputBindings(resourceShape, operationShape)
                            .values()
                            .contains(memberShape.getMemberName())) {
                        memberShapeDoesNotRequireProperty.put(memberShape.toShapeId(), true);
                    } else {
                        memberShapeDoesNotRequireProperty.put(memberShape.toShapeId(),
                                doesNotRequireProperty(memberShape));
                    }
                    if (doesMemberShapeRequireProperty(memberShape)
                            || propertyNames.contains(memberShape.getMemberName())) {
                        memberShapeToPropertyName.put(memberShape.getId(),
                                getPropertyTraitName(memberShape)
                                        .orElse(memberShape.getMemberName()));
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
                    if (identifierIndex.getOperationOutputBindings(resourceShape, operationShape)
                            .values()

                            .contains(memberShape.getMemberName())) {
                        memberShapeDoesNotRequireProperty.put(memberShape.toShapeId(), true);
                    } else {
                        memberShapeDoesNotRequireProperty.put(memberShape.toShapeId(),
                                doesNotRequireProperty(memberShape));
                    }
                    if (doesMemberShapeRequireProperty(memberShape)
                            || propertyNames.contains(memberShape.getMemberName())) {
                        memberShapeToPropertyName.put(memberShape.getId(),
                                getPropertyTraitName(memberShape)
                                        .orElse(memberShape.getMemberName()));
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

    public static PropertyBindingIndex of(Model model) {
        return model.getKnowledge(PropertyBindingIndex.class, PropertyBindingIndex::new);
    }

    /**
     * Indexes an operation bound to a resource through the {@code list}
     * lifecycle or the {@code collectionOperations} property.
     *
     * <p>Collection operation inputs carry filters and pagination details
     * rather than resource state, so no input member binds to a property.
     * Output properties are carried per element of the list member, so
     * top-level output members never bind properties directly. Within the
     * element structure, members bound to resource identifiers are identifier
     * bindings, members matching resource properties (or carrying the
     * {@code @property} trait) bind to those properties, and remaining
     * members are not required to bind since elements frequently carry
     * derived or summary-only data.
     */
    private void indexCollectionBoundOperation(
            Model model,
            ResourceShape resource,
            OperationShape operation,
            IdentifierBindingIndex identifierIndex
    ) {
        operationToInputPropertiesShape.put(operation.getId(), operation.getInputShape());
        for (MemberShape member : model.expectShape(operation.getInputShape()).members()) {
            memberShapeDoesNotRequireProperty.put(member.toShapeId(), true);
        }

        for (MemberShape member : model.expectShape(operation.getOutputShape()).members()) {
            memberShapeDoesNotRequireProperty.put(member.toShapeId(), true);
        }

        Optional<StructureShape> elementShape = CollectionElementResolver.resolveOutputElement(model, operation);
        if (!elementShape.isPresent()) {
            operationToOutputPropertiesShape.put(operation.getId(), operation.getOutputShape());
            return;
        }

        operationToOutputPropertiesShape.put(operation.getId(), elementShape.get().getId());
        Set<String> propertyNames = resource.getProperties().keySet();
        Set<String> identifierMembers = new HashSet<>(identifierIndex
                .getOperationOutputElementBindings(resource, operation)
                .values());
        for (MemberShape member : elementShape.get().members()) {
            if (identifierMembers.contains(member.getMemberName())
                    || resource.getIdentifiers().containsKey(member.getMemberName())) {
                memberShapeDoesNotRequireProperty.put(member.toShapeId(), true);
            } else if (getPropertyTraitName(member).isPresent()
                    || propertyNames.contains(member.getMemberName())) {
                memberShapeDoesNotRequireProperty.put(member.toShapeId(), doesNotRequireProperty(member));
                memberShapeToPropertyName.put(member.getId(),
                        getPropertyTraitName(member).orElse(member.getMemberName()));
            } else {
                memberShapeDoesNotRequireProperty.put(member.toShapeId(), true);
            }
        }
    }

    private boolean isCollectionBoundOperation(OperationShape operation) {
        return collectionBoundOperations.contains(operation.getId());
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
     * {@see PropertyBindingIndex#getPropertyName(ShapeId)} will return a non-empty Optional if this method
     * returns true.
     *
     * @param memberShape the member shape to check
     * @return true if member shape maps to a property on the given resource
     */
    public boolean isMemberShapeProperty(MemberShape memberShape) {
        return memberShapeToPropertyName.containsKey(memberShape.toShapeId());
    }

    /**
     * Resolves and returns the output shape of an operation that contains the
     * top-level resource bound properties. Handles adjustments made with
     * {@code @nestedProperties} trait. For operations bound to a resource
     * through the {@code list} lifecycle or the {@code collectionOperations}
     * property, resolves to the structure targeted by the output's list
     * member when that member is unambiguous.
     *
     * @param operation operation to retrieve output properties shape for.
     * @return the output shape of an operation that contains top-level resource
     *  properties.
     */
    public StructureShape getOutputPropertiesShape(OperationShape operation) {
        Model model = getModel();
        if (isCollectionBoundOperation(operation)) {
            return CollectionElementResolver.resolveOutputElement(model, operation)
                    .orElseGet(() -> model.expectShape(operation.getOutputShape(), StructureShape.class));
        }
        return getPropertiesShape(operationIndex.getOutputMembers(operation).values(),
                model.expectShape(operation.getOutputShape(), StructureShape.class));
    }

    /**
     * Resolves and returns the input shape of an operation that contains the
     * top-level resource bound properties. Handles adjustments made with
     * {@code @nestedProperties} trait.
     *
     * @param operation operation to retrieve output properties shape for
     * @return the input shape of an operation that contains top-level resource
     *  properties.
     */
    public StructureShape getInputPropertiesShape(OperationShape operation) {
        Model model = getModel();
        return getPropertiesShape(operationIndex.getInputMembers(operation).values(),
                model.expectShape(operation.getInputShape(), StructureShape.class));
    }

    /**
     * Returns true if member is required to have an associated property mapping.
     *
     * @param memberShape the member shape to check
     * @return True if input/output member is required to have a property mapping.
     */
    public boolean doesMemberShapeRequireProperty(MemberShape memberShape) {
        return !memberShapeDoesNotRequireProperty.getOrDefault(memberShape.toShapeId(), false);
    }

    private Model getModel() {
        return Objects.requireNonNull(model.get(), "The dereferenced WeakReference<Model> is null");
    }

    private Set<ShapeId> computeNotPropertyTraits() {
        Model model = getModel();
        return model.getShapesWithTrait(NotPropertyTrait.class)
                .stream()
                .filter(shape -> shape.hasTrait(TraitDefinition.ID))
                .map(Shape::toShapeId)
                .collect(Collectors.toSet());
    }

    private Optional<String> getPropertyTraitName(MemberShape memberShape) {
        return memberShape.getTrait(PropertyTrait.class).flatMap(PropertyTrait::getName);
    }

    private boolean doesNotRequireProperty(MemberShape memberShape) {
        return notPropertyMetaTraitSet.stream().anyMatch(memberShape::hasTrait);
    }

    private StructureShape getPropertiesShape(Collection<MemberShape> members, StructureShape presumedShape) {
        Model model = getModel();
        for (MemberShape member : members) {
            if (member.hasTrait(NestedPropertiesTrait.ID)) {
                Shape shape = model.expectShape(member.getTarget());
                if (shape.isStructureShape()) {
                    return shape.asStructureShape().get();
                }
            }
        }
        return presumedShape;
    }
}
