/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

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
import java.util.TreeSet;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.TopologicalShapeSort;
import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
import software.amazon.smithy.model.shapes.CollectionShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.SimpleShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.MixinTrait;

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
        Collection<Shape> shouldReplace = determineShapesToReplace(model);
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
        builder.addShapes(getUpdatedContainers(model, shouldReplace));

        // Builds the model, then returns a model that removes any shapes that
        // need to be removed after mapping over the shapes.
        return transformer.removeShapes(builder.build(), getRemovedMembers(model, shouldReplace));
    }

    private Collection<Shape> determineShapesToReplace(Model model) {
        // Sort the replacements to ensure that members come after container shapes.
        // This ensures that updates to members take precedence over updates to containers.
        Set<Shape> result = new TreeSet<>((a, b) -> {
            if (a.isMemberShape() ^ b.isMemberShape()) {
                return a.isMemberShape() ? 1 : -1;
            } else {
                return a.compareTo(b);
            }
        });

        for (Shape shape : replacements) {
            // Only replace shapes if they don't exist in the model or if they are different from the current shape.
            // This check prevents infinite recursion when this transformer and the RemoveShapes transformer
            // recursively call each other, and it prevents unnecessary work.
            if (!Objects.equals(shape, model.getShape(shape.getId()).orElse(null))) {
                result.add(shape);
            }
        }

        return result;
    }

    private void assertShapeTypeChangesSound(Model model, Collection<Shape> shouldReplace) {
        // Throws if any mappings attempted to change a shape's type.
        for (Shape shape : shouldReplace) {
            model.getShape(shape.getId()).ifPresent(previousShape -> {
                if (shape.getType() != previousShape.getType() && !isReplacementValid(shape, previousShape)) {
                    throw new ModelTransformException(String.format(
                            "Cannot change the type of %s from %s to %s",
                            previousShape.getId(),
                            previousShape.getType(),
                            shape.getType()));
                }
            });
        }
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

    private Model.Builder createReplacedModelBuilder(Model model, Collection<Shape> shouldReplace) {
        // Add member shapes to the builder. This builder is mutated
        // by the visitor, which will ensure that newly added members
        // show up in the model.
        Model.Builder builder = model.toBuilder();
        for (Shape shape : shouldReplace) {
            builder.addShape(shape);
            builder.addShapes(shape.members());
        }
        return builder;
    }

    private void updateMixins(Model model, Model.Builder builder, Collection<Shape> replacements) {
        // Create a map to function as a mutable kind of intermediate model index so that as
        // shapes are updated and built, they're used as mixins in shapes that depend on it.
        Map<ShapeId, Shape> updatedShapes = new HashMap<>();
        for (Shape replaced : replacements) {
            updatedShapes.put(replaced.getId(), replaced);
        }

        // Topologically sort the updated shapes to ensure shapes are updated in order.
        TopologicalShapeSort sorter = new TopologicalShapeSort();

        // Add shapes that are mixins or use mixins.
        for (Shape shape : model.toSet()) {
            if (!shape.isMemberShape() && (shape.hasTrait(MixinTrait.class) || !shape.getMixins().isEmpty())) {
                sorter.enqueue(shape);
            }
        }

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

    private Set<Shape> getRemovedMembers(Model model, Collection<Shape> beingReplaced) {
        // Ensure that when members are removed from a container shape (e.g., a structure with fewer members),
        // the removed members are removed from the model.
        Set<Shape> removedMembers = new HashSet<>();
        for (Shape currentShape : beingReplaced) {
            // Find the previous shape by ID from the model, and if present, determine what members were removed.
            model.getShape(currentShape.getId()).ifPresent(previousShape -> {
                Map<String, MemberShape> currentMembers = currentShape.getAllMembers();
                for (MemberShape previousMember : previousShape.members()) {
                    if (!currentMembers.containsKey(previousMember.getMemberName())) {
                        removedMembers.add(previousMember);
                    }
                }
            });
        }
        return removedMembers;
    }

    private Set<Shape> getUpdatedContainers(Model model, Collection<Shape> shouldReplace) {
        // Determine what shapes being updated are members, and group them by their container shapes.
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

    private Optional<Shape> findMostUpToDateShape(ShapeId shapeId, Model model, Collection<Shape> shouldReplace) {
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
}
