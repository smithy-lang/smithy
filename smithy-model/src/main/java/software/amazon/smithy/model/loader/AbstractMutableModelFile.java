/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Validator;

/**
 * Base class used for mutable model files.
 */
abstract class AbstractMutableModelFile implements ModelFile {

    protected TraitContainer.VersionAwareTraitContainer traitContainer;

    private final String filename;
    private final Set<ShapeId> allShapeIds = new HashSet<>();
    private final Map<ShapeId, AbstractShapeBuilder<?, ?>> shapes = new LinkedHashMap<>();
    private final Map<ShapeId, Map<String, MemberShape.Builder>> members = new HashMap<>();
    private final Map<ShapeId, List<PendingShapeModifier>> pendingModifications = new HashMap<>();
    private final Map<ShapeId, Set<ShapeId>> pendingDependencies = new HashMap<>();
    private final List<ValidationEvent> events = new ArrayList<>();
    private final MetadataContainer metadata = new MetadataContainer(events);
    private final TraitFactory traitFactory;

    /**
     * @param traitFactory Factory used to create traits when merging traits.
     */
    AbstractMutableModelFile(String filename, TraitFactory traitFactory) {
        this.filename = filename;
        this.traitFactory = Objects.requireNonNull(traitFactory, "traitFactory must not be null");
        TraitContainer traitStore = new TraitContainer.TraitHashMap(traitFactory, events);
        traitContainer = new TraitContainer.VersionAwareTraitContainer(traitStore);
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public final Version getVersion() {
        return traitContainer.getVersion();
    }

    /**
     * Adds a shape to the ModelFile, checking for conflicts with other shapes.
     *
     * @param builder Shape builder to register.
     */
    void onShape(AbstractShapeBuilder<?, ?> builder) {
        allShapeIds.add(builder.getId());

        if (!getVersion().isShapeTypeSupported(builder.getShapeType())) {
            throw new SourceException(String.format(
                    "%s shapes may only be used with Smithy version 2 or later.", builder.getShapeType().toString()),
                    builder.getSourceLocation());
        }

        if (builder instanceof MemberShape.Builder) {
            String memberName = builder.getId().getMember().get();
            ShapeId containerId = builder.getId().withoutMember();
            if (!members.containsKey(containerId)) {
                members.put(containerId, new LinkedHashMap<>());
            } else if (members.get(containerId).containsKey(memberName)) {
                throw onConflict(builder, members.get(containerId).get(memberName));
            }
            members.get(containerId).put(memberName, (MemberShape.Builder) builder);
        } else if (shapes.containsKey(builder.getId())) {
            throw onConflict(builder, shapes.get(builder.getId()));
        } else {
            shapes.put(builder.getId(), builder);
        }
    }

    void addPendingMixin(ShapeId shape, ShapeId mixin) {
        addPendingModification(shape, new ApplyMixin(mixin, events));
    }

    void addPendingModification(ShapeId shape, PendingShapeModifier pendingModification) {
        pendingDependencies.computeIfAbsent(shape, id -> new LinkedHashSet<>())
                .addAll(pendingModification.getDependencies());
        pendingModifications.computeIfAbsent(shape, id -> new ArrayList<>()).add(pendingModification);
    }

    private SourceException onConflict(AbstractShapeBuilder<?, ?> builder, AbstractShapeBuilder<?, ?> previous) {
        // Duplicate shapes in the same model file are not allowed.
        ValidationEvent event = LoaderUtils.onShapeConflict(builder.getId(), builder.getSourceLocation(),
                                                            previous.getSourceLocation());
        return new SourceException(event.getMessage(), event.getSourceLocation());
    }

    /**
     * Adds metadata to be reported by the ModelFile.
     *
     * @param key Metadata key to set.
     * @param value Metadata value to set.
     */
    final void putMetadata(String key, Node value) {
        metadata.putMetadata(key, value);
    }

    /**
     * Invoked when a trait is to be reported by the ModelFile.
     *
     * @param target The shape the trait is applied to.
     * @param trait The trait shape ID.
     * @param value The node value of the trait.
     */
    final void onTrait(ShapeId target, ShapeId trait, Node value) {
        traitContainer.onTrait(target, trait, value);
    }

    /**
     * Invoked when a trait is to be reported by the ModelFile.
     *
     * @param target The shape the trait is applied to.
     * @param trait The trait to apply to the shape.
     */
    final void onTrait(ShapeId target, Trait trait) {
        traitContainer.onTrait(target, trait);
    }

    /**
     * Sets the version of the model file being loaded.
     *
     * @param version Version to set.
     */
    final void setVersion(Version version) {
        traitContainer.setVersion(version);
    }

    @Override
    public final List<ValidationEvent> events() {
        return events;
    }

    @Override
    public final Map<String, Node> metadata() {
        return metadata.getData();
    }

    @Override
    public final Set<ShapeId> shapeIds() {
        return allShapeIds;
    }

    @Override
    public final ShapeType getShapeType(ShapeId id) {
        return shapes.containsKey(id) ? shapes.get(id).getShapeType() : null;
    }

    @Override
    public final CreatedShapes createShapes(TraitContainer resolvedTraits) {
        List<Shape> resolvedShapes = new ArrayList<>(shapes.size());
        List<PendingShape> pendingMixins = new ArrayList<>();

        for (Map.Entry<ShapeId, List<PendingShapeModifier>> entry : this.pendingModifications.entrySet()) {
            ShapeId subject = entry.getKey();
            List<PendingShapeModifier> pendingModifications = entry.getValue();
            Set<ShapeId> dependencies = pendingDependencies.getOrDefault(subject, Collections.emptySet());
            AbstractShapeBuilder<?, ?> builder = shapes.get(entry.getKey());
            Map<String, MemberShape.Builder> builderMembers = claimMembersOfContainer(builder.getId());
            shapes.remove(entry.getKey());
            pendingMixins.add(createPendingShape(
                    subject, builder, builderMembers, dependencies, pendingModifications, traitContainer));
        }

        // Build members and add them to top-level shapes.
        for (Map<String, MemberShape.Builder> memberBuilders : members.values()) {
            for (MemberShape.Builder builder : memberBuilders.values()) {
                ShapeId id = builder.getId();
                AbstractShapeBuilder<?, ?> container = shapes.get(id.withoutMember());
                if (container == null) {
                    throw new RuntimeException("Container shape not found for member: " + id);
                }
                for (Trait trait : resolvedTraits.getTraitsForShape(id).values()) {
                    builder.addTrait(trait);
                }
                container.addMember(builder.build());
            }
        }

        // Build top-level shapes that don't use mixins.
        for (AbstractShapeBuilder<?, ?> builder : shapes.values()) {
            buildShape(builder, resolvedTraits).ifPresent(resolvedShapes::add);
        }

        return new CreatedShapes(resolvedShapes, pendingMixins);
    }

    private Map<String, MemberShape.Builder> claimMembersOfContainer(ShapeId id) {
        Map<String, MemberShape.Builder> result = members.remove(id);
        return result == null ? Collections.emptyMap() : result;
    }

    private PendingShape createPendingShape(
            ShapeId subject,
            AbstractShapeBuilder<?, ?> builder,
            Map<String, MemberShape.Builder> builderMembers,
            Set<ShapeId> mixins,
            List<PendingShapeModifier> pendingModifications,
            TraitContainer resolvedTraits
    ) {
        return PendingShape.create(subject, builder, mixins, shapeMap -> {
            for (MemberShape.Builder memberBuilder : builderMembers.values()) {
                for (PendingShapeModifier pendingModification : pendingModifications) {
                    pendingModification.modifyMember(builder, memberBuilder, resolvedTraits, shapeMap);
                }
                buildShape(memberBuilder, resolvedTraits).ifPresent(builder::addMember);
            }

            for (PendingShapeModifier pendingModification : pendingModifications) {
                pendingModification.modifyShape(builder, builderMembers, resolvedTraits, shapeMap);
            }

            buildShape(builder, resolvedTraits).ifPresent(result -> shapeMap.put(result.getId(), result));
        });
    }

    private <S extends Shape, B extends AbstractShapeBuilder<? extends B, S>> Optional<S> buildShape(
            B builder,
            TraitContainer resolvedTraits
    ) {
        try {
            for (Trait trait : resolvedTraits.getTraitsForShape(builder.getId()).values()) {
                builder.addTrait(trait);
            }
            return Optional.of(builder.build());
        } catch (IllegalStateException e) {
            if (builder.getShapeType() == ShapeType.MEMBER && ((MemberShape.Builder) builder).getTarget() == null) {
                events.add(ValidationEvent.builder()
                        .severity(Severity.ERROR)
                        .id(Validator.MODEL_ERROR)
                        .shapeId(builder.getId())
                        .sourceLocation(builder.getSourceLocation())
                        .message("Member target was elided, but no bound resource or mixin contained a matching "
                                + "identifier or member name.")
                        .build());
                return Optional.empty();
            }
            throw e;
        } catch (SourceException e) {
            events.add(ValidationEvent.fromSourceException(e, "", builder.getId()));
            resolvedTraits.clearTraitsForShape(builder.getId());
            return Optional.empty();
        }
    }

}
