/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.aws.cloudformation.traits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.BottomUpIndex;
import software.amazon.smithy.model.knowledge.IdentifierBindingIndex;
import software.amazon.smithy.model.knowledge.KnowledgeIndex;
import software.amazon.smithy.model.knowledge.PropertyBindingIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.OptionalUtils;
import software.amazon.smithy.utils.SetUtils;

/**
 * Index of resources to their CloudFormation identifiers and properties.
 *
 * <p>This index performs no validation that the identifiers and reference
 * valid shapes.
 */
public final class CfnResourceIndex implements KnowledgeIndex {

    static final Set<Mutability> FULLY_MUTABLE = SetUtils.of(
            Mutability.CREATE, Mutability.READ, Mutability.WRITE);
    static final Set<Mutability> INHERITED_MUTABILITY = SetUtils.of(
            Mutability.CREATE, Mutability.READ);

    private final Map<ShapeId, CfnResource> resourceDefinitions = new HashMap<>();

    /**
     * Mutability options derived through lifecycle operations or traits.
     * These mutability options are used to derive the CloudFormation
     * mutabilities of the properties they are associated with.
     *
     * @see CfnResource#getCreateOnlyProperties
     * @see CfnResource#getReadOnlyProperties
     * @see CfnResource#getWriteOnlyProperties
     */
    public enum Mutability {
        /**
         * Applied when a property was derived from a lifecycle operation that
         * creates a resource or the {@link CfnMutabilityTrait} specifying {@code full},
         * {@code create}, or {@code create-and-read} mutability.
         */
        CREATE,

        /**
         * Applied when a property was derived from a lifecycle operation that
         * retrieves a resource or the {@link CfnMutabilityTrait} specifying {@code full},
         * {@code read}, or {@code create-and-read} mutability.
         */
        READ,

        /**
         * Applied when a property was derived from a lifecycle operation that
         * updates a resource or the {@link CfnMutabilityTrait} specifying {@code full}
         * or {@code write} mutability.
         */
        WRITE
    }

    public CfnResourceIndex(Model model) {
        PropertyBindingIndex propertyIndex = PropertyBindingIndex.of(model);
        BottomUpIndex bottomUpIndex = BottomUpIndex.of(model);
        model.shapes(ResourceShape.class)
                .filter(shape -> shape.hasTrait(CfnResourceTrait.ID))
                .forEach(resource -> {
                    CfnResource.Builder builder = CfnResource.builder();
                    ShapeId resourceId = resource.getId();

                    Set<ResourceShape> parentResources = model.getServiceShapes()
                            .stream()
                            .map(service -> bottomUpIndex.getResourceBinding(service, resourceId))
                            .flatMap(OptionalUtils::stream)
                            .collect(Collectors.toSet());

                    // Start with the explicit resource identifiers.
                    builder.primaryIdentifiers(resource.getIdentifiers().keySet());
                    setIdentifierMutabilities(builder, resource, parentResources);

                    // Use the read lifecycle's input to collect the additional identifiers
                    // and its output to collect readable properties.
                    resource.getRead().ifPresent(operationId -> {
                        OperationShape operation = model.expectShape(operationId, OperationShape.class);
                        StructureShape input = model.expectShape(operation.getInputShape(), StructureShape.class);
                        addAdditionalIdentifiers(builder, computeResourceAdditionalIdentifiers(input));

                        StructureShape output = propertyIndex.getOutputPropertiesShape(operation);
                        updatePropertyMutabilities(builder, model, resourceId, operationId, output,
                                SetUtils.of(Mutability.READ), this::addReadMutability);
                    });

                    // Use the put lifecycle's input to collect put-able properties.
                    resource.getPut().ifPresent(operationId -> {
                        OperationShape operation = model.expectShape(operationId, OperationShape.class);
                        StructureShape input = propertyIndex.getInputPropertiesShape(operation);
                        updatePropertyMutabilities(builder, model, resourceId, operationId, input,
                                SetUtils.of(Mutability.CREATE, Mutability.WRITE), this::addPutMutability);
                    });

                    // Use the create lifecycle's input to collect creatable properties.
                    resource.getCreate().ifPresent(operationId -> {
                        OperationShape operation = model.expectShape(operationId, OperationShape.class);
                        StructureShape input = propertyIndex.getInputPropertiesShape(operation);
                        updatePropertyMutabilities(builder, model, resourceId, operationId, input,
                                SetUtils.of(Mutability.CREATE), this::addCreateMutability);
                    });

                    // Use the update lifecycle's input to collect writeable properties.
                    resource.getUpdate().ifPresent(operationId -> {
                        OperationShape operation = model.expectShape(operationId, OperationShape.class);
                        StructureShape input = propertyIndex.getInputPropertiesShape(operation);
                        updatePropertyMutabilities(builder, model, resourceId, operationId, input,
                                SetUtils.of(Mutability.WRITE), this::addWriteMutability);
                    });

                    // Apply any members found through the trait's additionalSchemas property.
                    CfnResourceTrait trait = resource.expectTrait(CfnResourceTrait.class);
                    for (ShapeId additionalSchema : trait.getAdditionalSchemas()) {
                        // These shapes should be present given the @idRef failWhenMissing
                        // setting, but gracefully handle if they're not.
                        model.getShape(additionalSchema)
                                .flatMap(Shape::asStructureShape)
                                .ifPresent(shape -> {
                                    addAdditionalIdentifiers(builder, computeResourceAdditionalIdentifiers(shape));
                                    updatePropertyMutabilities(builder, model, resourceId, null, shape,
                                            SetUtils.of(), Function.identity());
                                });
                    }

                    resourceDefinitions.put(resourceId, builder.build());
                });
    }

