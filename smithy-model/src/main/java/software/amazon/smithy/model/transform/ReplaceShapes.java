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

package software.amazon.smithy.model.transform;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.TopologicalShapeSort;
import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
import software.amazon.smithy.model.shapes.CollectionShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.SimpleShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.MixinTrait;
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
 *     <li>When a mixin is updated, ensures that all shapes that use the
 *     mixin are updated.</li>
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
    private final Collection<? extends Shape> replacements;

    ReplaceShapes(Collection<? extends Shape> replacements) {
        this.replacements = Objects.requireNonNull(replacements);
    }

    Model transform(ModelTransformer transformer, Model model) {
        List<Shape> shouldReplace = determineShapesToReplace(model);
        if (shouldReplace.isEmpty()) {
            return model;
        }

        assertShapeTypeChangesSound(model, shouldReplace);
        Model.Builder builder = createReplacedModelBuilder(model, shouldReplace);

        // Update shapes that relied on mixins that were updated.
        updateMixins(model, builder, shouldReplace);

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

    private void assertShapeTypeChangesSound(Model model, List<Shape> shouldReplace) {
        // Throws if any mappings attempted to change a shape's type.
        shouldReplace.stream()
                .flatMap(previous -> Pair.flatMapStream(previous, p -> model.getShape(p.getId())))
                .filter(pair -> pair.getLeft().getType() != pair.getRight().getType())
                .filter(pair -> !isReplacementValid(pair.getLeft(), pair.getRight()))
                .forEach(pair -> {
                    throw new ModelTransformException(String.format(
                            "Cannot change the type of %s from %s to %s",
                            pair.getLeft().getId(), pair.getRight().getType(), pair.getLeft().getType()));
                });
    }

    private boolean isReplacementValid(Shape left, Shape right) {
        if (left instanceof CollectionShape && right instanceof CollectionShape) {
            return true;
        } else if (left instanceof StructureShape && right instanceof UnionShape) {
            return true;
        } else if (left instanceof UnionShape && right instanceof StructureShape) {
            return true;
        } else {
            return left instanceof SimpleShape && right instanceof SimpleShape;
        }
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

    private void updateMixins(Model model, Model.Builder builder, List<Shape> replacements) {
        // Create a map to function as a mutable kind of intermediate model index so that as
        // shapes are updated and built, they're used as mixins in shapes that depend on it.
        Map<ShapeId, Shape> updatedShapes = new HashMap<>();
        for (Shape replaced : replacements) {
            updatedShapes.put(replaced.getId(), replaced);
        }

        // Topologically sort the updated shapes to ensure shapes are updated in order.
        TopologicalShapeSort sorter = new TopologicalShapeSort();

        // Add shapes that are mixins or use mixins.
        model.shapes()
            .filter(shape -> !shape.isMemberShape())
            .filter(shape -> shape.hasTrait(MixinTrait.class) || !shape.getMixins().isEmpty())
            .forEach(sorter::enqueue);

        // Add _all_ of the replacements in case mixins or the Mixin trait were removed from updated shapes.
        for (Shape shape : replacements) {
            // Enqueue member shapes with empty dependencies since the parent will be enqueued with them.
            if (shape.isMemberShape()) {
                sorter.enqueue(shape.getId(), Collections.emptySet());
            } else {
                sorter.enqueue(shape);
            }
        }

        List<ShapeId> sorted = sorter.dequeueSortedShapes();
        for (ShapeId toRebuild : sorted) {
            Shape shape = updatedShapes.containsKey(toRebuild)
                    ? updatedShapes.get(toRebuild)
                    : model.expectShape(toRebuild);
            if (!shape.getMixins().isEmpty()) {
                // We don't clear mixins here because a shape might have an inherited
                // mixin member that was updated with an applied trait. Clearing mixins
                // would remove this member but not re-add it properly. Re-adding already
                // present mixins, however, will update members in-place.
                AbstractShapeBuilder<?, ?> shapeBuilder = Shape.shapeToBuilder(shape);
                for (ShapeId mixin : shape.getMixins()) {
                    Shape mixinShape = updatedShapes.containsKey(mixin)
                            ? updatedShapes.get(mixin)
                            : model.expectShape(mixin);
                    shapeBuilder.addMixin(mixinShape);
                }
                Shape rebuilt = shapeBuilder.build();
                builder.addShape(rebuilt);
                updatedShapes.put(rebuilt.getId(), rebuilt);
            }
        }
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
        // Determine which shapes being updated are members, and group them by their container shapes.
        Map<Shape, List<MemberShape>> containerToMemberMapping = new HashMap<>();
        for (Shape shape : shouldReplace) {
            shape.asMemberShape().ifPresent(member -> {
                findMostUpToDateShape(member.getContainer(), model, shouldReplace).ifPresent(container -> {
                    // Ignore members that are already a current member of a shape.
                    if (!isMemberPresent(member, container)) {
                        containerToMemberMapping.computeIfAbsent(container, c -> new ArrayList<>()).add(member);
                    }
                });
            });
        }

        Set<Shape> containersToUpdate = new HashSet<>(containerToMemberMapping.size());

        // Update the containers with members.
        for (Map.Entry<Shape, List<MemberShape>> entry : containerToMemberMapping.entrySet()) {
            Shape container = entry.getKey();
            List<MemberShape> members = entry.getValue();
            AbstractShapeBuilder<?, ?> builder = Shape.shapeToBuilder(container);
            for (MemberShape member : members) {
                builder.addMember(member);
            }
            containersToUpdate.add(builder.build());
        }

        return containersToUpdate;
    }

    private static boolean isMemberPresent(MemberShape member, Shape shape) {
        // Instance equality is preferred here to account for doing things like updating source locations which aren't
        // part of the equals method of shapes.
        return shape.getMember(member.getMemberName()).filter(m -> m == member).isPresent();
    }

    private Optional<Shape> findMostUpToDateShape(ShapeId shapeId, Model model, List<Shape> shouldReplace) {
        // Shapes in the replacement set take precedence over shapes in the previous model.
        // This accounts for newly added shapes and not overwriting changes also made to the
        // container shape.
        for (Shape shape : shouldReplace) {
            if (shape.getId().equals(shapeId)) {
                return Optional.of(shape);
            }
        }
        return model.getShape(shapeId);
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
            // Use previous.members() in case of a type change to prevent needing to call asUnion.
            return onNamedMemberContainer(shape.getAllMembers(), previous.members());
        }

        @Override
        public Collection<Shape> structureShape(StructureShape shape) {
            // Use previous.members() in case of a type change to prevent needing to call asStructure.
            return onNamedMemberContainer(shape.getAllMembers(), previous.members());
        }

        private Collection<Shape> onNamedMemberContainer(
                Map<String, MemberShape> members,
                Collection<MemberShape> previous
        ) {
            List<Shape> result = new ArrayList<>();
            for (MemberShape previousMember : previous) {
                if (!members.containsKey(previousMember.getMemberName())) {
                    result.add(previousMember);
                }
            }
            return result;
        }
    }
}
