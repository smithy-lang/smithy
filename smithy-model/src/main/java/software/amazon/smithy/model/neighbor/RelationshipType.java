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

package software.amazon.smithy.model.neighbor;

import java.util.Optional;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.IntEnumShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.SetShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Defines the relationship types between neighboring shapes.
 */
public enum RelationshipType {
    /**
     * A resource relationship exists between a service or resource and the
     * resources bound through the "resources" property.
     */
    RESOURCE("resource", RelationshipDirection.DIRECTED),

    /**
     * An operation relationship exists between a service and the operations
     * bound to the service in the "operations" property, and between a
     * resource and the operations bound to the resource in the
     * "operations", "collectionOperations", and lifecycle properties.
     */
    OPERATION("operation", RelationshipDirection.DIRECTED),

    /**
     * A collection operation relationship exists between a resource and the
     * operations bound to the resource in the "collectionOperations" property.
     */
    COLLECTION_OPERATION("collectionOperation", RelationshipDirection.DIRECTED),

    @Deprecated
    @SmithyInternalApi
    INSTANCE_OPERATION("instanceOperation", RelationshipDirection.DIRECTED),

    @Deprecated
    @SmithyInternalApi
    BOUND("bound", RelationshipDirection.INVERTED),

    /**
     * Relationships that exist between a resource and the put lifecycle
     * operation.
     */
    PUT("put", RelationshipDirection.DIRECTED),

    /**
     * Relationships that exist between a resource and the create lifecycle
     * operation.
     */
    CREATE("create", RelationshipDirection.DIRECTED),

    /**
     * Relationships that exist between a resource and the get lifecycle
     * operation.
     */
    READ("read", RelationshipDirection.DIRECTED),

    /**
     * Relationships that exist between a resource and the update lifecycle
     * operation.
     */
    UPDATE("update", RelationshipDirection.DIRECTED),

    /**
     * Relationships that exist between a resource and the delete lifecycle
     * operation.
     */
    DELETE("delete", RelationshipDirection.DIRECTED),

    /**
     * Relationships that exist between a resource and the list lifecycle
     * operation.
     */
    LIST("list", RelationshipDirection.DIRECTED),

    /**
     * Relationships that exist between a {@link ResourceShape member} and
     * the shapes that are referenced by its identifiers property.
     */
    IDENTIFIER("identifier", RelationshipDirection.DIRECTED),

    /**
     * Relationships that exist between a {@link ResourceShape member} and
     * the shapes that are referenced by its properties property.
     */
    PROPERTY("property", RelationshipDirection.DIRECTED),

    /**
     * Relationships exist on {@link MemberShape member} shapes. The subject
     * of the relationship is the member shape, and the neighbor is the
     * aggregate shape that contains the member.
     */
    MEMBER_CONTAINER(null, RelationshipDirection.INVERTED),

    /**
     * Relationships exist on {@link MemberShape member} shapes. The subject
     * of the relationship is the member shape, and the neighbor is the shape
     * that the member targets.
     */
    MEMBER_TARGET(null, RelationshipDirection.DIRECTED),

    /**
     * Relationships that exist on {@link OperationShape operation} shapes.
     * They reference {@link StructureShape structure} shapes that are used
     * as input.
     */
    INPUT("input", RelationshipDirection.DIRECTED),

    /**
     * Relationships that exist on {@link OperationShape operation} shapes.
     * They reference {@link StructureShape structure} shapes that are used
     * as output.
     */
    OUTPUT("output", RelationshipDirection.DIRECTED),

    /**
     * Relationships that exist on {@link OperationShape operation} shapes.
     * They reference {@link StructureShape structure} shapes that can be
     * returned from the operation.
     */
    ERROR("error", RelationshipDirection.DIRECTED),

    /**
     * Relationships that exist on {@link EnumShape enum} shapes to their
     * {@link MemberShape member shapes}.
     */
    ENUM_MEMBER("member", RelationshipDirection.DIRECTED),

    /**
     * Relationships that exist on {@link IntEnumShape intEnum} shapes to their
     * {@link MemberShape member shapes}.
     */
    INT_ENUM_MEMBER("member", RelationshipDirection.DIRECTED),

    /**
     * Relationships that exist on {@link ListShape list} shapes to their
     * {@link MemberShape member shapes}.
     */
    LIST_MEMBER("member", RelationshipDirection.DIRECTED),