    public static CfnResourceIndex of(Model model) {
        return model.getKnowledge(CfnResourceIndex.class, CfnResourceIndex::new);
    }

    /**
     * Gets the definition of the specified CloudFormation resource.
     *
     * @param resource ShapeID of a resource
     * @return The resource definition.
     */
    public Optional<CfnResource> getResource(ToShapeId resource) {
        return Optional.ofNullable(resourceDefinitions.get(resource.toShapeId()));
    }

    private boolean identifierIsInherited(String identifier, Set<ResourceShape> parentResources) {
        return parentResources.stream()
                .anyMatch(parentResource -> parentResource.getIdentifiers().containsKey(identifier));
    }

    private void setIdentifierMutabilities(
            CfnResource.Builder builder,
            ResourceShape resource,
            Set<ResourceShape> parentResources) {
        Set<Mutability> defaultIdentifierMutability = getDefaultIdentifierMutabilities(resource);

        resource.getIdentifiers().forEach((name, shape) -> {
            builder.putPropertyDefinition(name, CfnResourceProperty.builder()
                    .hasExplicitMutability(true)
                    .mutabilities(identifierIsInherited(name, parentResources)
                            ? INHERITED_MUTABILITY : defaultIdentifierMutability)
                    .addShapeId(shape)
                    .build());
        });
    }

    private Set<Mutability> getDefaultIdentifierMutabilities(ResourceShape resource) {
        // If we have a put operation, the identifier will be specified
        // on creation. Otherwise, it's read only.
        if (resource.getPut().isPresent()) {
            return SetUtils.of(Mutability.CREATE, Mutability.READ);
        }

        return SetUtils.of(Mutability.READ);
    }

    private List<Map<String, ShapeId>> computeResourceAdditionalIdentifiers(StructureShape readInput) {
        List<Map<String, ShapeId>> identifiers = new ArrayList<>();
        for (MemberShape member : readInput.members()) {
            if (!member.hasTrait(CfnAdditionalIdentifierTrait.class)) {
                continue;
            }

            identifiers.add(MapUtils.of(member.getMemberName(), member.getId()));
        }
        return identifiers;
    }

    private void addAdditionalIdentifiers(
            CfnResource.Builder builder,
            List<Map<String, ShapeId>> addedIdentifiers
    ) {
        if (addedIdentifiers.isEmpty()) {
            return;
        }

        // Make sure we have properties entries for the additional identifiers.
        for (Map<String, ShapeId> addedIdentifier : addedIdentifiers) {
            for (Map.Entry<String, ShapeId> idEntry : addedIdentifier.entrySet()) {
                builder.putPropertyDefinition(idEntry.getKey(), CfnResourceProperty.builder()
                        .mutabilities(SetUtils.of(Mutability.READ))
                        .addShapeId(idEntry.getValue())
                        .build());
            }
            builder.addAdditionalIdentifier(addedIdentifier.keySet());
        }
    }

    private void updatePropertyMutabilities(
            CfnResource.Builder builder,
            Model model,
            ShapeId resourceId,
            ShapeId operationId,
            StructureShape propertyContainer,
            Set<Mutability> defaultMutabilities,
            Function<Set<Mutability>, Set<Mutability>> updater
    ) {
        // Handle the @excludeProperty trait.
        propertyContainer.accept(new ExcludedPropertiesVisitor(model))
                .forEach(builder::addExcludedProperty);

        for (MemberShape member : propertyContainer.members()) {
            // We've explicitly set identifier mutability based on how the
            // resource instance comes about, so only handle non-identifiers.
            if (inputOperationMemberIsIdentifier(model, resourceId, operationId, member)) {
                continue;
            }

            String memberName = member.getMemberName();
            Set<Mutability> explicitMutability = getExplicitMutability(model, member);

            // Set the correct mutability for if this is a new property.
            Set<Mutability> mutabilities = !explicitMutability.isEmpty()
                    ? explicitMutability
                    : defaultMutabilities;

            if (builder.hasPropertyDefinition(memberName)) {
                builder.updatePropertyDefinition(memberName,
                        getCfnResourcePropertyUpdater(member, explicitMutability, updater));
            } else {
                builder.putPropertyDefinition(memberName,
                        CfnResourceProperty.builder()
                                .addShapeId(member.getId())
                                .mutabilities(mutabilities)
                                .hasExplicitMutability(!explicitMutability.isEmpty())
                                .build());
            }
        }
    }

