/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.knowledge;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
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
import software.amazon.smithy.model.shapes.ToShapeId;
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

    /** Map of resource shape ID to a map of operation shape ID to element bindings. */
    private final Map<ShapeId, Map<ShapeId, ElementBinding>> inputElementBindings = new HashMap<>();
    private final Map<ShapeId, Map<ShapeId, ElementBinding>> outputElementBindings = new HashMap<>();

    private PropertyBindingIndex(Model model) {
        this.model = new WeakReference<>(model);
        this.notPropertyMetaTraitSet = computeNotPropertyTraits();
        this.operationIndex = OperationIndex.of(model);
        IdentifierBindingIndex identifierIndex = IdentifierBindingIndex.of(model);

        for (ResourceShape resourceShape : model.getResourceShapes()) {
            Set<String> propertyNames = resourceShape.getProperties().keySet();
            inputElementBindings.put(resourceShape.getId(), new HashMap<>());
            outputElementBindings.put(resourceShape.getId(), new HashMap<>());
            for (ShapeId operationShapeId : resourceShape.getAllOperations()) {
                OperationShape operationShape = (OperationShape) model.getShape(operationShapeId).get();
                if (CollectionElementResolver.isElementCarrier(resourceShape, operationShapeId)) {
                    indexElementBindings(model, resourceShape, operationShape, identifierIndex);
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
     * Computes the element bindings of an operation bound to a resource
     * through the {@code list} lifecycle or the {@code collectionOperations}
     * property.
     *
     * <p>The element structure is resolved through a member marked with the
     * {@code @nestedProperties} trait that targets a list of structures, and
     * for the output of {@code list} lifecycle operations without that trait,
     * through a single unambiguous list-of-structures member. Element
     * bindings are scoped to the resource and operation because element
     * structures may be shared between resources, so element members are
     * never added to the member-keyed property maps used for top-level
     * input and output members.
     */
    private void indexElementBindings(
            Model model,
            ResourceShape resource,
            OperationShape operation,
            IdentifierBindingIndex identifierIndex
    ) {
        Optional<StructureShape> explicitInput =
                CollectionElementResolver.resolveExplicitElement(model, operation.getInputShape());
        explicitInput.ifPresent(element -> inputElementBindings.get(resource.getId())
                .put(operation.getId(),
                        createElementBinding(resource,
                                element,
                                true,
                                identifierIndex.getOperationInputElementBindings(resource, operation))));

        Optional<StructureShape> explicitOutput =
                CollectionElementResolver.resolveExplicitElement(model, operation.getOutputShape());
        boolean explicit = explicitOutput.isPresent();
        Optional<StructureShape> outputElement;
        if (explicit) {
            outputElement = explicitOutput;
        } else if (CollectionElementResolver.hasElementMarker(model, operation.getOutputShape())) {
            // A @nestedProperties member is present but does not resolve to a list of
            // structures. Never fall back to automatic detection so the misuse can be
            // reported instead of being silently reinterpreted.
            outputElement = Optional.empty();
        } else if (CollectionElementResolver.isListLifecycle(resource, operation.getId())) {
            outputElement = CollectionElementResolver.resolveAutoOutputElement(model, operation);
        } else {
            outputElement = Optional.empty();
        }
        outputElement.ifPresent(element -> outputElementBindings.get(resource.getId())
                .put(operation.getId(),
                        createElementBinding(resource,
                                element,
                                explicit,
                                identifierIndex.getOperationOutputElementBindings(resource, operation))));
    }

    private ElementBinding createElementBinding(
            ResourceShape resource,
            StructureShape element,
            boolean explicit,
            Map<String, String> identifierBindings
    ) {
        Set<String> propertyNames = resource.getProperties().keySet();
        Set<String> identifierMembers = new HashSet<>(identifierBindings.values());
        Map<String, String> properties = new HashMap<>();
        for (MemberShape member : element.members()) {
            if (identifierMembers.contains(member.getMemberName())) {
                continue;
            }
            Optional<String> traitName = getPropertyTraitName(member);
            if (traitName.isPresent()) {
                properties.put(member.getMemberName(), traitName.get());
            } else if (!doesNotRequireProperty(member) && propertyNames.contains(member.getMemberName())) {
                properties.put(member.getMemberName(), member.getMemberName());
            }
        }
        return new ElementBinding(element.getId(), explicit, properties);
    }

    /**
     * Gets the element structure of a collection-bound operation's output
     * that carries per-instance resource state.
     *
     * <p>The element structure is the structure targeted by the members of
     * the list targeted by the output member marked with the
     * {@code @nestedProperties} trait, or, for the {@code list} lifecycle
     * without that trait, by a single unambiguous list-of-structures output
     * member.
     *
     * @param resource Shape ID of a resource.
     * @param operation Shape ID of an operation.
     * @return the element structure ID of the operation's output, if any.
     */
    public Optional<ShapeId> getOperationOutputElementShape(ToShapeId resource, ToShapeId operation) {
        return getBinding(outputElementBindings, resource, operation).map(binding -> binding.elementShape);
    }

    /**
     * Gets the element structure of a collection-bound operation's input
     * that carries per-instance resource state, resolved through the input
     * member marked with the {@code @nestedProperties} trait.
     *
     * @param resource Shape ID of a resource.
     * @param operation Shape ID of an operation.
     * @return the element structure ID of the operation's input, if any.
     */
    public Optional<ShapeId> getOperationInputElementShape(ToShapeId resource, ToShapeId operation) {
        return getBinding(inputElementBindings, resource, operation).map(binding -> binding.elementShape);
    }

    /**
     * Returns true if the output element structure of the operation was
     * explicitly marked with the {@code @nestedProperties} trait rather than
     * automatically detected.
     *
     * @param resource Shape ID of a resource.
     * @param operation Shape ID of an operation.
     * @return true if the output element binding is explicit.
     */
    public boolean isOperationOutputElementExplicit(ToShapeId resource, ToShapeId operation) {
        return getBinding(outputElementBindings, resource, operation).map(binding -> binding.explicit).orElse(false);
    }

    /**
     * Gets a map of element member names to resource property names for the
     * element structure of a collection-bound operation's output.
     *
     * @param resource Shape ID of a resource.
     * @param operation Shape ID of an operation.
     * @return the member name to property name map, or an empty map.
     */
    public Map<String, String> getOperationOutputElementProperties(ToShapeId resource, ToShapeId operation) {
        return getBinding(outputElementBindings, resource, operation)
                .map(binding -> Collections.unmodifiableMap(binding.properties))
                .orElseGet(Collections::emptyMap);
    }

    /**
     * Gets a map of element member names to resource property names for the
     * element structure of a collection-bound operation's input.
     *
     * @param resource Shape ID of a resource.
     * @param operation Shape ID of an operation.
     * @return the member name to property name map, or an empty map.
     */
    public Map<String, String> getOperationInputElementProperties(ToShapeId resource, ToShapeId operation) {
        return getBinding(inputElementBindings, resource, operation)
                .map(binding -> Collections.unmodifiableMap(binding.properties))
                .orElseGet(Collections::emptyMap);
    }

    private Optional<ElementBinding> getBinding(
            Map<ShapeId, Map<ShapeId, ElementBinding>> bindings,
            ToShapeId resource,
            ToShapeId operation
    ) {
        return Optional.ofNullable(bindings.get(resource.toShapeId()))
                .flatMap(resourceMap -> Optional.ofNullable(resourceMap.get(operation.toShapeId())));
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
     * {@code @nestedProperties} trait.
     *
     * @param operation operation to retrieve output properties shape for.
     * @return the output shape of an operation that contains top-level resource
     *  properties.
     */
    public StructureShape getOutputPropertiesShape(OperationShape operation) {
        Model model = getModel();
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

    private static final class ElementBinding {
        private final ShapeId elementShape;
        private final boolean explicit;
        private final Map<String, String> properties;

        private ElementBinding(ShapeId elementShape, boolean explicit, Map<String, String> properties) {
            this.elementShape = elementShape;
            this.explicit = explicit;
            this.properties = properties;
        }
    }
}
