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
import java.util.List;
import java.util.Map;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.neighbor.RelationshipType;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.Pair;

/**
 * Handles fluently building shapes in a ModelIndex.Builder.
 */
public class ShapeCreator implements ToShapeId, FromSourceLocation {

    protected final ModelIndex.Builder builder;
    protected SourceLocation sourceLocation = SourceLocation.NONE;
    protected final Map<ShapeId, Trait> traits = new HashMap<>();
    protected final List<ModelIndex.Edge> edges = new ArrayList<>();
    private final ShapeId id;
    private final ShapeType shapeType;
    private Map<String, Pair<ShapeId, SourceLocation>> members;

    ShapeCreator(ModelIndex.Builder builder, ShapeId id, ShapeType shapeType) {
        this.builder = builder;
        this.id = id;
        this.shapeType = shapeType;
    }

    @Override
    public ShapeId toShapeId() {
        return id;
    }

    public final ShapeType getShapeType() {
        return shapeType;
    }

    public final Map<ShapeId, Trait> getTraits() {
        return traits;
    }

    public ShapeCreator putTrait(Trait trait) {
        traits.put(trait.toShapeId(), trait);
        return this;
    }

    public ShapeCreator sourceLocation(SourceLocation sourceLocation) {
        this.sourceLocation = sourceLocation;
        return this;
    }

    public ShapeCreator addMixin(ShapeId mixin) {
        this.edges.add(new ModelIndex.Edge(toShapeId(), RelationshipType.MIXIN, mixin));
        return this;
    }

    public ShapeCreator putMember(String name, ShapeId target) {
        return putMember(name, target, SourceLocation.NONE);
    }

    public ShapeCreator putMember(String name, ShapeId target, SourceLocation location) {
        if (name.isEmpty()) {
            throw new IllegalArgumentException("member name cannot be empty: " + name);
        }
        ShapeId memberId = toShapeId().withMember(name);
        switch (getShapeType()) {
            case STRUCTURE:
                edges.add(new ModelIndex.Edge(toShapeId(), RelationshipType.STRUCTURE_MEMBER, memberId, name));
                break;
            case UNION:
                edges.add(new ModelIndex.Edge(toShapeId(), RelationshipType.UNION_MEMBER, memberId, name));
                break;
            case INT_ENUM:
                edges.add(new ModelIndex.Edge(toShapeId(), RelationshipType.INT_ENUM_MEMBER, memberId, name));
                break;
            case ENUM:
                edges.add(new ModelIndex.Edge(toShapeId(), RelationshipType.ENUM_MEMBER, memberId, name));
                break;
            case LIST:
                if (!name.equals("member")) {
                    throw new IllegalArgumentException("Member names of lists must be 'member', found " + name);
                }
                edges.add(new ModelIndex.Edge(toShapeId(), RelationshipType.LIST_MEMBER, memberId, "member"));
                break;
            case SET:
                if (!name.equals("member")) {
                    throw new IllegalArgumentException("Member names of sets must be 'member', found " + name);
                }
                edges.add(new ModelIndex.Edge(toShapeId(), RelationshipType.SET_MEMBER, memberId, "member"));
                break;
            case MAP:
                if (name.equals("key")) {
                    edges.add(new ModelIndex.Edge(toShapeId(), RelationshipType.MAP_KEY, memberId, "key"));
                    break;
                }
                if (name.equals("value")) {
                    edges.add(new ModelIndex.Edge(toShapeId(), RelationshipType.MAP_VALUE, memberId, "value"));
                    break;
                }
                throw new IllegalArgumentException("Member names of maps must be 'key'/'value', found " + name);
            default:
                throw new IllegalArgumentException(getShapeType() + " does not contain members: " + name);
        }

        if (members == null) {
            members = new LinkedHashMap<>();
        }

        members.put(name, Pair.of(target, location));
        return this;
    }

    ModelIndex.Builder create() {
        builder.putShape(toShapeId(), getShapeType(), getSourceLocation());
        traits.forEach((i, t) -> builder.putTrait(toShapeId(), t));
        edges.forEach(builder::addEdge);

        if (members != null) {
            members.forEach((name, pair) -> builder.putMember(toShapeId(), name, pair.left, pair.right));
        }

        return builder;
    }

    public static final class Operation extends ShapeCreator {
        public Operation(ModelIndex.Builder builder, ShapeId id) {
            super(builder, id, ShapeType.OPERATION);
        }

        @Override
        public Operation putTrait(Trait trait) {
            super.putTrait(trait);
            return this;
        }

        @Override
        public Operation sourceLocation(SourceLocation sourceLocation) {
            super.sourceLocation(sourceLocation);
            return this;
        }

        @Override
        public Operation addMixin(ShapeId mixin) {
            super.addMixin(mixin);
            return this;
        }

        public Operation input(ShapeId id) {
            edges.add(new ModelIndex.Edge(toShapeId(), RelationshipType.INPUT, id));
            return this;
        }

