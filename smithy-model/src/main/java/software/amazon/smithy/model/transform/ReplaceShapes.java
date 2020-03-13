/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.transform;

import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.SetShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.utils.OptionalUtils;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SetUtils;

/**
 * Replaces shapes while ensuring that the transformed model is in a
 * consistent state.
 *
 * <p>This transformer accounts for the following scenarios:
 *
 * <ul>
 *     <li>When a member is modified, ensures that its containing shape
 *     references the new member.</li>
 *     <li>When an aggregate shape is modified, ensures that all members are
 *     updated in the model.</li>
 *     <li>When a member is removed from a structure or union,
 *     ensures that the member is removed from the model.</li>
 * </ul>
 *
 * <p>Only shapes that are not currently in a model or shapes that are
 * different than the existing shape in a model are inserted into a
 * transformed model.
 *
 * <p>When updates are made both to a container shape and to a member owned
 * by the container shape, the member updates takes precedent over any
 * updated made to the old member referenced by the container (e.g., if you
 * update a structure and change one of the members of the structure without
 * also updating the member in the model *and* you update a member in
 * the model, the member in the model will overwrite the member referenced
 * by the structure).
 *
 * <p>This transformer only supports replacing shapes if the previous shape
 * and new shape are of the same type. Any replacements encountered that
 * attempt to change the type of a shape will throw an exception.
 */
final class ReplaceShapes {
    private Collection<Shape> replacements;

    ReplaceShapes(Collection<Shape> replacements) {
        this.replacements = Objects.requireNonNull(replacements);
    }

    Model transform(ModelTransformer transformer, Model model) {
        List<Shape> shouldReplace = determineShapesToReplace(model);
        if (shouldReplace.isEmpty()) {
            return model;
        }

        assertNoShapeChangedType(model, shouldReplace);
        Model.Builder builder = createReplacedModelBuilder(model, shouldReplace);

        // If a member shape changes, then ensure that the containing shape
        // is also updated to reference the updated member. Note that the updated container
        // shapes will be a modified version of shapes present in the shouldReplace Set
        // over shapes in the provided model.
        getUpdatedContainers(model, shouldReplace).forEach(builder::addShape);

        // Builds the model, then returns a model that removes any shapes that
        // need to be removed after mapping over the shapes.
        return transformer.removeShapes(builder.build(), getShapesToRemove(model, shouldReplace));
    }

    private List<Shape> determineShapesToReplace(Model model) {
        return replacements.stream()
                // Only replace shapes if they don't exist in the model or if they are
                // different than the current shape in the model.
                //
                // This prevents infinite recursion when this transformer and the
                // RemoveShapes transformer recursively call each other. It also
                // prevents unnecessary allocations.
                .filter(shape -> !model.getShape(shape.getId())
                        .filter(original -> original.equals(shape))
                        .isPresent())
                // Sort the replacements to ensure that members come after container shapes.
                // This ensures that updates to members take precedence over updates to containers.
                .sorted((a, b) -> {
                    if (a.isMemberShape() ^ b.isMemberShape()) {
                        return a.isMemberShape() ? 1 : -1;
                    } else {
                        return 0;
                    }
                })
                .collect(toList());
    }

    private void assertNoShapeChangedType(Model model, List<Shape> shouldReplace) {
        // Throws if any mappings attempted to change a shape's type.
        shouldReplace.stream()
                .flatMap(previous -> Pair.flatMapStream(previous, p -> model.getShape(p.getId())))
                .filter(pair -> pair.getLeft().getType() != pair.getRight().getType())
                .forEach(pair -> {
                    throw new RuntimeException(String.format(
                            "Cannot change the type of %s from %s to %s",
                            pair.getLeft().getId(), pair.getLeft().getType(), pair.getRight().getType()));
                });
    }

    private Model.Builder createReplacedModelBuilder(Model model, List<Shape> shouldReplace) {
        // Add member shapes to the builder. This builder is mutated
        // by the visitor, which will ensure that newly added members
        // show up in the model.
        Model.Builder builder = model.toBuilder();
        shouldReplace.forEach(shape -> {
            builder.addShape(shape);
            builder.addShapes(shape.members());
        });
        return builder;
    }

    private Set<Shape> getShapesToRemove(Model model, List<Shape> shouldReplace) {
        // Ensure that when members are removed from a container shape
        // (e.g., a structure with fewer members), the removed members are
        // removed from the model.
        return shouldReplace.stream()
                .flatMap(shape -> Pair.flatMapStream(shape, s -> model.getShape(s.getId())))
                .flatMap(pair -> {
                    RemoveShapesVisitor removeShapesVisitor = new RemoveShapesVisitor(pair.getRight());
                    return pair.getLeft().accept(removeShapesVisitor).stream();
                })
                .collect(Collectors.toSet());
    }

