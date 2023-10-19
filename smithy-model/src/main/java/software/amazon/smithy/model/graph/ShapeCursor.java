/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.neighbor.RelationshipType;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.MixinTrait;
import software.amazon.smithy.model.traits.Trait;

/**
 * Creates an easy to use a structured pointer into the edges of a ModelIndex for each type of shape, allowing
 * seamless navigation between shapes.
 */
public interface ShapeCursor extends FromSourceLocation, ToShapeId {

    ModelIndex index();

    @Override
    default SourceLocation getSourceLocation() {
        return index().getSourceLocation(toShapeId());
    }

    default ShapeType type() {
        return index().getType(toShapeId());
    }

    default ModelIndex.Edge getFirstEdge(RelationshipType type) {
        for (ModelIndex.Edge edge : getEdges()) {
            if (edge.type() == type) {
                return edge;
            }
        }
        return null;
    }

    // TODO: We'd want to cache this in a real implementation.
    default List<ModelIndex.Edge> getEdges() {
        LinkedList<ModelIndex.Edge> edges = new LinkedList<>();
        // Return a properly merged set of edges taking mixins and relationship cardinality into account.
        getMixins().forEach(mixin -> mixin.getEdges().forEach(e -> addEdge(toShapeId(), e, edges)));
        index().getEdges(toShapeId()).forEach(e -> addEdge(toShapeId(), e, edges));
        return edges;
    }

    // TODO: This is pretty inefficient.
    static void addEdge(ShapeId fromShape, ModelIndex.Edge e, LinkedList<ModelIndex.Edge> edges) {
        // Don't give back edges for mixed-in rels using this mixin shape ID.
        ModelIndex.Edge updatedEdge = e.from().equals(fromShape)
                                  ? e
                                  : new ModelIndex.Edge(fromShape, e.type(), e.target(), e.label());
        switch (e.type().getCardinality()) {
            case ONE:
                edges.removeIf(existing -> updatedEdge.type() == existing.type());
                edges.addFirst(updatedEdge);
                break;
            case MANY:
                edges.addFirst(updatedEdge);
                break;
            case MANY_NAMED:
                edges.removeIf(existing -> updatedEdge.type() == existing.type()
                                           && updatedEdge.label().equals(existing.label()));
                edges.addFirst(updatedEdge);
                break;
        }
    }

    default List<ModelIndex.Edge> getEdgesWithTraits() {
        List<ModelIndex.Edge> edges = new ArrayList<>(index().getEdges(toShapeId()));
        for (Trait trait : getTraits().values()) {
            edges.add(new ModelIndex.Edge(toShapeId(), RelationshipType.TRAIT, trait.toShapeId()));
        }
        return edges;
    }

    default List<ModelIndex.Edge> getReverseEdges() {
        return index().getReverseEdges(toShapeId());
    }

    // TODO: Cache in a real implementation.
    default Map<ShapeId, Trait> getTraits() {
        Map<ShapeId, Trait> traits = new HashMap<>();

        for (ShapeCursor cursor : getMixins()) {
            for (Map.Entry<ShapeId, Trait> entry : cursor.getTraits().entrySet()) {
                // TODO: Filter out mixin exclusions without hardcoding this.
                if (!entry.getKey().equals(MixinTrait.ID)) {
                    traits.put(entry.getKey(), entry.getValue());
                }
            }
        }

        traits.putAll(getDeclaredTraits());
        return traits;
    }

    default Map<ShapeId, Trait> getDeclaredTraits() {
        return index().getTraits(toShapeId());
    }

    default <T extends Trait> Optional<T> getTrait(Class<T> trait) {
        for (Trait t : getTraits().values()) {
            if (trait.isInstance(t)) {
                return Optional.of(trait.cast(t));
            }
        }
        return Optional.empty();
    }

    default  <T extends Trait> T expectTrait(Class<T> trait) {
        return getTrait(trait)
                .orElseThrow(() -> new ExpectationNotMetException("Trait not found", SourceLocation.NONE));
    }

    // TODO: Cache in a real implementation.
    default List<ShapeCursor> getMixins() {
        ModelIndex index = index();
        List<ShapeCursor> mixins = new ArrayList<>();
        for (ModelIndex.Edge edge : index.getEdges(toShapeId())) {
            if (edge.type() == RelationshipType.MIXIN) {
                mixins.add(index.getShape(edge.target()));
            }
        }
        return mixins;
    }

    default boolean hasMember(String name) {
        if (getDeclaredMembers().containsKey(name)) {
            return true;
        }
        for (ShapeCursor mixin : getMixins()) {
            if (mixin.hasMember(name)) {
                return true;
            }
        }
        return false;
    }

    // TODO: Cache in a real implementation.
    default Map<String, MemberCursor> getMembers() {
        Map<String, MemberCursor> members = new LinkedHashMap<>();
        for (ShapeCursor mixin : getMixins()) {
            members.putAll(mixin.getMembers());
        }
        // TODO: Merge mixed in members and their traits?
        members.putAll(getDeclaredMembers());
        return members;
    }

    // TODO: Cache in a real implementation.
    default Map<String, MemberCursor> getDeclaredMembers() {
        ModelIndex index = index();
        Map<String, MemberCursor> members = new LinkedHashMap<>();
        for (ModelIndex.Edge edge : index.getEdges(toShapeId())) {
            switch (edge.type()) {
                case STRUCTURE_MEMBER:
                case LIST_MEMBER:
                case SET_MEMBER:
                case UNION_MEMBER:
                case ENUM_MEMBER:
                case INT_ENUM_MEMBER:
                    members.put(edge.label(), new MemberCursor(index, edge.target()));
                default:
                    break;
            }
        }
        return members;
    }
}
