/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform.plugins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.IntEnumShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.model.transform.ModelTransformerPlugin;

/**
 * Cleans up structure, union, enum, and intEnum shapes after shapes are removed.
 *
 * <ul>
 *     <li>Ensures that structure and union shapes are updated to no
 *     longer reference any removed members.</li>
 *     <li>Ensures that structure/union members that reference shapes
 *     that have been removed are also removed.</li>
 * </ul>
 */
public final class CleanStructureAndUnionMembers implements ModelTransformerPlugin {
    @Override
    public Model onRemove(ModelTransformer transformer, Collection<Shape> removed, Model model) {
        // Remove members from containers that have been removed from the index.
        Model result = removeMembersFromContainers(transformer, removed, model);
        // Remove members from containers when the member targets a removed shape.
        return transformer.removeShapes(result, findMembersThatNeedRemoval(result, removed));
    }

    private Model removeMembersFromContainers(ModelTransformer transformer, Collection<Shape> removed, Model model) {
        List<Shape> replacements = new ArrayList<>(getStructureReplacements(model, removed));
        replacements.addAll(getUnionReplacements(model, removed));
        replacements.addAll(getEnumReplacements(model, removed));
        replacements.addAll(getIntEnumReplacements(model, removed));
        return transformer.replaceShapes(model, replacements);
    }

    private Collection<Shape> getEnumReplacements(Model model, Collection<Shape> removed) {
        return createUpdatedShapes(model, removed, Shape::asEnumShape, entry -> {
            EnumShape.Builder builder = entry.getKey().toBuilder();
            for (MemberShape member : entry.getValue()) {
                builder.removeMember(member.getMemberName());
            }
            return builder.build();
        });
    }

    private Collection<Shape> getIntEnumReplacements(Model model, Collection<Shape> removed) {
        return createUpdatedShapes(model, removed, Shape::asIntEnumShape, entry -> {
            IntEnumShape.Builder builder = entry.getKey().toBuilder();
            for (MemberShape member : entry.getValue()) {
                builder.removeMember(member.getMemberName());
            }
            return builder.build();
        });
    }

    private Collection<Shape> getStructureReplacements(Model model, Collection<Shape> removed) {
        return createUpdatedShapes(model, removed, Shape::asStructureShape, entry -> {
            StructureShape.Builder builder = entry.getKey().toBuilder();
            for (MemberShape member : entry.getValue()) {
                builder.removeMember(member.getMemberName());
            }
            return builder.build();
        });
    }

    private Collection<Shape> getUnionReplacements(Model model, Collection<Shape> removed) {
        return createUpdatedShapes(model, removed, Shape::asUnionShape, entry -> {
            UnionShape.Builder builder = entry.getKey().toBuilder();
            for (MemberShape member : entry.getValue()) {
                builder.removeMember(member.getMemberName());
            }
            return builder.build();
        });
    }

    /**
     * Finds structure and union shapes that have had members removed,
     * converts them to a builder, and then returns new versions of those
     * shapes without the members.
     *
     * <p>The removal of members is done using a single transformation of a
     * shape. For example, if multiple members are removed from a single
     * structure shape, the structure shape is rebuilt a single time. This is
     * done by first grouping the members into a map of {@code S} to a list
     * of members that were removed, allowing the grouped members to be
     * removed in a single pass.
     *
     * @param model Model used to get the container.
     * @param removed The collection of shapes that were removed.
     * @param containerShapeMapper A function that accepts a shape and tries
     *  to convert it to {@code S}.
     * @param entryMapperAndFactory A function that takes {@code S} and create
     *  a new version without the members.
     * @param <S> The shape type being transformed.
     * @return Returns a list of shapes that need to be modified in the model.
     */
    private <S extends Shape> Collection<Shape> createUpdatedShapes(
            Model model,
            Collection<Shape> removed,
            Function<Shape, Optional<S>> containerShapeMapper,
            Function<Map.Entry<S, List<MemberShape>>, S> entryMapperAndFactory
    ) {
        Map<S, List<MemberShape>> containerMemberMap = new HashMap<>();
        for (Shape shape : removed) {
            if (!shape.isMemberShape()) {
                continue;
            }

            MemberShape member = (MemberShape) shape;
            Optional<S> container = model.getShape(member.getContainer()).flatMap(containerShapeMapper);
            if (container.isPresent()) {
                containerMemberMap.computeIfAbsent(container.get(), k -> new ArrayList<>()).add(member);
            }
        }

        Collection<Shape> updatedShapes = new ArrayList<>();
        for (Map.Entry<S, List<MemberShape>> entry : containerMemberMap.entrySet()) {
            updatedShapes.add(entryMapperAndFactory.apply(entry));
        }
        return updatedShapes;
    }

    /**
     * Find members that target a shape that was removed, and ensure
     * that the member is removed.
     *
     * @param model Model to check.
     * @param removed The shapes that were removed.
     * @return Returns the member shapes that need to be removed because
     *  their target was removed.
     */
    private Collection<Shape> findMembersThatNeedRemoval(Model model, Collection<Shape> removed) {
        Set<ShapeId> removedIds = new HashSet<>();
        for (Shape shape : removed) {
            removedIds.add(shape.getId());
        }

        Collection<Shape> removeMembers = new HashSet<>();
        for (StructureShape structure : model.getStructureShapes()) {
            for (MemberShape member : structure.members()) {
                if (removedIds.contains(member.getTarget())) {
                    removeMembers.add(member);
                }
            }
        }
        for (UnionShape structure : model.getUnionShapes()) {
            for (MemberShape member : structure.members()) {
                if (removedIds.contains(member.getTarget())) {
                    removeMembers.add(member);
                }
            }
        }
        return removeMembers;
    }
}