    private Set<Shape> getUpdatedContainers(Model model, List<Shape> shouldReplace) {
        // Account for multiple members being updated on the same container.
        Map<Shape, List<MemberShape>> containerToMemberMapping = shouldReplace.stream()
                .flatMap(shape -> OptionalUtils.stream(shape.asMemberShape()))
                .flatMap(member -> Pair.flatMapStream(
                        member, m -> findContainerShape(m.getContainer(), model, shouldReplace)))
                .collect(Collectors.groupingBy(Pair::getRight, mapping(Pair::getLeft, toList())));

        // TODO: This could be made more efficient by building containers only once.
        return containerToMemberMapping.entrySet().stream()
                .map(entry -> {
                    Shape container = entry.getKey();
                    List<MemberShape> members = entry.getValue();
                    for (MemberShape member : members) {
                        container = container.accept(new UpdateContainerVisitor(member)).orElse(container);
                    }
                    // Only update if changed.
                    return container.equals(entry.getKey()) ? null : container;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Optional<Shape> findContainerShape(ShapeId shapeId, Model model, List<Shape> shouldReplace) {
        // Shapes in the replacement set take precedence over shapes in the previous model.
        // This accounts for newly added shapes and not overwriting changes also made to the
        // container shape.
        Optional<Shape> result = shouldReplace.stream().filter(shape -> shape.getId().equals(shapeId)).findFirst();
        return result.isPresent() ? result : model.getShape(shapeId);
    }

    /**
     * Gets the member shapes of structures and unions that were
     * removed as a result of mapping. These removed members need to also be
     * removed from the Model.
     */
    private static final class RemoveShapesVisitor extends ShapeVisitor.Default<Collection<Shape>> {

        private final Shape previous;

        RemoveShapesVisitor(Shape previous) {
            this.previous = previous;
        }

        @Override
        public Collection<Shape> getDefault(Shape shape) {
            return SetUtils.of();
        }

        @Override
        public Collection<Shape> unionShape(UnionShape shape) {
            return onNamedMemberContainer(
                    shape.getAllMembers(),
                    previous.expectUnionShape().getAllMembers());
        }

        @Override
        public Collection<Shape> structureShape(StructureShape shape) {
            return onNamedMemberContainer(
                    getStructureMemberMap(shape),
                    getStructureMemberMap(previous.expectStructureShape()));
        }

        private Map<String, MemberShape> getStructureMemberMap(StructureShape shape) {
            return shape.getAllMembers().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        private Collection<Shape> onNamedMemberContainer(
                Map<String, MemberShape> members,
                Map<String, MemberShape> previousMembers
        ) {
            // Find members that were removed as a result of transforming the shape.
            Set<String> removedMembers = new HashSet<>(previousMembers.keySet());
            removedMembers.removeAll(members.keySet());
            return removedMembers.stream().map(previousMembers::get).collect(Collectors.toSet());
        }
    }

    /**
     * Updates the container shape of a member when a member changes,
     * IFF the member differs from what's in the shape.
     */
    private static final class UpdateContainerVisitor extends ShapeVisitor.Default<Optional<Shape>> {

        private final MemberShape member;

        UpdateContainerVisitor(MemberShape member) {
            this.member = member;
        }

        @Override
        public Optional<Shape> getDefault(Shape shape) {
            return Optional.empty();
        }

        @Override
        public Optional<Shape> listShape(ListShape shape) {
            return shape.getMember() == member
                   ? Optional.empty()
                   : Optional.of(shape.toBuilder().member(member).build());
        }

        @Override
        public Optional<Shape> setShape(SetShape shape) {
            return shape.getMember() == member
                   ? Optional.empty()
                   : Optional.of(shape.toBuilder().member(member).build());
        }

        @Override
        public Optional<Shape> mapShape(MapShape shape) {
            if (member.getMemberName().equals("key")) {
                return shape.getKey() == member
                       ? Optional.empty()
                       : Optional.of(shape.toBuilder().key(member).build());
            } else {
                return shape.getValue() == member
                       ? Optional.empty()
                       : Optional.of(shape.toBuilder().value(member).build());
            }
        }

        @Override
        public Optional<Shape> structureShape(StructureShape shape) {
            // Replace the existing structure member with the new member.
            return shape.getMember(member.getMemberName())
                    .filter(oldMember -> !oldMember.equals(member))
                    .map(oldMember -> shape.toBuilder().addMember(member).build());
        }

        @Override
        public Optional<Shape> unionShape(UnionShape shape) {
            // Replace the existing union member with the new member.
            return shape.getMember(member.getMemberName())
                    .filter(oldMember -> oldMember != member)
                    .map(oldMember -> shape.toBuilder().addMember(member).build());
        }
    }
}