    /**
     * Relationships that exist on {@link SetShape set} shapes to their
     * {@link MemberShape member shapes}.
     */
    @Deprecated
    SET_MEMBER("member", RelationshipDirection.DIRECTED),

    /**
     * Relationships that exist on {@link MapShape map} shapes. They reference
     * {@link MemberShape member} shapes that define the key type for the map.
     */
    MAP_KEY("member", RelationshipDirection.DIRECTED),

    /**
     * Relationships that exist on {@link MapShape map} shapes. They
     * reference {@link MemberShape member} shapes that define the value type
     * for the map.
     */
    MAP_VALUE("member", RelationshipDirection.DIRECTED),

    /**
     * Relationships that exist on {@link StructureShape structure} shapes.
     * They reference {@link MemberShape member} shapes that define the
     * attributes of a structure.
     */
    STRUCTURE_MEMBER("member", RelationshipDirection.DIRECTED),

    /**
     * Relationships that exist on {@link UnionShape union}
     * shapes. They reference the {@link MemberShape member} shapes that define
     * the members of the union.
     */
    UNION_MEMBER("member", RelationshipDirection.DIRECTED),

    /**
     * Relationships that exist between a shape and traits bound to the
     * shape. They reference shapes marked with the {@link TraitDefinition}
     * trait.
     *
     * <p>This kind of relationship is not returned by default from a
     * {@link NeighborProvider}. You must explicitly wrap a {@link NeighborProvider}
     * with {@link NeighborProvider#withTraitRelationships(Model, NeighborProvider)}
     * in order to yield trait relationships.
     */
    TRAIT("trait", RelationshipDirection.DIRECTED),

    /**
     * Relationship that exists between a structure or union and a mixin applied
     * to the shape.
     */
    MIXIN("mixin", RelationshipDirection.DIRECTED);

    private String selectorLabel;
    private RelationshipDirection direction;

    RelationshipType(String selectorLabel, RelationshipDirection direction) {
        this.selectorLabel = selectorLabel;
        this.direction = direction;
    }

    /**
     * Gets the token that is used in {@link Selector} expressions when
     * referring to the relationship or an empty {@code Optional} if this
     * relationship is not used directly in a selector.
     *
     * @return Returns the optionally present selector token for this relationship.
     */
    public Optional<String> getSelectorLabel() {
        return Optional.ofNullable(selectorLabel);
    }

    /**
     * Gets the direction of the relationship.
     *
     * <p>A {@link RelationshipDirection#DIRECTED} direction is formed from a shape
     * that defines a reference to another shape (for example, when a resource
     * defines operations or resources it contains).
     *
     * <p>A {@link RelationshipDirection#INVERTED} relationship is a relationship
     * from a shape to a shape that defines a relationship to it. The target
     * of such a relationship doesn't define the relationship, but is the
     * target of the relationship.
     *
     * @return Returns the direction of the relationship.
     */
    public RelationshipDirection getDirection() {
        return direction;
    }

    /**
     * Checks if the given relationship connects a container shape to a member.
     *
     * @return Returns true if a member.
     */
    public boolean isMemberBinding() {
        switch (this) {
            case STRUCTURE_MEMBER:
            case UNION_MEMBER:
            case LIST_MEMBER:
            case SET_MEMBER:
            case MAP_KEY:
            case MAP_VALUE:
            case INT_ENUM_MEMBER:
            case ENUM_MEMBER:
                return true;
            default:
                return false;
        }
    }

    /**
     * Checks if the binding is to an operation (create|read|update|delete|put|list|operation|collectionOperation).
     *
     * @return Returns true if the binding is for any kind of operation binding.
     */
    public boolean isOperationBinding() {
        return isInstanceOperationBinding() || isCollectionOperationBinding();
    }

    /**
     * Returns true if relationship connects a resource to an instance operation.
     *
     * @return True if an instance operation.
     */
    public boolean isInstanceOperationBinding() {
        switch (this) {
            case OPERATION:
            case READ:
            case UPDATE:
            case DELETE:
            case PUT:
                return true;
            default:
                return false;
        }
    }

    /**
     * Returns true if relationship connects a resource to a collection operation.
     *
     * @return True if a collection operation.
     */
    public boolean isCollectionOperationBinding() {
        switch (this) {
            case COLLECTION_OPERATION:
            case CREATE:
            case LIST:
                return true;
            default:
                return false;
        }
    }
}
