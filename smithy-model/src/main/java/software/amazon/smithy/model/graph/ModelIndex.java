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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.neighbor.RelationshipType;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

public final class ModelIndex implements ToSmithyBuilder<ModelIndex>, Iterable<ShapeId> {

    public static final class Edge {
        private final ShapeId from;
        private final ShapeId target;
        private final RelationshipType type;
        private final String label;

        public Edge(ShapeId from, RelationshipType type, ShapeId target) {
            this(from, type, target, null);
        }

        public Edge(ShapeId from, RelationshipType type, ShapeId target, String label) {
            this.from = from;
            this.type = type;
            this.target = target;
            this.label = label == null ? "" : label;
        }

        public ShapeId from() {
            return from;
        }

        public ShapeId target() {
            return target;
        }

        public RelationshipType type() {
            return type;
        }

        public String label() {
            return label;
        }

        @Override
        public String toString() {
            return "Edge{" +
                   "from=" + from +
                   ", target=" + target +
                   ", type=" + type +
                   ", label='" + label + '\'' +
                   '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Edge edge = (Edge) o;
            return from.equals(edge.from) && target.equals(edge.target) && type == edge.type
                   && label.equals(edge.label);
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, target, type, label);
        }
    }

    private static final class ShapeEntry {
        private final ShapeType type;
        private final ShapeId id;
        private final SourceLocation location;

        public ShapeEntry(ShapeType type, ShapeId id, SourceLocation location) {
            this.type = type;
            this.id = id;
            this.location = location;
        }
    }

    private final Map<ShapeId, ShapeEntry> shapes;
    private final Map<ShapeId, Map<ShapeId, Trait>> traits;
    private final Map<ShapeId, Map<String, Node>> properties;
    private final Map<ShapeId, List<Edge>> edges;
    private final Map<ShapeId, List<Edge>> reverseEdges;

    private ModelIndex(Builder builder) {
        this.shapes = builder.shapes.copy();
        this.traits = builder.traits.copy();
        this.properties = builder.properties.copy();
        this.edges = builder.edges.copy();
        this.reverseEdges = builder.reverseEdges.copy();

        /*
        for (ShapeId hasMixin : builder().hasMixins) {
            ShapeEntry entry = shapes.get(hasMixin);
            if (entry == null || !(entry.type.getCategory() == ShapeType.Category.AGGREGATE
                                   || entry.type == ShapeType.INT_ENUM
                                   || entry.type == ShapeType.ENUM)) {
                continue;
            }

            // TODO: Create synthetic members based on direct members and mixed in members.
            //  More complicated than I thought. Needs toposort.
            Map<String, ShapeId> directMembers = determineMembers(hasMixin);
            List<Map<String, ShapeId>> mixinMembers = new ArrayList<>();
            for (Edge edge : getEdges(hasMixin)) {
                if (edge.type == RelationshipType.MIXIN) {
                    mixinMembers.add(determineMembers(edge.target));
                }
            }
        }
        */
    }

    /*
    private Map<String, ShapeId> determineMembers(ShapeId id) {
        Map<String, ShapeId> members = new HashMap<>();
        for (Edge edge : getEdges(id)) {
            switch (edge.type) {
                case STRUCTURE_MEMBER:
                case UNION_MEMBER:
                case LIST_MEMBER:
                case SET_MEMBER:
                case MAP_KEY:
                case MAP_VALUE:
                case INT_ENUM_MEMBER:
                case ENUM_MEMBER:
                    members.put(edge.label, edge.target);
                    break;
                default:
                    break;
            }
        }
        return members;
    }
    */

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        Builder builder = builder();
        builder.shapes.get().putAll(shapes);
        builder.properties.get().putAll(properties);
        builder.traits.get().putAll(traits);
        builder.edges.get().putAll(edges);
        builder.reverseEdges.get().putAll(reverseEdges);
        return builder;
    }

    public boolean containsShape(ShapeId id) {
        if (shapes.containsKey(id)) {
            return true;
        } else if (id.hasMember()) {
            ShapeId without = id.withoutMember();
            if (shapes.containsKey(without)) {
                return getShape(without).hasMember(id.getMember().get());
            }
        }
        return false;
    }

    @Override
    public Iterator<ShapeId> iterator() {
        // TODO: Return shape IDs of synthesized members.
        return shapes.keySet().iterator();
    }

    public ShapeType getType(ShapeId id) {
        return Objects.requireNonNull(shapes.get(id)).type;
    }

    public SourceLocation getSourceLocation(ShapeId id) {
        ShapeEntry entry = shapes.get(id);
        if (entry == null) {
            return SourceLocation.NONE;
        }
        return entry.location;
    }

    public Map<String, Node> getProperties(ShapeId id) {
        return properties.getOrDefault(id, Collections.emptyMap());
    }

    public List<Edge> getEdges(ShapeId shape) {
        return edges.getOrDefault(shape, Collections.emptyList());
    }

    public List<Edge> getEdgesWithTraits(ShapeId shape) {
        return edges.getOrDefault(shape, Collections.emptyList());
    }

    public List<Edge> getReverseEdges(ShapeId shape) {
        return reverseEdges.getOrDefault(shape, Collections.emptyList());
    }

    public Set<ShapeId> getShapesWithTrait(ShapeId trait) {
        return getReverseEdges(trait).stream().map(e -> e.from).collect(Collectors.toSet());
    }

    public Map<ShapeId, Trait> getTraits(ShapeId id) {
        return traits.getOrDefault(id, Collections.emptyMap());
    }

    public ShapeCursor getShape(ShapeId id) {
        ShapeEntry entry = Objects.requireNonNull(shapes.get(id));
        switch (entry.type) {
            case STRUCTURE:
                return getStructure(id);
            case MEMBER:
                return getMember(id);
            default:
                return new ShapeCursor() {
                    @Override
                    public ShapeId toShapeId() {
                        return entry.id;
                    }

                    @Override
                    public ModelIndex index() {
                        return ModelIndex.this;
                    }
                };
        }
    }

    public StructureCursor getStructure(ShapeId id) {
        ShapeEntry entry = Objects.requireNonNull(shapes.get(id));
        if (entry.type != ShapeType.STRUCTURE) {
            throw new IllegalArgumentException("Expected a structure");
        }
        return new StructureCursor(this, id);
    }

    public MemberCursor getMember(ShapeId id) {
        ShapeEntry entry = Objects.requireNonNull(shapes.get(id));
        if (entry.type != ShapeType.MEMBER) {
            throw new IllegalArgumentException("Expected a member");
        }
        return new MemberCursor(this, id);
    }

    public OperationCursor getOperation(ShapeId id) {
        ShapeEntry entry = Objects.requireNonNull(shapes.get(id));
        if (entry.type != ShapeType.OPERATION) {
            throw new IllegalArgumentException("Expected an operation");
        }
        return new OperationCursor(this, id);
    }

    public static final class Builder implements SmithyBuilder<ModelIndex> {
        private final BuilderRef<Map<ShapeId, ShapeEntry>> shapes = BuilderRef.forUnorderedMap();
        private final BuilderRef<Map<ShapeId, Map<ShapeId, Trait>>> traits = BuilderRef.forUnorderedMap();
        private final BuilderRef<Map<ShapeId, Map<String, Node>>> properties = BuilderRef.forUnorderedMap();
        private final BuilderRef<Map<ShapeId, List<Edge>>> edges = BuilderRef.forUnorderedMap();
        private final BuilderRef<Map<ShapeId, List<Edge>>> reverseEdges = BuilderRef.forUnorderedMap();

        public Builder putTrait(ShapeId id, Trait trait) {
            traits.get().computeIfAbsent(id, i -> new HashMap<>()).put(trait.toShapeId(), trait);
            reverseEdges.get().computeIfAbsent(trait.toShapeId(), t -> new ArrayList<>())
                    .add(new Edge(id, RelationshipType.TRAIT, trait.toShapeId()));
            return this;
        }

        public ShapeCreator createShape(ShapeId id, ShapeType type) {
            return new ShapeCreator(this, id, type);
        }

        public ShapeCreator.Service createService(ShapeId shapeId) {
            return new ShapeCreator.Service(this, shapeId);
        }

        public ShapeCreator.Operation createOperation(ShapeId shapeId) {
            return new ShapeCreator.Operation(this, shapeId);
        }

        public Builder removeShape(ShapeId id) {
            shapes.get().remove(id);
            traits.get().remove(id);
            properties.get().remove(id);
            edges.get().remove(id);

            // Remove forward and reverse edges.
            for (Edge edge : reverseEdges.peek().getOrDefault(id, Collections.emptyList())) {
                List<Edge> forward = edges.get().get(edge.target);
                if (forward != null) {
                    forward.removeIf(e -> e.target == id || e.from == id);
                }
            }

            return this;
        }

        public Builder removeTrait(ShapeId id, ShapeId trait) {
            Map<ShapeId, Trait> traitMap = traits.get().get(id);
            if (traitMap != null) {
                if (traitMap.remove(trait) != null) {
                    // Remove the reverse edge to the trait.
                    List<Edge> reverse = reverseEdges.get().get(trait.toShapeId());
                    if (reverse != null) {
                        reverse.removeIf(e -> e.from.equals(id));
                    }
                }
            }
            return this;
        }

        public Builder removeEdge(Edge edge) {
            List<Edge> forward = edges.get().get(edge.from);
            if (forward != null) {
                forward.remove(edge);
            }
            List<Edge> reverse = reverseEdges.get().get(edge.target);
            if (reverse != null) {
                reverse.remove(edge);
            }
            return this;
        }

        Builder putShape(ShapeId id, ShapeType type, SourceLocation sourceLocation) {
            shapes.get().put(id, new ShapeEntry(type, id, sourceLocation));
            return this;
        }

        Builder putProperty(ShapeId id, String label, Node value) {
            properties.get().computeIfAbsent(id, i -> new HashMap<>()).put(label, value);
            return this;
        }

        Builder addEdge(Edge edge) {
            edges.get().computeIfAbsent(edge.from, i -> new ArrayList<>()).add(edge);
            reverseEdges.get().computeIfAbsent(edge.target, i -> new ArrayList<>()).add(edge);
            return this;
        }

        Builder putMember(ShapeId container, String name, ShapeId target, SourceLocation location) {
            ShapeId memberId = container.withMember(name);
            putShape(memberId, ShapeType.MEMBER, location);
            addEdge(new Edge(memberId, RelationshipType.MEMBER_CONTAINER, container));
            addEdge(new Edge(memberId, RelationshipType.MEMBER_TARGET, target));
            return this;
        }

        @Override
        public ModelIndex build() {
            return new ModelIndex(this);
        }
    }
}
