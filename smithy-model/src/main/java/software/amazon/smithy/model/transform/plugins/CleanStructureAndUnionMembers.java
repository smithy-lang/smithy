/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform.plugins;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
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
import software.amazon.smithy.utils.OptionalUtils;
import software.amazon.smithy.utils.Pair;

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
            entry.getValue().forEach(member -> builder.removeMember(member.getMemberName()));
            return builder.build();
        });
    }

    private Collection<Shape> getIntEnumReplacements(Model model, Collection<Shape> removed) {
        return createUpdatedShapes(model, removed, Shape::asIntEnumShape, entry -> {
            IntEnumShape.Builder builder = entry.getKey().toBuilder();
            entry.getValue().forEach(member -> builder.removeMember(member.getMemberName()));
            return builder.build();
        });
    }

    private Collection<Shape> getStructureReplacements(Model model, Collection<Shape> removed) {
        return createUpdatedShapes(model, removed, Shape::asStructureShape, entry -> {
            StructureShape.Builder builder = entry.getKey().toBuilder();
            entry.getValue().forEach(member -> builder.removeMember(member.getMemberName()));
            return builder.build();
        });
    }

    private Collection<Shape> getUnionReplacements(Model model, Collection<Shape> removed) {
        return createUpdatedShapes(model, removed, Shape::asUnionShape, entry -> {
            UnionShape.Builder builder = entry.getKey().toBuilder();
            entry.getValue().forEach(member -> builder.removeMember(member.getMemberName()));
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
        return removed.stream()
                .flatMap(shape -> OptionalUtils.stream(shape.asMemberShape()))
                .flatMap(member -> OptionalUtils.stream(model.getShape(member.getContainer())
                        .flatMap(containerShapeMapper)
                        .map(container -> Pair.of(container, member))))
                .collect(groupingBy(Pair::getLeft, mapping(Pair::getRight, Collectors.toList())))
                .entrySet()
                .stream()
                .map(entryMapperAndFactory)
                .collect(Collectors.toList());
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
        Set<ShapeId> removedIds = removed.stream().map(Shape::getId).collect(Collectors.toSet());
        Collection<Shape> removeMembers = new HashSet<>();
        model.shapes(StructureShape.class)
                .flatMap(shape -> shape.getAllMembers().values().stream())
                .filter(value -> removedIds.contains(value.getTarget()))
                .forEach(removeMembers::add);
        model.shapes(UnionShape.class)
                .flatMap(shape -> shape.getAllMembers().values().stream())
                .filter(value -> removedIds.contains(value.getTarget()))
                .forEach(removeMembers::add);
        return removeMembers;
    }
}