    private Function<CfnResourceProperty, CfnResourceProperty> getCfnResourcePropertyUpdater(
            MemberShape member,
            Set<Mutability> explicitMutability,
            Function<Set<Mutability>, Set<Mutability>> updater
    ) {
        return definition -> {
            CfnResourceProperty.Builder builder = definition.toBuilder().addShapeId(member.getId());

            if (explicitMutability.isEmpty()) {
                // Update the existing mutabilities.
                builder.mutabilities(updater.apply(definition.getMutabilities()));
            } else {
                // Handle explicit mutability from any trait location.
                builder.hasExplicitMutability(true)
                        .mutabilities(explicitMutability);
            }

            return builder.build();
        };
    }

    private boolean inputOperationMemberIsIdentifier(
            Model model,
            ShapeId resourceId,
            ShapeId operationId,
            MemberShape member
    ) {
        // The operationId will be null in the case of additionalSchemas, so
        // we shouldn't worry if these are bound to operation identifiers.
        if (operationId == null) {
            return false;
        }

        IdentifierBindingIndex index = IdentifierBindingIndex.of(model);
        Map<String, String> bindings = index.getOperationInputBindings(resourceId, operationId);
        String memberName = member.getMemberName();
        // Check for literal identifier bindings.
        for (String bindingMemberName : bindings.values()) {
            if (memberName.equals(bindingMemberName)) {
                return true;
            }
        }

        return false;
    }

    private Set<Mutability> getExplicitMutability(
            Model model,
            MemberShape member
    ) {
        Optional<CfnMutabilityTrait> traitOptional = member.getMemberTrait(model, CfnMutabilityTrait.class);
        if (!traitOptional.isPresent()) {
            return SetUtils.of();
        }

        CfnMutabilityTrait trait = traitOptional.get();
        if (trait.isFullyMutable()) {
            return FULLY_MUTABLE;
        } else if (trait.isCreateAndRead()) {
            return SetUtils.of(Mutability.CREATE, Mutability.READ);
        } else if (trait.isCreate()) {
            return SetUtils.of(Mutability.CREATE);
        } else if (trait.isRead()) {
            return SetUtils.of(Mutability.READ);
        } else if (trait.isWrite()) {
            return SetUtils.of(Mutability.WRITE);
        }
        return SetUtils.of();
    }

    private Set<Mutability> addReadMutability(Set<Mutability> mutabilities) {
        Set<Mutability> newMutabilities = new HashSet<>(mutabilities);
        newMutabilities.add(Mutability.READ);
        return SetUtils.copyOf(newMutabilities);
    }

    private Set<Mutability> addCreateMutability(Set<Mutability> mutabilities) {
        Set<Mutability> newMutabilities = new HashSet<>(mutabilities);
        newMutabilities.add(Mutability.CREATE);
        return SetUtils.copyOf(newMutabilities);
    }

    private Set<Mutability> addWriteMutability(Set<Mutability> mutabilities) {
        Set<Mutability> newMutabilities = new HashSet<>(mutabilities);
        newMutabilities.add(Mutability.WRITE);
        return SetUtils.copyOf(newMutabilities);
    }

    private Set<Mutability> addPutMutability(Set<Mutability> mutabilities) {
        return addWriteMutability(addCreateMutability(mutabilities));
    }

    private static final class ExcludedPropertiesVisitor extends ShapeVisitor.Default<Set<ShapeId>> {
        private final Model model;
        private final PropertyBindingIndex propertyBindingIndex;

        private ExcludedPropertiesVisitor(Model model) {
            this.model = model;
            this.propertyBindingIndex = PropertyBindingIndex.of(model);
        }

        @Override
        protected Set<ShapeId> getDefault(Shape shape) {
            return SetUtils.of();
        }

        @Override
        public Set<ShapeId> structureShape(StructureShape shape) {
            Set<ShapeId> excludedShapes = new HashSet<>();
            for (MemberShape member : shape.members()) {
                if (member.hasTrait(CfnExcludePropertyTrait.ID)) {
                    excludedShapes.add(member.getId());
                } else if (!propertyBindingIndex.doesMemberShapeRequireProperty(member)) {
                    excludedShapes.add(member.getId());
                } else {
                    excludedShapes.addAll(model.expectShape(member.getTarget()).accept(this));
                }
            }
            return excludedShapes;
        }
    }
}
