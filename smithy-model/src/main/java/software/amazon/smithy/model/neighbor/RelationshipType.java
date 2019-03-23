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

import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.SetShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;

/**
 * Defines the relationship types between neighboring shapes.
 */
public enum RelationshipType {
    /**
     * A resource relationship exists between a service or resource and the
     * resources bound through the "resources" property.
     */
    RESOURCE,

    /**
     * An operation relationship exists between a service and the operations
     * bound to the service in the "operations" property, and between a
     * resource and the operations bound to the resource in only the
     * "operations" property.
     */
    OPERATION,

    /**
     * A BINDING relationship exists between the following shapes:
     *
     * <ul>
     *     <li>Between an operation and the service or resource that the
     *     operation is bound to (through operations or a lifecycle).</li>
     *     <li>Between a resource and the resource or service that the
     *     resource is bound to in the "resources" property.</li>
     * </ul>
     *
     * The subject of the relationship is that shape that was bound, and the
     * target is the shape that declared the binding.
     */
    BOUND,

    /**
     * Relationships that exist between a resource and the create lifecycle
     * operation.
     */
    CREATE,

    /**
     * Relationships that exist between a resource and the get lifecycle
     * operation.
     */
    READ,

    /**
     * Relationships that exist between a resource and the update lifecycle
     * operation.
     */
    UPDATE,

    /**
     * Relationships that exist between a resource and the delete lifecycle
     * operation.
     */
    DELETE,

    /**
     * Relationships that exist between a resource and the list lifecycle
     * operation.
     */
    LIST,

    /**
     * Relationships that exist between a {@link ResourceShape member} and
     * the shapes that are referenced by its identifiers property.
     */
    IDENTIFIER,

    /**
     * Relationships exist on {@link MemberShape member} shapes. The subject
     * of the relationship is the member shape, and the neighbor is the
     * aggregate shape that contains the member.
     */
    MEMBER_CONTAINER,

    /**
     * Relationships exist on {@link MemberShape member} shapes. The subject
     * of the relationship is the member shape, and the neighbor is the shape
     * that the member targets.
     */
    MEMBER_TARGET,

    /**
     * Relationships that exist on {@link OperationShape operation} shapes.
     * They reference {@link StructureShape structure} shapes that are used
     * as input.
     */
    INPUT,

    /**
     * Relationships that exist on {@link OperationShape operation} shapes.
     * They reference {@link StructureShape structure} shapes that are used
     * as output.
     */
    OUTPUT,

    /**
     * Relationships that exist on {@link OperationShape operation} shapes.
     * They reference {@link StructureShape structure} shapes that can be
     * returned from the operation.
     */
    ERROR,

    /**
     * Relationships that exist on {@link ListShape list} shapes to their
     * {@link MemberShape member shapes}.
     */
    LIST_MEMBER,

    /**
     * Relationships that exist on {@link SetShape set} shapes to their
     * {@link MemberShape member shapes}.
     */
    SET_MEMBER,

    /**
     * Relationships that exist on {@link MapShape map} shapes. They reference
     * {@link MemberShape member} shapes that define the key type for the map.
     */
    MAP_KEY,

    /**
     * Relationships that exist on {@link MapShape map} shapes. They
     * reference {@link MemberShape member} shapes that define the value type
     * for the map.
     */
    MAP_VALUE,

    /**
     * Relationships that exist on {@link StructureShape structure} shapes.
     * They reference {@link MemberShape member} shapes that define the
     * attributes of a structure.
     */
    STRUCTURE_MEMBER,

    /**
     * Relationships that exist on {@link UnionShape union}
     * shapes. They reference the {@link MemberShape member} shapes that define
     * the members of the union.
     */
    UNION_MEMBER;
}
