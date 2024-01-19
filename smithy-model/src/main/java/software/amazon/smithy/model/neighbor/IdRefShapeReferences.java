/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.model.neighbor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.selector.PathFinder;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.IdRefTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitDefinition;

/**
 * Finds all shapes referenced by {@link IdRefTrait} from within trait
 * values.
 *
 * <p>This works by finding all paths from {@link TraitDefinition} shapes
 * to shapes with {@link IdRefTrait}, then using those paths to search
 * the node values of each application of the trait to extract the {@link ShapeId}
 * value. Because we don't have a fixed set of traits known to potentially have
 * idRef members, this has to be done dynamically.
 */
final class IdRefShapeReferences {
    private static final Selector WITH_ID_REF = Selector.parse("[trait|idRef]");
    private final Model model;
    private final Set<ShapeId> found = new HashSet<>();

    IdRefShapeReferences(Model model) {
        this.model = model;
    }

    Set<ShapeId> compute(Set<Shape> connected) {
        PathFinder finder = PathFinder.create(model);
        for (Shape traitDef : model.getShapesWithTrait(TraitDefinition.class)) {
            if (traitDef.hasTrait(IdRefTrait.class)) {
                // PathFinder doesn't handle the case where the trait def has the idRef
                NodeQuery query = new NodeQuery().self();
                addReferences(traitDef, query, connected);
                continue;
            }
            List<PathFinder.Path> paths = finder.search(traitDef, WITH_ID_REF);
            if (!paths.isEmpty()) {
                for (PathFinder.Path path : paths) {
                    NodeQuery query = buildNodeQuery(path);
                    addReferences(traitDef, query, connected);
                }
            }
        }
        return found;
    }

    private void addReferences(Shape traitDef, NodeQuery query, Set<Shape> connected) {
        model.getShapesWithTrait(traitDef.getId()).stream()
                .filter(connected::contains)
                .forEach(shape -> {
                    Trait trait = shape.findTrait(traitDef.getId()).get(); // We already know the shape has the trait.
                    Node node = trait.toNode();
                    query.execute(node).forEach(n ->
                            // Invalid shape ids are handled by the idRef trait validator, so ignore them here.
                            n.asStringNode().flatMap(StringNode::asShapeId).ifPresent(found::add)
                    );
                });
    }

    private static NodeQuery buildNodeQuery(PathFinder.Path path) {
        NodeQuery query = new NodeQuery();
        // The path goes from trait definition -> shape with idRef
        for (Relationship relationship : path) {
            if (!relationship.getNeighborShape().isPresent()) {
                break;
            }
            switch (relationship.getRelationshipType()) {
                case MEMBER_TARGET:
                case MAP_KEY:
                case TRAIT:
                case MIXIN:
                default:
                    break;
                case MAP_VALUE:
                    query.anyMember();
                    break;
                case LIST_MEMBER:
                case SET_MEMBER:
                    query.anyElement();
                    break;
                case UNION_MEMBER:
                case STRUCTURE_MEMBER:
                    MemberShape member = (MemberShape) relationship.getNeighborShape().get();
                    query.member(member.getMemberName());
                    break;
            }
        }
        return query;
    }
}