        public Operation output(ShapeId id) {
            edges.add(new ModelIndex.Edge(toShapeId(), RelationshipType.OUTPUT, id));
            return this;
        }

        public Operation addError(ShapeId id) {
            edges.add(new ModelIndex.Edge(toShapeId(), RelationshipType.ERROR, id));
            return this;
        }
    }

    public static final class Resource extends ShapeCreator {
        public Resource(ModelIndex.Builder builder, ShapeId id) {
            super(builder, id, ShapeType.RESOURCE);
        }

        @Override
        public Resource putTrait(Trait trait) {
            super.putTrait(trait);
            return this;
        }

        @Override
        public Resource sourceLocation(SourceLocation sourceLocation) {
            super.sourceLocation(sourceLocation);
            return this;
        }

        @Override
        public Resource addMixin(ShapeId mixin) {
            super.addMixin(mixin);
            return this;
        }

        Resource addOperation(ShapeId id) {
            edges.add(new ModelIndex.Edge(toShapeId(), RelationshipType.OPERATION, id));
            return this;
        }

        Resource addResource(ShapeId id) {
            edges.add(new ModelIndex.Edge(toShapeId(), RelationshipType.RESOURCE, id));
            return this;
        }

        Resource putProperty(String name, ShapeId id) {
            edges.add(new ModelIndex.Edge(toShapeId(), RelationshipType.PROPERTY, id, name));
            return this;
        }

        Resource putIdentifier(String name, ShapeId id) {
            edges.add(new ModelIndex.Edge(toShapeId(), RelationshipType.IDENTIFIER, id, name));
            return this;
        }

        Resource create(ShapeId id) {
            edges.add(new ModelIndex.Edge(toShapeId(), RelationshipType.CREATE, id));
            edges.add(new ModelIndex.Edge(toShapeId(), RelationshipType.COLLECTION_OPERATION, id));
            return this;
        }

        Resource put(ShapeId id) {
            edges.add(new ModelIndex.Edge(toShapeId(), RelationshipType.PUT, id));
            edges.add(new ModelIndex.Edge(toShapeId(), RelationshipType.INSTANCE_OPERATION, id));
            return this;
        }

        Resource read(ShapeId id) {
            edges.add(new ModelIndex.Edge(toShapeId(), RelationshipType.READ, id));
            edges.add(new ModelIndex.Edge(toShapeId(), RelationshipType.INSTANCE_OPERATION, id));
            return this;
        }

        Resource update(ShapeId id) {
            edges.add(new ModelIndex.Edge(toShapeId(), RelationshipType.UPDATE, id));
            edges.add(new ModelIndex.Edge(toShapeId(), RelationshipType.INSTANCE_OPERATION, id));
            return this;
        }

        Resource delete(ShapeId id) {
            edges.add(new ModelIndex.Edge(toShapeId(), RelationshipType.DELETE, id));
            edges.add(new ModelIndex.Edge(toShapeId(), RelationshipType.INSTANCE_OPERATION, id));
            return this;
        }

        Resource list(ShapeId id) {
            edges.add(new ModelIndex.Edge(toShapeId(), RelationshipType.LIST, id));
            edges.add(new ModelIndex.Edge(toShapeId(), RelationshipType.COLLECTION_OPERATION, id));
            return this;
        }

        Resource addCollectionOperation(ShapeId id) {
            edges.add(new ModelIndex.Edge(toShapeId(), RelationshipType.COLLECTION_OPERATION, id));
            return this;
        }

        ModelIndex.Builder create() {
            super.create();
            return builder;
        }
    }

    public static final class Service extends ShapeCreator {

        private String version;
        private final ObjectNode.Builder rename = Node.objectNodeBuilder();

        public Service(ModelIndex.Builder builder, ShapeId id) {
            super(builder, id, ShapeType.SERVICE);
        }

        @Override
        public Service putTrait(Trait trait) {
            super.putTrait(trait);
            return this;
        }

        @Override
        public Service sourceLocation(SourceLocation sourceLocation) {
            super.sourceLocation(sourceLocation);
            return this;
        }

        @Override
        public Service addMixin(ShapeId mixin) {
            super.addMixin(mixin);
            return this;
        }

        public Service version(String version) {
            this.version = version;
            return this;
        }

        public Service rename(ShapeId shape, String toName) {
            this.rename.withMember(shape.toString(), toName);
            return this;
        }

        Service addOperation(ShapeId id) {
            edges.add(new ModelIndex.Edge(toShapeId(), RelationshipType.OPERATION, id));
            return this;
        }

        Service addResource(ShapeId id) {
            edges.add(new ModelIndex.Edge(toShapeId(), RelationshipType.RESOURCE, id));
            return this;
        }

        ModelIndex.Builder create() {
            super.create();
            if (version != null) {
                builder.putProperty(toShapeId(), "version", Node.from(version));
            }
            ObjectNode built = rename.build();
            if (!built.isEmpty()) {
                builder.putProperty(toShapeId(), "rename", built);
            }
            return builder;
        }
    }
}
